from datetime import datetime

from app.domain.models import WeatherSnapshot
from app.domain.scoring import assess_sunset


def snapshot(**overrides: float) -> WeatherSnapshot:
    values = {
        "valid_date": "2026-08-25",
        "sunset": datetime.fromisoformat("2026-08-25T18:55:00+08:00"),
        "sampled_at": datetime.fromisoformat("2026-08-25T19:00:00+08:00"),
        "low_cloud_percent": 20,
        "mid_cloud_percent": 40,
        "high_cloud_percent": 55,
        "precipitation_probability_percent": 5,
        "visibility_meters": 25000,
        "relative_humidity_percent": 65,
        "source_id": "test:fixture",
        "retrieved_at": datetime.fromisoformat("2026-08-25T10:00:00+00:00"),
    }
    values.update(overrides)
    return WeatherSnapshot.model_validate(values)


def test_favorable_conditions_score_higher_than_poor_conditions() -> None:
    favorable = assess_sunset(snapshot())
    poor = assess_sunset(
        snapshot(
            low_cloud_percent=95,
            mid_cloud_percent=95,
            high_cloud_percent=0,
            precipitation_probability_percent=90,
            visibility_meters=3000,
            relative_humidity_percent=95,
        )
    )

    assert favorable.score >= 75
    assert favorable.recommendation == "go"
    assert poor.score < 40
    assert poor.recommendation == "skip"


def test_assessment_contract_is_bounded_and_explainable() -> None:
    result = assess_sunset(snapshot())

    assert 0 <= result.score <= 100
    assert 0 <= result.baseline_probability <= 1
    assert result.probability_status == "uncalibrated_baseline"
    assert len(result.factors) == 6
    assert result.sources[0].source_id == "test:fixture"
    assert result.model_version == "sunset-rules-wuhan-v0.1.0"
