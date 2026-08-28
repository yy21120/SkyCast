"""Validate a SkyCast synthetic radar replay package."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).absolute().parents[1]
SERVER_ROOT = REPOSITORY_ROOT / "server"
DEFAULT_MANIFEST = Path("data") / "sample" / "wuhan" / "radar_synthetic_v1" / "manifest.json"
sys.path.insert(0, str(SERVER_ROOT))

from app.domain.radar import RadarPackageValidationError
from app.services.radar_packages import validate_radar_event_package


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", nargs="?", type=Path, default=DEFAULT_MANIFEST)
    args = parser.parse_args(argv)
    try:
        event = validate_radar_event_package(args.manifest)
    except RadarPackageValidationError as exc:
        print(f"ERROR: {exc}")
        return 1
    print(
        f"radar event is valid: {event.event_id}, "
        f"{len(event.frames)} frame(s), checksum={event.package_checksum_sha256}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
