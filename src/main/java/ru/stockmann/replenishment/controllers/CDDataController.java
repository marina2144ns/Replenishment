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
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHRequestedBy;

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
    public ResponseEntity<?> bulk(
            @RequestBody DWHExcelLoadRequest req,
            @RequestParam(required = false) String requestedBy
    ) {
        if (req == null || req.getFilePath() == null || req.getFilePath().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(DWHExcelLoadResult.error(null, "filePath is empty"));
        }

        String resolvedRequestedBy = DWHRequestedBy.resolve(requestedBy, req.getRequestedBy());
        DWHExcelLoadResult result = resolvedRequestedBy == null
                ? bulkLoader.acceptFile(req.getFilePath())
                : bulkLoader.acceptFile(req.getFilePath(), resolvedRequestedBy);

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
            @RequestParam(required = false) Integer sezon,
            @RequestParam(required = false) String requestedBy
    ) {
        if (god == null || sezon == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "god and sezon are required"));
        }
        requestedBy = DWHRequestedBy.normalizeAndValidate(requestedBy);
        return ResponseEntity.ok(requestedBy == null
                ? deletionService.deleteByPeriod(god, sezon)
                : deletionService.deleteByPeriod(god, sezon, requestedBy));
    }

    @DeleteMapping("/session")
    public ResponseEntity<?> deleteByLoadSessionId(
            @RequestParam Long loadSessionId,
            @RequestParam(required = false) String requestedBy
    ) {
        if (loadSessionId == null || loadSessionId <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "loadSessionId must be positive"));
        }
        requestedBy = DWHRequestedBy.normalizeAndValidate(requestedBy);
        DWHDataDeleteResult result = requestedBy == null
                ? deletionService.deleteByLoadSessionId(loadSessionId)
                : deletionService.deleteByLoadSessionId(loadSessionId, requestedBy);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/nazvanie-den")
    public ResponseEntity<?> deleteByNazvanieAndDen(
            @RequestParam Map<String, String> parameters,
            @RequestParam(required = false) String requestedBy
    ) {
        if (!java.util.Set.of("nazvanie", "den").equals(parameters.keySet())
                && !java.util.Set.of("nazvanie", "den", "requestedBy").equals(parameters.keySet())) {
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
            requestedBy = DWHRequestedBy.normalizeAndValidate(requestedBy);
            return ResponseEntity.ok(requestedBy == null
                    ? deletionService.deleteByNazvanieAndDen(nazvanie, den)
                    : deletionService.deleteByNazvanieAndDen(nazvanie, den, requestedBy));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "den must be an integer"));
        }
    }
}
