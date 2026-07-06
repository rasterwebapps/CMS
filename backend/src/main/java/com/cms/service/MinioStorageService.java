package com.cms.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;

@Service
public class MinioStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageService(MinioClient minioClient,
                               @Value("${cms.minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise MinIO bucket '" + bucket + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void upload(String objectKey, InputStream data, long size, String contentType) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(data, size, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("MinIO upload failed for key '" + objectKey + "': " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("MinIO download failed for key '" + objectKey + "': " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadBytes(String objectKey) {
        try (InputStream is = download(objectKey)) {
            return is.readAllBytes();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("MinIO read failed for key '" + objectKey + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception e) {
            log.warn("MinIO delete failed for key '{}': {}", objectKey, e.getMessage());
        }
    }

    @Override
    public boolean exists(String objectKey) {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new IllegalStateException("MinIO stat failed for key '" + objectKey + "': " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("MinIO stat failed for key '" + objectKey + "': " + e.getMessage(), e);
        }
    }

    /**
     * Generates a unique object key for a document.
     * Format: {@code {folder}/{entityId}-{uuid8}-{sanitizedFileName}}
     * e.g. {@code aadhar_card/123-a1b2c3d4-aadhaar.pdf}
     */
    public static String buildKey(String folder, long entityId, String fileName) {
        String safe = fileName != null
            ? fileName.replaceAll("[^a-zA-Z0-9._\\-]", "_")
            : "file";
        String uid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return folder + "/" + entityId + "-" + uid + "-" + safe;
    }

    /**
     * Returns the MinIO folder name for a given document type.
     * DocumentType enum name lowercased, e.g. AADHAR_CARD → aadhar_card.
     */
    public static String folderFor(com.cms.model.enums.DocumentType documentType) {
        return documentType.name().toLowerCase();
    }
}
