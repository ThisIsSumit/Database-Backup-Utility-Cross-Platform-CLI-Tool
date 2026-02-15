package com.databasebackuputility.core.connector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.bson.Document;
import org.springframework.stereotype.Component;

import com.databasebackuputility.model.BackupType;
import com.databasebackuputility.model.DatabaseConfig;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import lombok.extern.slf4j.Slf4j;

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
        // Note: NOT using --gzip here. CompressionService will handle compression separately.
        // This ensures proper decompression flow: .archive.gz -> decompress -> .archive -> restore
        ProcessBuilder pb;
        if (config.getUsername() != null && config.getPassword() != null) {
            pb = new ProcessBuilder(
                    "C:\\mongodb-tools\\bin\\mongodump",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--username=" + config.getUsername(),
                    "--password=" + config.getPassword(),
                    "--authenticationDatabase=" + (config.getAuthDatabase() != null ? config.getAuthDatabase() : "admin"),
                    "--db=" + config.getDatabaseName(),
                    "--archive"  // output as uncompressed mongodump archive
            );
        } else {
            pb = new ProcessBuilder(
                    "C:\\mongodb-tools\\bin\\mongodump",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--db=" + config.getDatabaseName(),
                    "--archive"  // output as uncompressed mongodump archive
            );
        }

        // DO NOT use redirectErrorStream(true) - it corrupts the archive by mixing stderr with stdout!
        // stdout = archive data, stderr = progress messages
        Process process = pb.start();

        // Handle stderr in a separate thread to avoid blocking and to log progress
        Thread errorHandler = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("mongodump: {}", line);
                }
            } catch (IOException e) {
                log.warn("Error reading mongodump stderr: {}", e.getMessage());
            }
        });
        errorHandler.start();

        // Pipe ONLY stdout (archive data) to the provided OutputStream
        try (InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        int exitCode = process.waitFor();
        errorHandler.join(); // Wait for error handler to finish
        
        if (exitCode != 0) {
            throw new IOException("mongodump failed with exit code: " + exitCode);
        }

        log.info("MongoDB backup completed successfully");
    }

    @Override
    public void restore(DatabaseConfig config, String backupFilePath) throws Exception {
        log.info("Starting MongoDB restore from: {}", backupFilePath);

        File backupFile = new File(backupFilePath);
        if (!backupFile.exists()) {
            throw new IOException("Backup file not found: " + backupFilePath);
        }

        // Build mongorestore command
        // Note: NOT using --gzip here. RestoreService has already decompressed the file.
        // The backup file at this point is an uncompressed .archive file.
        ProcessBuilder pb;
        String targetDb = config.getDatabaseName(); // target DB
        if (config.getUsername() != null && config.getPassword() != null) {
            pb = new ProcessBuilder(
                    "C:\\mongodb-tools\\bin\\mongorestore",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--username=" + config.getUsername(),
                    "--password=" + config.getPassword(),
                    "--authenticationDatabase=" + (config.getAuthDatabase() != null ? config.getAuthDatabase() : "admin"),
                    "--archive=" + backupFile.getAbsolutePath(),
                    "--drop",
                    "--nsInclude=" + targetDb + ".*"  // map all collections to target DB
            );
        } else {
            pb = new ProcessBuilder(
                    "C:\\mongodb-tools\\bin\\mongorestore",
                    "--host=" + config.getHost(),
                    "--port=" + config.getPort(),
                    "--archive=" + backupFile.getAbsolutePath(),
                    "--drop",
                    "--nsInclude=" + targetDb + ".*"
            );
        }

        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Capture output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
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