package com.databasebackuputility.service;

import com.databasebackuputility.model.BackupType;
import com.databasebackuputility.model.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Quartz job for scheduled backups
 */
@Slf4j
class ScheduledBackupJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();

        DatabaseConfig config = (DatabaseConfig) dataMap.get("databaseConfig");
        BackupType backupType = (BackupType) dataMap.get("backupType");

        log.info("Executing scheduled backup for database: {}", config.getDatabaseName());

        // Get BackupService from application context
        try {
            BackupService backupService = (BackupService) context.getScheduler()
                    .getContext().get("backupService");

            if (backupService != null) {
                backupService.backup(
                        config,
                        backupType,
                        CompressionService.CompressionType.GZIP,
                        StorageService.StorageProvider.LOCAL
                );
            } else {
                log.error("BackupService not found in scheduler context");
            }
        } catch (Exception e) {
            log.error("Scheduled backup failed: {}", e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }
}
