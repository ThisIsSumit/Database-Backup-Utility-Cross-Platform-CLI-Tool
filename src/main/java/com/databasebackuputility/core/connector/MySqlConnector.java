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
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/",
                config.getUsername(),
                config.getPassword())) {
            return true;
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
                "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump",
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

//        pb.redirectErrorStream(true);
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

        // 1️⃣ Create database if not exists
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/",
                config.getUsername(),
                config.getPassword())) {

            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + config.getDatabaseName());
        }

        log.info("Starting MySQL restore from: {}", backupFilePath);

        // 2️⃣ If file is gz, decompress first to temp file
        File sqlFile;

        if (backupFilePath.endsWith(".gz")) {
            sqlFile = File.createTempFile("mysql_restore_", ".sql");

            try (InputStream gis = new java.util.zip.GZIPInputStream(
                    new FileInputStream(backupFilePath));
                 FileOutputStream fos = new FileOutputStream(sqlFile)) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = gis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }
        } else {
            sqlFile = new File(backupFilePath);
        }

        // 2️⃣a Clean dump file from mysqldump warnings
        File cleanedFile = File.createTempFile("mysql_restore_cleaned_", ".sql");
        try (BufferedReader reader = new BufferedReader(new FileReader(sqlFile));
             PrintWriter writer = new PrintWriter(new FileWriter(cleanedFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("mysqldump:")) {  // skip warnings
                    writer.println(line);
                }
            }
        }

        // Use cleaned file for restore
        sqlFile = cleanedFile;

        // 3️⃣ Use redirectInput (NO manual pipe writing)
        ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql",
                "--host=" + config.getHost(),
                "--port=" + config.getPort(),
                "--user=" + config.getUsername(),
                "--password=" + config.getPassword(),
                config.getDatabaseName()
        );

        pb.redirectInput(sqlFile);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 4️⃣ Read MySQL output (IMPORTANT)
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                log.error(line);   // show real MySQL errors
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