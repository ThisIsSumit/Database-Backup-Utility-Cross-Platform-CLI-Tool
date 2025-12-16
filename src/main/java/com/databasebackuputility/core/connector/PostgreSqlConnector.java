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
                "pg_dump",
                "--host=" + config.getHost(),
                "--port=" + config.getPort(),
                "--username=" + config.getUsername(),
                "--format=custom",
                "--no-owner",
                "--no-acl",
                "--verbose",
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

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "pg_restore",
                "--host=" + config.getHost(),
                "--port=" + config.getPort(),
                "--username=" + config.getUsername(),
                "--dbname=" + config.getDatabaseName(),
                "--clean",
                "--if-exists",
                "--verbose",
                backupFilePath
        );

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
}