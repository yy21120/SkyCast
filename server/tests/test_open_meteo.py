from datetime import UTC, datetime, timedelta, timezone

from app.providers.open_meteo import OpenMeteoWeatherProvider


def test_parse_payload_uses_hour_nearest_sunset() -> None:
    payload = {
        "utc_offset_seconds": 28800,
        "hourly": {
            "time": ["2026-08-25T18:00", "2026-08-25T19:00", "2026-08-25T20:00"],
            "relative_humidity_2m": [70, 72, 75],
            "precipitation_probability": [10, 20, 30],
            "cloud_cover_low": [20, 30, 40],
            "cloud_cover_mid": [30, 40, 50],
            "cloud_cover_high": [40, 50, 60],
            "visibility": [20000, 18000, 16000],
        },
        "daily": {
            "time": ["2026-08-25"],
            "sunset": ["2026-08-25T18:52"],
        },
    }

    result = OpenMeteoWeatherProvider.parse_payload(
        payload,
        "https://example.test/forecast",
        datetime(2026, 8, 25, tzinfo=UTC),
    )

    assert len(result) == 1
    assert result[0].sampled_at == datetime(
        2026, 8, 25, 19, 0, tzinfo=timezone(timedelta(hours=8))
    )
    assert result[0].low_cloud_percent == 30
    assert result[0].source_id == "open-meteo:forecast"
