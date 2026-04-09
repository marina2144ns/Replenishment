package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.stockmann.replenishment.models.CDDataLoadRequest;
import ru.stockmann.replenishment.services.CDDataBulkLoader;
import ru.stockmann.replenishment.models.CDDataLoadResult;

@RestController
@RequestMapping("/cddata/v1.0")
public class CDDataController {

    private final CDDataBulkLoader bulkLoader;

    public CDDataController(CDDataBulkLoader bulkLoader) {
        this.bulkLoader = bulkLoader;
    }

    @PostMapping("/bulk")
    public ResponseEntity<CDDataLoadResult> bulk(@RequestBody CDDataLoadRequest req) {
        if (req == null || req.getFilePath() == null || req.getFilePath().isBlank()) {
            return ResponseEntity.badRequest().body(CDDataLoadResult.error(null, "filePath is empty"));
        }

        CDDataLoadResult result =
                bulkLoader.bulkLoad(req.getFilePath());

        HttpStatus status = "OK".equals(result.status())
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(result, status);
    }
}