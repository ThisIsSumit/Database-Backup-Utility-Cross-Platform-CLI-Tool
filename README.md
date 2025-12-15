# Database Backup Utility

A comprehensive command-line tool for backing up and restoring multiple database types with support for compression, cloud storage, and automated scheduling.

## Features

✅ **Multi-Database Support**
- MySQL
- PostgreSQL
- MongoDB
- SQLite

✅ **Backup Types**
- Full backup
- Incremental backup (MySQL, PostgreSQL)
- Differential backup (MySQL)

✅ **Compression**
- GZIP compression
- ZIP compression
- No compression option

✅ **Storage Options**
- Local filesystem
- AWS S3
- Google Cloud Storage
- Azure Blob Storage

✅ **Additional Features**
- Automated scheduling with cron expressions
- Slack notifications
- Comprehensive logging
- Backup encryption
- Connection testing
- Backup validation

## Prerequisites

### Required Software

1. **Java 17+**
   ```bash
   java -version
   ```

2. **Maven 3.8+**
   ```bash
   mvn -version
   ```

3. **Database CLI Tools**
   - MySQL: `mysqldump` and `mysql`
   - PostgreSQL: `pg_dump` and `pg_restore`
   - MongoDB: `mongodump` and `mongorestore`

### Installing Database CLI Tools

**Ubuntu/Debian:**
```bash
# MySQL
sudo apt-get install mysql-client

# PostgreSQL
sudo apt-get install postgresql-client

# MongoDB
sudo apt-get install mongodb-database-tools
```

**macOS:**
```bash
# MySQL
brew install mysql-client

# PostgreSQL
brew install postgresql

# MongoDB
brew install mongodb-database-tools
```

**Windows:**
- Download and install from official websites
- Add to PATH environment variable

## Installation

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/database-backup-utility.git
cd database-backup-utility
```

### 2. Build the Project
```bash
mvn clean package
```

This creates an executable JAR file in `target/database-backup-utility-1.0.0.jar`

### 3. Create Alias (Optional)
```bash
# Linux/macOS
echo 'alias dbbackup="java -jar /path/to/database-backup-utility-1.0.0.jar"' >> ~/.bashrc
source ~/.bashrc

# Windows (PowerShell)
Set-Alias -Name dbbackup -Value "java -jar C:\path\to\database-backup-utility-1.0.0.jar"
```

## Configuration

### Application Configuration

Edit `application.yml` or create `application-prod.yml`:

```yaml
backup:
  default-output-dir: /backups
  compression:
    enabled: true
    default-format: gzip

storage:
  default-provider: local
  local:
    base-path: /var/backups/database
  
  s3:
    enabled: true
    bucket-name: my-backup-bucket
    region: us-east-1

notification:
  slack:
    enabled: true
    webhook-url: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
    channel: #database-backups
```

### Environment Variables

```bash
# AWS S3
export AWS_ACCESS_KEY=your_access_key
export AWS_SECRET_KEY=your_secret_key
export AWS_S3_BUCKET=my-backup-bucket

# Slack
export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...

# Encryption
export ENCRYPTION_KEY=your-32-character-encryption-key
```

## Usage

### Basic Commands

#### 1. Backup Database

**MySQL:**
```bash
java -jar dbbackup.jar backup \
  --type mysql \
  --host localhost \
  --port 3306 \
  --database mydb \
  --user root \
  --password \
  --compress gzip \
  --storage local
```

**PostgreSQL:**
```bash
java -jar dbbackup.jar backup \
  --type postgresql \
  --host localhost \
  --database mydb \
  --user postgres \
  --password \
  --compress gzip
```

**MongoDB:**
```bash
java -jar dbbackup.jar backup \
  --type mongodb \
  --host localhost \
  --port 27017 \
  --database mydb \
  --user admin \
  --password \
  --compress gzip
