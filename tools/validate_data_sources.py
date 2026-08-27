"""Validate SkyCast's machine-readable data-source admission catalog."""

from __future__ import annotations

import argparse
import json
import re
from datetime import date
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

CATALOG_PATH = (
    Path(__file__).resolve().parents[1] / "data" / "catalog" / "data_sources.v1.json"
)

SOURCE_ID_PATTERN = re.compile(r"^[a-z0-9][a-z0-9-]*(?::[a-z0-9][a-z0-9-]*)+$")
ADMISSION_STATUSES = {"approved", "restricted", "blocked"}
CATEGORIES = {"weather_forecast", "radar", "official_alert", "satellite"}
ACCESS_METHODS = {"public_api", "public_web_page", "application_api", "registered_portal"}
AUTHENTICATION_MODES = {"none", "approved_application", "verified_account"}
COST_MODES = {"free_non_commercial", "not_stated", "contact_provider"}
PERMISSIONS = {"allowed", "conditional", "prohibited"}
FRESHNESS_MODES = {"scheduled", "event_driven"}
USE_CASES = {"local_research", "public_portfolio", "production", "redistribution"}


def load_catalog(path: Path = CATALOG_PATH) -> dict[str, Any]:
    """Load a JSON catalog from disk."""
    with path.open(encoding="utf-8") as file:
        value = json.load(file)
    if not isinstance(value, dict):
        raise TypeError("catalog root must be an object")
    return value


def _is_https_url(value: object) -> bool:
    if not isinstance(value, str):
        return False
    parsed = urlparse(value)
    return parsed.scheme == "https" and bool(parsed.netloc)


def _is_iso_date(value: object) -> bool:
    if not isinstance(value, str):
        return False
    try:
        date.fromisoformat(value)
    except ValueError:
        return False
    return True


def _is_positive_int_or_none(value: object) -> bool:
    return value is None or (isinstance(value, int) and not isinstance(value, bool) and value > 0)


def _is_positive_int(value: object) -> bool:
    return isinstance(value, int) and not isinstance(value, bool) and value > 0


def _require_keys(value: object, keys: set[str], path: str, errors: list[str]) -> bool:
    if not isinstance(value, dict):
        errors.append(f"{path} must be an object")
        return False
    for key in sorted(keys - value.keys()):
        errors.append(f"{path}.{key} is required")
    return keys.issubset(value.keys())


def _validate_permissions(
    permissions: object, status: object, path: str, errors: list[str]
) -> None:
    if not _require_keys(permissions, USE_CASES, path, errors):
        return
    assert isinstance(permissions, dict)
    for use_case in sorted(USE_CASES):
        if permissions[use_case] not in PERMISSIONS:
            errors.append(f"{path}.{use_case} has an invalid permission")

    if status == "approved" and permissions["public_portfolio"] == "prohibited":
        errors.append(f"{path}.public_portfolio cannot be prohibited for an approved source")
    if status == "blocked":
        for use_case in ("public_portfolio", "production", "redistribution"):
            if permissions[use_case] != "prohibited":
                errors.append(f"{path}.{use_case} must be prohibited for a blocked source")


def _validate_freshness(freshness: object, status: object, path: str, errors: list[str]) -> None:
    required = {
        "mode",
        "expected_interval_minutes",
        "typical_latency_minutes",
        "expires_after_minutes",
        "policy_basis",
    }
    if not _require_keys(freshness, required, path, errors):
        return
    assert isinstance(freshness, dict)
    mode = freshness["mode"]
    if mode not in FRESHNESS_MODES:
        errors.append(f"{path}.mode has an invalid value")
    for key in (
        "expected_interval_minutes",
        "typical_latency_minutes",
        "expires_after_minutes",
    ):
        if not _is_positive_int_or_none(freshness[key]):
            errors.append(f"{path}.{key} must be a positive integer or null")
    if (
        mode == "scheduled"
        and status == "approved"
        and freshness["expected_interval_minutes"] is None
    ):
        errors.append(f"{path}.expected_interval_minutes is required for scheduled data")
    if mode == "event_driven" and freshness["expected_interval_minutes"] is not None:
        errors.append(f"{path}.expected_interval_minutes must be null for event-driven data")
    if status == "approved" and freshness["expires_after_minutes"] is None:
        errors.append(f"{path}.expires_after_minutes is required for an approved source")
    if not isinstance(freshness["policy_basis"], str) or not freshness["policy_basis"].strip():
        errors.append(f"{path}.policy_basis must be a non-empty string")


def _validate_evidence(
    evidence: object, catalog_date: object, path: str, errors: list[str]
) -> None:
    if not isinstance(evidence, list) or not evidence:
        errors.append(f"{path} must contain at least one official reference")
        return
    for index, item in enumerate(evidence):
        item_path = f"{path}[{index}]"
        if not _require_keys(item, {"url", "claim", "verified_at"}, item_path, errors):
            continue
        assert isinstance(item, dict)
        if not _is_https_url(item["url"]):
            errors.append(f"{item_path}.url must be an HTTPS URL")
        if not isinstance(item["claim"], str) or not item["claim"].strip():
            errors.append(f"{item_path}.claim must be a non-empty string")
        if not _is_iso_date(item["verified_at"]):
            errors.append(f"{item_path}.verified_at must be an ISO date")
        elif _is_iso_date(catalog_date) and item["verified_at"] > catalog_date:
            errors.append(f"{item_path}.verified_at cannot be newer than the catalog")


