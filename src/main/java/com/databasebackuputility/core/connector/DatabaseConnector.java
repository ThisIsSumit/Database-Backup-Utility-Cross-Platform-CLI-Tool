package com.databasebackuputility.core.connector;

import com.databasebackuputility.model.BackupType;
import com.databasebackuputility.model.DatabaseConfig;

import java.io.OutputStream;

/**
 * Interface for database connectors
 * Each database type implements this interface to provide backup/restore functionality
 */
public interface DatabaseConnector {

    /**
     * Test database connection
     * @param config Database configuration
     * @return true if connection successful, false otherwise
     */
    boolean testConnection(DatabaseConfig config);

    /**
     * Execute backup operation
     * @param config Database configuration
     * @param backupType Type of backup (full, incremental, differential)
     * @param outputStream Stream to write backup data
     * @throws Exception if backup fails
     */
    void backup(DatabaseConfig config, BackupType backupType, OutputStream outputStream) throws Exception;

    /**
     * Execute restore operation
     * @param config Database configuration
     * @param backupFilePath Path to backup file
     * @throws Exception if restore fails
     */
    void restore(DatabaseConfig config, String backupFilePath) throws Exception;

    /**
     * Get database size in bytes
     * @param config Database configuration
     * @return Database size in bytes
     * @throws Exception if unable to get size
     */
    long getDatabaseSize(DatabaseConfig config) throws Exception;

    /**
     * Check if incremental backup is supported
     * @return true if supported
     */
    boolean supportsIncrementalBackup();

    /**
     * Check if differential backup is supported
     * @return true if supported
     */
    boolean supportsDifferentialBackup();
}