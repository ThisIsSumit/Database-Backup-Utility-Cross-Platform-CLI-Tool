package com.databasebackuputility.cli;

import com.databasebackuputility.cli.command.BackupCommand;
import com.databasebackuputility.cli.command.ListCommand;
import com.databasebackuputility.cli.command.RestoreCommand;
import java.util.Arrays;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CliRunner implements CommandLineRunner {

    private final BackupCommand backupCommand;
    private final RestoreCommand restoreCommand;
    private final ListCommand listCommand;

    public CliRunner(BackupCommand backupCommand, RestoreCommand restoreCommand, ListCommand listCommand) {
        this.backupCommand = backupCommand;
        this.restoreCommand = restoreCommand;
        this.listCommand = listCommand;
    }

    @Override
    public void run(String... args) {
        if (args == null || args.length == 0) {
            printHelp();
            return;
        }

        String command = args[0].trim().toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (command) {
            case "backup" -> backupCommand.execute(rest);
            case "restore" -> restoreCommand.execute(rest);
            case "list" -> listCommand.execute(rest);
            case "help" -> printHelp();
            default -> {
                System.err.println("Unknown command: " + command);
                printHelp();
            }
        }
    }

    private void printHelp() {
        System.out.println("""
        database-backup-utility CLI
        Usage:
          backup  <options>
          restore <options>
          list    <options>
          help

        Examples:
          backup  --db=MYSQL --name=mydb
          restore --db=POSTGRESQL --file=backup.zip
          list    --db=MONGODB
        """);
    }
}