package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ru.stockmann.replenishment.services.CDEcomBulkLoader;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomDeletionService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelAsyncLoadService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHRequestedBy;

import java.util.Map;

@RestController
@RequestMapping("/cdecom/v1.0")
public class CDEcomController {

    private final CDEcomBulkLoader bulkLoader;
    private final DWHExcelAsyncLoadService asyncLoadService;
    private final CDEcomDeletionService deletionService;

    public CDEcomController(
            CDEcomBulkLoader bulkLoader,
            DWHExcelAsyncLoadService asyncLoadService,
            CDEcomDeletionService deletionService
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

    @DeleteMapping("/year-season")
    public ResponseEntity<?> deleteByPeriod(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer season
    ) {
        if (year == null || season == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "year and season are required"));
        }
        String requestedBy = DWHRequestedBy.fromCurrentRequest();
        return ResponseEntity.ok(requestedBy == null
                ? deletionService.deleteByPeriod(year, season)
                : deletionService.deleteByPeriod(year, season, requestedBy));
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

    @DeleteMapping("/nazvanie-den")
    public ResponseEntity<?> deleteByNazvanieAndDen(@RequestParam Map<String, String> parameters) {
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
            String requestedBy = parameters.get("requestedBy");
            return ResponseEntity.ok(requestedBy == null
                    ? deletionService.deleteByNazvanieAndDen(nazvanie, den)
                    : deletionService.deleteByNazvanieAndDen(nazvanie, den, requestedBy));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "den must be an integer"));
        }
    }
}
