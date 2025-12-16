package com.databasebackuputility.core.connector;

import com.databasebackuputility.model.DatabaseConfig;
import com.databasebackuputility.model.DatabaseType;
import org.springframework.stereotype.Component;

@Component
public class MongoDbConnector implements DatabaseConnector {

    @Override
    public DatabaseType supportedType() {
        return DatabaseType.MONGODB;
    }

    @Override
    public String backup(DatabaseConfig config) {
        // TODO implement using mongodump
        return "mongo-dump";
    }

    @Override
    public void restore(DatabaseConfig config, String dumpPath) {
        // TODO implement using mongorestore
    }
}
