# Habit Tracker - Spring Boot backend

REST API for the Habit Tracker frontend (React / Capacitor Android app).
Java 21, Spring Boot 3.3, Spring Data JPA, H2 (local) or PostgreSQL (production).

## Run it on your PC

    mvn spring-boot:run          # http://localhost:8080

Data is kept in `./data/habits.mv.db` (H2). Check it: http://localhost:8080/api/health

The frontend finds this server on port 8080 of the same host, so `npm run dev` just works.

## Settings (environment variables)

| Variable | Default | What it does |
|---|---|---|
| `PORT` | `8080` | Port to listen on (Render sets this for you) |
| `DB_URL` | `jdbc:h2:file:./data/habits` | JDBC URL. For Postgres: `jdbc:postgresql://HOST:5432/DBNAME` |
| `DB_USER` | `sa` | Database user |
| `DB_PASSWORD` | empty | Database password |
| `APP_TIMEZONE` | `Asia/Kolkata` | Decides what "today" means for streaks and check-ins |
| `CORS_ALLOWED_ORIGINS` | `*` | Comma separated list of allowed origins |

Tables are created automatically on first start (`ddl-auto=update`).

## Deploy on Render

1. Create a **PostgreSQL** database on Render. Copy its host, database name, user and password.
2. Create a **Web Service** from this folder (Runtime: Docker, it uses the `Dockerfile`).
3. Add the variables `DB_URL`, `DB_USER`, `DB_PASSWORD` (and `APP_TIMEZONE` if you are not in India).
4. Set the health check path to `/api/health`.
5. Put the service address, with `/api` on the end, in the frontend:
   `VITE_API_URL=https://YOUR-SERVICE.onrender.com/api`
   (the GitHub workflow `build-apk.yml` has this value in its `VITE_API_URL` line).

Do not use the default H2 database on Render's free tier: its disk is wiped on every restart.

## How users are told apart

There are no accounts. The app creates a random id once and sends it with every request in the
`X-Owner-Id` header (16-64 letters, digits or dashes). Every query is filtered by it, so each
install only sees its own habits and walks. Anyone who knows an id can read that data, so treat
the id like a password (the app can adopt another device's code so two devices share the same data).

## Endpoints (all under /api, all need `X-Owner-Id`)

| Method | Path | Body / params | Returns |
|---|---|---|---|
| GET | `/habits` | | list of habits |
| POST | `/habits` | habit body | the new habit (201) |
| PUT | `/habits/{id}` | habit body | the updated habit |
| DELETE | `/habits/{id}` | | 204 (also deletes its check-ins) |
| POST | `/habits/{id}/toggle` | `?date=yyyy-MM-dd` (optional) | the habit, with `completedToday` |
| GET | `/habits/{id}/logs` | `?from=&to=` | `["2026-10-01", ...]` days done |
| GET | `/habits/{id}/stats` | | `currentStreak`, `longestStreak`, `totalCompletions`, `completionRateLast30Days` |
| GET | `/walks` | | list of walks, newest first |
| POST | `/walks` | walk body | the new walk (201) |
| DELETE | `/walks/{id}` | | 204 |
| GET | `/api/health` and `/` | | `{"status":"ok"}` (no header needed) |

Habit body: `name` (required, max 100), `description` (max 255), `color`, `reminderTime` ("HH:mm"),
`startDate` (daily habits), `scheduledDates` (empty = daily).

Walk body: `habitId` (optional), `walkDate`, `distanceMeters`, `durationSeconds`, `activityType` (`WALK` or `RUN`).

Errors: `{ "message": "..." }`, and for form problems also `{ "errors": { "field": "text" } }`.

## Not included

The `/push/*` endpoints that `api.js` still defines are not implemented. The current frontend never
calls them: alarms are scheduled on the phone itself.

## Tests

    mvn test

`HabitStatsTest` covers the streak and completion-rate maths.
