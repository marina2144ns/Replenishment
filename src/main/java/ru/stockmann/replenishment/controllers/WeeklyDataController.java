package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.stockmann.replenishment.models.WeeklyDataLoadRequest;
import ru.stockmann.replenishment.services.WeeklyDataBulkLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;

@RestController
@RequestMapping("/weeklydata/v1.0")
public class WeeklyDataController {

    private final WeeklyDataBulkLoader bulkLoader;

    public WeeklyDataController(WeeklyDataBulkLoader bulkLoader) {
        this.bulkLoader = bulkLoader;
    }

    @PostMapping("/bulk")
    public ResponseEntity<DWHExcelLoadResult> bulk(@RequestBody WeeklyDataLoadRequest req) {

        if (req == null || req.getFilePath() == null || req.getFilePath().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(DWHExcelLoadResult.error(null, "filePath is empty"));
        }

        DWHExcelLoadResult result = bulkLoader.acceptFile(req.getFilePath());

        HttpStatus status = "OK".equals(result.status())
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(result, status);
    }
}