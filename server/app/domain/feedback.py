from __future__ import annotations

from datetime import datetime
from enum import StrEnum
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator


class SunsetOutcome(StrEnum):
    VIVID = "vivid"
    VISIBLE = "visible"
    NOT_VISIBLE = "not_visible"


class SunsetFeedbackCreate(BaseModel):
    model_config = ConfigDict(str_strip_whitespace=True)

    client_feedback_id: UUID
    scene_id: str = Field(
        min_length=1,
        max_length=128,
        pattern=r"^[a-z0-9]+(?:-[a-z0-9]+)*$",
    )
    outcome: SunsetOutcome
    shooting_quality: int = Field(ge=1, le=5)
    notes: str | None = Field(default=None, max_length=200)
    submitted_at: datetime

    @field_validator("notes")
    @classmethod
    def normalize_empty_notes(cls, value: str | None) -> str | None:
        return value or None

    @field_validator("submitted_at")
    @classmethod
    def require_submitted_at_timezone(cls, value: datetime) -> datetime:
        if value.tzinfo is None or value.utcoffset() is None:
            raise ValueError("submitted_at must include a timezone offset")
        return value


class SunsetFeedbackRecord(BaseModel):
    client_feedback_id: UUID
    scene_id: str
    outcome: SunsetOutcome
    shooting_quality: int
    notes: str | None
    submitted_at: datetime
    created_at: datetime


class SunsetFeedbackResponse(BaseModel):
    status: str = "accepted"
    duplicate: bool
    feedback: SunsetFeedbackRecord
