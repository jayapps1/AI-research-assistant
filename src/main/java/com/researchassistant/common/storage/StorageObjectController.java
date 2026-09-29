package com.researchassistant.common.storage;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/storage-objects")
public class StorageObjectController {

    private final AuthenticatedUserResolver userResolver;
    private final StorageObjectDownloadService downloadService;

    public StorageObjectController(
            AuthenticatedUserResolver userResolver,
            StorageObjectDownloadService downloadService
    ) {
        this.userResolver = userResolver;
        this.downloadService = downloadService;
    }

    @GetMapping("/{storageObjectId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            Authentication authentication,
            @PathVariable UUID storageObjectId
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return downloadService.download(storageObjectId, user);
    }
}
