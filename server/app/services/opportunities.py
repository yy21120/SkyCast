from __future__ import annotations

from datetime import UTC, datetime

from app.domain.models import City, OpportunitiesResponse
from app.domain.scoring import assess_sunset
from app.providers.base import WeatherProvider


class OpportunityService:
    def __init__(self, provider: WeatherProvider, mode: str) -> None:
        self._provider = provider
        self._mode = mode

    def list_sunset_opportunities(self, city: City, days: int) -> OpportunitiesResponse:
        snapshots = self._provider.sunset_snapshots(city, days)
        return OpportunitiesResponse(
            city=city,
            mode=self._mode,
            generated_at=datetime.now(UTC),
            opportunities=[assess_sunset(snapshot) for snapshot in snapshots],
        )
