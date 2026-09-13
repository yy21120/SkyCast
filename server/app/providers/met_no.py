from __future__ import annotations

import json
import math
from datetime import UTC, date, datetime, timedelta, timezone, tzinfo
from typing import Any
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from zoneinfo import ZoneInfo

from app.domain.models import City, WeatherSnapshot

MET_NO_URL = "https://api.met.no/weatherapi/locationforecast/2.0/complete"
SOURCE_ID = "met-no:locationforecast"
USER_AGENT = "SkyCast/0.2 (+https://github.com/yy21120/SkyCast)"


class MetNoWeatherProvider:
    """Secondary live forecast used when the primary public API is rate limited."""

    def __init__(self, base_url: str = MET_NO_URL, timeout_seconds: float = 30.0) -> None:
        self._base_url = base_url
        self._timeout_seconds = timeout_seconds

    def sunset_snapshots(self, city: City, days: int) -> list[WeatherSnapshot]:
        query = urlencode(
            {
                "lat": round(city.latitude, 4),
                "lon": round(city.longitude, 4),
            }
        )
        source_url = f"{self._base_url}?{query}"
        request = Request(
            source_url,
            headers={"Accept": "application/json", "User-Agent": USER_AGENT},
        )
        with urlopen(request, timeout=self._timeout_seconds) as response:
            payload = json.load(response)
        return self.parse_payload(payload, source_url, city, days, datetime.now(UTC))

    @staticmethod
    def parse_payload(
        payload: dict[str, Any],
        source_url: str,
        city: City,
        days: int,
        retrieved_at: datetime,
    ) -> list[WeatherSnapshot]:
        zone = city_zone(city.timezone)
        local_start = retrieved_at.astimezone(zone).date()
        series = payload["properties"]["timeseries"]
        samples = [
            (datetime.fromisoformat(item["time"]), item["data"])
            for item in series
        ]
        if not samples:
            raise ValueError("MET Norway returned no forecast samples")

        snapshots: list[WeatherSnapshot] = []
        for offset in range(days):
            valid_date = local_start + timedelta(days=offset)
            sunset = calculate_sunset(valid_date, city.latitude, city.longitude, zone)
            sampled_at, data = min(
                samples,
                key=lambda sample: abs((sample[0] - sunset).total_seconds()),
            )
            details = data["instant"]["details"]
            next_hour = data.get("next_1_hours", {}).get("details", {})
            precipitation_probability = next_hour.get("probability_of_precipitation")
            if precipitation_probability is None:
                precipitation_probability = precipitation_amount_probability(
                    float(next_hour.get("precipitation_amount", 0.0))
                )

            humidity = float(details["relative_humidity"])
            fog = float(details.get("fog_area_fraction", 0.0))
            visibility = float(
                details.get("visibility", estimate_visibility_meters(humidity, fog))
            )
            total_cloud = float(details.get("cloud_area_fraction", 0.0))

            snapshots.append(
                WeatherSnapshot(
                    valid_date=valid_date,
                    sunset=sunset,
                    sampled_at=sampled_at,
                    low_cloud_percent=float(
                        details.get("cloud_area_fraction_low", total_cloud)
                    ),
                    mid_cloud_percent=float(
                        details.get("cloud_area_fraction_medium", total_cloud)
                    ),
                    high_cloud_percent=float(
                        details.get("cloud_area_fraction_high", total_cloud)
                    ),
                    precipitation_probability_percent=float(precipitation_probability),
                    visibility_meters=visibility,
                    relative_humidity_percent=humidity,
                    source_id=SOURCE_ID,
                    source_url=source_url,
                    retrieved_at=retrieved_at,
                )
            )
        return snapshots


def precipitation_amount_probability(amount_mm: float) -> float:
    if amount_mm <= 0:
        return 0.0
    if amount_mm < 0.2:
        return 20.0
    if amount_mm < 1.0:
        return 50.0
    return 80.0


def estimate_visibility_meters(humidity_percent: float, fog_percent: float) -> float:
    humidity_factor = max(0.3, min(1.0, (110.0 - humidity_percent) / 35.0))
    fog_factor = max(0.15, 1.0 - fog_percent / 100.0)
    return round(25_000.0 * humidity_factor * fog_factor)


def calculate_sunset(
    valid_date: date,
    latitude: float,
    longitude: float,
    zone: tzinfo,
) -> datetime:
    """Calculate civil sunset with the NOAA sunrise/sunset approximation."""

    day_of_year = valid_date.timetuple().tm_yday
    longitude_hour = longitude / 15.0
    approximate_time = day_of_year + (18.0 - longitude_hour) / 24.0
    mean_anomaly = 0.9856 * approximate_time - 3.289
    true_longitude = (
        mean_anomaly
        + 1.916 * math.sin(math.radians(mean_anomaly))
        + 0.020 * math.sin(math.radians(2.0 * mean_anomaly))
        + 282.634
    ) % 360.0
    right_ascension = math.degrees(
        math.atan(0.91764 * math.tan(math.radians(true_longitude)))
    ) % 360.0
    longitude_quadrant = math.floor(true_longitude / 90.0) * 90.0
    ascension_quadrant = math.floor(right_ascension / 90.0) * 90.0
    right_ascension = (right_ascension + longitude_quadrant - ascension_quadrant) / 15.0
    sin_declination = 0.39782 * math.sin(math.radians(true_longitude))
    cos_declination = math.cos(math.asin(sin_declination))
    cos_hour_angle = (
        math.cos(math.radians(90.833))
        - sin_declination * math.sin(math.radians(latitude))
    ) / (cos_declination * math.cos(math.radians(latitude)))
    if not -1.0 <= cos_hour_angle <= 1.0:
        raise ValueError("sunset is unavailable for this date and latitude")
    hour_angle = math.degrees(math.acos(cos_hour_angle)) / 15.0
    local_mean_time = hour_angle + right_ascension - 0.06571 * approximate_time - 6.622
    utc_hours = (local_mean_time - longitude_hour) % 24.0
    sunset_utc = datetime.combine(valid_date, datetime.min.time(), tzinfo=UTC) + timedelta(
        hours=utc_hours
    )
    return sunset_utc.astimezone(zone)


def city_zone(name: str) -> tzinfo:
    # Wuhan does not observe daylight saving time; this also keeps Windows test
    # environments working when the optional IANA tzdata package is absent.
    if name == "Asia/Shanghai":
        return timezone(timedelta(hours=8))
    return ZoneInfo(name)
