package ru.stockmann.replenishment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomProcessor;
import ru.stockmann.replenishment.services.CDEcomBulkLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelAsyncLoadService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CDEcomControllerTest {

    @Test
    void uploadReturnsCommonResponseAndStartsAsyncProcessing() {
        FakeCDEcomBulkLoader loader = new FakeCDEcomBulkLoader(
                DWHExcelLoadResult.ok(10L, "CD ecom file accepted for processing")
        );
        FakeAsyncLoadService asyncLoadService = new FakeAsyncLoadService();
        CDEcomController controller = new CDEcomController(loader, asyncLoadService, null);
        DWHExcelLoadRequest request = new DWHExcelLoadRequest();
        request.setFilePath("/tmp/cdecom.xlsx");

        ResponseEntity<?> response = controller.bulk(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(new DWHExcelLoadResult(10L, "OK", "CD ecom file accepted for processing"), response.getBody());
        assertEquals("/tmp/cdecom.xlsx", loader.filePath);
        assertSame(loader, asyncLoadService.loader);
        assertEquals(10L, asyncLoadService.loadSessionId);
        assertEquals("/tmp/cdecom.xlsx", asyncLoadService.filePath);
    }

    @Test
    void invalidFilePathReturnsCommonBadRequestResponse() {
        FakeCDEcomBulkLoader loader = new FakeCDEcomBulkLoader(
                DWHExcelLoadResult.ok(10L, "not used")
        );
        CDEcomController controller = new CDEcomController(loader, new FakeAsyncLoadService(), null);

        ResponseEntity<?> response = controller.bulk(new DWHExcelLoadRequest());

        assertEquals(400, response.getStatusCode().value());
        assertEquals(DWHExcelLoadResult.error(null, "filePath is empty"), response.getBody());
        assertEquals(0, loader.calls);
    }

    private static final class FakeCDEcomBulkLoader extends CDEcomBulkLoader {

        private final DWHExcelLoadResult result;
        private int calls;
        private String filePath;

        private FakeCDEcomBulkLoader(DWHExcelLoadResult result) {
            super(null, new CDEcomExcelLoadDefinition(), new FakeCDEcomProcessor());
            this.result = result;
        }

        @Override
        public DWHExcelLoadResult acceptFile(String filePath) {
            this.calls++;
            this.filePath = filePath;
            return result;
        }
    }

    private static final class FakeCDEcomProcessor extends CDEcomProcessor {

        private FakeCDEcomProcessor() {
            super(null, null, null, null, null, null, null);
        }
    }

    private static final class FakeAsyncLoadService extends DWHExcelAsyncLoadService {

        private AbstractDWHExcelLoader loader;
        private Long loadSessionId;
        private String filePath;

        @Override
        public void startAsync(AbstractDWHExcelLoader loader, Long loadSessionId, String filePath) {
            this.loader = loader;
            this.loadSessionId = loadSessionId;
            this.filePath = filePath;
        }
    }
}
