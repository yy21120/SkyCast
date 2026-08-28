"""Build SkyCast's deterministic Wuhan synthetic radar replay package."""

from __future__ import annotations

import argparse
import hashlib
import json
import random
from datetime import UTC, datetime, timedelta
from pathlib import Path

DEFAULT_OUTPUT = Path("data") / "sample" / "wuhan" / "radar_synthetic_v1"
EVENT_ID = "wuhan-synthetic-convection-v1"
SOURCE_ID = "skycast:synthetic-radar:wuhan-v1"
SEED = 21120
FRAME_COUNT = 12
INTERVAL_MINUTES = 6
WIDTH = 80
HEIGHT = 80
WEST = 113.2
SOUTH = 29.6
EAST = 115.2
NORTH = 31.6
START_TIME = datetime(2026, 7, 15, 7, 0, tzinfo=UTC)


def _reflectivity_value(
    x: int, y: int, center_x: float, center_y: float, frame_index: int, rng: random.Random
) -> int:
    main_distance = ((x - center_x) / 9.0) ** 2 + ((y - center_y) / 7.0) ** 2
    main_strength = 62 - abs(frame_index - 6) // 2
    main_cell = max(0, round(main_strength - main_distance * 18))

    secondary_x = center_x - 13 + frame_index * 0.25
    secondary_y = center_y + 8
    secondary_distance = ((x - secondary_x) / 7.0) ** 2 + ((y - secondary_y) / 5.0) ** 2
    secondary_cell = max(0, round(48 - secondary_distance * 17))

    value = max(main_cell, secondary_cell)
    if value > 0:
        value = max(1, min(75, value + rng.randint(-2, 2)))
    return value


def _frame_values(frame_index: int) -> tuple[bytes, float, float]:
    center_x = 18 + frame_index * 3.0
    center_y = 62 - frame_index * 2.0
    rng = random.Random(SEED + frame_index)
    values = bytes(
        _reflectivity_value(x, y, center_x, center_y, frame_index, rng)
        for y in range(HEIGHT)
        for x in range(WIDTH)
    )
    longitude_resolution = (EAST - WEST) / WIDTH
    latitude_resolution = (NORTH - SOUTH) / HEIGHT
    centroid_longitude = WEST + (center_x + 0.5) * longitude_resolution
    centroid_latitude = NORTH - (center_y + 0.5) * latitude_resolution
    return values, centroid_longitude, centroid_latitude


def _package_checksum(frames: list[dict]) -> str:
    value = "\n".join(
        f"{frame['frame_id']}:{frame['checksum_sha256']}" for frame in frames
    )
    return hashlib.sha256(value.encode()).hexdigest()


def build_event(output_dir: Path = DEFAULT_OUTPUT) -> Path:
    frame_dir = output_dir / "frames"
    frame_dir.mkdir(parents=True, exist_ok=True)
    frames: list[dict] = []
    for index in range(FRAME_COUNT):
        frame_id = f"frame-{index:03d}"
        data_uri = f"frames/{frame_id}.bin"
        values, longitude, latitude = _frame_values(index)
        (output_dir / data_uri).write_bytes(values)
        observed_at = START_TIME + timedelta(minutes=index * INTERVAL_MINUTES)
        ingested_at = observed_at + timedelta(minutes=2)
        frames.append(
            {
                "schema_version": 1,
                "event_id": EVENT_ID,
                "frame_id": frame_id,
                "observed_at": observed_at.isoformat().replace("+00:00", "Z"),
                "ingested_at": ingested_at.isoformat().replace("+00:00", "Z"),
                "expires_at": (ingested_at + timedelta(minutes=18))
                .isoformat()
                .replace("+00:00", "Z"),
                "source_id": SOURCE_ID,
                "product": "composite_reflectivity",
                "unit": "dBZ",
                "crs": "EPSG:4326",
                "bounding_box": {
                    "west": WEST,
                    "south": SOUTH,
                    "east": EAST,
                    "north": NORTH,
                },
                "grid": {
                    "width": WIDTH,
                    "height": HEIGHT,
                    "resolution_longitude_degrees": (EAST - WEST) / WIDTH,
                    "resolution_latitude_degrees": (NORTH - SOUTH) / HEIGHT,
                    "encoding": "uint8-dbz-v1",
                    "nodata_value": 255,
                },
                "data_uri": data_uri,
                "checksum_sha256": hashlib.sha256(values).hexdigest(),
                "max_reflectivity_dbz": max(values),
                "storm_centroid": {
                    "longitude": round(longitude, 6),
                    "latitude": round(latitude, 6),
                },
                "quality_flags": ["complete"],
                "synthetic": True,
                "official": False,
            }
        )

    manifest = {
        "schema_version": 1,
        "event_id": EVENT_ID,
        "title": "武汉合成强对流回放 v1",
        "city_id": "wuhan",
        "display_timezone": "Asia/Shanghai",
        "source_id": SOURCE_ID,
        "interval_minutes": INTERVAL_MINUTES,
        "synthetic_seed": SEED,
        "synthetic": True,
        "official": False,
        "disclaimer": "历史合成回放，非实时官方雷达，不用于防灾决策或实际追风导航。",
        "package_checksum_sha256": _package_checksum(frames),
        "frames": frames,
    }
    manifest_path = output_dir / "manifest.json"
    manifest_path.write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    return manifest_path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args(argv)
    manifest_path = build_event(args.output)
    print(f"synthetic radar event written to {manifest_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
