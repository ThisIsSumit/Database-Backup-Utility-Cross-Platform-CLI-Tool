package com.databasebackuputility.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseConfig {

    private DatabaseType type;

    private String host;
    private Integer port;

    private String databaseName;

    private String username;
    private String password; // keep as placeholder; consider secrets management later

    @Builder.Default
    private BackupType backupType = BackupType.COMPRESSED;
}


