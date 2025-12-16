package com.databasebackuputility.service;

import com.databasebackuputility.core.connector.DatabaseConnector;
import com.databasebackuputility.model.DatabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

/**
 * Service for database restore operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestoreService {

    private final Map<String, DatabaseConnector> connectors;
    private final CompressionService compressionService;
    private final StorageService storageService;

    /**
     * Restore database from backup file
     */
    public boolean restore(DatabaseConfig config, String backupPath) {
        try {
            log.info("Starting restore operation for database: {}", config.getDatabaseName());

            // Validate configuration
            config.validate();

            // Get connector
            DatabaseConnector connector = getConnector(config);

            // Test connection
            if (!connector.testConnection(config)) {
                log.error("Database connection test failed");
                return false;
            }

            // Retrieve backup file from storage
            File backupFile = storageService.retrieve(backupPath);

            if (!backupFile.exists()) {
                log.error("Backup file not found: {}", backupPath);
                return false;
            }

            // Decompress if needed
            File decompressedFile = backupFile;
            if (backupFile.getName().endsWith(".gz") || backupFile.getName().endsWith(".zip")) {
                log.info("Decompressing backup file...");
                decompressedFile = compressionService.decompress(backupFile);
            }

            // Execute restore
            log.info("Restoring from: {}", decompressedFile.getAbsolutePath());
            connector.restore(config, decompressedFile.getAbsolutePath());

            // Cleanup temporary files
            if (!decompressedFile.equals(backupFile)) {
                if (decompressedFile.delete()) {
                    log.debug("Cleaned up temporary decompressed file");
                }
            }

            log.info("Restore completed successfully");
            return true;

        } catch (Exception e) {
            log.error("Restore operation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validate backup file
     */
    public boolean validateBackup(String backupPath) {
        try {
            File backupFile = new File(backupPath);

            if (!backupFile.exists()) {
                log.error("Backup file does not exist: {}", backupPath);
                return false;
            }

            if (backupFile.length() == 0) {
                log.error("Backup file is empty: {}", backupPath);
                return false;
            }

            log.info("Backup file validated: {} ({} bytes)",
                    backupFile.getName(), backupFile.length());
            return true;

        } catch (Exception e) {
            log.error("Backup validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get database connector
     */
    private DatabaseConnector getConnector(DatabaseConfig config) {
        String connectorName = config.getType().name();
        DatabaseConnector connector = connectors.get(connectorName);

        if (connector == null) {
            throw new IllegalArgumentException(
                    "No connector found for database type: " + config.getType());
        }

        return connector;
    }
}