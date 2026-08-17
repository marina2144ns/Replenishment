package ru.stockmann.replenishment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.stockmann.replenishment.services.DWHExcelStatusService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionNotFoundException;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadStatusResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DWHExcelStatusControllerTest {

    @Test
    void existingSuccessSessionReturnsOk() {
        DWHExcelStatusController controller = new DWHExcelStatusController(
                new FakeStatusService(result("SUCCESS"))
        );

        ResponseEntity<DWHExcelLoadStatusResult> response = controller.getStatus(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("SUCCESS", response.getBody().status());
        assertEquals(10L, response.getBody().loadSessionId());
    }

    @Test
    void existingRunningSessionReturnsOk() {
        DWHExcelStatusController controller = new DWHExcelStatusController(
                new FakeStatusService(result("RUNNING"))
        );

        ResponseEntity<DWHExcelLoadStatusResult> response = controller.getStatus(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("RUNNING", response.getBody().status());
    }

    @Test
    void existingErrorSessionReturnsOkWithBody() {
        DWHExcelStatusController controller = new DWHExcelStatusController(
                new FakeStatusService(result("ERROR"))
        );

        ResponseEntity<DWHExcelLoadStatusResult> response = controller.getStatus(10L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ERROR", response.getBody().status());
        assertEquals("message", response.getBody().message());
        assertEquals("2026-01-01T10:00", response.getBody().startedAt());
        assertEquals("2026-01-01T10:05", response.getBody().finishedAt());
    }

    @Test
    void deleteSessionJsonIncludesPersistedOperationMetadata() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new DWHExcelStatusController(
                new FakeStatusService(deleteResult())
        )).build();

        mvc.perform(get("/dwhexcel/v1.0/status/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadSessionId").value(10))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.operationType").value("DELETE"))
                .andExpect(jsonPath("$.operationMode").value("BY_CRITERIA"))
                .andExpect(jsonPath("$.deleteCriterion").value("NAZVANIE_DEN"))
                .andExpect(jsonPath("$.deleteParameter1Name").value("nazvanie"))
                .andExpect(jsonPath("$.deleteParameter1Value").value("Main"))
                .andExpect(jsonPath("$.deleteParameter2Name").value("den"))
                .andExpect(jsonPath("$.deleteParameter2Value").value("15"))
                .andExpect(jsonPath("$.deletedRows").value(27));
    }

    @Test
    void missingSessionReturnsNotFound() {
        DWHExcelStatusController controller = new DWHExcelStatusController(
                new FakeStatusService(null, null)
        );

        ResponseEntity<DWHExcelLoadStatusResult> response = controller.getStatus(404L);

        assertEquals(404, response.getStatusCode().value());
        assertEquals(null, response.getBody());
    }

    @Test
    void unexpectedArgumentErrorIsNotConvertedToNotFound() {
        DWHExcelStatusController controller = new DWHExcelStatusController(
                new FakeStatusService(null, new IllegalArgumentException("bad argument"))
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> controller.getStatus(10L)
        );

        assertEquals("bad argument", exception.getMessage());
    }

    private static DWHExcelLoadStatusResult result(String status) {
        return new DWHExcelLoadStatusResult(
                10L,
                "CD_ECOM",
                "CD ecom",
                "file.xlsx",
                "/tmp/file.xlsx",
                "LOAD",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                status,
                "message",
                "2026-01-01T10:00",
                "2026-01-01T10:05"
        );
    }

    private static DWHExcelLoadStatusResult deleteResult() {
        return new DWHExcelLoadStatusResult(
                10L,
                "CD_DATA",
                "CD data",
                null,
                null,
                "DELETE",
                "BY_CRITERIA",
                null,
                null,
                null,
                null,
                null,
                null,
                "NAZVANIE_DEN",
                "nazvanie",
                "Main",
                "den",
                "15",
                27L,
                "SUCCESS",
                "deleted",
                "2026-01-01T10:00",
                "2026-01-01T10:05"
        );
    }

    private static final class FakeStatusService extends DWHExcelStatusService {
        private final DWHExcelLoadStatusResult result;
        private final RuntimeException exception;

        private FakeStatusService(DWHExcelLoadStatusResult result) {
            this(result, null);
        }

        private FakeStatusService(DWHExcelLoadStatusResult result, RuntimeException exception) {
            super(null);
            this.result = result;
            this.exception = exception;
        }

        @Override
        public DWHExcelLoadStatusResult getStatus(Long loadSessionId) {
            if (exception != null) {
                throw exception;
            }
            if (result == null) {
                throw new DWHExcelLoadSessionNotFoundException(loadSessionId);
            }
            return result;
        }
    }
}
