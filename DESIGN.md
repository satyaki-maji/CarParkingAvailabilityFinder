# Architecture Overview

The solution is built as a single, multi-threaded Spring Boot service (Monolith) integrating with a PostgreSQL database equipped with the PostGIS spatial extension. This architecture explicitly balances the requirement for fast, accurate proximity sorting with the need for graceful degradation when external dependencies fail.

## Core Design Decisions

### 1. Database & Proximity Search (PostGIS)
We selected PostgreSQL with PostGIS over an in-memory store like Redis. PostGIS enables atomic queries that combine geospatial proximity sorting (using the `<->` nearest-neighbor operator and `ST_DWithin`) with standard multi-attribute filtering (`lots_available > 0` and `lot_type`).
*   **Coordinate Transformation:** The static dataset provides locations in SVY21 format (EPSG:3414). This is transformed natively into WGS84 (EPSG:4326) during the initial `COPY` command in the `init.sql` script to match standard user latitude/longitude inputs.

### 2. Data Ingestion (Background Scheduler)
Ingesting the live API synchronously during a client request introduces severe latency and risks rate-limiting bans. Instead, data ingestion is fully decoupled:
*   A background `@Scheduled` task polls the `/transport/carpark-availability` API every 60 seconds.
*   We utilize Spring Data JDBC `batchUpdate` to execute highly efficient `INSERT ... ON CONFLICT DO UPDATE` upserts, avoiding standard ORM memory overhead for bulk processing.

### 3. Resilience & Graceful Degradation
The live API mirrors real-world partner integrations: it is prone to failure, slow responses, or returning anomalous data.
*   **Decoupled Foreign Keys:** We discovered ~10 orphaned car parks in the live API that do not exist in the static dataset. We removed the strict Foreign Key constraint on the `carpark_availability` table to ensure bulk batch upserts do not trigger constraint violations and fail. Unmatched records are naturally filtered out via `INNER JOIN` on reads.
*   **Pipeline Health Tracking (`last_sync_time`):** The `carpark_availability` schema includes a `last_sync_time` column. If the live API fails or times out, the background scheduler catches the exception and leaves the database intact.
*   **Staleness Flag:** The client API reads the `last_sync_time`. If the most recent record is older than 5 minutes, it serves the last known state but appends a `warning` flag to the payload, degrading gracefully while maintaining transparency with the user.

## Trade-offs & Future Evolution

*   **Monolith vs. Microservices:** To fit the 6-8 hour time constraint, the ingestion scheduler and the client API were combined into a single JVM deployment. Given more time or at a larger scale, these should be decoupled into separate deployments. This ensures a memory leak or crash in the background ingestion worker cannot compromise the client-facing API's availability.
*   **Static Data Refresh:** Currently, static car park data is loaded via a CSV file on Docker startup. In a production evolution, a daily scheduled job would hit the official Datastore Search API to pull and upsert physical infrastructure changes dynamically, keeping the base dataset perfectly aligned with the live sensors.