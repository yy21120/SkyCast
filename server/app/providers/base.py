from __future__ import annotations

from typing import Protocol

from app.domain.models import City, WeatherSnapshot
from app.domain.radar import RadarEvent, RadarFrame, RadarFrameData


class WeatherProvider(Protocol):
    def sunset_snapshots(self, city: City, days: int) -> list[WeatherSnapshot]: ...


class RadarProvider(Protocol):
    def event(self, event_id: str) -> RadarEvent: ...

    def frames(self, event_id: str) -> list[RadarFrame]: ...

    def frame_data(self, event_id: str, frame_id: str) -> RadarFrameData: ...
