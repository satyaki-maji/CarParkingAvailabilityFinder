# Singapore Car Park Availability Finder

This project provides a Spring Boot/PostGIS backend and a Streamlit UI for finding nearby Singapore car parks with available lots.

## Prerequisites

- Docker Desktop (or Docker Engine with Docker Compose)
- Ports `5432`, `8080`, and `8501` available on the host

Run the commands below from the project directory containing `docker-compose.yml`.

## Start the application

Build the images and start PostgreSQL, the backend, and the UI:

```bash
docker compose up -d --build
```

Check service status:

```bash
docker compose ps
```

The first build downloads dependencies and compiles the Java application inside Docker, so a local `app/target` directory or pre-built JAR is not required.

## Open the application

- UI: [http://localhost:8501](http://localhost:8501)
- Backend: [http://localhost:8080](http://localhost:8080)
- Nearby car parks endpoint: `GET http://localhost:8080/api/v1/carparks/nearby`

The UI lets you choose coordinates from the map, configure radius/lot type/limit, view results on a map and table, inspect the raw API JSON, and select the timezone used to display timestamps.

## PostgreSQL connection

Use these settings from an external database client such as DBeaver, DataGrip, or pgAdmin:

| Setting | Value |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `parking` |
| Username | `admin` |
| Password | `password` |
| JDBC URL | `jdbc:postgresql://localhost:5432/parking` |

Example using `psql`:

```bash
psql "postgresql://admin:password@localhost:5432/parking"
```

From another Compose service, use host `db` instead of `localhost`.

## Test with curl or Postman

Example request (Singapore city-centre coordinates):

```bash
curl "http://localhost:8080/api/v1/carparks/nearby?latitude=1.3521&longitude=103.8198&radiusMeters=2000&lotType=C&limit=20"
```

In Postman, create a `GET` request to:

```text
http://localhost:8080/api/v1/carparks/nearby
```

Add these query parameters:

| Key | Example | Required |
|---|---:|---|
| `latitude` | `1.3521` | Yes |
| `longitude` | `103.8198` | Yes |
| `radiusMeters` | `2000` | No (default `2000`) |
| `lotType` | `C` | No |
| `limit` | `20` | No (default `20`) |

The response contains nearby car-park details, available/total lots, distance, coordinates, `lastSyncTime`, and staleness information when live ingestion is delayed.

## Logs and shutdown

Follow logs for a service:

```bash
docker compose logs -f app
docker compose logs -f db
docker compose logs -f ui
```

Stop containers while keeping the database volume:

```bash
docker compose down
```

Stop containers and delete the PostgreSQL volume (this removes all database data and causes `db/init.sql` to run again on the next start):

```bash
docker compose down -v --remove-orphans
docker compose up -d --build
```
