package com.researchassistant.document.storage;

import com.researchassistant.common.storage.ObjectStorageService;
import com.researchassistant.common.storage.StorageObject;
import com.researchassistant.common.storage.StoredObject;

import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class ObjectBackedDocumentStorageService implements DocumentStorageService {

    private final ObjectStorageService objectStorageService;

    public ObjectBackedDocumentStorageService(ObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    @Override
    public StoredDocumentObject store(String storageKey, InputStream inputStream) {
        StoredObject stored = objectStorageService.store(storageKey, inputStream);
        return toStoredDocumentObject(stored);
    }

    @Override
    public StoredDocumentObject store(String storageKey, InputStream inputStream, String mediaType) {
        StoredObject stored = objectStorageService.store(storageKey, inputStream, mediaType);
        return toStoredDocumentObject(stored);
    }

    @Override
    public DocumentStorageObject open(String storageKey) {
        StorageObject object = objectStorageService.open(storageKey);
        return new DocumentStorageObject(object.inputStream(), object.sizeBytes());
    }

    @Override
    public boolean exists(String storageKey) {
        return objectStorageService.exists(storageKey);
    }

    @Override
    public void delete(String storageKey) {
        objectStorageService.delete(storageKey);
    }

    private StoredDocumentObject toStoredDocumentObject(StoredObject stored) {
        return new StoredDocumentObject(
                stored.key(),
                stored.sizeBytes(),
                stored.checksumSha256(),
                stored.providerFileId(),
                stored.providerParentId()
        );
    }
}
