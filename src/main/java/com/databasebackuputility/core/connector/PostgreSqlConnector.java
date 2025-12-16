package com.databasebackuputility.core.connector;

import com.databasebackuputility.model.DatabaseConfig;
import com.databasebackuputility.model.DatabaseType;


import org.springframework.stereotype.Component;


import com.databasebackuputility.model.DatabaseConfig;
import com.databasebackuputility.model.DatabaseType;
import org.springframework.stereotype.Component;

@Component
public class PostgreSqlConnector implements DatabaseConnector {

    @Override
    public DatabaseType supportedType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public String backup(DatabaseConfig config) {
        // TODO implement using pg_dump
        return "postgres-dump.sql";
    }

    @Override
    public void restore(DatabaseConfig config, String dumpPath) {
        // TODO implement using psql/pg_restore
    }
}

