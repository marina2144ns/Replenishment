package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ru.stockmann.replenishment.models.CDDataLoadRequest;
import ru.stockmann.replenishment.services.CDDataAsyncLoadService;
import ru.stockmann.replenishment.services.CDDataBulkLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadStatusService;

@RestController
@RequestMapping("/cddata/v1.0")
public class CDDataController {

    private final CDDataBulkLoader bulkLoader;
    private final CDDataAsyncLoadService asyncLoadService;
    private final DWHExcelLoadStatusService statusService;

    public CDDataController(
            CDDataBulkLoader bulkLoader,
            CDDataAsyncLoadService asyncLoadService,
            DWHExcelLoadStatusService statusService
    ) {
        this.bulkLoader = bulkLoader;
        this.asyncLoadService = asyncLoadService;
        this.statusService = statusService;
    }
    /*
    @PostMapping("/bulk")
    public ResponseEntity<DWHExcelLoadResult> bulk(@RequestBody CDDataLoadRequest req) {
        if (req == null || req.getFilePath() == null || req.getFilePath().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(DWHExcelLoadResult.error(null, "filePath is empty"));
        }

        DWHExcelLoadResult result = bulkLoader.loadFromExcel(req.getFilePath());

        HttpStatus status = "OK".equals(result.status())
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(result, status);
    }
     */

    @PostMapping("/bulk")
    public ResponseEntity<?> bulk(@RequestBody CDDataLoadRequest req) {
        if (req == null || req.getFilePath() == null || req.getFilePath().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(DWHExcelLoadResult.error(null, "filePath is empty"));
        }

        DWHExcelLoadResult result = bulkLoader.acceptFile(req.getFilePath());

        if (!"OK".equals(result.status())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(result);
        }

        asyncLoadService.startAsync(
                result.loadSessionId(),
                req.getFilePath()
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(result);
    }

    @GetMapping("/status/{loadSessionId}")
    public ResponseEntity<?> status(@PathVariable Long loadSessionId) {
        try {
            return ResponseEntity.ok(statusService.getStatus(loadSessionId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(DWHExcelLoadResult.error(loadSessionId, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(DWHExcelLoadResult.error(loadSessionId, e.getMessage()));
        }
    }
}