package com.researchassistant.document.storage;

import java.io.InputStream;

public record DocumentStorageObject(
        InputStream inputStream,
        long contentLength
) {
}
