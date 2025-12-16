package com.databasebackuputility.cli.command;

import com.databasebackuputility.model.DatabaseConfig;
import com.databasebackuputility.model.DatabaseType;
import org.springframework.stereotype.Component;


import com.databasebackuputility.service.RestoreService;

@Component
public class RestoreCommand {

    private final RestoreService restoreService;

    public RestoreCommand(RestoreService restoreService) {
        this.restoreService = restoreService;
    }

    public void execute(String[] args) {
        // TODO parse args properly (e.g., --db, --file, --targetDb, etc.)
        DatabaseConfig config = DatabaseConfig.builder()
                .type(DatabaseType.POSTGRESQL)
                .databaseName("example")
                .build();

        String backupFilePath = "backup.zip";
        restoreService.restore(config, backupFilePath);
        System.out.println("Restore finished.");
    }
}

