from __future__ import annotations

import json
from datetime import UTC, datetime, timedelta, timezone
from typing import Any
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from app.domain.models import City, WeatherSnapshot

OPEN_METEO_URL = "https://api.open-meteo.com/v1/forecast"
HOURLY_FIELDS = (
    "relative_humidity_2m",
    "precipitation_probability",
    "cloud_cover_low",
    "cloud_cover_mid",
    "cloud_cover_high",
    "visibility",
)

SOURCE_ID = "open-meteo:forecast"


class OpenMeteoWeatherProvider:
    def __init__(self, base_url: str = OPEN_METEO_URL, timeout_seconds: float = 8.0) -> None:
        self._base_url = base_url
        self._timeout_seconds = timeout_seconds

    def sunset_snapshots(self, city: City, days: int) -> list[WeatherSnapshot]:
        query = urlencode(
            {
                "latitude": city.latitude,
                "longitude": city.longitude,
                "timezone": city.timezone,
                "forecast_days": days,
                "hourly": ",".join(HOURLY_FIELDS),
                "daily": "sunset",
            }
        )
        source_url = f"{self._base_url}?{query}"
        request = Request(source_url, headers={"User-Agent": "SkyCast/0.1"})
        with urlopen(request, timeout=self._timeout_seconds) as response:
            payload = json.load(response)
        return self.parse_payload(payload, source_url, datetime.now(UTC))

    @staticmethod
    def parse_payload(
        payload: dict[str, Any], source_url: str, retrieved_at: datetime
    ) -> list[WeatherSnapshot]:
        hourly = payload["hourly"]
        local_timezone = timezone(timedelta(seconds=payload.get("utc_offset_seconds", 0)))
        hourly_times = [
            datetime.fromisoformat(value).replace(tzinfo=local_timezone)
            for value in hourly["time"]
        ]
        snapshots: list[WeatherSnapshot] = []

        for date_text, sunset_text in zip(
            payload["daily"]["time"], payload["daily"]["sunset"], strict=True
        ):
            sunset = datetime.fromisoformat(sunset_text).replace(tzinfo=local_timezone)
            nearest_index = min(
                range(len(hourly_times)),
                key=lambda index: abs((hourly_times[index] - sunset).total_seconds()),
            )
            snapshots.append(
                WeatherSnapshot(
                    valid_date=date_text,
                    sunset=sunset,
                    sampled_at=hourly_times[nearest_index],
                    low_cloud_percent=hourly["cloud_cover_low"][nearest_index],
                    mid_cloud_percent=hourly["cloud_cover_mid"][nearest_index],
                    high_cloud_percent=hourly["cloud_cover_high"][nearest_index],
                    precipitation_probability_percent=hourly[
                        "precipitation_probability"
                    ][nearest_index],
                    visibility_meters=hourly["visibility"][nearest_index],
                    relative_humidity_percent=hourly["relative_humidity_2m"][nearest_index],
                    source_id=SOURCE_ID,
                    source_url=source_url,
                    retrieved_at=retrieved_at,
                )
            )
        return snapshots
