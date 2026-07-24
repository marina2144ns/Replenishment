package ru.stockmann.replenishment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelAsyncLoadService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.SalesByChannelExcelLoadDefinition;
import ru.stockmann.replenishment.services.salesbychannel.SalesByChannelBulkLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SalesByChannelControllerTest {

    @Test
    void endpointMatchesEstablishedServicePattern() throws Exception {
        RequestMapping classMapping = SalesByChannelController.class.getAnnotation(RequestMapping.class);
        PostMapping methodMapping = SalesByChannelController.class
                .getDeclaredMethod("bulk", DWHExcelLoadRequest.class)
                .getAnnotation(PostMapping.class);

        assertEquals("/salesbychannel/v1.0", classMapping.value()[0]);
        assertEquals("/bulk", methodMapping.value()[0]);
    }

    @Test
    void uploadUsesCommonResponseAndStartsAsyncRawLoad() {
        FakeLoader loader = new FakeLoader(DWHExcelLoadResult.ok(31L, "accepted"));
        FakeAsyncLoadService async = new FakeAsyncLoadService();
        SalesByChannelController controller = new SalesByChannelController(loader, async);
        DWHExcelLoadRequest request = new DWHExcelLoadRequest();
        request.setFilePath("/tmp/sales-by-channel.xlsx");

        ResponseEntity<?> response = controller.bulk(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(new DWHExcelLoadResult(31L, "OK", "accepted"), response.getBody());
        assertEquals("/tmp/sales-by-channel.xlsx", loader.filePath);
        assertSame(loader, async.loader);
        assertEquals(31L, async.loadSessionId);
    }

    @Test
    void blankPathReturnsCommonBadRequest() {
        FakeLoader loader = new FakeLoader(DWHExcelLoadResult.ok(31L, "unused"));
        SalesByChannelController controller = new SalesByChannelController(loader, new FakeAsyncLoadService());

        ResponseEntity<?> response = controller.bulk(new DWHExcelLoadRequest());

        assertEquals(400, response.getStatusCode().value());
        assertEquals(DWHExcelLoadResult.error(null, "filePath is empty"), response.getBody());
        assertEquals(0, loader.calls);
    }

    private static final class FakeLoader extends SalesByChannelBulkLoader {
        private final DWHExcelLoadResult result;
        private int calls;
        private String filePath;

        private FakeLoader(DWHExcelLoadResult result) {
            super(null, new SalesByChannelExcelLoadDefinition());
            this.result = result;
        }

        @Override
        public DWHExcelLoadResult acceptFile(String filePath) {
            calls++;
            this.filePath = filePath;
            return result;
        }
    }

    private static final class FakeAsyncLoadService extends DWHExcelAsyncLoadService {
        private AbstractDWHExcelLoader loader;
        private Long loadSessionId;

        @Override
        public void startAsync(AbstractDWHExcelLoader loader, Long loadSessionId, String filePath) {
            this.loader = loader;
            this.loadSessionId = loadSessionId;
        }
    }
}
