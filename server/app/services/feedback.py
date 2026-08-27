from __future__ import annotations

import sqlite3
from datetime import UTC, datetime
from pathlib import Path
from typing import Protocol

from app.domain.feedback import (
    SunsetFeedbackCreate,
    SunsetFeedbackRecord,
    SunsetOutcome,
)


class FeedbackConflictError(Exception):
    """Raised when an idempotency key is reused for different feedback."""


class FeedbackRepository(Protocol):
    def submit(
        self,
        feedback: SunsetFeedbackCreate,
    ) -> tuple[SunsetFeedbackRecord, bool]: ...


class SQLiteFeedbackRepository:
    def __init__(self, database_path: Path) -> None:
        self._database_path = database_path

    def submit(
        self,
        feedback: SunsetFeedbackCreate,
    ) -> tuple[SunsetFeedbackRecord, bool]:
        self._ensure_database_directory()
        created_at = datetime.now(UTC)
        values = self._values(feedback, created_at)

        with self._connect() as connection:
            self._create_schema(connection)
            cursor = connection.execute(
                """
                INSERT OR IGNORE INTO sunset_feedback (
                    client_feedback_id,
                    scene_id,
                    outcome,
                    shooting_quality,
                    notes,
                    submitted_at,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                values,
            )
            inserted = cursor.rowcount == 1
            row = connection.execute(
                """
                SELECT
                    client_feedback_id,
                    scene_id,
                    outcome,
                    shooting_quality,
                    notes,
                    submitted_at,
                    created_at
                FROM sunset_feedback
                WHERE client_feedback_id = ?
                """,
                (str(feedback.client_feedback_id),),
            ).fetchone()

        if row is None:
            raise RuntimeError("feedback could not be read after submission")

        record = self._record(row)
        if not inserted and not self._same_payload(record, feedback):
            raise FeedbackConflictError(
                "client_feedback_id is already used by different feedback"
            )
        return record, not inserted

    def count(self) -> int:
        if not self._database_path.exists():
            return 0
        with self._connect() as connection:
            self._create_schema(connection)
            row = connection.execute("SELECT COUNT(*) FROM sunset_feedback").fetchone()
        return int(row[0]) if row is not None else 0

    def get(self, client_feedback_id: str) -> SunsetFeedbackRecord | None:
        if not self._database_path.exists():
            return None
        with self._connect() as connection:
            self._create_schema(connection)
            row = connection.execute(
                """
                SELECT
                    client_feedback_id,
                    scene_id,
                    outcome,
                    shooting_quality,
                    notes,
                    submitted_at,
                    created_at
                FROM sunset_feedback
                WHERE client_feedback_id = ?
                """,
                (client_feedback_id,),
            ).fetchone()
        return self._record(row) if row is not None else None

    def _ensure_database_directory(self) -> None:
        self._database_path.parent.mkdir(parents=True, exist_ok=True)

    def _connect(self) -> sqlite3.Connection:
        connection = sqlite3.connect(self._database_path, timeout=5.0)
        connection.row_factory = sqlite3.Row
        return connection

    @staticmethod
    def _create_schema(connection: sqlite3.Connection) -> None:
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS sunset_feedback (
                client_feedback_id TEXT PRIMARY KEY,
                scene_id TEXT NOT NULL,
                outcome TEXT NOT NULL CHECK (
                    outcome IN ('vivid', 'visible', 'not_visible')
                ),
                shooting_quality INTEGER NOT NULL CHECK (
                    shooting_quality BETWEEN 1 AND 5
                ),
                notes TEXT CHECK (notes IS NULL OR length(notes) <= 200),
                submitted_at TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
            """
        )

    @staticmethod
    def _values(
        feedback: SunsetFeedbackCreate,
        created_at: datetime,
    ) -> tuple[str, str, str, int, str | None, str, str]:
        return (
            str(feedback.client_feedback_id),
            feedback.scene_id,
            feedback.outcome.value,
            feedback.shooting_quality,
            feedback.notes,
            feedback.submitted_at.isoformat(),
            created_at.isoformat(),
        )

    @staticmethod
    def _record(row: sqlite3.Row) -> SunsetFeedbackRecord:
        return SunsetFeedbackRecord(
            client_feedback_id=row["client_feedback_id"],
            scene_id=row["scene_id"],
            outcome=SunsetOutcome(row["outcome"]),
            shooting_quality=row["shooting_quality"],
            notes=row["notes"],
            submitted_at=datetime.fromisoformat(row["submitted_at"]),
            created_at=datetime.fromisoformat(row["created_at"]),
        )

    @staticmethod
    def _same_payload(
        record: SunsetFeedbackRecord,
        feedback: SunsetFeedbackCreate,
    ) -> bool:
        return (
            record.scene_id == feedback.scene_id
            and record.outcome == feedback.outcome
            and record.shooting_quality == feedback.shooting_quality
            and record.notes == feedback.notes
            and record.submitted_at == feedback.submitted_at
        )
