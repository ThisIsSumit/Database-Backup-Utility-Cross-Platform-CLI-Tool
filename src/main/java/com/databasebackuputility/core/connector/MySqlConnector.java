package com.databasebackuputility.core.connector;

import com.databasebackuputility.model.DatabaseConfig;
import com.databasebackuputility.model.DatabaseType;
import org.springframework.stereotype.Component;

@Component
public class MySqlConnector implements DatabaseConnector {

    @Override
    public DatabaseType supportedType() {
        return DatabaseType.MYSQL;
    }

    @Override
    public String backup(DatabaseConfig config) {
        // TODO implement using mysqldump or native driver export approach
        return "mysql-dump.sql";
    }

    @Override
    public void restore(DatabaseConfig config, String dumpPath) {
        // TODO implement using mysql client import
    }
}
