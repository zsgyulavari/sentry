RUN '006-SENTRY-711.derby.sql';
RUN '007-SENTRY-1365.derby.sql';
RUN '008-SENTRY-1569.derby.sql';

-- Version update
UPDATE SENTRY_VERSION SET SCHEMA_VERSION='1.8.0', VERSION_COMMENT='Sentry release version 1.8.0' WHERE VER_ID=1;
