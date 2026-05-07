package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ru.stockmann.replenishment.services.CDDataBulkLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelAsyncLoadService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;

@RestController
@RequestMapping("/cddata/v1.0")
public class CDDataController {

    private final CDDataBulkLoader bulkLoader;
    private final DWHExcelAsyncLoadService asyncLoadService;

    public CDDataController(
            CDDataBulkLoader bulkLoader,
            DWHExcelAsyncLoadService  asyncLoadService
    ) {
        this.bulkLoader = bulkLoader;
        this.asyncLoadService = asyncLoadService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> bulk(@RequestBody DWHExcelLoadRequest req) {
        if (req == null || req.getFilePath() == null || req.getFilePath().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(DWHExcelLoadResult.error(null, "filePath is empty"));
        }

        DWHExcelLoadResult result = bulkLoader.acceptFile(req.getFilePath());

        if ("OK".equals(result.status()) && result.loadSessionId() != null) {
            asyncLoadService.startAsync(
                    bulkLoader,
                    result.loadSessionId(),
                    req.getFilePath()
            );
        }

        HttpStatus status = "OK".equals(result.status())
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(result, status);
    }
}