import asyncio

from httpx import ASGITransport, AsyncClient, Response

from app.domain.agent import AgentChatMessage
from app.main import create_app
from app.services.agent import DeepSeekLanguageModel


class StubLanguageModel:
    provider_name = "stub"
    model_name = "stub-chat"

    def complete(self, system_prompt: str, messages: list[AgentChatMessage]) -> str:
        assert "FACTS=" in system_prompt
        assert "score" in system_prompt
        assert messages[-1].content == "今天值得去哪里拍？"
        return "建议去东湖凌波门，并在染色窗口前到达。"


def test_deepseek_uses_current_flash_model_by_default(monkeypatch) -> None:
    monkeypatch.setenv("DEEPSEEK_API_KEY", "test-key")
    monkeypatch.delenv("DEEPSEEK_MODEL", raising=False)

    model = DeepSeekLanguageModel.from_environment()

    assert model is not None
    assert model.model_name == "deepseek-flash"


def post(payload: dict, language_model: StubLanguageModel | None = None) -> Response:
    async def request() -> Response:
        transport = ASGITransport(app=create_app(agent_language_model=language_model))
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            return await client.post("/v1/agent/chat", json=payload)

    return asyncio.run(request())


def test_agent_has_grounded_fallback_without_api_key(monkeypatch) -> None:
    monkeypatch.delenv("DEEPSEEK_API_KEY", raising=False)

    response = post({"message": "今天晚霞怎么样？", "mode": "replay"})

    assert response.status_code == 200
    assert response.headers["content-type"] == "application/json; charset=utf-8"
    assert "这次晚霞机会" in response.content.decode("utf-8")
    payload = response.json()
    assert payload["score"] == 93
    assert "93/100" in payload["reply"]
    assert payload["provider"] == "skycast-rules"
    assert payload["fallback"] is True
    assert payload["used_tools"] == ["get_sunset_assessment"]


def test_agent_can_use_language_model_without_letting_it_set_facts() -> None:
    response = post(
        {"message": "今天值得去哪里拍？", "mode": "replay"},
        language_model=StubLanguageModel(),
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["reply"].startswith("这次晚霞机会评分为 93/100")
    assert payload["reply"].endswith("建议去东湖凌波门，并在染色窗口前到达。")
    assert payload["score"] == 93
    assert payload["provider"] == "stub"
    assert payload["fallback"] is False
    assert payload["used_tools"] == ["get_sunset_assessment", "list_shooting_spots"]
    assert len(payload["recommended_spots"]) == 3


def test_selected_spot_is_ranked_first() -> None:
    response = post(
        {
            "message": "这个地点怎么拍？",
            "mode": "replay",
            "selected_spot_id": "shahu-park",
        }
    )

    assert response.status_code == 200
    assert response.json()["recommended_spots"][0]["id"] == "shahu-park"


def test_numeric_language_model_output_is_rejected() -> None:
    class UnsafeLanguageModel(StubLanguageModel):
        def complete(self, system_prompt: str, messages: list[AgentChatMessage]) -> str:
            return "模型擅自建议在 18:00 到达。"

    response = post(
        {"message": "今天怎么拍？", "mode": "replay"},
        language_model=UnsafeLanguageModel(),
    )

    assert response.status_code == 200
    assert response.json()["provider"] == "skycast-rules"
    assert response.json()["fallback"] is True


def test_agent_validates_city_and_message() -> None:
    unknown_city = post({"city_id": "beijing", "message": "晚霞如何", "mode": "replay"})
    empty_message = post({"message": "", "mode": "replay"})

    assert unknown_city.status_code == 404
    assert empty_message.status_code == 422
