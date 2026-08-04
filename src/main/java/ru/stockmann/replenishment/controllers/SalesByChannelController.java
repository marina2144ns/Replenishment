package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelAsyncLoadService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;
import ru.stockmann.replenishment.services.salesbychannel.SalesByChannelBulkLoader;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelDeletionService;

import java.util.Map;

@RestController
@RequestMapping("/salesbychannel/v1.0")
public class SalesByChannelController {

    private final SalesByChannelBulkLoader bulkLoader;
    private final DWHExcelAsyncLoadService asyncLoadService;
    private final SalesByChannelDeletionService deletionService;

    public SalesByChannelController(
            SalesByChannelBulkLoader bulkLoader,
            DWHExcelAsyncLoadService asyncLoadService,
            SalesByChannelDeletionService deletionService
    ) {
        this.bulkLoader = bulkLoader;
        this.asyncLoadService = asyncLoadService;
        this.deletionService = deletionService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> bulk(@RequestBody DWHExcelLoadRequest request) {
        if (request == null || request.getFilePath() == null || request.getFilePath().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(DWHExcelLoadResult.error(null, "filePath is empty"));
        }

        DWHExcelLoadResult result = bulkLoader.acceptFile(request.getFilePath());
        if ("OK".equals(result.status()) && result.loadSessionId() != null) {
            asyncLoadService.startAsync(
                    bulkLoader,
                    result.loadSessionId(),
                    request.getFilePath()
            );
        }

        HttpStatus status = "OK".equals(result.status())
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(result, status);
    }

    @DeleteMapping("/year-week")
    public ResponseEntity<?> deleteByPeriod(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer week
    ) {
        if (year == null || week == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "year and week are required"));
        }
        return ResponseEntity.ok(deletionService.deleteByPeriod(year, week));
    }

    @DeleteMapping("/session")
    public ResponseEntity<?> deleteByLoadSessionId(@RequestParam Long loadSessionId) {
        if (loadSessionId == null || loadSessionId <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "loadSessionId must be positive")
            );
        }
        DWHDataDeleteResult result = deletionService.deleteByLoadSessionId(loadSessionId);
        return ResponseEntity.ok(result);
    }
}
