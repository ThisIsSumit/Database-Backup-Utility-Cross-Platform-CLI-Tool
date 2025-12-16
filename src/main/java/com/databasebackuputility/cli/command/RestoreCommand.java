package com.databasebackuputility.cli.command;

import com.databasebackuputility.model.DatabaseConfig;
import com.databasebackuputility.model.DatabaseType;
import com.databasebackuputility.service.RestoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * CLI command for database restore operations
 */
@Component
@Command(
        name = "restore",
        description = "Restore a database from backup",
        mixinStandardHelpOptions = true
)
@RequiredArgsConstructor
public class RestoreCommand implements Callable<Integer> {

    private final RestoreService restoreService;

    @Option(names = {"-b", "--backup-file"}, required = true,
            description = "Path to backup file")
    private String backupFile;

    @Option(names = {"-t", "--type"}, required = true,
            description = "Database type: mysql, postgresql, mongodb, sqlite")
    private String databaseType;

    @Option(names = {"-h", "--host"}, description = "Database host")
    private String host = "localhost";

    @Option(names = {"-p", "--port"}, description = "Database port")
    private Integer port;

    @Option(names = {"-d", "--database"}, required = true,
            description = "Target database name")
    private String databaseName;

    @Option(names = {"-u", "--user"}, description = "Database username")
    private String username;

    @Option(names = {"--password"}, description = "Database password",
            interactive = true, arity = "0..1")
    private String password;

    @Option(names = {"-f", "--file"}, description = "SQLite file path")
    private String filePath;

    @Option(names = {"--validate-only"},
            description = "Only validate backup file without restoring")
    private boolean validateOnly;

    @Override
    public Integer call() {
        try {
            System.out.println("═══════════════════════════════════════════");
            System.out.println("    Database Restore Utility");
            System.out.println("═══════════════════════════════════════════");
            System.out.println();

            // Validate backup file
            System.out.println("Validating backup file...");
            boolean valid = restoreService.validateBackup(backupFile);

            if (!valid) {
                System.err.println("❌ Backup file validation failed!");
                return 1;
            }

            System.out.println("✅ Backup file is valid");

            if (validateOnly) {
                return 0;
            }

            // Build database configuration
            DatabaseConfig config = buildDatabaseConfig();

            // Display restore configuration
            System.out.println();
            System.out.println("Restore Configuration:");
            System.out.println("  Backup File: " + backupFile);
            System.out.println("  Database Type: " + config.getType());
            System.out.println("  Target Database: " + config.getDatabaseName());
            System.out.println("  Host: " + config.getHost());
            System.out.println();

            // Confirm restore
            System.out.println("⚠️  WARNING: This will overwrite the target database!");
            System.out.print("Continue? (yes/no): ");

            Scanner scanner = new Scanner(System.in);
            String confirmation = scanner.nextLine().trim().toLowerCase();

            if (!confirmation.equals("yes")) {
                System.out.println("Restore cancelled.");
                return 0;
            }

            // Execute restore
            System.out.println();
            System.out.println("Starting restore...");
            boolean success = restoreService.restore(config, backupFile);

            System.out.println();
            if (success) {
                System.out.println("✅ Restore completed successfully!");
                return 0;
            } else {
                System.err.println("❌ Restore failed!");
                return 1;
            }

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    /**
     * Build database configuration from CLI options
     */
    private DatabaseConfig buildDatabaseConfig() {
        DatabaseType dbType = DatabaseType.fromString(databaseType);

        DatabaseConfig.DatabaseConfigBuilder builder = DatabaseConfig.builder()
                .type(dbType)
                .databaseName(databaseName);

        if (port == null) {
            port = dbType.getDefaultPort();
        }

        if (dbType == DatabaseType.SQLITE) {
            builder.filePath(filePath != null ? filePath : databaseName + ".db");
        } else {
            builder.host(host)
                    .port(port)
                    .username(username)
                    .password(password);
        }

        return builder.build();
    }
}