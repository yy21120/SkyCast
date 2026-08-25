from __future__ import annotations

from datetime import date, datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class City(BaseModel):
    model_config = ConfigDict(frozen=True)

    id: str
    name: str
    latitude: float
    longitude: float
    timezone: str


class WeatherSnapshot(BaseModel):
    model_config = ConfigDict(frozen=True)

    valid_date: date
    sunset: datetime
    sampled_at: datetime
    low_cloud_percent: float = Field(ge=0, le=100)
    mid_cloud_percent: float = Field(ge=0, le=100)
    high_cloud_percent: float = Field(ge=0, le=100)
    precipitation_probability_percent: float = Field(ge=0, le=100)
    visibility_meters: float = Field(ge=0)
    relative_humidity_percent: float = Field(ge=0, le=100)
    source_id: str
    source_url: str | None = None
    retrieved_at: datetime


class AssessmentFactor(BaseModel):
    code: str
    label: str
    value: float
    unit: str
    contribution: float
    effect: Literal["favorable", "neutral", "limiting"]
    explanation: str


class SourceReference(BaseModel):
    source_id: str
    source_url: str | None = None
    sampled_at: datetime
    retrieved_at: datetime


class SunsetOpportunity(BaseModel):
    scene_id: str
    date: date
    sunset: datetime
    coloring_window_start: datetime
    coloring_window_end: datetime
    score: int = Field(ge=0, le=100)
    baseline_probability: float = Field(ge=0, le=1)
    probability_status: Literal["uncalibrated_baseline"] = "uncalibrated_baseline"
    confidence: Literal["low", "medium", "high"]
    recommendation: Literal["go", "watch", "skip"]
    summary: str
    factors: list[AssessmentFactor]
    sources: list[SourceReference]
    model_version: str


class OpportunitiesResponse(BaseModel):
    city: City
    scene_type: Literal["sunset"] = "sunset"
    mode: Literal["replay", "live"]
    generated_at: datetime
    opportunities: list[SunsetOpportunity]
