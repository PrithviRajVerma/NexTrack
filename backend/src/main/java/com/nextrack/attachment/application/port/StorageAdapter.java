package com.nextrack.attachment.application.port;

import java.io.InputStream;

public interface StorageAdapter {

    void store(String storageKey, InputStream inputStream, long expectedSize);

    InputStream load(String storageKey);

    void delete(String storageKey);

    boolean exists(String storageKey);
}
