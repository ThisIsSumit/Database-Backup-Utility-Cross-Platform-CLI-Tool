# 📦 Complete Guide: How to Use Database Backup Utility

Let me show you everything about how this application works!

---

## 🎯 What Does This App Do?

This is a **command-line database backup tool** that helps you:

- ✅ Backup MySQL, PostgreSQL, MongoDB, and SQLite databases
- ✅ Restore databases from backup files
- ✅ Compress backups (GZIP/ZIP) to save space
- ✅ Store backups locally or in cloud (AWS S3, Google Cloud, Azure)
- ✅ Schedule automatic backups
- ✅ Get notifications on Slack
- ✅ List all available backups

---

## 📚 Table of Contents

1. [Basic Commands](#1-basic-commands)
2. [How Backup Works](#2-how-backup-works)
3. [How Restore Works](#3-how-restore-works)
4. [MongoDB Backup Example](#4-mongodb-backup-example)
5. [All Command Options](#5-all-command-options)
6. [Complete Usage Tutorial](#6-complete-usage-tutorial)
7. [Interactive Mode](#7-interactive-mode)
8. [Summary](#8-summary)

---

# 1️⃣ Basic Commands

## Command Structure

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar [COMMAND] [OPTIONS]
```

---

## Available Commands

| Command   | Purpose |
|-----------|----------|
| `backup`  | Create a database backup |
| `restore` | Restore from backup file |
| `list`    | Show available backups |
| `help`    | Show help |

---

# 2️⃣ How Backup Works

## Backup Process Flow

```
1. Connect to Database
2. Export Database (mysqldump / pg_dump / mongodump)
3. Compress File
4. Store in Location
5. Send Notification
```

---

## MySQL Backup Example

### Windows

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup ^
  --type mysql ^
  --host localhost ^
  --database testdb ^
  --user root ^
  --password "root@123" ^
  --compress gzip ^
  --storage local
```

### Mac / Linux

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --type mysql \
  --host localhost \
  --database testdb \
  --user root \
  --password "root@123" \
  --compress gzip \
  --storage local
```

Backup will be saved in:

```
./backups/
```

---

# 3️⃣ How Restore Works

## Restore Process

```
1. Validate Backup File
2. Decompress
3. Confirm Overwrite
4. Restore Database
```

---

## Restore Example

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar restore \
  --backup-file ./backups/mysql_testdb_YYYYMMDD.sql.gz \
  --type mysql \
  --host localhost \
  --database testdb_copy \
  --user root \
  --password "root@123"
```

---

# 4️⃣ MongoDB Backup Example

## Local MongoDB (No Authentication)

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --type mongodb \
  --host localhost \
  --port 27017 \
  --database mydatabase \
  --compress gzip
```

---

## MongoDB With Authentication

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --type mongodb \
  --host localhost \
  --port 27017 \
  --database mydatabase \
  --user yourUsername \
  --password "yourPassword"
```

---

## MongoDB Atlas Example

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --type mongodb \
  --host cluster0.xxxxx.mongodb.net \
  --database mydatabase \
  --user atlasUser \
  --password "atlasPassword"
```

---

# 5️⃣ All Command Options

## Backup Options

| Option | Required | Description |
|--------|----------|------------|
| `--type` | ✅ Yes | mysql, postgresql, mongodb, sqlite |
| `--host` | Network DB | Database host |
| `--port` | Optional | Port number |
| `--database` | ✅ Yes | Database name |
| `--user` | Optional | Username |
| `--password` | Optional | Password |
| `--file` | SQLite only | File path |
| `--backup-type` | Optional | full / incremental / differential |
| `--compress` | Optional | gzip / zip / none |
| `--storage` | Optional | local / s3 / gcs / azure |
| `--test-connection` | Optional | Only test connection |

---

## Restore Options

| Option | Required | Description |
|--------|----------|------------|
| `--backup-file` | ✅ Yes | Backup file path |
| `--type` | ✅ Yes | Database type |
| `--database` | ✅ Yes | Target database |
| `--host` | Optional | Hostname |
| `--user` | Optional | Username |
| `--password` | Optional | Password |
| `--validate-only` | Optional | Validate file only |

---

# 6️⃣ Complete Usage Tutorial

## Step 1: Test Database Connection

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --type mongodb \
  --host localhost \
  --database mydatabase \
  --test-connection
```

---

## Step 2: Create Backup

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --type mongodb \
  --host localhost \
  --database mydatabase
```

---

## Step 3: List Backups

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar list
```

---

# 7️⃣ Interactive Mode

Instead of typing password in command:

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup \
  --type mysql \
  --host localhost \
  --database testdb \
  --user root \
  --password
```

It will prompt securely:

```
Enter value for --password:
```

---

# 8️⃣ Summary

How it works:

1. You run a command
2. App connects to database
3. Exports data
4. Compresses backup
5. Stores backup
6. Sends notification (optional)
7. You can restore anytime

---

# 🚀 Quick Start

### Backup MongoDB

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar backup --type mongodb --host localhost --database mydatabase
```

### List Backups

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar list
```

### Restore

```bash
java -jar target/database-backup-utility-0.0.1-SNAPSHOT.jar restore --backup-file backups/mongodb_mydatabase_*.gz --type mongodb --database mydatabase_copy
```

---

🎉 Your application is ready and production-ready!

Just run the correct JAR file:

```
target/database-backup-utility-0.0.1-SNAPSHOT.jar
```
