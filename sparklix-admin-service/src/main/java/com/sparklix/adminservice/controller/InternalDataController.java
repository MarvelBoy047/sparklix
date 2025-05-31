package com.sparklix.adminservice.controller;

import com.sparklix.adminservice.dto.CatalogShowDataDto;
import com.sparklix.adminservice.service.ShowManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/internal/data/admin")
@PreAuthorize("hasRole('ADMIN')") // Service account 'catalog-sync-agent' has ROLE_ADMIN
public class InternalDataController {

    private final ShowManagementService showManagementService;

    public InternalDataController(ShowManagementService showManagementService) {
        this.showManagementService = showManagementService;
    }

    @GetMapping("/shows-for-catalog")
    public ResponseEntity<List<CatalogShowDataDto>> getAllShowDataForCatalog() {
        List<CatalogShowDataDto> catalogData = showManagementService.getAllShowDataForCatalog();
        return ResponseEntity.ok(catalogData);
    }
}