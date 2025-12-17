package com.databasebackuputility.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Database connection configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfig {

    private DatabaseType type;
    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;
    private String authDatabase; // For MongoDB
    private String sslMode; // For PostgreSQL
    private String filePath; // For SQLite

    /**
     * Build JDBC connection URL
     */
    public String getConnectionUrl() {
        switch (type) {
            case MYSQL:
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                        host, port, databaseName);

            case POSTGRESQL:
                String ssl = sslMode != null ? "?sslmode=" + sslMode : "";
                return String.format("jdbc:postgresql://%s:%d/%s%s",
                        host, port, databaseName, ssl);

            case SQLITE:
                return "jdbc:sqlite:" + filePath;

            case MONGODB:
                String authDb = authDatabase != null ? authDatabase : "admin";
                if (username != null && password != null) {
                    return String.format("mongodb://%s:%s@%s:%d/%s?authSource=%s",
                            username, password, host, port, databaseName, authDb);
                }
                return String.format("mongodb://%s:%d/%s", host, port, databaseName);

            default:
                throw new IllegalStateException("Unsupported database type: " + type);
        }
    }

    /**
     * Validate configuration
     */
    public void validate() {
        if (type == null) {
            throw new IllegalArgumentException("Database type is required");
        }

        if (type == DatabaseType.SQLITE) {
            if (filePath == null || filePath.isEmpty()) {
                throw new IllegalArgumentException("File path is required for SQLite");
            }
        } else {
            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Host is required");
            }
            if (databaseName == null || databaseName.isEmpty()) {
                throw new IllegalArgumentException("Database name is required");
            }
        }
    }
}