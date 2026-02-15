package com.databasebackuputility.core.connector;

import com.databasebackuputility.model.BackupType;
import com.databasebackuputility.model.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * PostgreSQL database connector implementation
 * Uses pg_dump for backup and pg_restore for restore
 */
@Slf4j
@Component("POSTGRESQL")
public class PostgreSqlConnector implements DatabaseConnector {

    @Override
    public boolean testConnection(DatabaseConfig config) {
        try (Connection conn = DriverManager.getConnection(
                config.getConnectionUrl(),
                config.getUsername(),
                config.getPassword())) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.error("PostgreSQL connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void backup(DatabaseConfig config, BackupType backupType, OutputStream outputStream) throws Exception {
        log.info("Starting PostgreSQL backup for database: {}", config.getDatabaseName());

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "C:\\Program Files\\PostgreSQL\\17\\bin\\pg_dump",
                "--host=" + config.getHost(),
                "--port=" + config.getPort(),
                "--username=" + config.getUsername(),
                "--format=plain",  // Changed to plain SQL format
                "--no-owner",
                "--no-acl",
                "--verbose",
                "--inserts",  // Use INSERT statements instead of COPY for better compatibility
                config.getDatabaseName()
        );

        // Set password via environment variable
        pb.environment().put("PGPASSWORD", config.getPassword());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("pg_dump failed with exit code: " + exitCode);
        }

        log.info("PostgreSQL backup completed successfully");
    }

    @Override
    public void restore(DatabaseConfig config, String backupFilePath) throws Exception {
        log.info("Starting PostgreSQL restore from: {}", backupFilePath);

        // 1️⃣ Create database if not exists
        createDatabaseIfNotExists(config);
        System.out.println("Database created if it did not exist: " + config.getDatabaseName());

        // Determine if the file is plain SQL format or custom format
        boolean isPlainSql = backupFilePath.endsWith(".sql");
        
        ProcessBuilder pb = new ProcessBuilder();
        
        if (isPlainSql) {
            // Use psql for plain SQL files
            log.info("Detected plain SQL format, using psql for restore");
            pb.command(
                    "C:\\Program Files\\PostgreSQL\\17\\bin\\psql",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--username=" + config.getUsername(),
                    "--dbname=" + config.getDatabaseName(),
                    "--file=" + backupFilePath,
                    "--echo-errors"
            );
        } else {
            // Use pg_restore for custom format files
            log.info("Detected custom format, using pg_restore");
            pb.command(
                    "C:\\Program Files\\PostgreSQL\\17\\bin\\pg_restore",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--username=" + config.getUsername(),
                    "--dbname=" + config.getDatabaseName(),
                    "--clean",
                    "--if-exists",
                    "--verbose",
                    backupFilePath
            );
        }

        pb.environment().put("PGPASSWORD", config.getPassword());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("pg_restore completed with warnings (exit code: {})", exitCode);
        }

        log.info("PostgreSQL restore completed");
    }

    @Override
    public long getDatabaseSize(DatabaseConfig config) throws Exception {
        String query = String.format(
                "SELECT pg_database_size('%s') as size",
                config.getDatabaseName()
        );

        try (Connection conn = DriverManager.getConnection(
                config.getConnectionUrl(),
                config.getUsername(),
                config.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                return rs.getLong("size");
            }
            return 0;
        }
    }

    @Override
    public boolean supportsIncrementalBackup() {
        return true;
    }

    @Override
    public boolean supportsDifferentialBackup() {
        return false;
    }

    /**
     * Creates the target database if it doesn't exist
     * Connects to the default 'postgres' database to execute CREATE DATABASE
     */
    private void createDatabaseIfNotExists(DatabaseConfig config) throws Exception {
        String targetDatabase = config.getDatabaseName();
        
        // Connect to the default 'postgres' database
        String postgresUrl = String.format("jdbc:postgresql://%s:%d/postgres",
                config.getHost(), config.getPort());
        
        try (Connection conn = DriverManager.getConnection(
                postgresUrl,
                config.getUsername(),
                config.getPassword());
             Statement stmt = conn.createStatement()) {
            
            // Check if database exists
            String checkQuery = String.format(
                    "SELECT 1 FROM pg_database WHERE datname = '%s'", targetDatabase);
            
            try (ResultSet rs = stmt.executeQuery(checkQuery)) {
                if (!rs.next()) {
                    // Database doesn't exist, create it using template0 to avoid collation issues
                    log.info("Database '{}' does not exist. Creating it...", targetDatabase);
                    stmt.executeUpdate("CREATE DATABASE " + targetDatabase + " TEMPLATE template0");
                    log.info("Database '{}' created successfully", targetDatabase);
                } else {
                    log.debug("Database '{}' already exists", targetDatabase);
                }
            }
        } catch (Exception e) {
            log.error("Failed to create database '{}': {}", targetDatabase, e.getMessage());
            throw e;
        }
    }
}