def _validate_source(
    source: object, index: int, catalog_date: object, errors: list[str]
) -> str | None:
    path = f"sources[{index}]"
    required = {
        "source_id",
        "provider",
        "product",
        "category",
        "admission_status",
        "decision_summary",
        "access",
        "licensing",
        "freshness",
        "evidence",
        "fallback",
        "risk_note",
    }
    if not _require_keys(source, required, path, errors):
        return None
    assert isinstance(source, dict)

    source_id = source["source_id"]
    if not isinstance(source_id, str) or not SOURCE_ID_PATTERN.fullmatch(source_id):
        errors.append(f"{path}.source_id has an invalid format")
        source_id = None
    if source["category"] not in CATEGORIES:
        errors.append(f"{path}.category has an invalid value")
    status = source["admission_status"]
    if status not in ADMISSION_STATUSES:
        errors.append(f"{path}.admission_status has an invalid value")
    for key in ("provider", "product", "decision_summary", "fallback", "risk_note"):
        if not isinstance(source[key], str) or not source[key].strip():
            errors.append(f"{path}.{key} must be a non-empty string")

    access = source["access"]
    access_keys = {"method", "authentication", "cost", "endpoint"}
    if _require_keys(access, access_keys, f"{path}.access", errors):
        assert isinstance(access, dict)
        if access["method"] not in ACCESS_METHODS:
            errors.append(f"{path}.access.method has an invalid value")
        if access["authentication"] not in AUTHENTICATION_MODES:
            errors.append(f"{path}.access.authentication has an invalid value")
        if access["cost"] not in COST_MODES:
            errors.append(f"{path}.access.cost has an invalid value")
        if not _is_https_url(access["endpoint"]):
            errors.append(f"{path}.access.endpoint must be an HTTPS URL")

    licensing = source["licensing"]
    licensing_keys = {
        "license_name",
        "license_url",
        "attribution_required",
        "attribution_text",
        "permissions",
    }
    if _require_keys(licensing, licensing_keys, f"{path}.licensing", errors):
        assert isinstance(licensing, dict)
        if not isinstance(licensing["license_name"], str) or not licensing["license_name"].strip():
            errors.append(f"{path}.licensing.license_name must be a non-empty string")
        if not _is_https_url(licensing["license_url"]):
            errors.append(f"{path}.licensing.license_url must be an HTTPS URL")
        if not isinstance(licensing["attribution_required"], bool):
            errors.append(f"{path}.licensing.attribution_required must be a boolean")
        if licensing["attribution_required"] and (
            not isinstance(licensing["attribution_text"], str)
            or not licensing["attribution_text"].strip()
        ):
            errors.append(f"{path}.licensing.attribution_text is required")
        _validate_permissions(
            licensing["permissions"], status, f"{path}.licensing.permissions", errors
        )

    _validate_freshness(source["freshness"], status, f"{path}.freshness", errors)
    _validate_evidence(source["evidence"], catalog_date, f"{path}.evidence", errors)
    return source_id


def validate_catalog(catalog: object) -> list[str]:
    """Return all catalog validation errors without stopping at the first one."""
    errors: list[str] = []
    if not _require_keys(
        catalog,
        {"schema_version", "catalog_verified_at", "review_after_days", "sources"},
        "catalog",
        errors,
    ):
        return errors
    assert isinstance(catalog, dict)
    if catalog["schema_version"] != 1:
        errors.append("catalog.schema_version must equal 1")
    if not _is_iso_date(catalog["catalog_verified_at"]):
        errors.append("catalog.catalog_verified_at must be an ISO date")
    if not _is_positive_int(catalog["review_after_days"]):
        errors.append("catalog.review_after_days must be a positive integer")
    sources = catalog["sources"]
    if not isinstance(sources, list) or not sources:
        errors.append("catalog.sources must be a non-empty array")
        return errors

    seen_ids: set[str] = set()
    for index, source in enumerate(sources):
        source_id = _validate_source(source, index, catalog["catalog_verified_at"], errors)
        if source_id in seen_ids:
            errors.append(f"sources[{index}].source_id is duplicated: {source_id}")
        elif source_id is not None:
            seen_ids.add(source_id)
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("catalog", nargs="?", type=Path, default=CATALOG_PATH)
    args = parser.parse_args(argv)
    try:
        catalog = load_catalog(args.catalog)
    except (OSError, TypeError, json.JSONDecodeError) as exc:
        print(f"catalog load failed: {exc}")
        return 1

    errors = validate_catalog(catalog)
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        print(f"data-source catalog failed with {len(errors)} error(s)")
        return 1
    print(f"data-source catalog is valid: {len(catalog['sources'])} source(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
