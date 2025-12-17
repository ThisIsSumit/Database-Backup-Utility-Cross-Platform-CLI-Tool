package com.databasebackuputility.cli;

import com.databasebackuputility.cli.command.BackupCommand;
import com.databasebackuputility.cli.command.ListCommand;
import com.databasebackuputility.cli.command.RestoreCommand;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main CLI runner that coordinates all commands
 */
@Component
@Command(
        name = "dbbackup",
        description = "Database Backup and Restore Utility",
        version = "1.0.0",
        mixinStandardHelpOptions = true,
        subcommands = {
                BackupCommand.class,
                RestoreCommand.class,
                ListCommand.class,
                CommandLine.HelpCommand.class
        }
)
public class CliRunner implements CommandLineRunner, ExitCodeGenerator {

    private final CommandLine.IFactory factory;
    private int exitCode;

    public CliRunner(@Qualifier("picocliSpringFactoryImpl") CommandLine.IFactory factory) {
        this.factory = factory;
    }

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            printBanner();
            args = new String[]{"--help"};
        }

        CommandLine cmd = new CommandLine(this, factory);
        cmd.setExecutionExceptionHandler(this::handleExecutionException);

        exitCode = cmd.execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Handle execution exceptions
     */
    private int handleExecutionException(Exception ex, CommandLine cmd,
                                         CommandLine.ParseResult parseResult) {
        cmd.getErr().println(cmd.getColorScheme().errorText("Error: " + ex.getMessage()));

        if (cmd.getCommandSpec().findOption("--verbose") != null) {
            ex.printStackTrace(cmd.getErr());
        }

        return cmd.getExitCodeExceptionMapper() != null
                ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : cmd.getCommandSpec().exitCodeOnExecutionException();
    }

    /**
     * Print application banner
     */
    private void printBanner() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                           ║");
        System.out.println("║       Database Backup & Restore Utility v1.0.0           ║");
        System.out.println("║                                                           ║");
        System.out.println("║   Supports: MySQL, PostgreSQL, MongoDB, SQLite           ║");
        System.out.println("║   Features: Compression, Cloud Storage, Scheduling       ║");
        System.out.println("║                                                           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}