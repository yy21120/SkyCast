from __future__ import annotations

from datetime import datetime
from math import isclose
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator


def _is_timezone_aware(value: datetime) -> bool:
    return value.tzinfo is not None and value.utcoffset() is not None


class RadarBoundingBox(BaseModel):
    model_config = ConfigDict(frozen=True)

    west: float = Field(ge=-180, le=180)
    south: float = Field(ge=-90, le=90)
    east: float = Field(ge=-180, le=180)
    north: float = Field(ge=-90, le=90)

    @model_validator(mode="after")
    def validate_order(self) -> RadarBoundingBox:
        if self.west >= self.east:
            raise ValueError("west must be smaller than east")
        if self.south >= self.north:
            raise ValueError("south must be smaller than north")
        return self

    def contains(self, longitude: float, latitude: float) -> bool:
        return self.west <= longitude <= self.east and self.south <= latitude <= self.north


class RadarGridSpec(BaseModel):
    model_config = ConfigDict(frozen=True)

    width: int = Field(gt=0, le=4096)
    height: int = Field(gt=0, le=4096)
    resolution_longitude_degrees: float = Field(gt=0)
    resolution_latitude_degrees: float = Field(gt=0)
    encoding: Literal["uint8-dbz-v1"] = "uint8-dbz-v1"
    nodata_value: Literal[255] = 255


class RadarCentroid(BaseModel):
    model_config = ConfigDict(frozen=True)

    longitude: float = Field(ge=-180, le=180)
    latitude: float = Field(ge=-90, le=90)


class RadarFrame(BaseModel):
    model_config = ConfigDict(frozen=True)

    schema_version: Literal[1] = 1
    event_id: str = Field(pattern=r"^[a-z0-9][a-z0-9-]*$")
    frame_id: str = Field(pattern=r"^[a-z0-9][a-z0-9-]*$")
    observed_at: datetime
    ingested_at: datetime
    expires_at: datetime
    source_id: str
    product: Literal["composite_reflectivity"] = "composite_reflectivity"
    unit: Literal["dBZ"] = "dBZ"
    crs: Literal["EPSG:4326"] = "EPSG:4326"
    bounding_box: RadarBoundingBox
    grid: RadarGridSpec
    data_uri: str = Field(pattern=r"^frames/[a-z0-9][a-z0-9-]*\.bin$")
    checksum_sha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    max_reflectivity_dbz: int = Field(ge=0, le=75)
    storm_centroid: RadarCentroid
    quality_flags: list[Literal["complete"]] = Field(min_length=1)
    synthetic: bool
    official: bool

    @model_validator(mode="after")
    def validate_semantics(self) -> RadarFrame:
        for field_name in ("observed_at", "ingested_at", "expires_at"):
            if not _is_timezone_aware(getattr(self, field_name)):
                raise ValueError(f"{field_name} must be timezone-aware")
        if self.observed_at > self.ingested_at:
            raise ValueError("observed_at cannot be newer than ingested_at")
        if self.ingested_at >= self.expires_at:
            raise ValueError("expires_at must be newer than ingested_at")
        if self.synthetic and self.official:
            raise ValueError("a radar frame cannot be both synthetic and official")
        if not self.bounding_box.contains(
            self.storm_centroid.longitude, self.storm_centroid.latitude
        ):
            raise ValueError("storm centroid must be inside the bounding box")
        longitude_span = self.bounding_box.east - self.bounding_box.west
        latitude_span = self.bounding_box.north - self.bounding_box.south
        if not isclose(
            self.grid.width * self.grid.resolution_longitude_degrees,
            longitude_span,
            rel_tol=1e-9,
        ):
            raise ValueError("grid longitude resolution must match the bounding box")
        if not isclose(
            self.grid.height * self.grid.resolution_latitude_degrees,
            latitude_span,
            rel_tol=1e-9,
        ):
            raise ValueError("grid latitude resolution must match the bounding box")
        return self


class RadarEvent(BaseModel):
    model_config = ConfigDict(frozen=True)

    schema_version: Literal[1] = 1
    event_id: str = Field(pattern=r"^[a-z0-9][a-z0-9-]*$")
    title: str
    city_id: Literal["wuhan"] = "wuhan"
    display_timezone: Literal["Asia/Shanghai"] = "Asia/Shanghai"
    source_id: str
    interval_minutes: int = Field(gt=0)
    synthetic_seed: int | None = None
    synthetic: bool
    official: bool
    disclaimer: str
    package_checksum_sha256: str = Field(pattern=r"^[0-9a-f]{64}$")
    frames: list[RadarFrame] = Field(min_length=1)

    @model_validator(mode="after")
    def validate_frames(self) -> RadarEvent:
        if self.synthetic and self.official:
            raise ValueError("a radar event cannot be both synthetic and official")
        previous: datetime | None = None
        frame_ids: set[str] = set()
        for frame in self.frames:
            if frame.event_id != self.event_id:
                raise ValueError("every frame must reference the parent event")
            if frame.source_id != self.source_id:
                raise ValueError("every frame must use the event source_id")
            if frame.synthetic != self.synthetic or frame.official != self.official:
                raise ValueError("frame classification must match the event")
            if frame.frame_id in frame_ids:
                raise ValueError("frame_id values must be unique")
            frame_ids.add(frame.frame_id)
            if previous is not None:
                difference_minutes = (frame.observed_at - previous).total_seconds() / 60
                if difference_minutes != self.interval_minutes:
                    raise ValueError("frames must use the declared interval in time order")
            previous = frame.observed_at
        return self


class RadarFrameData(BaseModel):
    model_config = ConfigDict(frozen=True)

    frame: RadarFrame
    values: bytes


class RadarEventNotFoundError(LookupError):
    """Raised when a provider does not contain the requested event."""


class RadarFrameNotFoundError(LookupError):
    """Raised when a provider does not contain the requested frame."""


class RadarPackageValidationError(ValueError):
    """Raised when a radar replay package is corrupt or unsafe to load."""
