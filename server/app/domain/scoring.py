from __future__ import annotations

from datetime import timedelta

from app.domain.models import (
    AssessmentFactor,
    SourceReference,
    SunsetOpportunity,
    WeatherSnapshot,
)

MODEL_VERSION = "sunset-rules-wuhan-v0.1.0"


def _clamp(value: float, lower: float = 0.0, upper: float = 1.0) -> float:
    return max(lower, min(upper, value))


def _triangle(value: float, center: float, half_width: float) -> float:
    return _clamp(1.0 - abs(value - center) / half_width)


def _effect(ratio: float) -> str:
    if ratio >= 0.7:
        return "favorable"
    if ratio >= 0.4:
        return "neutral"
    return "limiting"


def assess_sunset(snapshot: WeatherSnapshot) -> SunsetOpportunity:
    """Return an explainable rule baseline, not a calibrated weather forecast."""

    rain_ratio = _clamp(1.0 - snapshot.precipitation_probability_percent / 100.0)
    low_cloud_ratio = _clamp(1.0 - snapshot.low_cloud_percent / 100.0)
    high_cloud_ratio = _triangle(snapshot.high_cloud_percent, center=55.0, half_width=50.0)
    mid_cloud_ratio = _triangle(snapshot.mid_cloud_percent, center=40.0, half_width=55.0)
    visibility_ratio = _clamp(snapshot.visibility_meters / 25_000.0)
    humidity_ratio = _clamp((95.0 - snapshot.relative_humidity_percent) / 35.0)

    weighted = [
        ("precipitation_probability", "降水概率", snapshot.precipitation_probability_percent,
         "%", 25.0, rain_ratio, "降水概率越低，拍摄窗口越稳定。"),
        ("low_cloud", "低云量", snapshot.low_cloud_percent, "%", 25.0,
         low_cloud_ratio, "西侧低云过多时容易遮挡近地平线阳光。"),
        ("high_cloud", "高云量", snapshot.high_cloud_percent, "%", 20.0,
         high_cloud_ratio, "适量高云有利于形成大范围染色层。"),
        ("mid_cloud", "中云量", snapshot.mid_cloud_percent, "%", 10.0,
         mid_cloud_ratio, "适量中云可增加层次，过多则会遮光。"),
        ("visibility", "能见度", snapshot.visibility_meters, "m", 15.0,
         visibility_ratio, "较高能见度通常意味着更清晰的远景与色彩。"),
        ("humidity", "相对湿度", snapshot.relative_humidity_percent, "%", 5.0,
         humidity_ratio, "近地面湿度过高可能伴随雾霾或低云。"),
    ]

    factors: list[AssessmentFactor] = []
    score_value = 0.0
    for code, label, value, unit, weight, ratio, explanation in weighted:
        contribution = round(weight * ratio, 2)
        score_value += contribution
        factors.append(
            AssessmentFactor(
                code=code,
                label=label,
                value=value,
                unit=unit,
                contribution=contribution,
                effect=_effect(ratio),
                explanation=explanation,
            )
        )

    score = round(score_value)
    if score >= 75:
        recommendation = "go"
        summary = "晚霞条件较有利，建议在染色窗口开始前到达机位。"
    elif score >= 55:
        recommendation = "watch"
        summary = "存在拍摄机会，但云量或降水仍有不确定性，建议临近时复核。"
    else:
        recommendation = "skip"
        summary = "当前基线条件不理想，不建议仅为晚霞专程出发。"

    confidence = "medium" if snapshot.source_id.startswith("open-meteo") else "low"
    scene_id = f"wuhan-sunset-{snapshot.valid_date.isoformat()}"

    return SunsetOpportunity(
        scene_id=scene_id,
        date=snapshot.valid_date,
        sunset=snapshot.sunset,
        coloring_window_start=snapshot.sunset - timedelta(minutes=25),
        coloring_window_end=snapshot.sunset + timedelta(minutes=25),
        score=score,
        baseline_probability=round(score / 100.0, 2),
        confidence=confidence,
        recommendation=recommendation,
        summary=summary,
        factors=factors,
        sources=[
            SourceReference(
                source_id=snapshot.source_id,
                source_url=snapshot.source_url,
                sampled_at=snapshot.sampled_at,
                retrieved_at=snapshot.retrieved_at,
            )
        ],
        model_version=MODEL_VERSION,
    )
