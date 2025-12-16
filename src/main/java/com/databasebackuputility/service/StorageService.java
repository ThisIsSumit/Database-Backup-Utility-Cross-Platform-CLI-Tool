package com.databasebackuputility.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Service for managing backup file storage (local and cloud)
 */
@Slf4j
@Service
public class StorageService {

    @Value("${storage.default-provider:local}")
    private String defaultProvider;

    @Value("${storage.local.base-path:./backups}")
    private String localBasePath;

    @Value("${storage.s3.bucket-name:}")
    private String s3BucketName;

    @Value("${storage.s3.enabled:false}")
    private boolean s3Enabled;

    public enum StorageProvider {
        LOCAL, S3, GCS, AZURE
    }

    /**
     * Store backup file
     */
    public String store(File file, StorageProvider provider) throws IOException {
        log.info("Storing backup file: {} using provider: {}", file.getName(), provider);

        switch (provider) {
            case LOCAL:
                return storeLocal(file);
            case S3:
                return storeS3(file);
            default:
                throw new UnsupportedOperationException("Storage provider not implemented: " + provider);
        }
    }

    /**
     * Store file locally
     */
    private String storeLocal(File file) throws IOException {
        Path targetDir = Paths.get(localBasePath);
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(file.getName());
        Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File stored locally at: {}", targetPath.toAbsolutePath());
        return targetPath.toAbsolutePath().toString();
    }

    /**
     * Store file in AWS S3
     */
    private String storeS3(File file) throws IOException {
        if (!s3Enabled) {
            throw new IllegalStateException("S3 storage is not enabled");
        }

        try {
            S3Client s3Client = S3Client.builder().build();

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(file.getName())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(file));

            String s3Path = String.format("s3://%s/%s", s3BucketName, file.getName());
            log.info("File stored in S3 at: {}", s3Path);

            return s3Path;

        } catch (Exception e) {
            log.error("Failed to upload to S3: {}", e.getMessage());
            throw new IOException("S3 upload failed", e);
        }
    }

    /**
     * Retrieve file from storage
     */
    public File retrieve(String storagePath) throws IOException {
        if (storagePath.startsWith("s3://")) {
            return retrieveFromS3(storagePath);
        } else {
            return new File(storagePath);
        }
    }

    /**
     * Retrieve file from S3
     */
    private File retrieveFromS3(String s3Path) throws IOException {
        // Implementation for S3 download
        throw new UnsupportedOperationException("S3 download not yet implemented");
    }

    /**
     * Delete backup file
     */
    public boolean delete(String storagePath) {
        try {
            if (storagePath.startsWith("s3://")) {
                return deleteFromS3(storagePath);
            } else {
                File file = new File(storagePath);
                return file.delete();
            }
        } catch (Exception e) {
            log.error("Failed to delete file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Delete file from S3
     */
    private boolean deleteFromS3(String s3Path) {
        // Implementation for S3 deletion
        throw new UnsupportedOperationException("S3 deletion not yet implemented");
    }

    /**
     * List backups in storage
     */
    public File[] listBackups() {
        File backupDir = new File(localBasePath);
        if (!backupDir.exists()) {
            return new File[0];
        }
        return backupDir.listFiles((dir, name) ->
                name.endsWith(".sql") || name.endsWith(".gz") || name.endsWith(".zip"));
    }

    /**
     * Get storage provider from string
     */
    public StorageProvider getProvider(String providerName) {
        if (providerName == null || providerName.isEmpty()) {
            providerName = defaultProvider;
        }
        return StorageProvider.valueOf(providerName.toUpperCase());
    }
}