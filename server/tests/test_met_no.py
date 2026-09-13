from datetime import UTC, datetime, timedelta, timezone

import pytest

from app.domain.models import City
from app.providers.met_no import (
    SOURCE_ID,
    MetNoWeatherProvider,
    calculate_sunset,
    estimate_visibility_meters,
    precipitation_amount_probability,
)

WUHAN = City(
    id="wuhan",
    name="武汉",
    latitude=30.5928,
    longitude=114.3055,
    timezone="Asia/Shanghai",
)


def test_parse_payload_selects_nearest_sunset_sample() -> None:
    payload = {
        "properties": {
            "timeseries": [
                {
                    "time": "2026-09-13T10:00:00Z",
                    "data": {
                        "instant": {
                            "details": {
                                "cloud_area_fraction": 45,
                                "cloud_area_fraction_low": 10,
                                "cloud_area_fraction_medium": 35,
                                "cloud_area_fraction_high": 55,
                                "fog_area_fraction": 0,
                                "relative_humidity": 62,
                            }
                        },
                        "next_1_hours": {"details": {"precipitation_amount": 0.1}},
                    },
                }
            ]
        }
    }

    result = MetNoWeatherProvider.parse_payload(
        payload,
        "https://api.met.no/example",
        WUHAN,
        1,
        datetime(2026, 9, 13, 2, tzinfo=UTC),
    )

    assert len(result) == 1
    assert result[0].source_id == SOURCE_ID
    assert result[0].low_cloud_percent == 10
    assert result[0].precipitation_probability_percent == 20
    assert result[0].visibility_meters == 25000
    assert result[0].sunset.hour == 18


@pytest.mark.parametrize(
    ("amount", "probability"),
    [(0, 0), (0.1, 20), (0.5, 50), (2, 80)],
)
def test_precipitation_amount_probability(amount: float, probability: float) -> None:
    assert precipitation_amount_probability(amount) == probability


def test_visibility_estimate_is_bounded_by_weather_conditions() -> None:
    assert estimate_visibility_meters(50, 0) == 25000
    assert estimate_visibility_meters(100, 100) == 1125


def test_wuhan_september_sunset_is_in_expected_local_hour() -> None:
    sunset = calculate_sunset(
        datetime(2026, 9, 13, tzinfo=UTC).date(),
        WUHAN.latitude,
        WUHAN.longitude,
        timezone(timedelta(hours=8)),
    )

    assert sunset.hour == 18
