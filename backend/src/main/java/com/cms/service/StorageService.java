package com.cms.service;

import java.io.InputStream;

/**
 * Abstraction over object storage (MinIO).
 * All paths are full object keys including the folder prefix,
 * e.g. {@code aadhar_card/123-abc-filename.pdf}.
 */
public interface StorageService {

    /**
     * Upload an object. The bucket is created automatically if it does not exist.
     *
     * @param objectKey   full path within the bucket (folder/filename)
     * @param data        content to upload
     * @param size        byte length of data (-1 to let the client determine it)
     * @param contentType MIME type
     */
    void upload(String objectKey, InputStream data, long size, String contentType);

    /**
     * Download an object as an InputStream. Caller must close the stream.
     */
    InputStream download(String objectKey);

    /**
     * Download an object as a byte array.
     */
    byte[] downloadBytes(String objectKey);

    /**
     * Delete an object. No-ops silently if the object does not exist.
     */
    void delete(String objectKey);

    /**
     * Returns true if the object exists in the bucket.
     */
    boolean exists(String objectKey);
}
