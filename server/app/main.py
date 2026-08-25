from __future__ import annotations

from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException, Query

from app.domain.models import City, OpportunitiesResponse
from app.providers.fixture import FixtureWeatherProvider
from app.providers.open_meteo import OpenMeteoWeatherProvider
from app.services.opportunities import OpportunityService

APP_DIR = Path(__file__).resolve().parent
REPLAY_FIXTURE = (
    APP_DIR.parent.parent / "data" / "sample" / "wuhan" / "sunset_replay_v1.json"
)

WUHAN = City(
    id="wuhan",
    name="武汉",
    latitude=30.5928,
    longitude=114.3055,
    timezone="Asia/Shanghai",
)
CITIES = {WUHAN.id: WUHAN}

app = FastAPI(
    title="SkyCast API",
    version="0.1.0",
    description="Explainable weather decision API. AI output is separated from official alerts.",
)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "version": app.version}


@app.get(
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
        raise HTTPException(status_code=502, detail="weather provider unavailable") from exc
