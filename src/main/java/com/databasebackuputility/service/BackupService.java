package com.databasebackuputility.service;

import com.databasebackuputility.core.connector.DatabaseConnector;
import com.databasebackuputility.model.BackupResult;
import com.databasebackuputility.model.BackupType;
import com.databasebackuputility.model.DatabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * Main service for backup operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final Map<String, DatabaseConnector> connectors;
    private final CompressionService compressionService;
    private final StorageService storageService;
    private final NotificationService notificationService;

    /**
     * Execute backup operation
     */
    public BackupResult backup(DatabaseConfig config, BackupType backupType,
                               CompressionService.CompressionType compressionType,
                               StorageService.StorageProvider storageProvider) {

        BackupResult result = BackupResult.builder()
                .backupId(UUID.randomUUID().toString())
                .startTime(LocalDateTime.now())
                .backupType(backupType)
                .databaseType(config.getType())
                .databaseName(config.getDatabaseName())
                .build();

        try {
            log.info("Starting backup for database: {} ({})",
                    config.getDatabaseName(), config.getType());

            // Validate configuration
            config.validate();

            // Get appropriate connector
            DatabaseConnector connector = getConnector(config);

            // Test connection
            if (!connector.testConnection(config)) {
                throw new RuntimeException("Database connection test failed");
            }

            // Create temporary backup file
            File tempFile = createTempBackupFile(config);

            // Execute backup
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                connector.backup(config, backupType, fos);
            }

            result.setFileSize(tempFile.length());
            log.info("Backup file created: {} ({} bytes)",
                    tempFile.getName(), tempFile.length());

            // Compress if needed
            File finalFile = tempFile;
            if (compressionType != CompressionService.CompressionType.NONE) {
                finalFile = compressionService.compress(tempFile, compressionType);
                result.setFileSize(finalFile.length());

                // Delete uncompressed file
                if (!tempFile.delete()) {
                    log.warn("Failed to delete temporary file: {}", tempFile.getName());
                }
            }

            // Store backup
            String storagePath = storageService.store(finalFile, storageProvider);
            result.setFilePath(storagePath);

            // Mark as successful
            result.setSuccess(true);
            result.setEndTime(LocalDateTime.now());
            result.calculateDuration();

            log.info("Backup completed successfully in {}", result.getFormattedDuration());

            // Send notification
            notificationService.sendBackupNotification(result);

        } catch (Exception e) {
            log.error("Backup failed: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setEndTime(LocalDateTime.now());
            result.calculateDuration();

            // Send failure notification
            notificationService.sendBackupNotification(result);
        }

        return result;
    }

    /**
     * Test database connection
     */
    public boolean testConnection(DatabaseConfig config) {
        try {
            config.validate();
            DatabaseConnector connector = getConnector(config);
            return connector.testConnection(config);
        } catch (Exception e) {
            log.error("Connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get database connector for the specified type
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

    /**
     * Create temporary backup file
     */
    private File createTempBackupFile(DatabaseConfig config) throws IOException {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String fileName = String.format("%s_%s_%s.sql",
                config.getType().getName(),
                config.getDatabaseName(),
                timestamp);

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "dbbackup");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        return new File(tempDir, fileName);
    }

    /**
     * Get database size
     */
    public long getDatabaseSize(DatabaseConfig config) {
        try {
            DatabaseConnector connector = getConnector(config);
            return connector.getDatabaseSize(config);
        } catch (Exception e) {
            log.error("Failed to get database size: {}", e.getMessage());
            return -1;
        }
    }
}