FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PORT=8000 \
    SKYCAST_REPLAY_FIXTURE=/app/data/sample/wuhan/sunset_replay_v1.json \
    SKYCAST_FEEDBACK_DATABASE=/tmp/skycast/skycast.db

WORKDIR /app

COPY server/pyproject.toml server/README.md ./
COPY server/app ./app
COPY data/sample/wuhan/sunset_replay_v1.json ./data/sample/wuhan/sunset_replay_v1.json

RUN python -m pip install --no-cache-dir .

EXPOSE 8000

CMD ["sh", "-c", "python -m uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]
