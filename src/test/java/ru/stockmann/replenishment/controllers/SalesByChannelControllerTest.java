package ru.stockmann.replenishment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelAsyncLoadService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.SalesByChannelExcelLoadDefinition;
import ru.stockmann.replenishment.services.salesbychannel.SalesByChannelBulkLoader;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelProcessResult;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelProcessor;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelDeletionService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;

import java.util.Map;

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

        DeleteMapping yearMonth = SalesByChannelController.class
                .getDeclaredMethod("deleteByYearAndMonth", Map.class)
                .getAnnotation(DeleteMapping.class);
        DeleteMapping loadSession = SalesByChannelController.class
                .getDeclaredMethod("deleteByLoadSessionId", Long.class)
                .getAnnotation(DeleteMapping.class);
        assertEquals("/year-month", yearMonth.value()[0]);
        assertEquals("/session", loadSession.value()[0]);
    }

    @Test
    void deleteEndpointsUseEstablishedResponseAndValidation() {
        FakeDeletionService deletion = new FakeDeletionService();
        SalesByChannelController controller =
                new SalesByChannelController(null, null, deletion);

        assertEquals(new DWHDataDeleteResult(12), controller.deleteByYearAndMonth(
                Map.of("year", "2026", "month", "7")).getBody());
        assertEquals("2026", deletion.year);
        assertEquals("7", deletion.month);

        assertEquals(new DWHDataDeleteResult(7),
                controller.deleteByLoadSessionId(10521L).getBody());
        assertEquals(10521L, deletion.loadSessionId);

        assertEquals(400, controller.deleteByLoadSessionId(0L).getStatusCode().value());
    }

    @Test
    void uploadUsesCommonResponseAndStartsAsyncRawLoad() {
        FakeLoader loader = new FakeLoader(DWHExcelLoadResult.ok(31L, "accepted"));
        FakeAsyncLoadService async = new FakeAsyncLoadService();
        SalesByChannelController controller = new SalesByChannelController(loader, async, null);
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
        SalesByChannelController controller =
                new SalesByChannelController(loader, new FakeAsyncLoadService(), null);

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
            super(null, new SalesByChannelExcelLoadDefinition(), new NoOpProcessor());
            this.result = result;
        }

        @Override
        public DWHExcelLoadResult acceptFile(String filePath) {
            calls++;
            this.filePath = filePath;
            return result;
        }
    }

    private static final class NoOpProcessor extends SalesByChannelProcessor {
        private NoOpProcessor() {
            super(null, null, null, null, null, null, null, 1_000);
        }

        @Override
        public SalesByChannelProcessResult process(long loadSessionId) {
            throw new AssertionError("Controller must not invoke processor directly");
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

    private static final class FakeDeletionService extends SalesByChannelDeletionService {
        private String year;
        private String month;
        private long loadSessionId;

        private FakeDeletionService() {
            super(null, null);
        }

        @Override
        public DWHDataDeleteResult deleteByYearAndMonth(String year, String month) {
            this.year = year;
            this.month = month;
            return new DWHDataDeleteResult(12);
        }

        @Override
        public DWHDataDeleteResult deleteByLoadSessionId(long loadSessionId) {
            this.loadSessionId = loadSessionId;
            return new DWHDataDeleteResult(7);
        }
    }
}
