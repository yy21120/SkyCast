from __future__ import annotations

from typing import Protocol

from app.domain.models import City, WeatherSnapshot


class WeatherProvider(Protocol):
    def sunset_snapshots(self, city: City, days: int) -> list[WeatherSnapshot]: ...
