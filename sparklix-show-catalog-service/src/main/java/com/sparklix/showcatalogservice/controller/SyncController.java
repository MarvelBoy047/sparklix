package com.sparklix.showcatalogservice.controller;

import com.sparklix.showcatalogservice.service.ShowDataSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // If you want to secure it
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/sync")
public class SyncController {

    private static final Logger logger = LoggerFactory.getLogger(SyncController.class);
    private final ShowDataSyncService showDataSyncService;

    public SyncController(ShowDataSyncService showDataSyncService) {
        this.showDataSyncService = showDataSyncService;
    }

    @PostMapping("/trigger")
    // For testing, can be public. For staging/prod, secure it (e.g., ROLE_ADMIN or service token).
    // @PreAuthorize("hasRole('ADMIN')") 
    public ResponseEntity<String> triggerSync() {
        logger.info("Received manual trigger for data synchronization.");
        try {
            showDataSyncService.syncShowData(); // Call the sync method directly
            return ResponseEntity.ok("Data synchronization triggered and likely completed successfully.");
        } catch (Exception e) {
            logger.error("Error during manually triggered sync: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error during sync: " + e.getMessage());
        }
    }
}