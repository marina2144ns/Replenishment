package ru.stockmann.replenishment.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHRequestedBy;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHDatasetFieldsResponse;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHDatasetsResponse;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHUniversalDeleteRequest;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHUniversalDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHUniversalDeletionService;

@RestController
@RequestMapping("/data/v1.0")
public class DWHDataController {

    private final DWHUniversalDeletionService service;

    public DWHDataController(DWHUniversalDeletionService service) {
        this.service = service;
    }

    @GetMapping("/datasets")
    public DWHDatasetsResponse datasets() {
        return service.datasets();
    }

    @GetMapping("/datasets/{dataset}/fields")
    public DWHDatasetFieldsResponse fields(@PathVariable String dataset) {
        return service.fields(dataset);
    }

    @PostMapping("/delete")
    public ResponseEntity<DWHUniversalDeleteResult> delete(
            @RequestBody(required = false) DWHUniversalDeleteRequest request
    ) {
        String requestedBy = request == null
                ? null
                : DWHRequestedBy.normalizeAndValidate(request.requestedBy());
        return ResponseEntity.ok(service.execute(request, requestedBy));
    }
}
