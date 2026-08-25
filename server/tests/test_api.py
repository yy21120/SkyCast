import asyncio

from httpx import ASGITransport, AsyncClient, Response

from app.main import app


def get(path: str) -> Response:
    async def request() -> Response:
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            return await client.get(path)

    return asyncio.run(request())


def test_health() -> None:
    response = get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "version": "0.1.0"}


def test_wuhan_replay_opportunities_are_rankable_and_traceable() -> None:
    response = get("/v1/cities/wuhan/opportunities?mode=replay&days=3")

    assert response.status_code == 200
    payload = response.json()
    assert payload["city"]["name"] == "武汉"
    assert payload["mode"] == "replay"
    assert len(payload["opportunities"]) == 3
    assert payload["opportunities"][0]["score"] > payload["opportunities"][2]["score"]
    assert payload["opportunities"][0]["sources"][0]["source_id"].startswith("skycast:")


def test_unknown_city_returns_404() -> None:
    response = get("/v1/cities/beijing/opportunities")

    assert response.status_code == 404


def test_days_are_validated() -> None:
    response = get("/v1/cities/wuhan/opportunities?days=0")

    assert response.status_code == 422
