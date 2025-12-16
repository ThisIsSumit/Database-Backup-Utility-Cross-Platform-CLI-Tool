package com.databasebackuputility.model;

/**
 * Types of backup strategies
 */
public enum BackupType {
    FULL,           // Complete database backup
    INCREMENTAL,    // Only changes since last backup
    DIFFERENTIAL    // Changes since last full backup
}