from __future__ import annotations

from copy import deepcopy

from tools.validate_data_sources import load_catalog, validate_catalog


def valid_catalog() -> dict:
    return load_catalog()


def source(catalog: dict, source_id: str) -> dict:
    return next(item for item in catalog["sources"] if item["source_id"] == source_id)


def test_repository_catalog_is_valid() -> None:
    assert validate_catalog(valid_catalog()) == []


def test_duplicate_source_id_is_rejected() -> None:
    catalog = valid_catalog()
    catalog["sources"].append(deepcopy(catalog["sources"][0]))

    errors = validate_catalog(catalog)

    assert any("source_id is duplicated" in error for error in errors)


def test_source_without_official_evidence_is_rejected() -> None:
    catalog = valid_catalog()
    catalog["sources"][0]["evidence"] = []

    errors = validate_catalog(catalog)

    assert any("at least one official reference" in error for error in errors)


def test_blocked_source_cannot_allow_public_portfolio_use() -> None:
    catalog = valid_catalog()
    radar = source(catalog, "cma:nmc-radar-mosaic:web")
    radar["licensing"]["permissions"]["public_portfolio"] = "allowed"

    errors = validate_catalog(catalog)

    assert any("must be prohibited for a blocked source" in error for error in errors)


def test_scheduled_source_requires_update_interval() -> None:
    catalog = valid_catalog()
    open_meteo = source(catalog, "open-meteo:forecast")
    open_meteo["freshness"]["expected_interval_minutes"] = None

    errors = validate_catalog(catalog)

    assert any("required for scheduled data" in error for error in errors)


def test_attribution_text_is_required_when_attribution_is_enabled() -> None:
    catalog = valid_catalog()
    open_meteo = source(catalog, "open-meteo:forecast")
    open_meteo["licensing"]["attribution_text"] = ""

    errors = validate_catalog(catalog)

    assert any("attribution_text is required" in error for error in errors)


def test_review_period_cannot_be_null() -> None:
    catalog = valid_catalog()
    catalog["review_after_days"] = None

    errors = validate_catalog(catalog)

    assert any("review_after_days must be a positive integer" in error for error in errors)
