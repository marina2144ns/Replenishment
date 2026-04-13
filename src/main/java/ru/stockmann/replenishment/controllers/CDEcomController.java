package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ru.stockmann.replenishment.models.CDEcomLoadRequest;
import ru.stockmann.replenishment.services.CDEcomBulkLoader;
import ru.stockmann.replenishment.models.CDEcomLoadResult;

@RestController
@RequestMapping("/cdecom/v1.0")
public class CDEcomController {

    private final CDEcomBulkLoader bulkLoader;

    public CDEcomController(CDEcomBulkLoader bulkLoader) {
        this.bulkLoader = bulkLoader;
    }

    @PostMapping("/bulk")
    public ResponseEntity<CDEcomLoadResult> bulk(@RequestBody CDEcomLoadRequest req) {

        if (req == null || req.getFilePath() == null || req.getFilePath().isBlank()) {
            return ResponseEntity.badRequest().body(CDEcomLoadResult.error(null, "filePath is empty"));
        }

        CDEcomLoadResult result =
                bulkLoader.bulkLoad(req.getFilePath());

        HttpStatus status = "OK".equals(result.status())
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;

        return new ResponseEntity<>(result, status);
    }
}