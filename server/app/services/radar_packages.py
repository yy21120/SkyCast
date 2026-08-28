from __future__ import annotations

import hashlib
import json
from pathlib import Path

from pydantic import ValidationError

from app.domain.radar import RadarEvent, RadarPackageValidationError

MAX_REFLECTIVITY_DBZ = 75
NODATA_VALUE = 255


def package_checksum(frame_checksums: list[tuple[str, str]]) -> str:
    value = "\n".join(f"{frame_id}:{checksum}" for frame_id, checksum in frame_checksums)
    return hashlib.sha256(value.encode()).hexdigest()


def _resolve_data_path(package_dir: Path, data_uri: str) -> Path:
    package_dir = package_dir.resolve()
    data_path = (package_dir / data_uri).resolve()
    if package_dir not in data_path.parents:
        raise RadarPackageValidationError(f"frame path leaves package directory: {data_uri}")
    return data_path


def validate_radar_event_package(manifest_path: Path) -> RadarEvent:
    try:
        payload = json.loads(manifest_path.read_text(encoding="utf-8"))
        event = RadarEvent.model_validate(payload)
    except (OSError, json.JSONDecodeError, ValidationError) as exc:
        raise RadarPackageValidationError(f"invalid radar manifest: {exc}") from exc

    if not event.synthetic or event.official:
        raise RadarPackageValidationError("fixture packages must be synthetic and non-official")

    package_dir = manifest_path.parent
    frame_checksums: list[tuple[str, str]] = []
    for frame in event.frames:
        data_path = _resolve_data_path(package_dir, frame.data_uri)
        try:
            values = data_path.read_bytes()
        except OSError as exc:
            raise RadarPackageValidationError(
                f"cannot read frame {frame.frame_id}: {data_path}"
            ) from exc
        expected_size = frame.grid.width * frame.grid.height
        if len(values) != expected_size:
            raise RadarPackageValidationError(
                f"frame {frame.frame_id} has {len(values)} bytes; expected {expected_size}"
            )
        checksum = hashlib.sha256(values).hexdigest()
        if checksum != frame.checksum_sha256:
            raise RadarPackageValidationError(f"frame {frame.frame_id} checksum mismatch")
        if any(MAX_REFLECTIVITY_DBZ < value < NODATA_VALUE for value in values):
            raise RadarPackageValidationError(
                f"frame {frame.frame_id} contains reflectivity above {MAX_REFLECTIVITY_DBZ} dBZ"
            )
        observed_max = max((value for value in values if value != NODATA_VALUE), default=0)
        if observed_max != frame.max_reflectivity_dbz:
            raise RadarPackageValidationError(
                f"frame {frame.frame_id} max reflectivity metadata mismatch"
            )
        frame_checksums.append((frame.frame_id, checksum))

    actual_package_checksum = package_checksum(frame_checksums)
    if actual_package_checksum != event.package_checksum_sha256:
        raise RadarPackageValidationError("package checksum mismatch")
    return event
