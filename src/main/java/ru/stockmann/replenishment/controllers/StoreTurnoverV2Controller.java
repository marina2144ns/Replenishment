package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelAsyncLoadService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;
import ru.stockmann.replenishment.services.storeturnover.StoreTurnoverBulkLoader;
import ru.stockmann.replenishment.services.storeturnover.process.StoreTurnoverDeletionService;

import java.util.Map;

@RestController
@RequestMapping("/storeturnover/v2.0")
public class StoreTurnoverV2Controller {
    private final StoreTurnoverBulkLoader loader;
    private final DWHExcelAsyncLoadService async;
    private final StoreTurnoverDeletionService deletionService;

    public StoreTurnoverV2Controller(StoreTurnoverBulkLoader loader, DWHExcelAsyncLoadService async,
                                     StoreTurnoverDeletionService deletionService) {
        this.loader = loader;
        this.async = async;
        this.deletionService = deletionService;
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> bulk(@RequestBody DWHExcelLoadRequest request) {
        if (request == null || request.getFilePath() == null || request.getFilePath().isBlank()) {
            return ResponseEntity.badRequest().body(DWHExcelLoadResult.error(null, "filePath is empty"));
        }
        DWHExcelLoadResult result = loader.acceptFile(request.getFilePath());
        if ("OK".equals(result.status()) && result.loadSessionId() != null) {
            async.startAsync(loader, result.loadSessionId(), request.getFilePath());
        }
        return new ResponseEntity<>(result,
                "OK".equals(result.status()) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/session")
    public ResponseEntity<?> deleteByLoadSessionId(@RequestParam Long loadSessionId) {
        if (loadSessionId == null || loadSessionId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "loadSessionId must be positive"));
        }
        DWHDataDeleteResult result = deletionService.deleteByLoadSessionId(loadSessionId);
        return ResponseEntity.ok(result);
    }
}
