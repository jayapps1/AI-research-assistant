package com.researchassistant.common.storage;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public record StorageObject(
        InputStream inputStream,
        long sizeBytes
) implements Closeable {

    public InputStream contentStream() {
        return inputStream();
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
