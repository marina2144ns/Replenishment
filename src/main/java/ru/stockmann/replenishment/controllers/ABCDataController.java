package ru.stockmann.replenishment.controllers;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.stockmann.replenishment.models.ABCDataLoadRequest;
import ru.stockmann.replenishment.services.ABCBulkLoader;


import java.util.Set;

@RestController
@RequestMapping("/abcdata/v1.0")
public class ABCDataController {

    private final ABCBulkLoader bulkLoader;

    public ABCDataController(ABCBulkLoader bulkLoader) {
        this.bulkLoader = bulkLoader;
    }

    private static final Set<String> VALID = Set.of("3U","3R","6U","6R","12U","12R");

    @PostMapping("/bulk")
    public ResponseEntity<ABCBulkLoader.LoadResult> bulk(@RequestBody ABCDataLoadRequest req) {
        if (req == null || req.getFilePath() == null || req.getFilePath().isBlank()) {
            return ResponseEntity.badRequest().body(ABCBulkLoader.LoadResult.error("filePath is empty"));
        }
        if (req.getMonth() == null || !VALID.contains(req.getMonth())) {
            return ResponseEntity.badRequest().body(ABCBulkLoader.LoadResult.error("month must be one of " + VALID));
        }

        ABCBulkLoader.LoadResult result = bulkLoader.bulkLoad(req.getFilePath(), req.getMonth());
        HttpStatus status = "OK".equals(result.status()) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        return new ResponseEntity<>(result, status);
    }
}