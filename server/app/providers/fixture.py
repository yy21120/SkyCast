from __future__ import annotations

import json
from pathlib import Path

from app.domain.models import City, WeatherSnapshot


class FixtureWeatherProvider:
    def __init__(self, fixture_path: Path) -> None:
        self._fixture_path = fixture_path

    def sunset_snapshots(self, city: City, days: int) -> list[WeatherSnapshot]:
        payload = json.loads(self._fixture_path.read_text(encoding="utf-8"))
        if payload["city"]["id"] != city.id:
            raise ValueError(f"fixture does not contain city {city.id}")
        return [WeatherSnapshot.model_validate(item) for item in payload["snapshots"][:days]]
