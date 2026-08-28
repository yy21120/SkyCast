from __future__ import annotations

from datetime import timedelta
from itertools import pairwise
from pathlib import Path

import pytest

from app.domain.radar import RadarEventNotFoundError, RadarFrameNotFoundError
from app.providers.synthetic_radar import SyntheticReplayRadarProvider

PACKAGE_ROOT = Path(__file__).resolve().parents[2] / "data" / "sample" / "wuhan"
EVENT_ID = "wuhan-synthetic-convection-v1"


def provider() -> SyntheticReplayRadarProvider:
    return SyntheticReplayRadarProvider(PACKAGE_ROOT)


def test_provider_loads_complete_synthetic_event_in_time_order() -> None:
    event = provider().event(EVENT_ID)

    assert event.synthetic is True
    assert event.official is False
    assert event.display_timezone == "Asia/Shanghai"
    assert len(event.frames) == 12
    assert all(
        later.observed_at - earlier.observed_at == timedelta(minutes=6)
        for earlier, later in pairwise(event.frames)
    )


def test_storm_centroid_moves_from_southwest_to_northeast() -> None:
    frames = provider().frames(EVENT_ID)

    assert all(
        later.storm_centroid.longitude > earlier.storm_centroid.longitude
        for earlier, later in pairwise(frames)
    )
    assert all(
        later.storm_centroid.latitude > earlier.storm_centroid.latitude
        for earlier, later in pairwise(frames)
    )


def test_provider_reads_binary_grid() -> None:
    radar_provider = provider()
    frame = radar_provider.frames(EVENT_ID)[0]

    result = radar_provider.frame_data(EVENT_ID, frame.frame_id)

    assert result.frame == frame
    assert len(result.values) == frame.grid.width * frame.grid.height
    assert max(result.values) == frame.max_reflectivity_dbz


def test_unknown_event_has_domain_error() -> None:
    with pytest.raises(RadarEventNotFoundError, match="missing-event"):
        provider().event("missing-event")


def test_unknown_frame_has_domain_error() -> None:
    with pytest.raises(RadarFrameNotFoundError, match="missing-frame"):
        provider().frame_data(EVENT_ID, "missing-frame")
