package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.stockmann.replenishment.services.WeeklyDataBulkLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelAsyncLoadService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHRequestedBy;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataDeletionService;

import java.util.Map;

@RestController
@RequestMapping("/weeklydata/v1.0")
public class WeeklyDataController {

    private final WeeklyDataBulkLoader bulkLoader;
    private final DWHExcelAsyncLoadService asyncLoadService;
    private final WeeklyDataDeletionService deletionService;

    public WeeklyDataController(
            WeeklyDataBulkLoader bulkLoader,
            DWHExcelAsyncLoadService asyncLoadService,
            WeeklyDataDeletionService deletionService
    ) {
        this.bulkLoader = bulkLoader;
        this.asyncLoadService = asyncLoadService;
        this.deletionService = deletionService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<DWHExcelLoadResult> bulk(@RequestBody DWHExcelLoadRequest req) {

        if (req == null || req.getFilePath() == null || req.getFilePath().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(DWHExcelLoadResult.error(null, "filePath is empty"));
        }

        DWHExcelLoadResult result = req.getRequestedBy() == null
                ? bulkLoader.acceptFile(req.getFilePath())
                : bulkLoader.acceptFile(req.getFilePath(), req.getRequestedBy());

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

    @DeleteMapping("/year-week")
    public ResponseEntity<?> deleteByPeriod(
            @RequestParam(required = false) Short year,
            @RequestParam(required = false) Short week
    ) {
        if (year == null || week == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "year and week are required"));
        }
        String requestedBy = DWHRequestedBy.fromCurrentRequest();
        return ResponseEntity.ok(requestedBy == null
                ? deletionService.deleteByPeriod(year, week)
                : deletionService.deleteByPeriod(year, week, requestedBy));
    }

    @DeleteMapping("/session")
    public ResponseEntity<?> deleteByLoadSessionId(@RequestParam Long loadSessionId) {
        if (loadSessionId == null || loadSessionId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "loadSessionId must be positive"));
        }
        String requestedBy = DWHRequestedBy.fromCurrentRequest();
        DWHDataDeleteResult result = requestedBy == null
                ? deletionService.deleteByLoadSessionId(loadSessionId)
                : deletionService.deleteByLoadSessionId(loadSessionId, requestedBy);
        return ResponseEntity.ok(result);
    }
}
