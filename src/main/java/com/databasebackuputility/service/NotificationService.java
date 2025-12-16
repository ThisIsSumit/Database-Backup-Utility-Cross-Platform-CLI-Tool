package com.databasebackuputility.service;

import com.databasebackuputility.model.BackupResult;
import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications about backup operations
 */
@Slf4j
@Service
public class NotificationService {

    @Value("${notification.slack.enabled:false}")
    private boolean slackEnabled;

    @Value("${notification.slack.webhook-url:}")
    private String slackWebhookUrl;

    @Value("${notification.slack.channel:#backups}")
    private String slackChannel;

    /**
     * Send backup completion notification
     */
    public void sendBackupNotification(BackupResult result) {
        if (slackEnabled) {
            sendSlackNotification(result);
        }
    }

    /**
     * Send Slack notification
     */
    private void sendSlackNotification(BackupResult result) {
        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            log.debug("Slack webhook URL not configured, skipping notification");
            return;
        }

        try {
            Slack slack = Slack.getInstance();

            String color = result.isSuccess() ? "good" : "danger";
            String status = result.isSuccess() ? "✅ SUCCESS" : "❌ FAILED";

            StringBuilder message = new StringBuilder();
            message.append(String.format("*%s: Database Backup*\n", status));
            message.append(String.format("Database: %s (%s)\n",
                    result.getDatabaseName(), result.getDatabaseType()));
            message.append(String.format("Backup Type: %s\n", result.getBackupType()));
            message.append(String.format("Duration: %s\n", result.getFormattedDuration()));

            if (result.isSuccess()) {
                message.append(String.format("File Size: %s\n", result.getFormattedFileSize()));
                message.append(String.format("Location: %s\n", result.getFilePath()));
            } else {
                message.append(String.format("Error: %s\n", result.getErrorMessage()));
            }

            Payload payload = Payload.builder()
                    .channel(slackChannel)
                    .username("Backup Bot")
                    .iconEmoji(":floppy_disk:")
                    .text(message.toString())
                    .build();

            slack.send(slackWebhookUrl, payload);
            log.info("Slack notification sent successfully");

        } catch (Exception e) {
            log.error("Failed to send Slack notification: {}", e.getMessage());
        }
    }

    /**
     * Send test notification
     */
    public void sendTestNotification() {
        if (!slackEnabled) {
            log.info("Slack notifications are disabled");
            return;
        }

        try {
            Slack slack = Slack.getInstance();

            Payload payload = Payload.builder()
                    .channel(slackChannel)
                    .username("Backup Bot")
                    .iconEmoji(":floppy_disk:")
                    .text("✅ Test notification from Database Backup Utility")
                    .build();

            slack.send(slackWebhookUrl, payload);
            log.info("Test notification sent successfully");

        } catch (Exception e) {
            log.error("Failed to send test notification: {}", e.getMessage());
        }
    }
}