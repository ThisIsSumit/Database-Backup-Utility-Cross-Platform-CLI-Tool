package com.databasebackuputility.cli.command;

import com.databasebackuputility.model.DatabaseType;
import com.databasebackuputility.service.StorageService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListCommand {

    private final StorageService storageService;

    public ListCommand(StorageService storageService) {
        this.storageService = storageService;
    }

    public void execute(String[] args) {
        // TODO parse args (e.g., --db)
        DatabaseType type = DatabaseType.MYSQL;

        List<String> backups = storageService.listBackups(type);
        if (backups.isEmpty()) {
            System.out.println("No backups found.");
            return;
        }

        backups.forEach(System.out::println);
    }
}
