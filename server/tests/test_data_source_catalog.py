from __future__ import annotations

import json
from pathlib import Path

from app.providers.open_meteo import SOURCE_ID as OPEN_METEO_SOURCE_ID
from app.providers.synthetic_radar import SOURCE_ID as SYNTHETIC_RADAR_SOURCE_ID

CATALOG_PATH = (
    Path(__file__).resolve().parents[2] / "data" / "catalog" / "data_sources.v1.json"
)


def test_open_meteo_provider_is_registered_in_admission_catalog() -> None:
    catalog = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))
    registered_ids = {source["source_id"] for source in catalog["sources"]}

    assert OPEN_METEO_SOURCE_ID in registered_ids


def test_synthetic_radar_provider_is_registered_in_admission_catalog() -> None:
    catalog = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))
    registered_ids = {source["source_id"] for source in catalog["sources"]}

    assert SYNTHETIC_RADAR_SOURCE_ID in registered_ids
