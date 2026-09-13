from datetime import UTC, date, datetime

from app.domain.models import City, WeatherSnapshot
from app.providers.live_weather import LiveWeatherProvider


class FakeProvider:
    def __init__(self, result=None, error: Exception | None = None) -> None:
        self.result = result
        self.error = error
        self.calls = 0

    def sunset_snapshots(self, city: City, days: int):
        self.calls += 1
        if self.error:
            raise self.error
        return self.result


def test_secondary_provider_is_used_when_primary_is_rate_limited() -> None:
    city = City(
        id="wuhan",
        name="武汉",
        latitude=30.5928,
        longitude=114.3055,
        timezone="Asia/Shanghai",
    )
    snapshot = WeatherSnapshot(
        valid_date=date(2026, 9, 13),
        sunset=datetime(2026, 9, 13, 10, 20, tzinfo=UTC),
        sampled_at=datetime(2026, 9, 13, 10, tzinfo=UTC),
        low_cloud_percent=10,
        mid_cloud_percent=30,
        high_cloud_percent=50,
        precipitation_probability_percent=0,
        visibility_meters=25000,
        relative_humidity_percent=60,
        source_id="met-no:locationforecast",
        source_url="https://api.met.no/example",
        retrieved_at=datetime(2026, 9, 13, 9, tzinfo=UTC),
    )
    primary = FakeProvider(error=OSError("rate limited"))
    secondary = FakeProvider(result=[snapshot])

    result = LiveWeatherProvider(primary, secondary).sunset_snapshots(city, 1)

    assert result == [snapshot]
    assert primary.calls == 1
    assert secondary.calls == 1


def test_successful_response_is_cached() -> None:
    city = City(
        id="wuhan",
        name="武汉",
        latitude=30.5928,
        longitude=114.3055,
        timezone="Asia/Shanghai",
    )
    primary = FakeProvider(result=[])
    provider = LiveWeatherProvider(
        primary=primary,
        secondary=FakeProvider(result=[]),
        cache_seconds=600,
        clock=lambda: 100.0,
    )

    assert provider.sunset_snapshots(city, 3) == []
    assert provider.sunset_snapshots(city, 3) == []
    assert primary.calls == 1
