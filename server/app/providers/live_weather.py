from __future__ import annotations

import time
from collections.abc import Callable
from threading import Lock

from app.domain.models import City, WeatherSnapshot
from app.providers.base import WeatherProvider
from app.providers.met_no import MetNoWeatherProvider
from app.providers.open_meteo import OpenMeteoWeatherProvider


class LiveWeatherProvider:
    def __init__(
        self,
        primary: WeatherProvider | None = None,
        secondary: WeatherProvider | None = None,
        cache_seconds: float = 600.0,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self._primary = primary or OpenMeteoWeatherProvider()
        self._secondary = secondary or MetNoWeatherProvider()
        self._cache_seconds = cache_seconds
        self._clock = clock
        self._cache: dict[tuple[str, int], tuple[float, list[WeatherSnapshot]]] = {}
        self._cache_lock = Lock()

    def sunset_snapshots(self, city: City, days: int) -> list[WeatherSnapshot]:
        cache_key = (city.id, days)
        now = self._clock()
        with self._cache_lock:
            cached = self._cache.get(cache_key)
        if cached is not None and now - cached[0] < self._cache_seconds:
            return cached[1]

        try:
            snapshots = self._primary.sunset_snapshots(city, days)
        except (OSError, ValueError, KeyError, TypeError):
            snapshots = self._secondary.sunset_snapshots(city, days)
        with self._cache_lock:
            self._cache[cache_key] = (now, snapshots)
        return snapshots
