from __future__ import annotations

import asyncio
from pathlib import Path
from typing import Any
from uuid import uuid4

import pytest
from fastapi import FastAPI
from httpx import ASGITransport, AsyncClient, Response

from app.main import create_app
from app.services.feedback import SQLiteFeedbackRepository


def post(app: FastAPI, payload: dict[str, Any]) -> Response:
    async def request() -> Response:
        transport = ASGITransport(app=app)
        async with AsyncClient(transport=transport, base_url="http://test") as client:
            return await client.post("/v1/feedback/sunset", json=payload)

    return asyncio.run(request())


def feedback_payload(**overrides: Any) -> dict[str, Any]:
    payload: dict[str, Any] = {
        "client_feedback_id": str(uuid4()),
        "scene_id": "wuhan-sunset-2026-08-27",
        "outcome": "vivid",
        "shooting_quality": 5,
        "notes": "东湖边可以看到明显染色。",
        "submitted_at": "2026-08-27T19:20:00+08:00",
    }
    payload.update(overrides)
    return payload


def test_valid_feedback_is_created_and_persisted(tmp_path: Path) -> None:
    database_path = tmp_path / "feedback.db"
    repository = SQLiteFeedbackRepository(database_path)
    payload = feedback_payload()

    response = post(create_app(repository), payload)

    assert response.status_code == 201
    body = response.json()
    assert body["status"] == "accepted"
    assert body["duplicate"] is False
    assert body["feedback"]["client_feedback_id"] == payload["client_feedback_id"]
    assert body["feedback"]["outcome"] == "vivid"
    assert repository.count() == 1

    reopened_repository = SQLiteFeedbackRepository(database_path)
    persisted = reopened_repository.get(payload["client_feedback_id"])
    assert persisted is not None
    assert persisted.notes == payload["notes"]


def test_identical_idempotent_retry_returns_existing_feedback(tmp_path: Path) -> None:
    repository = SQLiteFeedbackRepository(tmp_path / "feedback.db")
    app = create_app(repository)
    payload = feedback_payload()

    first = post(app, payload)
    second = post(app, payload)

    assert first.status_code == 201
    assert second.status_code == 201
    assert second.json()["duplicate"] is True
    assert second.json()["feedback"] == first.json()["feedback"]
    assert repository.count() == 1


def test_reused_id_with_different_payload_is_rejected(tmp_path: Path) -> None:
    repository = SQLiteFeedbackRepository(tmp_path / "feedback.db")
    app = create_app(repository)
    payload = feedback_payload()

    assert post(app, payload).status_code == 201
    conflicting = {**payload, "notes": "different observation"}
    response = post(app, conflicting)

    assert response.status_code == 409
    assert response.json()["detail"] == (
        "client_feedback_id is already used by different feedback"
    )
    assert repository.count() == 1


@pytest.mark.parametrize("outcome", ["vivid", "visible", "not_visible"])
def test_all_supported_outcomes_are_accepted(tmp_path: Path, outcome: str) -> None:
    repository = SQLiteFeedbackRepository(tmp_path / f"{outcome}.db")

    response = post(create_app(repository), feedback_payload(outcome=outcome))

    assert response.status_code == 201
    assert response.json()["feedback"]["outcome"] == outcome


@pytest.mark.parametrize(
    ("overrides", "error_fragment"),
    [
        ({"outcome": "storm"}, "Input should be 'vivid', 'visible' or 'not_visible'"),
        ({"shooting_quality": 0}, "greater than or equal to 1"),
        ({"shooting_quality": 6}, "less than or equal to 5"),
        ({"notes": "x" * 201}, "at most 200 characters"),
        ({"submitted_at": "2026-08-27T19:20:00"}, "timezone offset"),
        ({"scene_id": "武汉 晚霞"}, "String should match pattern"),
    ],
)
def test_invalid_feedback_is_rejected(
    tmp_path: Path,
    overrides: dict[str, Any],
    error_fragment: str,
) -> None:
    repository = SQLiteFeedbackRepository(tmp_path / "invalid.db")

    response = post(create_app(repository), feedback_payload(**overrides))

    assert response.status_code == 422
    assert error_fragment in response.text
    assert repository.count() == 0


def test_blank_notes_are_normalized_to_null(tmp_path: Path) -> None:
    repository = SQLiteFeedbackRepository(tmp_path / "feedback.db")

    response = post(create_app(repository), feedback_payload(notes="   "))

    assert response.status_code == 201
    assert response.json()["feedback"]["notes"] is None
