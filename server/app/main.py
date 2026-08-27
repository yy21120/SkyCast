from __future__ import annotations

from pathlib import Path
from typing import Literal

from fastapi import FastAPI, HTTPException, Query, status

from app.domain.feedback import SunsetFeedbackCreate, SunsetFeedbackResponse
from app.domain.models import City, OpportunitiesResponse
from app.providers.fixture import FixtureWeatherProvider
from app.providers.open_meteo import OpenMeteoWeatherProvider
from app.services.feedback import (
    FeedbackConflictError,
    FeedbackRepository,
    SQLiteFeedbackRepository,
)
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
DEFAULT_FEEDBACK_DATABASE = APP_DIR.parent / "var" / "skycast.db"

def create_app(feedback_repository: FeedbackRepository | None = None) -> FastAPI:
    repository = feedback_repository or SQLiteFeedbackRepository(DEFAULT_FEEDBACK_DATABASE)
    skycast_app = FastAPI(
        title="SkyCast API",
        version="0.1.0",
        description=(
            "Explainable weather decision API. "
            "AI output is separated from official alerts."
        ),
    )

    @skycast_app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok", "version": skycast_app.version}

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
            raise HTTPException(status_code=502, detail="weather provider unavailable") from exc

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
