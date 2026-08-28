from __future__ import annotations

import hashlib
import json
import shutil
from pathlib import Path

import pytest

from app.domain.radar import RadarPackageValidationError
from app.services.radar_packages import package_checksum, validate_radar_event_package

SOURCE_PACKAGE = (
    Path(__file__).resolve().parents[2]
    / "data"
    / "sample"
    / "wuhan"
    / "radar_synthetic_v1"
)


def copy_package(tmp_path: Path) -> Path:
    destination = tmp_path / "event"
    shutil.copytree(SOURCE_PACKAGE, destination)
    return destination / "manifest.json"


def load_manifest(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def save_manifest(path: Path, manifest: dict) -> None:
    path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def refresh_package_checksum(manifest: dict) -> None:
    manifest["package_checksum_sha256"] = package_checksum(
        [(frame["frame_id"], frame["checksum_sha256"]) for frame in manifest["frames"]]
    )


def test_repository_package_is_valid() -> None:
    event = validate_radar_event_package(SOURCE_PACKAGE / "manifest.json")

    assert event.event_id == "wuhan-synthetic-convection-v1"


def test_checksum_corruption_is_rejected(tmp_path: Path) -> None:
    manifest_path = copy_package(tmp_path)
    manifest = load_manifest(manifest_path)
    frame_path = manifest_path.parent / manifest["frames"][0]["data_uri"]
    values = bytearray(frame_path.read_bytes())
    values[0] = 1 if values[0] == 0 else 0
    frame_path.write_bytes(values)

    with pytest.raises(RadarPackageValidationError, match="checksum mismatch"):
        validate_radar_event_package(manifest_path)


def test_non_increasing_time_is_rejected(tmp_path: Path) -> None:
    manifest_path = copy_package(tmp_path)
    manifest = load_manifest(manifest_path)
    manifest["frames"][1]["observed_at"] = manifest["frames"][0]["observed_at"]
    save_manifest(manifest_path, manifest)

    with pytest.raises(RadarPackageValidationError, match="declared interval"):
        validate_radar_event_package(manifest_path)


def test_reflectivity_above_domain_limit_is_rejected(tmp_path: Path) -> None:
    manifest_path = copy_package(tmp_path)
    manifest = load_manifest(manifest_path)
    frame = manifest["frames"][0]
    frame_path = manifest_path.parent / frame["data_uri"]
    values = bytearray(frame_path.read_bytes())
    values[0] = 100
    frame_path.write_bytes(values)
    frame["checksum_sha256"] = hashlib.sha256(values).hexdigest()
    refresh_package_checksum(manifest)
    save_manifest(manifest_path, manifest)

    with pytest.raises(RadarPackageValidationError, match="above 75 dBZ"):
        validate_radar_event_package(manifest_path)
