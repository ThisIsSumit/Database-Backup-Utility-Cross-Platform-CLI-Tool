package com.databasebackuputility.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of a backup operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupResult {

    private boolean success;
    private String backupId;
    private String filePath;
    private long fileSize;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long durationMs;
    private String errorMessage;
    private BackupType backupType;
    private DatabaseType databaseType;
    private String databaseName;

    /**
     * Calculate duration from start and end time
     */
    public void calculateDuration() {
        if (startTime != null && endTime != null) {
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    /**
     * Get formatted file size
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    /**
     * Get formatted duration
     */
    public String getFormattedDuration() {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }
}