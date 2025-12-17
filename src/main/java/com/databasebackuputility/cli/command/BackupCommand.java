package com.databasebackuputility.cli.command;

import com.databasebackuputility.model.BackupResult;
import com.databasebackuputility.model.BackupType;
import com.databasebackuputility.model.DatabaseConfig;
import com.databasebackuputility.model.DatabaseType;
import com.databasebackuputility.service.BackupService;
import com.databasebackuputility.service.CompressionService;
import com.databasebackuputility.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * CLI command for database backup operations
 */
@Component
@Command(
        name = "backup",
        description = "Backup a database",
        mixinStandardHelpOptions = true
)
@RequiredArgsConstructor
public class BackupCommand implements Callable<Integer> {

    private final BackupService backupService;

    @Option(names = {"-t", "--type"}, required = true,
            description = "Database type: mysql, postgresql, mongodb, sqlite")
    private String databaseType;

    @Option(names = {"-h", "--host"}, description = "Database host")
    private String host = "localhost";

    @Option(names = {"-p", "--port"}, description = "Database port")
    private Integer port;

    @Option(names = {"-d", "--database"}, required = true,
            description = "Database name")
    private String databaseName;

    @Option(names = {"-u", "--user"}, description = "Database username")
    private String username;

    @Option(names = {"--password"}, description = "Database password",
            interactive = true, arity = "0..1")
    private String password;

    @Option(names = {"-f", "--file"}, description = "SQLite file path")
    private String filePath;

    @Option(names = {"--backup-type"},
            description = "Backup type: full, incremental, differential")
    private String backupTypeStr = "full";

    @Option(names = {"-c", "--compress"},
            description = "Compression type: none, gzip, zip")
    private String compressionStr = "gzip";

    @Option(names = {"-s", "--storage"},
            description = "Storage provider: local, s3, gcs, azure")
    private String storageStr = "local";

    @Option(names = {"--test-connection"},
            description = "Test database connection only")
    private boolean testConnection;

    @Override
    public Integer call() {
        try {
            System.out.println("═══════════════════════════════════════════");
            System.out.println("    Database Backup Utility");
            System.out.println("═══════════════════════════════════════════");
            System.out.println();

            // Build database configuration
            DatabaseConfig config = buildDatabaseConfig();
            System.out.println(config.toString());

            // Test connection if requested
            if (testConnection) {
                System.out.println("Testing database connection...");
                boolean connected = backupService.testConnection(config);

                if (connected) {
                    System.out.println("✅ Connection successful!");
                    return 0;
                } else {
                    System.err.println("❌ Connection failed!");
                    return 1;
                }
            }

            // Get backup settings
            BackupType backupType = BackupType.valueOf(backupTypeStr.toUpperCase());
            CompressionService.CompressionType compressionType =
                    CompressionService.CompressionType.valueOf(compressionStr.toUpperCase());
            StorageService.StorageProvider storageProvider =
                    StorageService.StorageProvider.valueOf(storageStr.toUpperCase());

            // Display backup configuration
            System.out.println("Backup Configuration:");
            System.out.println("  Database Type: " + config.getType());
            System.out.println("  Database Name: " + config.getDatabaseName());
            System.out.println("  Host: " + config.getHost());
            System.out.println("  Backup Type: " + backupType);
            System.out.println("  Compression: " + compressionType);
            System.out.println("  Storage: " + storageProvider);
            System.out.println();

            // Execute backup
            System.out.println("Starting backup...");
            BackupResult result = backupService.backup(
                    config, backupType, compressionType, storageProvider);

            // Display results
            System.out.println();
            if (result.isSuccess()) {
                System.out.println("✅ Backup completed successfully!");
                System.out.println();
                System.out.println("Backup Details:");
                System.out.println("  Backup ID: " + result.getBackupId());
                System.out.println("  File Path: " + result.getFilePath());
                System.out.println("  File Size: " + result.getFormattedFileSize());
                System.out.println("  Duration: " + result.getFormattedDuration());
                System.out.println("  Start Time: " + result.getStartTime());
                System.out.println("  End Time: " + result.getEndTime());
                return 0;
            } else {
                System.err.println("❌ Backup failed!");
                System.err.println("  Error: " + result.getErrorMessage());
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

        // Set defaults based on database type
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