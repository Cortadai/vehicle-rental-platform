-- Idempotent init script: creates schemas and dedicated users for each service
-- Safe for repeated container restarts with persisted volumes

-- Create schemas
CREATE SCHEMA IF NOT EXISTS reservation;
CREATE SCHEMA IF NOT EXISTS customer;
CREATE SCHEMA IF NOT EXISTS payment;
CREATE SCHEMA IF NOT EXISTS fleet;

-- Create dedicated users with schema-restricted permissions
DO $$
BEGIN
    -- reservation_user
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'reservation_user') THEN
        CREATE ROLE reservation_user WITH LOGIN PASSWORD 'reservation_pass';
    END IF;
    GRANT USAGE, CREATE ON SCHEMA reservation TO reservation_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA reservation TO reservation_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA reservation GRANT ALL ON TABLES TO reservation_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA reservation GRANT ALL ON SEQUENCES TO reservation_user;
    ALTER ROLE reservation_user SET search_path TO reservation, public;

    -- customer_user
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'customer_user') THEN
        CREATE ROLE customer_user WITH LOGIN PASSWORD 'customer_pass';
    END IF;
    GRANT USAGE, CREATE ON SCHEMA customer TO customer_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA customer TO customer_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA customer GRANT ALL ON TABLES TO customer_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA customer GRANT ALL ON SEQUENCES TO customer_user;
    ALTER ROLE customer_user SET search_path TO customer, public;

    -- payment_user
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'payment_user') THEN
        CREATE ROLE payment_user WITH LOGIN PASSWORD 'payment_pass';
    END IF;
    GRANT USAGE, CREATE ON SCHEMA payment TO payment_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA payment TO payment_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA payment GRANT ALL ON TABLES TO payment_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA payment GRANT ALL ON SEQUENCES TO payment_user;
    ALTER ROLE payment_user SET search_path TO payment, public;

    -- fleet_user
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'fleet_user') THEN
        CREATE ROLE fleet_user WITH LOGIN PASSWORD 'fleet_pass';
    END IF;
    GRANT USAGE, CREATE ON SCHEMA fleet TO fleet_user;
    GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA fleet TO fleet_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA fleet GRANT ALL ON TABLES TO fleet_user;
    ALTER DEFAULT PRIVILEGES IN SCHEMA fleet GRANT ALL ON SEQUENCES TO fleet_user;
    ALTER ROLE fleet_user SET search_path TO fleet, public;
END
$$;
