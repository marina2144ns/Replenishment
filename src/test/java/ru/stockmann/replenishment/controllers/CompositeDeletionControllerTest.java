package ru.stockmann.replenishment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.stockmann.replenishment.services.cddata.process.CDDataDeletionService;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomDeletionService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelDeletionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompositeDeletionControllerTest {

    @Test
    void cdDataDeletesByCompleteNazvanieDenOnly() throws Exception {
        FakeCDDataService service = new FakeCDDataService();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new CDDataController(null, null, service)).build();

        mvc.perform(delete("/cddata/v1.0/nazvanie-den")
                        .param("nazvanie", "Main").param("den", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(2));
        assertEquals("Main", service.nazvanie);
        assertEquals(15, service.den);
        assertInvalidCDRequests(mvc, "/cddata/v1.0/nazvanie-den");
        assertEquals(1, service.calls);
    }

    @Test
    void cdEcomDeletesByCompleteNazvanieDenOnly() throws Exception {
        FakeCDEcomService service = new FakeCDEcomService();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new CDEcomController(null, null, service)).build();

        mvc.perform(delete("/cdecom/v1.0/nazvanie-den")
                        .param("nazvanie", "Online").param("den", "16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(3));
        assertEquals("Online", service.nazvanie);
        assertEquals(16, service.den);
        assertInvalidCDRequests(mvc, "/cdecom/v1.0/nazvanie-den");
        assertEquals(1, service.calls);
    }

    @Test
    void salesDeletesByCompleteYearMonthAndRejectsMixedParameters() throws Exception {
        FakeSalesService service = new FakeSalesService();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new SalesByChannelController(null, null, service)).build();

        mvc.perform(delete("/salesbychannel/v1.0/year-month")
                        .param("year", "2026").param("month", "08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(0));
        assertEquals("2026", service.year);
        assertEquals("08", service.month);

        mvc.perform(delete("/salesbychannel/v1.0/year-month").param("year", "2026"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/salesbychannel/v1.0/year-month").param("month", "08"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/salesbychannel/v1.0/year-month")
                        .param("year", " ").param("month", "08"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/salesbychannel/v1.0/year-month")
                        .param("year", "2026").param("month", "08").param("week", "31"))
                .andExpect(status().isBadRequest());
        assertEquals(1, service.calls);
    }

    private static void assertInvalidCDRequests(MockMvc mvc, String url) throws Exception {
        mvc.perform(delete(url).param("nazvanie", "Main"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(url).param("den", "15"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(url).param("nazvanie", " ").param("den", "15"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(url).param("nazvanie", "Main").param("den", "bad"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(url).param("nazvanie", "x".repeat(256)).param("den", "15"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(url).param("nazvanie", "Main").param("den", "15")
                        .param("loadSessionId", "42"))
                .andExpect(status().isBadRequest());
    }

    private static final class FakeCDDataService extends CDDataDeletionService {
        private String nazvanie; private int den; private int calls;
        private FakeCDDataService() { super(null, null); }
        @Override public DWHDataDeleteResult deleteByNazvanieAndDen(String nazvanie, int den) {
            calls++; this.nazvanie = nazvanie; this.den = den; return new DWHDataDeleteResult(2);
        }
    }

    private static final class FakeCDEcomService extends CDEcomDeletionService {
        private String nazvanie; private int den; private int calls;
        private FakeCDEcomService() { super(null, null); }
        @Override public DWHDataDeleteResult deleteByNazvanieAndDen(String nazvanie, int den) {
            calls++; this.nazvanie = nazvanie; this.den = den; return new DWHDataDeleteResult(3);
        }
    }

    private static final class FakeSalesService extends SalesByChannelDeletionService {
        private String year; private String month; private int calls;
        private FakeSalesService() { super(null, null); }
        @Override public DWHDataDeleteResult deleteByYearAndMonth(String year, String month) {
            calls++; this.year = year; this.month = month; return new DWHDataDeleteResult(0);
        }
    }
}
