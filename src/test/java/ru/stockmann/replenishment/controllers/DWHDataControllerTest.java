package ru.stockmann.replenishment.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHDatasetField;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHDatasetFieldsResponse;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHDatasetSummary;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHDatasetsResponse;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHDeleteRequestException;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHUniversalDeleteRequest;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHUniversalDeleteResult;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHUniversalDeletionService;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DWHDataControllerTest {

    @Test
    void exposesDatasetAndFieldMetadata() throws Exception {
        FakeService service = new FakeService();
        service.datasets = new DWHDatasetsResponse(List.of(
                new DWHDatasetSummary("CD_DATA", "CD data")
        ));
        service.fields = new DWHDatasetFieldsResponse(
                "CD_DATA", List.of(new DWHDatasetField(
                        "plan_rub", "Plan, rub", DWHExcelValueKind.DECIMAL
                ))
        );
        MockMvc mvc = mvc(service);

        mvc.perform(get("/data/v1.0/datasets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datasets[0].code").value("CD_DATA"));
        mvc.perform(get("/data/v1.0/datasets/CD_DATA/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[0].name").value("plan_rub"))
                .andExpect(jsonPath("$.fields[0].displayName").value("Plan, rub"))
                .andExpect(jsonPath("$.fields[0].type").value("DECIMAL"));
    }

    @Test
    void executesDeleteUsingBodyRequestedByAndIgnoresQueryParameter() throws Exception {
        FakeService service = new FakeService();
        service.result = DWHUniversalDeleteResult.deleted(91L, "CD_DATA", 18L);
        MockMvc mvc = mvc(service);

        mvc.perform(post("/data/v1.0/delete")
                        .queryParam("requestedBy", "url-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataset": "CD_DATA",
                                  "requestedBy": "body-user",
                                  "filters": {"god": 2026, "sezon": 2}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(91))
                .andExpect(jsonPath("$.dataset").value("CD_DATA"))
                .andExpect(jsonPath("$.deletedRows").value(18))
                .andExpect(jsonPath("$.dryRun").value(false))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        org.junit.jupiter.api.Assertions.assertEquals("body-user", service.requestedBy);
        org.junit.jupiter.api.Assertions.assertEquals("body-user", service.request.requestedBy());
        org.junit.jupiter.api.Assertions.assertEquals("CD_DATA", service.request.dataset());
        org.junit.jupiter.api.Assertions.assertEquals(2026, service.request.filters().get("god"));
    }

    @Test
    void rejectsRequestedByLongerThanOneHundredCharactersBeforeServiceCall() throws Exception {
        FakeService service = new FakeService();

        mvc(service).perform(post("/data/v1.0/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataset": "CD_DATA",
                                  "requestedBy": "%s",
                                  "filters": {"god": 2026}
                                }
                                """.formatted("x".repeat(101))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "requestedBy must not be longer than 100 characters"
                ));

        org.junit.jupiter.api.Assertions.assertNull(service.request);
    }

    @Test
    void returnsStableValidationCode() throws Exception {
        FakeService service = new FakeService();
        service.fieldsFailure = DWHDeleteRequestException.invalidDataset("UNKNOWN");

        mvc(service).perform(get("/data/v1.0/datasets/UNKNOWN/fields"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATASET"))
                .andExpect(jsonPath("$.error").value("Dataset 'UNKNOWN' is not available"));
    }

    private MockMvc mvc(DWHUniversalDeletionService service) {
        return MockMvcBuilders.standaloneSetup(new DWHDataController(service))
                .setControllerAdvice(new DWHApiExceptionHandler())
                .build();
    }

    private static final class FakeService extends DWHUniversalDeletionService {
        private DWHDatasetsResponse datasets;
        private DWHDatasetFieldsResponse fields;
        private DWHDeleteRequestException fieldsFailure;
        private DWHUniversalDeleteResult result;
        private DWHUniversalDeleteRequest request;
        private String requestedBy;

        private FakeService() {
            super(null, null, null, new ObjectMapper());
        }

        @Override
        public DWHDatasetsResponse datasets() {
            return datasets;
        }

        @Override
        public DWHDatasetFieldsResponse fields(String dataset) {
            if (fieldsFailure != null) {
                throw fieldsFailure;
            }
            return fields;
        }

        @Override
        public DWHUniversalDeleteResult execute(
                DWHUniversalDeleteRequest request,
                String requestedBy
        ) {
            this.request = request;
            this.requestedBy = requestedBy;
            return result;
        }
    }
}
