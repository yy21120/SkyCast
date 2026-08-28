from __future__ import annotations

import hashlib
from pathlib import Path

from tools.build_synthetic_radar_event import build_event


def directory_checksums(path: Path) -> dict[str, str]:
    return {
        file.relative_to(path).as_posix(): hashlib.sha256(file.read_bytes()).hexdigest()
        for file in sorted(path.rglob("*"))
        if file.is_file()
    }


def test_generator_is_deterministic(tmp_path: Path) -> None:
    first = tmp_path / "first"
    second = tmp_path / "second"

    build_event(first)
    build_event(second)

    assert directory_checksums(first) == directory_checksums(second)