```

**SQLite:**
```bash
java -jar dbbackup.jar backup \
  --type sqlite \
  --database mydb \
  --file /path/to/database.db \
  --compress zip
```

#### 2. Restore Database

```bash
java -jar dbbackup.jar restore \
  --backup-file /backups/mysql_mydb_20241215_143022.sql.gz \
  --type mysql \
  --host localhost \
  --database mydb_restored \
  --user root \
  --password
```

#### 3. List Backups

```bash
java -jar dbbackup.jar list
```

#### 4. Test Connection

```bash
java -jar dbbackup.jar backup \
  --type mysql \
  --host localhost \
  --database mydb \
  --user root \
  --password \
  --test-connection
```

### Advanced Usage

#### Backup to AWS S3

```bash
java -jar dbbackup.jar backup \
  --type mysql \
  --host localhost \
  --database mydb \
  --user root \
  --password \
  --compress gzip \
  --storage s3
```

#### Schedule Automated Backups

Create a cron job:

```bash
# Daily backup at 2 AM
0 2 * * * /usr/bin/java -jar /path/to/dbbackup.jar backup --type mysql --host localhost --database mydb --user root --password secret --compress gzip

# Weekly full backup on Sunday at 3 AM
0 3 * * 0 /usr/bin/java -jar /path/to/dbbackup.jar backup --type mysql --host localhost --database mydb --user root --password secret --backup-type full --compress gzip
```

#### Backup Script Example

Create `backup.sh`:

```bash
#!/bin/bash

BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d)
LOG_FILE="$BACKUP_DIR/backup_$DATE.log"

# Backup MySQL
java -jar /path/to/dbbackup.jar backup \
  --type mysql \
  --host localhost \
  --database production_db \
  --user backup_user \
  --password secret123 \
  --compress gzip \
  --storage s3 >> $LOG_FILE 2>&1

# Check exit status
if [ $? -eq 0 ]; then
    echo "Backup completed successfully" >> $LOG_FILE
else
    echo "Backup failed" >> $LOG_FILE
    # Send alert
    curl -X POST https://hooks.slack.com/services/YOUR/WEBHOOK \
      -d '{"text":"Database backup failed!"}'
fi
```

Make it executable:
```bash
chmod +x backup.sh
```

## Command Reference

### Backup Command Options

| Option | Description | Required | Default |
|--------|-------------|----------|---------|
| `-t, --type` | Database type (mysql, postgresql, mongodb, sqlite) | Yes | - |
| `-h, --host` | Database host | No | localhost |
| `-p, --port` | Database port | No | Default for DB type |
| `-d, --database` | Database name | Yes | - |
| `-u, --user` | Username | No | - |
| `--password` | Password (interactive) | No | - |
| `-f, --file` | SQLite file path | For SQLite | - |
| `--backup-type` | Backup type (full, incremental, differential) | No | full |
| `-c, --compress` | Compression (none, gzip, zip) | No | gzip |
| `-s, --storage` | Storage provider (local, s3, gcs, azure) | No | local |
| `--test-connection` | Test connection only | No | false |

### Restore Command Options

| Option | Description | Required |
|--------|-------------|----------|
| `-b, --backup-file` | Path to backup file | Yes |
| `-t, --type` | Database type | Yes |
| `-h, --host` | Database host | No |
| `-p, --port` | Database port | No |
| `-d, --database` | Target database name | Yes |
| `-u, --user` | Username | No |
| `--password` | Password | No |
| `--validate-only` | Only validate backup | No |

## Project Structure

```
database-backup-utility/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dbbackup/
│   │   │       ├── DatabaseBackupApplication.java
│   │   │       ├── cli/
│   │   │       │   ├── CliRunner.java
│   │   │       │   └── command/
│   │   │       │       ├── BackupCommand.java
│   │   │       │       ├── RestoreCommand.java
│   │   │       │       └── ListCommand.java
│   │   │       ├── core/
│   │   │       │   └── connector/
│   │   │       │       ├── DatabaseConnector.java
│   │   │       │       ├── MySqlConnector.java
│   │   │       │       ├── PostgreSqlConnector.java
│   │   │       │       └── MongoDbConnector.java
│   │   │       ├── service/
│   │   │       │   ├── BackupService.java
│   │   │       │   ├── RestoreService.java
│   │   │       │   ├── CompressionService.java
│   │   │       │   ├── StorageService.java
│   │   │       │   ├── NotificationService.java
│   │   │       │   └── SchedulerService.java
│   │   │       └── model/
│   │   │           ├── DatabaseType.java
│   │   │           ├── BackupType.java
│   │   │           ├── DatabaseConfig.java
│   │   │           └── BackupResult.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback.xml
│   └── test/
│       └── java/
│           └── com/dbbackup/
├── pom.xml
└── README.md
```

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Manual Testing

1. **Test MySQL Backup:**
```bash
java -jar dbbackup.jar backup --type mysql --host localhost --database testdb --user root --password test123 --test-connection
```

2. **Test Compression:**
```bash
java -jar dbbackup.jar backup --type mysql --host localhost --database testdb --user root --password test123 --compress gzip
```

3. **Test Restore:**
```bash
java -jar dbbackup.jar restore --backup-file backups/mysql_testdb_20241215.sql.gz --type mysql --host localhost --database testdb_restored --user root --password test123 --validate-only
```

## Troubleshooting

### Common Issues

#### 1. Connection Refused
```
Error: Connection refused to database
```
**Solution:** Check database is running and credentials are correct
```bash
# Test MySQL connection
mysql -h localhost -u root -p

