from __future__ import annotations

import json
import logging
import os
import time
from datetime import UTC, datetime, timedelta
from typing import Protocol
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

from app.domain.agent import (
    AgentChatMessage,
    AgentChatRequest,
    AgentChatResponse,
    ShootingSpot,
)
from app.domain.models import OpportunitiesResponse, SunsetOpportunity

LOGGER = logging.getLogger(__name__)

WUHAN_SHOOTING_SPOTS = [
    ShootingSpot(
        id="east-lake-lingbo-gate",
        name="东湖凌波门",
        latitude=30.5436,
        longitude=114.3661,
        direction="向西拍摄湖面与城市天际线",
        description="水面开阔，适合拍摄晚霞倒影；周末建议提前到达。",
    ),
    ShootingSpot(
        id="hankou-river-beach",
        name="汉口江滩",
        latitude=30.5915,
        longitude=114.3008,
        direction="沿江向西南取景",
        description="江面和桥梁元素丰富，注意汛期与临江安全提示。",
    ),
    ShootingSpot(
        id="shahu-park",
        name="沙湖公园",
        latitude=30.5718,
        longitude=114.3387,
        direction="向西拍摄湖面与建筑剪影",
        description="交通相对便利，适合快速到达和轻量化拍摄。",
    ),
]

SAFETY_NOTICE = "出发前请复核官方天气预警和现场开放情况，勿在雷雨、临水危险区域冒险拍摄。"


class AgentLanguageModel(Protocol):
    provider_name: str
    model_name: str

    def complete(self, system_prompt: str, messages: list[AgentChatMessage]) -> str: ...


