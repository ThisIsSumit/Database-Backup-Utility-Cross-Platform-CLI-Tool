package com.databasebackuputility.service;

import com.databasebackuputility.model.BackupResult;
import com.slack.api.Slack;
import com.slack.api.webhook.Payload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending notifications about backup operations
 */
@Slf4j
@Service
public class NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${notification.slack.enabled:false}")
    private boolean slackEnabled;

    @Value("${notification.slack.webhook-url:}")
    private String slackWebhookUrl;

    @Value("${notification.slack.channel:#backups}")
    private String slackChannel;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:}")
    private String emailFrom;

    @Value("${notification.email.to:}")
    private String emailTo;

    /**
     * Send backup completion notification
     */
    public void sendBackupNotification(BackupResult result) {
        if (slackEnabled) {
            sendSlackNotification(result);
        }
        if (emailEnabled) {
            sendEmailNotification(result);
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
     * Send email notification
     */
    private void sendEmailNotification(BackupResult result) {
        if (mailSender == null) {
            log.debug("Email sender not configured, skipping notification");
            return;
        }

        if (emailFrom == null || emailFrom.isEmpty() || emailTo == null || emailTo.isEmpty()) {
            log.debug("Email from/to not configured, skipping notification");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(emailTo);

            String status = result.isSuccess() ? "SUCCESS" : "FAILED";
            message.setSubject("Database Backup " + status + " - " + result.getDatabaseName());

            StringBuilder body = new StringBuilder();
            body.append("Database Backup Report\n");
            body.append("======================\n\n");
            body.append(String.format("Status: %s\n", status));
            body.append(String.format("Database: %s (%s)\n", result.getDatabaseName(), result.getDatabaseType()));
            body.append(String.format("Backup Type: %s\n", result.getBackupType()));
            body.append(String.format("Duration: %s\n", result.getFormattedDuration()));

            if (result.isSuccess()) {
                body.append(String.format("File Size: %s\n", result.getFormattedFileSize()));
                body.append(String.format("Location: %s\n", result.getFilePath()));
            } else {
                body.append(String.format("\nError: %s\n", result.getErrorMessage()));
            }

            body.append("\n--\nDatabase Backup Utility");
            message.setText(body.toString());

            mailSender.send(message);
            log.info("Email notification sent successfully to: {}", emailTo);

        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage());
        }
    }

    /**
     * Send test notification
     */
    public void sendTestNotification() {
        boolean sentAny = false;

        if (slackEnabled) {
            try {
                Slack slack = Slack.getInstance();

                Payload payload = Payload.builder()
                        .channel(slackChannel)
                        .username("Backup Bot")
                        .iconEmoji(":floppy_disk:")
                        .text("✅ Test notification from Database Backup Utility")
                        .build();

                slack.send(slackWebhookUrl, payload);
                log.info("Slack test notification sent successfully");
                sentAny = true;

            } catch (Exception e) {
                log.error("Failed to send Slack test notification: {}", e.getMessage());
            }
        }

        if (emailEnabled) {
            try {
                if (mailSender == null) {
                    log.warn("Email enabled but mail sender not configured");
                } else if (emailFrom == null || emailFrom.isEmpty() || emailTo == null || emailTo.isEmpty()) {
                    log.warn("Email enabled but from/to addresses not configured");
                } else {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(emailFrom);
                    message.setTo(emailTo);
                    message.setSubject("Test Notification - Database Backup Utility");
                    message.setText("✅ Test notification from Database Backup Utility\n\nThis is a test email to verify your email notification configuration is working correctly.");

                    mailSender.send(message);
                    log.info("Email test notification sent successfully to: {}", emailTo);
                    sentAny = true;
                }
            } catch (Exception e) {
                log.error("Failed to send email test notification: {}", e.getMessage());
            }
        }

        if (!sentAny) {
            log.info("No notifications are enabled or configured");
        }
    }
}