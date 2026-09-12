from __future__ import annotations

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, Field


class AgentChatMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=2_000)


class AgentChatRequest(BaseModel):
    city_id: str = "wuhan"
    message: str = Field(min_length=1, max_length=500)
    selected_spot_id: str | None = None
    history: list[AgentChatMessage] = Field(default_factory=list, max_length=12)
    mode: Literal["replay", "live"] = "live"


class ShootingSpot(BaseModel):
    id: str
    name: str
    latitude: float
    longitude: float
    direction: str
    description: str


class AgentChatResponse(BaseModel):
    reply: str
    scene_id: str
    score: int = Field(ge=0, le=100)
    confidence: Literal["low", "medium", "high"]
    recommended_spots: list[ShootingSpot]
    used_tools: list[str]
    provider: str
    model: str
    generated_at: datetime
    safety_notice: str
    fallback: bool = False
