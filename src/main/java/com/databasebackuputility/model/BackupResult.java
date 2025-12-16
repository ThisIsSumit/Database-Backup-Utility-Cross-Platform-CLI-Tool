package com.databasebackuputility.model;


import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupResult {

    private boolean success;

    private DatabaseType databaseType;

    private String artifactPath;

    private Instant startedAt;
    private Instant finishedAt;

    private String message;
}