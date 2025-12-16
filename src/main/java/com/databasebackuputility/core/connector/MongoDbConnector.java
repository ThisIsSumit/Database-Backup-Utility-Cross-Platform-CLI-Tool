package com.databasebackuputility.core.connector;

import com.databasebackuputility.model.BackupType;
import com.databasebackuputility.model.DatabaseConfig;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * MongoDB database connector implementation
 * Uses mongodump for backup and mongorestore for restore
 */
@Slf4j
@Component("MONGODB")
public class MongoDbConnector implements DatabaseConnector {

    @Override
    public boolean testConnection(DatabaseConfig config) {
        try (MongoClient client = MongoClients.create(config.getConnectionUrl())) {
            MongoDatabase database = client.getDatabase(config.getDatabaseName());
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            log.error("MongoDB connection test failed: {}", e.getMessage());
            return false;
        }
    }


    @Override
    public void backup(DatabaseConfig config, BackupType backupType, OutputStream outputStream) throws Exception {
        log.info("Starting MongoDB backup for database: {}", config.getDatabaseName());

        // Build mongodump command
        ProcessBuilder pb = new ProcessBuilder();

        if (config.getUsername() != null && config.getPassword() != null) {
            pb.command(
                    "mongodump",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--username=" + config.getUsername(),
                    "--password=" + config.getPassword(),
                    "--authenticationDatabase=" + (config.getAuthDatabase() != null ?
                            config.getAuthDatabase() : "admin"),
                    "--db=" + config.getDatabaseName(),
                    "--archive",
                    "--gzip"
            );
        } else {
            pb.command(
                    "mongodump",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--db=" + config.getDatabaseName(),
                    "--archive",
                    "--gzip"
            );
        }

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
            throw new IOException("mongodump failed with exit code: " + exitCode);
        }

        log.info("MongoDB backup completed successfully");
    }

    @Override
    public void restore(DatabaseConfig config, String backupFilePath) throws Exception {
        log.info("Starting MongoDB restore from: {}", backupFilePath);

        ProcessBuilder pb = new ProcessBuilder();

        if (config.getUsername() != null && config.getPassword() != null) {
            pb.command(
                    "mongorestore",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--username=" + config.getUsername(),
                    "--password=" + config.getPassword(),
                    "--authenticationDatabase=" + (config.getAuthDatabase() != null ?
                            config.getAuthDatabase() : "admin"),
                    "--db=" + config.getDatabaseName(),
                    "--archive=" + backupFilePath,
                    "--gzip",
                    "--drop"
            );
        } else {
            pb.command(
                    "mongorestore",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--db=" + config.getDatabaseName(),
                    "--archive=" + backupFilePath,
                    "--gzip",
                    "--drop"
            );
        }

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
            throw new IOException("mongorestore failed with exit code: " + exitCode);
        }

        log.info("MongoDB restore completed successfully");
    }

    @Override
    public long getDatabaseSize(DatabaseConfig config) throws Exception {
        try (MongoClient client = MongoClients.create(config.getConnectionUrl())) {
            MongoDatabase database = client.getDatabase(config.getDatabaseName());
            Document stats = database.runCommand(new Document("dbStats", 1));
            return stats.getLong("dataSize");
        }
    }

    @Override
    public boolean supportsIncrementalBackup() {
        return false;
    }

    @Override
    public boolean supportsDifferentialBackup() {
        return false;
    }
}