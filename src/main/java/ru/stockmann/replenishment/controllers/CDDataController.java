package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ru.stockmann.replenishment.services.CDDataBulkLoader;
import ru.stockmann.replenishment.services.cddata.process.CDDataDeletionService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelAsyncLoadService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;

import java.util.Map;

@RestController
@RequestMapping("/cddata/v1.0")
public class CDDataController {

    private final CDDataBulkLoader bulkLoader;
    private final DWHExcelAsyncLoadService asyncLoadService;
    private final CDDataDeletionService deletionService;

    public CDDataController(
            CDDataBulkLoader bulkLoader,
            DWHExcelAsyncLoadService asyncLoadService,
            CDDataDeletionService deletionService
    ) {
        this.bulkLoader = bulkLoader;
        this.asyncLoadService = asyncLoadService;
        this.deletionService = deletionService;
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

    @DeleteMapping("/god-sezon")
    public ResponseEntity<?> deleteByPeriod(
            @RequestParam(required = false) Integer god,
            @RequestParam(required = false) Integer sezon
    ) {
        if (god == null || sezon == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "god and sezon are required"));
        }
        return ResponseEntity.ok(deletionService.deleteByPeriod(god, sezon));
    }

    @DeleteMapping("/session")
    public ResponseEntity<?> deleteByLoadSessionId(@RequestParam Long loadSessionId) {
        if (loadSessionId == null || loadSessionId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "loadSessionId must be positive"));
        }
        DWHDataDeleteResult result = deletionService.deleteByLoadSessionId(loadSessionId);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/nazvanie-den")
    public ResponseEntity<?> deleteByNazvanieAndDen(@RequestParam Map<String, String> parameters) {
        if (!parameters.keySet().equals(java.util.Set.of("nazvanie", "den"))) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "exactly nazvanie and den are required")
            );
        }
        String nazvanie = parameters.get("nazvanie");
        if (nazvanie == null || nazvanie.isBlank() || nazvanie.length() > 255) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "nazvanie must not be blank or longer than 255 characters")
            );
        }
        try {
            int den = Integer.parseInt(parameters.get("den"));
            return ResponseEntity.ok(deletionService.deleteByNazvanieAndDen(nazvanie, den));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "den must be an integer"));
        }
    }
}
