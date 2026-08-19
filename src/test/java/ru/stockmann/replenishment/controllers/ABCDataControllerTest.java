package ru.stockmann.replenishment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ru.stockmann.replenishment.models.ABCDataLoadRequest;
import ru.stockmann.replenishment.services.ABCBulkLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ABCDataControllerTest {

    @Test
    void validRequestDelegatesWithoutChangingModeOrResultContract() {
        RecordingLoader loader = new RecordingLoader(ABCBulkLoader.LoadResult.ok(7));
        ABCDataLoadRequest request = request("/data/abc.csv", "12R");

        ResponseEntity<ABCBulkLoader.LoadResult> response = new ABCDataController(loader).bulk(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("/data/abc.csv", loader.filePath);
        assertEquals("12R", loader.mode);
        assertEquals(new ABCBulkLoader.LoadResult("OK", 7, List.of()), response.getBody());
    }

    @Test
    void invalidModeContractIsUnchanged() {
        RecordingLoader loader = new RecordingLoader(ABCBulkLoader.LoadResult.ok(1));

        ResponseEntity<ABCBulkLoader.LoadResult> response =
                new ABCDataController(loader).bulk(request("/data/abc.csv", "3X"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals(0, loader.calls);
        assertEquals("ERROR", response.getBody().status());
    }

    private static ABCDataLoadRequest request(String path, String mode) {
        ABCDataLoadRequest request = new ABCDataLoadRequest();
        request.setFilePath(path);
        request.setMonth(mode);
        return request;
    }

    private static final class RecordingLoader extends ABCBulkLoader {
        private final LoadResult result;
        private int calls;
        private String filePath;
        private String mode;

        private RecordingLoader(LoadResult result) {
            super(null);
            this.result = result;
        }

        @Override
        public LoadResult bulkLoad(String filePath, String timePeriod) {
            calls++;
            this.filePath = filePath;
            this.mode = timePeriod;
            return result;
        }
    }
}
