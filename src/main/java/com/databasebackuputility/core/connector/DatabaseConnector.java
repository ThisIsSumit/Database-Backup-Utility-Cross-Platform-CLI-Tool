package com.databasebackuputility.core.connector;

import com.databasebackuputility.model.DatabaseConfig;
import com.databasebackuputility.model.DatabaseType;

import java.util.List;

public interface DatabaseConnector {

    DatabaseType supportedType();

    /**
     * Perform a database dump and return a path to a produced dump file (or directory).
     */
    String backup(DatabaseConfig config);

    /**
     * Restore the database from a dump file (or directory).
     */
    void restore(DatabaseConfig config, String dumpPath);

    /**
     * Optional: list logical backups known to the database system itself.
     * Many implementations may return an empty list and rely on StorageService instead.
     */
    default List<String> listInternalBackups(DatabaseConfig config) {
        return List.of();
    }
}
