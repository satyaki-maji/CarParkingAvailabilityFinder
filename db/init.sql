-- Enable PostGIS extension for spatial queries
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. Create the static car park table
CREATE TABLE IF NOT EXISTS carpark (
    carpark_number VARCHAR(10) PRIMARY KEY,
    address VARCHAR(255),
    x_coord NUMERIC,
    y_coord NUMERIC,
    car_park_type VARCHAR(100),
    type_of_parking_system VARCHAR(100),
    short_term_parking VARCHAR(100),
    free_parking VARCHAR(100),
    night_parking VARCHAR(50),
    car_park_decks INTEGER,
    gantry_height NUMERIC,
    car_park_basement VARCHAR(5),
    -- Spatial column for fast proximity searching (WGS 84 / Lat Long)
    location GEOGRAPHY(Point, 4326)
);

-- 2. Create a staging table to ingest the raw CSV
CREATE TEMP TABLE carpark_staging (
    car_park_no VARCHAR(10),
    address VARCHAR(255),
    x_coord NUMERIC,
    y_coord NUMERIC,
    car_park_type VARCHAR(100),
    type_of_parking_system VARCHAR(100),
    short_term_parking VARCHAR(100),
    free_parking VARCHAR(100),
    night_parking VARCHAR(50),
    car_park_decks INTEGER,
    gantry_height NUMERIC,
    car_park_basement VARCHAR(5)
);

-- 3. Load CSV data into the staging table
COPY carpark_staging(car_park_no, address, x_coord, y_coord, car_park_type, type_of_parking_system, short_term_parking, free_parking, night_parking, car_park_decks, gantry_height, car_park_basement)
FROM '/docker-entrypoint-initdb.d/HDBCarparkInformation.csv'
DELIMITER ','
CSV HEADER;

-- 4. Insert into the main table and transform SVY21 (3414) to WGS84 (4326)
INSERT INTO carpark (
    carpark_number, address, x_coord, y_coord, car_park_type, type_of_parking_system,
    short_term_parking, free_parking, night_parking, car_park_decks, gantry_height, car_park_basement, location
)
SELECT
    car_park_no, address, x_coord, y_coord, car_park_type, type_of_parking_system,
    short_term_parking, free_parking, night_parking, car_park_decks, gantry_height, car_park_basement,
    -- Transform SVY21 (EPSG:3414) to WGS84 (EPSG:4326)
    ST_Transform(ST_SetSRID(ST_MakePoint(x_coord, y_coord), 3414), 4326)::geography
FROM carpark_staging
ON CONFLICT (carpark_number) DO NOTHING;

-- 5. Create spatial index for fast nearest-neighbor queries
CREATE INDEX idx_carpark_location ON carpark USING GIST (location);

-- 6. Create the dynamic availability table
CREATE TABLE IF NOT EXISTS carpark_availability (
    carpark_number VARCHAR(10) REFERENCES carpark(carpark_number),
    lot_type VARCHAR(5),
    total_lots INTEGER,
    lots_available INTEGER,
    update_datetime TIMESTAMP,
    PRIMARY KEY (carpark_number, lot_type)
);

-- Keeps existing named Docker volumes compatible with the availability freshness checks.
ALTER TABLE carpark_availability
    ADD COLUMN IF NOT EXISTS last_sync_time TIMESTAMP;

-- The live feed includes valid carparks that are absent from the static HDB dataset.
ALTER TABLE carpark_availability
    DROP CONSTRAINT IF EXISTS carpark_availability_carpark_number_fkey;

-- 7. Create index on availability filtering attributes
CREATE INDEX idx_carpark_availability_lots ON carpark_availability (lot_type, lots_available);
