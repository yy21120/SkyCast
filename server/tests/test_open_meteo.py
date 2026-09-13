import io
import json
from datetime import UTC, datetime, timedelta, timezone
from urllib.error import HTTPError, URLError
from urllib.request import Request

from app.providers import open_meteo
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


def test_provider_retries_transient_network_failures(monkeypatch) -> None:
    calls = 0

    def fake_urlopen(request, timeout):
        nonlocal calls
        calls += 1
        assert timeout == 20
        if calls < 3:
            raise URLError("temporary failure")
        return io.BytesIO(json.dumps({"ok": True}).encode())

    monkeypatch.setattr(open_meteo, "urlopen", fake_urlopen)
    monkeypatch.setattr(open_meteo.time, "sleep", lambda _: None)

    provider = OpenMeteoWeatherProvider(timeout_seconds=20)
    assert provider._load_payload(Request("https://example.test")) == {"ok": True}
    assert calls == 3


def test_provider_honors_bounded_retry_after(monkeypatch) -> None:
    sleeps: list[float] = []
    calls = 0

    def fake_urlopen(request, timeout):
        nonlocal calls
        calls += 1
        if calls == 1:
            raise HTTPError(
                request.full_url,
                429,
                "rate limited",
                {"Retry-After": "30"},
                None,
            )
        return io.BytesIO(json.dumps({"ok": True}).encode())

    monkeypatch.setattr(open_meteo, "urlopen", fake_urlopen)
    monkeypatch.setattr(open_meteo.time, "sleep", sleeps.append)

    provider = OpenMeteoWeatherProvider(timeout_seconds=20)
    assert provider._load_payload(Request("https://example.test")) == {"ok": True}
    assert sleeps == [5.0]


def test_provider_timeout_is_bounded_from_environment(monkeypatch) -> None:
    monkeypatch.setenv("OPEN_METEO_TIMEOUT_SECONDS", "120")
    assert OpenMeteoWeatherProvider()._timeout_seconds == 45

    monkeypatch.setenv("OPEN_METEO_TIMEOUT_SECONDS", "invalid")
    assert OpenMeteoWeatherProvider()._timeout_seconds == 20
