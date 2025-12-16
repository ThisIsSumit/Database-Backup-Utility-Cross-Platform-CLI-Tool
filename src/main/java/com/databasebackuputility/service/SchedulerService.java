package com.databasebackuputility.service;

import com.databasebackuputility.model.BackupType;
import com.databasebackuputility.model.DatabaseConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Service for scheduling automated backups using Quartz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final Scheduler scheduler;

    /**
     * Schedule a backup job
     */
    public void scheduleBackup(String jobName, DatabaseConfig config,
                               BackupType backupType, String cronExpression) throws SchedulerException {

        log.info("Scheduling backup job: {} with cron: {}", jobName, cronExpression);

        // Create job detail
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("databaseConfig", config);
        jobDataMap.put("backupType", backupType);

        JobDetail jobDetail = newJob(ScheduledBackupJob.class)
                .withIdentity(jobName, "backup-jobs")
                .usingJobData(jobDataMap)
                .build();

        // Create trigger with cron schedule
        Trigger trigger = newTrigger()
                .withIdentity(jobName + "-trigger", "backup-triggers")
                .withSchedule(cronSchedule(cronExpression))
                .build();

        // Schedule the job
        scheduler.scheduleJob(jobDetail, trigger);

        log.info("Backup job scheduled successfully: {}", jobName);
    }

    /**
     * Remove scheduled backup job
     */
    public void unscheduleBackup(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, "backup-jobs");

        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
            log.info("Backup job removed: {}", jobName);
        } else {
            log.warn("Backup job not found: {}", jobName);
        }
    }

    /**
     * List all scheduled jobs
     */
    public void listScheduledJobs() throws SchedulerException {
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                String jobName = jobKey.getName();
                String jobGroup = jobKey.getGroup();

                log.info("Job: {}.{}", jobGroup, jobName);

                // Get triggers for this job
                for (Trigger trigger : scheduler.getTriggersOfJob(jobKey)) {
                    log.info("  Trigger: {}", trigger.getKey());
                    log.info("  Next Fire Time: {}", trigger.getNextFireTime());
                }
            }
        }
    }

    /**
     * Pause backup job
     */
    public void pauseJob(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, "backup-jobs");
        scheduler.pauseJob(jobKey);
        log.info("Job paused: {}", jobName);
    }

    /**
     * Resume backup job
     */
    public void resumeJob(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, "backup-jobs");
        scheduler.resumeJob(jobKey);
        log.info("Job resumed: {}", jobName);
    }

    /**
     * Trigger job manually
     */
    public void triggerJob(String jobName) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey(jobName, "backup-jobs");
        scheduler.triggerJob(jobKey);
        log.info("Job triggered manually: {}", jobName);
    }
}

