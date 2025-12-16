package com.databasebackuputility.cli.command;

import com.databasebackuputility.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.concurrent.Callable;

/**
 * CLI command to list available backups
 */
@Component
@Command(
        name = "list",
        description = "List available backups",
        mixinStandardHelpOptions = true
)
@RequiredArgsConstructor
public class ListCommand implements Callable<Integer> {

    private final StorageService storageService;

    @Override
    public Integer call() {
        try {
            System.out.println("═══════════════════════════════════════════");
            System.out.println("    Available Backups");
            System.out.println("═══════════════════════════════════════════");
            System.out.println();

            File[] backups = storageService.listBackups();

            if (backups == null || backups.length == 0) {
                System.out.println("No backups found.");
                return 0;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            System.out.printf("%-50s %-15s %-20s%n", "Backup File", "Size", "Created");
            System.out.println("─".repeat(85));

            for (File backup : backups) {
                BasicFileAttributes attrs = Files.readAttributes(
                        backup.toPath(), BasicFileAttributes.class);

                String size = formatFileSize(backup.length());
                String created = dateFormat.format(attrs.creationTime().toMillis());

                System.out.printf("%-50s %-15s %-20s%n",
                        truncate(backup.getName(), 50),
                        size,
                        created);
            }

            System.out.println();
            System.out.println("Total backups: " + backups.length);

            return 0;

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            return 1;
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024)
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
}