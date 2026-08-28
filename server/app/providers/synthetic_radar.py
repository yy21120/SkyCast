from __future__ import annotations

from pathlib import Path

from app.domain.radar import (
    RadarEvent,
    RadarEventNotFoundError,
    RadarFrame,
    RadarFrameData,
    RadarFrameNotFoundError,
    RadarPackageValidationError,
)
from app.services.radar_packages import validate_radar_event_package

SOURCE_ID = "skycast:synthetic-radar:wuhan-v1"


class SyntheticReplayRadarProvider:
    def __init__(self, package_root: Path) -> None:
        self._package_root = package_root
        self._events: dict[str, tuple[RadarEvent, Path]] = {}
        for manifest_path in sorted(package_root.glob("*/manifest.json")):
            event = validate_radar_event_package(manifest_path)
            if event.source_id != SOURCE_ID:
                raise RadarPackageValidationError(
                    f"unexpected source_id for synthetic package: {event.source_id}"
                )
            if event.event_id in self._events:
                raise RadarPackageValidationError(f"duplicate radar event: {event.event_id}")
            self._events[event.event_id] = (event, manifest_path.parent)

    def event(self, event_id: str) -> RadarEvent:
        try:
            return self._events[event_id][0]
        except KeyError as exc:
            raise RadarEventNotFoundError(event_id) from exc

    def frames(self, event_id: str) -> list[RadarFrame]:
        return list(self.event(event_id).frames)

    def frame_data(self, event_id: str, frame_id: str) -> RadarFrameData:
        event, package_dir = self._event_package(event_id)
        frame = next((item for item in event.frames if item.frame_id == frame_id), None)
        if frame is None:
            raise RadarFrameNotFoundError(frame_id)
        values = (package_dir / frame.data_uri).read_bytes()
        return RadarFrameData(frame=frame, values=values)

    def _event_package(self, event_id: str) -> tuple[RadarEvent, Path]:
        try:
            return self._events[event_id]
        except KeyError as exc:
            raise RadarEventNotFoundError(event_id) from exc
