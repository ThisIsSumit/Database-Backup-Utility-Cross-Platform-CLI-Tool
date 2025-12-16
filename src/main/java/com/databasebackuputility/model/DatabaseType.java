package com.databasebackuputility.model;

import lombok.Getter;

/**
 * Supported database types
 */
@Getter
public enum DatabaseType {
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", 3306),
    POSTGRESQL("postgresql", "org.postgresql.Driver", 5432),
    MONGODB("mongodb", "mongodb", 27017),
    SQLITE("sqlite", "org.sqlite.JDBC", 0);

    private final String name;
    private final String driverClass;
    private final int defaultPort;

    DatabaseType(String name, String driverClass, int defaultPort) {
        this.name = name;
        this.driverClass = driverClass;
        this.defaultPort = defaultPort;
    }

    public static DatabaseType fromString(String type) {
        for (DatabaseType dbType : DatabaseType.values()) {
            if (dbType.name.equalsIgnoreCase(type)) {
                return dbType;
            }
        }
        throw new IllegalArgumentException("Unsupported database type: " + type);
    }
}