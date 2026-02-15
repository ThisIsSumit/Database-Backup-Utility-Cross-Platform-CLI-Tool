# MongoDB Backup & Restore Flow

## ✅ Correct Implementation (Current)

### 📤 Backup Flow

```
┌─────────────────────┐
│  MongoDB Database   │
│  (Collections +     │
│   Documents)        │
└──────────┬──────────┘
           │
           │ mongodump --archive (NO --gzip)
           │ Exports as uncompressed BSON archive
           │
           ▼
┌─────────────────────────────────┐
│  Uncompressed Archive File      │
│  mydatabase_20260215.archive    │
│  (BSON format)                  │
│  Size: ~10 MB                   │
└──────────┬──────────────────────┘
           │
           │ CompressionService.compress()
           │ (if compression enabled)
           │
           ▼
┌─────────────────────────────────┐
│  Compressed Archive File        │
│  mydatabase_20260215.archive.gz │
│  Size: ~2 MB (80% reduction)    │
└──────────┬──────────────────────┘
           │
           │ StorageService.store()
           │
           ▼
┌─────────────────────────────────┐
│  Storage (Local/Cloud)          │
│  backups/                       │
│  └─ mydatabase_20260215         │
│     .archive.gz                 │
└─────────────────────────────────┘
```

### 📥 Restore Flow

```
┌─────────────────────────────────┐
│  Storage (Local/Cloud)          │
│  backups/                       │
│  └─ mydatabase_20260215         │
│     .archive.gz                 │
└──────────┬──────────────────────┘
           │
           │ StorageService.retrieve()
           │
           ▼
┌─────────────────────────────────┐
│  Compressed Archive File        │
│  mydatabase_20260215.archive.gz │
└──────────┬──────────────────────┘
           │
           │ CompressionService.decompress()
           │ (detects .gz extension)
           │
           ▼
┌─────────────────────────────────┐
│  Uncompressed Archive File      │
│  mydatabase_20260215.archive    │
│  (BSON format)                  │
└──────────┬──────────────────────┘
           │
           │ mongorestore --archive (NO --gzip)
           │ Reads uncompressed BSON archive
           │
           ▼
┌─────────────────────┐
│  MongoDB Database   │
│  (Restored)         │
│  ✅ All collections │
│  ✅ All documents   │
└─────────────────────┘
```

## ❌ Previous Issue (Fixed)

### The Problem: Double Compression

**Backup:**
1. `mongodump --archive --gzip` → created compressed archive
2. `CompressionService.compress()` → compressed it AGAIN
3. Result: **Double compressed file** `.archive.gz` → `.gz.gz` (internally)

**Restore:**
1. `CompressionService.decompress()` → removed ONE layer
2. `mongorestore --archive --gzip` → expected uncompressed, but got compressed data
3. Result: **Restore FAILED** ❌

## 🔧 Implementation Details

### MongoDbConnector.backup()

```java
ProcessBuilder pb = new ProcessBuilder(
    "mongodump",
    "--host=" + host,
    "--port=" + port,
    "--db=" + database,
    "--archive"     // ✅ NO --gzip flag
);
// Output → uncompressed .archive file
```

### MongoDbConnector.restore()

```java
ProcessBuilder pb = new ProcessBuilder(
    "mongorestore",
    "--host=" + host,
    "--port=" + port,
    "--archive=" + archiveFile,  // expects uncompressed
    "--drop"                      // ✅ NO --gzip flag
);
```

### CompressionService

- **Detects** compression by file extension (`.gz`, `.zip`)
- **Compresses** `.archive` → `.archive.gz`
- **Decompresses** `.archive.gz` → `.archive`
- **Uniform handling** for all database types

## 📊 File Structure Examples

### After Backup (with compression)
```
backups/
├── mongodb_customers_20260215_143022.archive.gz    # MongoDB
├── mysql_orders_20260215_143100.sql.gz             # MySQL  
└── postgres_inventory_20260215_143200.sql.gz       # PostgreSQL
```

### After Decompression (before restore)
```
temp/
├── mongodb_customers_20260215_143022.archive       # BSON archive
├── mysql_orders_20260215_143100.sql                # SQL script
└── postgres_inventory_20260215_143200.sql          # SQL script
```

## ✅ Verification Commands

### Test Backup Flow
```bash
# 1. Create backup
java -jar database-backup-utility.jar backup \
  --type MONGODB \
  --host localhost \
  --port 27017 \
  --database testdb \
  --compression GZIP

# 2. Verify archive is valid uncompressed BSON
gunzip testdb.archive.gz
mongorestore --archive=testdb.archive --dry-run
```

### Test Restore Flow
```bash
# Restore from compressed backup
java -jar database-backup-utility.jar restore \
  --type MONGODB \
  --host localhost \
  --port 27017 \
  --database testdb_restored \
  --backup-file backups/testdb.archive.gz
```

## 🎯 Key Points

1. ✅ **No double compression** - mongodump creates uncompressed archives
2. ✅ **Consistent flow** - CompressionService handles ALL compression
3. ✅ **Proper restore** - mongorestore receives uncompressed archives
4. ✅ **Works with Java 21** - All tested and verified
5. ✅ **Flexible** - Can skip compression by using `CompressionType.NONE`

## 📝 Notes

- MongoDB archives contain BSON (Binary JSON) data
- BSON is efficient for storage and transmission
- Compression typically reduces file size by 70-80%
- Archive format preserves indexes and metadata
- Restore with `--drop` ensures clean restore (removes existing collections first)
