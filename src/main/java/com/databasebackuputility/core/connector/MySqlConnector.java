package com.databasebackuputility.core.connector;

import com.databasebackuputility.core.connector.DatabaseConnector;
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
 * MySQL database connector implementation
 * Uses mysqldump for backup and mysql client for restore
 */
@Slf4j
@Component("MYSQL")
public class MySqlConnector implements DatabaseConnector {

    @Override
    public boolean testConnection(DatabaseConfig config) {
        try (Connection conn = DriverManager.getConnection(
                config.getConnectionUrl(),
                config.getUsername(),
                config.getPassword())) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.error("MySQL connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void backup(DatabaseConfig config, BackupType backupType, OutputStream outputStream) throws Exception {
        log.info("Starting MySQL backup for database: {}", config.getDatabaseName());

        // Build mysqldump command
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "mysqldump",
                "--host=" + config.getHost(),
                "--port=" + config.getPort(),
                "--user=" + config.getUsername(),
                "--password=" + config.getPassword(),
                "--single-transaction",
                "--quick",
                "--lock-tables=false",
                "--routines",
                "--triggers",
                config.getDatabaseName()
        );

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Stream output to provided OutputStream
        try (InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("mysqldump failed with exit code: " + exitCode);
        }

        log.info("MySQL backup completed successfully");
    }

    @Override
    public void restore(DatabaseConfig config, String backupFilePath) throws Exception {
        log.info("Starting MySQL restore from: {}", backupFilePath);

        ProcessBuilder pb = new ProcessBuilder();
        pb.command(
                "mysql",
                "--host=" + config.getHost(),
                "--port=" + config.getPort(),
                "--user=" + config.getUsername(),
                "--password=" + config.getPassword(),
                config.getDatabaseName()
        );

        pb.redirectInput(new File(backupFilePath));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Read output/errors
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("MySQL restore failed with exit code: " + exitCode);
        }

        log.info("MySQL restore completed successfully");
    }

    @Override
    public long getDatabaseSize(DatabaseConfig config) throws Exception {
        String query = String.format(
                "SELECT SUM(data_length + index_length) as size " +
                        "FROM information_schema.TABLES " +
                        "WHERE table_schema = '%s'",
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
        return true;
    }
}