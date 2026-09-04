# MarketPulse Backend

Spring Boot API for the MarketPulse smart watchlist. The backend persists users, watchlists, market snapshots, change events, and checkpoints, then ranks what changed since the last meaningful check.

## Stack

- Java 21 / Spring Boot 3.5
- Spring Security + JWT + BCrypt
- Spring Data JPA / Hibernate
- PostgreSQL (production) or H2 (local `dev` profile)
- JUnit 5 + MockMvc

## Run locally

The default `dev` profile uses a file-based H2 database so you can start without Docker.

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080`  
Health: `GET /api/health`  
H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/marketpulse`)

## PostgreSQL (optional)

```powershell
docker compose up -d postgres
$env:SPRING_PROFILES_ACTIVE="prod"
.\mvnw.cmd spring-boot:run
```

## Environment

| Variable | Purpose | Default |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` / `prod` / `test` | `dev` |
| `JWT_SECRET` | HMAC secret (≥ 32 chars) | dev-only default |
| `CORS_ORIGINS` | Allowed frontend origins | `http://localhost:5173` |
| `MARKET_DATA_PROVIDER` | `mock` or `finnhub` | `mock` |
| `MARKET_DATA_API_KEY` | Finnhub token | empty |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Postgres (`prod`) | local defaults |

Never commit real secrets. Override via environment variables.

## Architecture

Controllers → services → repositories. External quotes go through `MarketDataProvider`, so Finnhub can replace the mock provider without touching change detection.

Checkpoint updates happen only after a successful review (`POST /api/checkpoints`), not on dashboard load.

## Tests

```powershell
.\mvnw.cmd test
```
