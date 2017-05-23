CREATE database "odk_db";
SELECT datname FROM pg_database WHERE datistemplate = false;
CREATE USER "odk_db" WITH ENCRYPTED PASSWORD 'odk_db';
GRANT ALL PRIVILEGES ON DATABASE "odk_db" to "odk_db";
ALTER DATABASE "odk_db" OWNER TO "odk_db";
\c "odk_db";
CREATE SCHEMA "odk_db";
GRANT ALL PRIVILEGES ON SCHEMA "odk_db" to "odk_db";