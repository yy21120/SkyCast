from __future__ import annotations

import os
from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException, Query, status
from fastapi.responses import JSONResponse

from app.domain.agent import AgentChatRequest, AgentChatResponse
from app.domain.feedback import SunsetFeedbackCreate, SunsetFeedbackResponse
from app.domain.models import City, OpportunitiesResponse
from app.providers.fixture import FixtureWeatherProvider
from app.providers.open_meteo import OpenMeteoWeatherProvider
from app.services.agent import AgentLanguageModel, DeepSeekLanguageModel, GroundedSunsetAgent
from app.services.feedback import (
    FeedbackConflictError,
    FeedbackRepository,
    SQLiteFeedbackRepository,
)
from app.services.opportunities import OpportunityService

APP_DIR = Path(__file__).resolve().parent
DEFAULT_REPLAY_FIXTURE = (
    APP_DIR.parent.parent / "data" / "sample" / "wuhan" / "sunset_replay_v1.json"
)
REPLAY_FIXTURE = Path(
    os.getenv("SKYCAST_REPLAY_FIXTURE", str(DEFAULT_REPLAY_FIXTURE))
).expanduser()

WUHAN = City(
    id="wuhan",
    name="武汉",
    latitude=30.5928,
    longitude=114.3055,
    timezone="Asia/Shanghai",
)
CITIES = {WUHAN.id: WUHAN}
DEFAULT_FEEDBACK_DATABASE = Path(
    os.getenv("SKYCAST_FEEDBACK_DATABASE", str(APP_DIR.parent / "var" / "skycast.db"))
).expanduser()


class Utf8JsonResponse(JSONResponse):
    media_type = "application/json; charset=utf-8"


def create_app(
    feedback_repository: FeedbackRepository | None = None,
    agent_language_model: AgentLanguageModel | None = None,
) -> FastAPI:
    repository = feedback_repository or SQLiteFeedbackRepository(DEFAULT_FEEDBACK_DATABASE)
    skycast_app = FastAPI(
        title="SkyCast API",
        version="0.2.0",
        default_response_class=Utf8JsonResponse,
        description=(
            "Explainable weather decision API. "
            "AI output is separated from official alerts."
        ),
    )

    @skycast_app.get("/health")
    def health() -> dict[str, str]:
        return {
            "status": "ok",
            "version": skycast_app.version,
            "agent_provider": (
                "deepseek" if os.getenv("DEEPSEEK_API_KEY", "").strip() else "rules"
            ),
        }

    @skycast_app.get("/ready")
    def ready() -> dict[str, str]:
        if not REPLAY_FIXTURE.is_file():
            raise HTTPException(status_code=503, detail="replay fixture is unavailable")
        try:
            DEFAULT_FEEDBACK_DATABASE.parent.mkdir(parents=True, exist_ok=True)
        except OSError as exc:
            raise HTTPException(status_code=503, detail="feedback storage is unavailable") from exc
        return {"status": "ready", "version": skycast_app.version}

    @skycast_app.get(
        "/v1/cities/{city_id}/opportunities",
        response_model=OpportunitiesResponse,
    )
    def list_opportunities(
        city_id: str,
        mode: Literal["replay", "live"] = Query(default="replay"),
        days: int = Query(default=3, ge=1, le=7),
    ) -> OpportunitiesResponse:
        city = CITIES.get(city_id.lower())
        if city is None:
            raise HTTPException(status_code=404, detail="city is not supported")

        provider = (
            FixtureWeatherProvider(REPLAY_FIXTURE)
            if mode == "replay"
            else OpenMeteoWeatherProvider()
        )
        try:
            return OpportunityService(provider, mode).list_sunset_opportunities(city, days)
        except (OSError, ValueError, KeyError, TypeError) as exc:
            raise HTTPException(
                status_code=502,
                detail=f"weather provider unavailable ({type(exc).__name__})",
            ) from exc

    @skycast_app.post(
        "/v1/agent/chat",
        response_model=AgentChatResponse,
    )
    def chat_with_sunset_agent(request: AgentChatRequest) -> AgentChatResponse:
        city = CITIES.get(request.city_id.lower())
        if city is None:
            raise HTTPException(status_code=404, detail="city is not supported")

        provider = (
            FixtureWeatherProvider(REPLAY_FIXTURE)
            if request.mode == "replay"
            else OpenMeteoWeatherProvider()
        )
        mode = request.mode
        try:
            opportunities = OpportunityService(provider, mode).list_sunset_opportunities(city, 3)
        except (OSError, ValueError, KeyError, TypeError) as exc:
            raise HTTPException(
                status_code=502,
                detail=f"weather provider unavailable ({type(exc).__name__})",
            ) from exc

        language_model = agent_language_model
        if language_model is None:
            language_model = DeepSeekLanguageModel.from_environment()
        return GroundedSunsetAgent(language_model).answer(request, opportunities)

    @skycast_app.post(
        "/v1/feedback/sunset",
        response_model=SunsetFeedbackResponse,
        status_code=status.HTTP_201_CREATED,
    )
    def submit_sunset_feedback(
        feedback: SunsetFeedbackCreate,
    ) -> SunsetFeedbackResponse:
        try:
            record, duplicate = repository.submit(feedback)
        except FeedbackConflictError as exc:
            raise HTTPException(status_code=409, detail=str(exc)) from exc
        return SunsetFeedbackResponse(duplicate=duplicate, feedback=record)

    return skycast_app


app = create_app()
