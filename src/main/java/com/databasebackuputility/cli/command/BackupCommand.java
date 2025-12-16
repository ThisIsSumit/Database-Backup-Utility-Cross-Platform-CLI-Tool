package com.databasebackuputility.cli.command;

import com.databasebackuputility.model.BackupResult;
import com.databasebackuputility.model.DatabaseConfig;
import com.databasebackuputility.model.DatabaseType;
import com.databasebackuputility.service.BackupService;
import org.springframework.stereotype.Component;

@Component
public class BackupCommand {

    private final BackupService backupService;

    public BackupCommand(BackupService backupService) {
        this.backupService = backupService;
    }

    public void execute(String[] args) {
        // TODO parse args properly (e.g., --db, --host, --port, --name, --user, --password, --backupType, etc.)
        DatabaseConfig config = DatabaseConfig.builder()
                .type(DatabaseType.MYSQL)
                .databaseName("example")
                .build();

        BackupResult result = backupService.backup(config);
        System.out.println("Backup finished: " + result);
    }
}