# Test PostgreSQL connection
psql -h localhost -U postgres -d mydb
```

#### 2. Command Not Found (mysqldump, pg_dump)
```
Error: Cannot run program "mysqldump"
```
**Solution:** Install database client tools and add to PATH

#### 3. Permission Denied
```
Error: Access denied for user
```
**Solution:** Check user has backup privileges
```sql
-- MySQL
GRANT SELECT, LOCK TABLES, SHOW VIEW, TRIGGER ON *.* TO 'backup_user'@'localhost';

-- PostgreSQL
GRANT CONNECT ON DATABASE mydb TO backup_user;
GRANT USAGE ON SCHEMA public TO backup_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO backup_user;
```

#### 4. Out of Memory
```
Error: Java heap space
```
**Solution:** Increase JVM memory
```bash
java -Xmx2G -jar dbbackup.jar backup ...
```

### Enable Debug Logging

```bash
java -jar dbbackup.jar --logging.level.com.dbbackup=DEBUG backup ...
```

Or edit `application.yml`:
```yaml
logging:
  level:
    com.dbbackup: DEBUG
```

## Performance Tips

1. **Use Compression:** Always enable compression for large databases
2. **Parallel Backups:** For databases with many tables, use parallel backup options
3. **Network Optimization:** Use compression when backing up to cloud storage
4. **Resource Limits:** Set appropriate JVM memory limits based on database size

## Security Best Practices

1. **Never Store Passwords in Scripts:** Use interactive password prompts or environment variables
2. **Use Encrypted Credentials:** Enable encryption in `application.yml`
3. **Restrict File Permissions:**
   ```bash
   chmod 600 backup-script.sh
   chmod 700 /backups
   ```
4. **Use Read-Only Database Users:** Create dedicated backup users with minimal privileges
5. **Encrypt Backup Files:** Enable backup encryption for sensitive data

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- GitHub Issues: https://github.com/yourusername/database-backup-utility/issues
- Email: support@example.com

## Changelog

### Version 1.0.0
- Initial release
- Support for MySQL, PostgreSQL, MongoDB, SQLite
- Compression support (GZIP, ZIP)
- Local and S3 storage
- Slack notifications
- Automated scheduling