class DeepSeekLanguageModel:
    provider_name = "deepseek"

    def __init__(
        self,
        api_key: str,
        model_name: str = "deepseek-flash",
        base_url: str = "https://api.deepseek.com",
        timeout_seconds: float = 12.0,
    ) -> None:
        self._api_key = api_key
        self.model_name = model_name
        self._base_url = base_url.rstrip("/")
        self._timeout_seconds = timeout_seconds

    @classmethod
    def from_environment(cls) -> DeepSeekLanguageModel | None:
        api_key = os.getenv("DEEPSEEK_API_KEY", "").strip()
        if not api_key:
            return None
        try:
            timeout_seconds = float(os.getenv("DEEPSEEK_TIMEOUT_SECONDS", "12"))
        except ValueError:
            timeout_seconds = 12.0
        return cls(
            api_key=api_key,
            model_name=os.getenv("DEEPSEEK_MODEL", "deepseek-flash").strip()
            or "deepseek-flash",
            base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com").strip(),
            timeout_seconds=min(max(timeout_seconds, 3.0), 30.0),
        )

    def complete(self, system_prompt: str, messages: list[AgentChatMessage]) -> str:
        payload = {
            "model": self.model_name,
            "messages": [
                {"role": "system", "content": system_prompt},
                *[message.model_dump() for message in messages],
            ],
            "thinking": {"type": "disabled"},
            "max_tokens": 500,
            "stream": False,
        }
        request = Request(
            f"{self._base_url}/chat/completions",
            data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
            headers={
                "Authorization": f"Bearer {self._api_key}",
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            method="POST",
        )
        body = self._send(request)
        content = body["choices"][0]["message"]["content"].strip()
        if not content:
            raise ValueError("DeepSeek returned an empty response")
        return content

    def _send(self, request: Request) -> dict:
        transient_statuses = {429, 500, 502, 503, 504}
        for attempt in range(2):
            try:
                with urlopen(request, timeout=self._timeout_seconds) as response:
                    return json.loads(response.read().decode("utf-8"))
            except HTTPError as exc:
                if exc.code not in transient_statuses or attempt == 1:
                    raise OSError(f"DeepSeek returned HTTP {exc.code}") from exc
            except (URLError, TimeoutError) as exc:
                if attempt == 1:
                    raise OSError("DeepSeek request failed") from exc
            time.sleep(0.25)
        raise OSError("DeepSeek request failed")


class GroundedSunsetAgent:
    def __init__(self, language_model: AgentLanguageModel | None = None) -> None:
        self._language_model = language_model

    def answer(
        self,
        request: AgentChatRequest,
        opportunities: OpportunitiesResponse,
    ) -> AgentChatResponse:
        opportunity = self._select_opportunity(request.message, opportunities)
        spots = self._rank_spots(request.selected_spot_id)
        tools = ["get_sunset_assessment"]
        if self._asks_about_spot(request.message) or request.selected_spot_id:
            tools.append("list_shooting_spots")

        fallback = self._language_model is None
        if self._language_model is None:
            reply = self._fallback_reply(request.message, opportunity, spots)
            provider = "skycast-rules"
            model = "grounded-agent-v0.2.0"
        else:
            try:
                prompt = self._system_prompt(opportunities, opportunity, spots)
                messages = [*request.history[-8:], AgentChatMessage(role="user", content=request.message)]
                advice = self._language_model.complete(prompt, messages)
                self._validate_language_model_advice(advice)
                reply = f"{self._grounded_summary(opportunity)} {advice}"
                provider = self._language_model.provider_name
                model = self._language_model.model_name
            except (OSError, TimeoutError, ValueError, KeyError, TypeError, IndexError) as exc:
                LOGGER.warning(
                    "DeepSeek request failed; using deterministic fallback (%s: %s)",
                    type(exc).__name__,
                    exc,
                )
                reply = self._fallback_reply(request.message, opportunity, spots)
                provider = "skycast-rules"
                model = "grounded-agent-v0.2.0"
                fallback = True

        return AgentChatResponse(
            reply=reply,
            scene_id=opportunity.scene_id,
            score=opportunity.score,
            confidence=opportunity.confidence,
            recommended_spots=spots,
            used_tools=tools,
            provider=provider,
            model=model,
            generated_at=datetime.now(UTC),
            safety_notice=SAFETY_NOTICE,
            fallback=fallback,
        )

    @staticmethod
    def _select_opportunity(
        message: str,
        opportunities: OpportunitiesResponse,
    ) -> SunsetOpportunity:
        if not opportunities.opportunities:
            raise ValueError("no sunset opportunity is available")
        if any(word in message for word in ("最高", "最好", "最值得", "哪天")):
            return max(opportunities.opportunities, key=lambda item: item.score)
        return opportunities.opportunities[0]

    @staticmethod
    def _rank_spots(selected_spot_id: str | None) -> list[ShootingSpot]:
        if selected_spot_id is None:
            return WUHAN_SHOOTING_SPOTS
        return sorted(
            WUHAN_SHOOTING_SPOTS,
            key=lambda spot: spot.id != selected_spot_id,
        )

    @staticmethod
    def _asks_about_spot(message: str) -> bool:
        return any(word in message for word in ("哪里", "地点", "机位", "去哪", "地图", "拍摄点"))

    @staticmethod
    def _fallback_reply(
        message: str,
        opportunity: SunsetOpportunity,
        spots: list[ShootingSpot],
    ) -> str:
        base = GroundedSunsetAgent._grounded_summary(opportunity)
        if any(word in message for word in ("器材", "镜头", "相机", "装备")):
            return (
                f"{base} 建议携带广角镜头、备用电池和小型三脚架；"
                "优先保住高光，日落后继续等待约 15 分钟。"
            )
        if any(word in message for word in ("哪里", "地点", "机位", "去哪", "地图", "拍摄点")):
            spot = spots[0]
            return f"{base} 首选{spot.name}，{spot.direction}。{spot.description}"
        if any(word in message for word in ("下雨", "降水", "云", "天气")):
            factors = "；".join(factor.explanation for factor in opportunity.factors[:3])
            return f"{base} 主要依据是：{factors}"
        return f"{base} {opportunity.summary} 你还可以问我拍摄地点、器材或云量影响。"

    @staticmethod
    def _grounded_summary(opportunity: SunsetOpportunity) -> str:
        window_start = opportunity.coloring_window_start.strftime("%H:%M")
        window_end = opportunity.coloring_window_end.strftime("%H:%M")
        arrival = (opportunity.coloring_window_start - timedelta(minutes=40)).strftime("%H:%M")
        recommendation = {"go": "值得出发", "watch": "建议持续关注", "skip": "暂不建议专程前往"}[
            opportunity.recommendation
        ]
        return (
            f"这次晚霞机会评分为 {opportunity.score}/100，{recommendation}。"
            f"预计染色窗口 {window_start}–{window_end}，建议最晚 {arrival} 到达。"
        )

    @staticmethod
    def _validate_language_model_advice(advice: str) -> None:
        if len(advice) > 500:
            raise ValueError("language model advice is too long")
        if any(character.isdigit() for character in advice) or "%" in advice:
            raise ValueError("language model advice must not contain numeric claims")
        if "官方预警" in advice or "安全等级" in advice:
            raise ValueError("language model advice crossed the safety boundary")

    @staticmethod
    def _system_prompt(
        opportunities: OpportunitiesResponse,
        selected: SunsetOpportunity,
        spots: list[ShootingSpot],
    ) -> str:
        facts = {
            "city": opportunities.city.model_dump(mode="json"),
            "selected_assessment": selected.model_dump(mode="json"),
            "shooting_spots": [spot.model_dump(mode="json") for spot in spots],
            "generated_at": opportunities.generated_at.isoformat(),
        }
        return (
            "你是 SkyCast 晚霞摄影 Agent。只依据下方经过校验的 JSON 事实回答，"
            "不得修改概率、时间、坐标、置信度或安全结论，不得声称这是官方天气预报。"
            "程序会单独展示评分、概率和时间。你只补充两句简洁的定性摄影建议，"
            "不得输出任何阿拉伯数字、百分号、具体时刻、坐标、官方预警或安全等级；"
            "如果事实不足，明确说不知道。"
            f"始终提醒必要的现场安全复核。\nFACTS={json.dumps(facts, ensure_ascii=False)}"
        )
