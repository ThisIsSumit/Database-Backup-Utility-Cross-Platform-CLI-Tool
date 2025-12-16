package com.databasebackuputility.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Service for file compression and decompression
 */
@Slf4j
@Service
public class CompressionService {

    public enum CompressionType {
        NONE, GZIP, ZIP
    }

    /**
     * Compress file using specified compression type
     */
    public File compress(File source, CompressionType type) throws IOException {
        if (type == CompressionType.NONE) {
            return source;
        }

        String outputPath = source.getAbsolutePath() + getExtension(type);
        File outputFile = new File(outputPath);

        log.info("Compressing file: {} -> {}", source.getName(), outputFile.getName());

        switch (type) {
            case GZIP:
                compressGzip(source, outputFile);
                break;
            case ZIP:
                compressZip(source, outputFile);
                break;
        }

        log.info("Compression completed. Original: {} bytes, Compressed: {} bytes",
                source.length(), outputFile.length());

        return outputFile;
    }

    /**
     * Decompress file
     */
    public File decompress(File compressed) throws IOException {
        String fileName = compressed.getName();
        CompressionType type = detectCompressionType(fileName);

        if (type == CompressionType.NONE) {
            return compressed;
        }

        String outputPath = removeExtension(compressed.getAbsolutePath(), type);
        File outputFile = new File(outputPath);

        log.info("Decompressing file: {} -> {}", compressed.getName(), outputFile.getName());

        switch (type) {
            case GZIP:
                decompressGzip(compressed, outputFile);
                break;
            case ZIP:
                decompressZip(compressed, outputFile);
                break;
        }

        log.info("Decompression completed");
        return outputFile;
    }

    /**
     * GZIP compression
     */
    private void compressGzip(File source, File output) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(output);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
            gzos.finish();
        }
    }

    /**
     * GZIP decompression
     */
    private void decompressGzip(File compressed, File output) throws IOException {
        try (FileInputStream fis = new FileInputStream(compressed);
             GZIPInputStream gis = new GZIPInputStream(fis);
             FileOutputStream fos = new FileOutputStream(output)) {

            byte[] buffer = new byte[8192];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
        }
    }

    /**
     * ZIP compression
     */
    private void compressZip(File source, File output) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(output);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            ZipEntry entry = new ZipEntry(source.getName());
            zos.putNextEntry(entry);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, len);
            }
            zos.closeEntry();
        }
    }

    /**
     * ZIP decompression
     */
    private void decompressZip(File compressed, File output) throws IOException {
        try (FileInputStream fis = new FileInputStream(compressed);
             ZipInputStream zis = new ZipInputStream(fis);
             FileOutputStream fos = new FileOutputStream(output)) {

            ZipEntry entry = zis.getNextEntry();
            if (entry != null) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
        }
    }

    /**
     * Detect compression type from file extension
     */
    private CompressionType detectCompressionType(String fileName) {
        if (fileName.endsWith(".gz")) {
            return CompressionType.GZIP;
        } else if (fileName.endsWith(".zip")) {
            return CompressionType.ZIP;
        }
        return CompressionType.NONE;
    }

    /**
     * Get file extension for compression type
     */
    private String getExtension(CompressionType type) {
        switch (type) {
            case GZIP: return ".gz";
            case ZIP: return ".zip";
            default: return "";
        }
    }

    /**
     * Remove compression extension from filename
     */
    private String removeExtension(String filePath, CompressionType type) {
        String ext = getExtension(type);
        if (filePath.endsWith(ext)) {
            return filePath.substring(0, filePath.length() - ext.length());
        }
        return filePath;
    }
}