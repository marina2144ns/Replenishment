package ru.stockmann.replenishment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.stockmann.replenishment.services.cddata.process.CDDataDeletionService;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomDeletionService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDataDeleteResult;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataDeletionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TargetDataDeletionControllerTest {

    @Test
    void weeklyDataEndpointsDeleteByPeriodAndLoadSession() throws Exception {
        FakeWeeklyDeletionService service = new FakeWeeklyDeletionService(1250, 17);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new WeeklyDataController(null, null, service))
                .build();

        mvc.perform(delete("/weeklydata/v1.0/year-week").param("year", "2026").param("week", "31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(1250));
        mvc.perform(delete("/weeklydata/v1.0/session").param("loadSessionId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(17));

        assertEquals(2026, service.year);
        assertEquals(31, service.week);
        assertEquals(42L, service.loadSessionId);
    }

    @Test
    void weeklyDataPeriodRequiresBothParameters() throws Exception {
        FakeWeeklyDeletionService service = new FakeWeeklyDeletionService(0, 0);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new WeeklyDataController(null, null, service))
                .build();

        assertMissingPeriodRejected(mvc, "/weeklydata/v1.0/year-week");
        assertEquals(0, service.calls);
    }

    @Test
    void cdDataEndpointsDeleteByPeriodAndLoadSession() throws Exception {
        FakeCDDataDeletionService service = new FakeCDDataDeletionService(25, 0);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new CDDataController(null, null, service))
                .build();

        mvc.perform(delete("/cddata/v1.0/god-sezon").param("god", "2026").param("sezon", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(25));
        mvc.perform(delete("/cddata/v1.0/session").param("loadSessionId", "43"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(0));

        assertEquals(2026, service.year);
        assertEquals(2, service.week);
        assertEquals(43L, service.loadSessionId);
    }

    @Test
    void cdDataPeriodRequiresBothParameters() throws Exception {
        FakeCDDataDeletionService service = new FakeCDDataDeletionService(0, 0);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new CDDataController(null, null, service))
                .build();

        assertMissingCDDataPeriodRejected(mvc);
        assertEquals(0, service.calls);
    }

    @Test
    void cdecomEndpointsDeleteByPeriodAndLoadSession() throws Exception {
        FakeCDEcomDeletionService service = new FakeCDEcomDeletionService(30, 5);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new CDEcomController(null, null, service))
                .build();

        mvc.perform(delete("/cdecom/v1.0/year-week").param("year", "2026").param("week", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(30));
        mvc.perform(delete("/cdecom/v1.0/session").param("loadSessionId", "44"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(5));

        assertEquals(2026, service.year);
        assertEquals(2, service.week);
        assertEquals(44L, service.loadSessionId);
    }

    @Test
    void cdecomPeriodRequiresBothParametersAndLoadSessionMustBePositive() throws Exception {
        FakeCDEcomDeletionService service = new FakeCDEcomDeletionService(0, 0);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new CDEcomController(null, null, service))
                .build();

        assertMissingPeriodRejected(mvc, "/cdecom/v1.0/year-week");
        mvc.perform(delete("/cdecom/v1.0/session").param("loadSessionId", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/cdecom/v1.0/session").param("loadSessionId", "not-a-number"))
                .andExpect(status().isBadRequest());
        assertEquals(0, service.calls);
    }

    private static void assertMissingPeriodRejected(MockMvc mvc, String url) throws Exception {
        mvc.perform(delete(url).param("year", "2026"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(url).param("week", "2"))
                .andExpect(status().isBadRequest());
    }

    private static void assertMissingCDDataPeriodRejected(MockMvc mvc) throws Exception {
        mvc.perform(delete("/cddata/v1.0/god-sezon").param("god", "2026"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/cddata/v1.0/god-sezon").param("sezon", "2"))
                .andExpect(status().isBadRequest());
    }

    private static final class FakeWeeklyDeletionService extends WeeklyDataDeletionService {
        private final long periodRows;
        private final long sessionRows;
        private int year;
        private int week;
        private long loadSessionId;
        private int calls;

        private FakeWeeklyDeletionService(long periodRows, long sessionRows) {
            super(null, null);
            this.periodRows = periodRows;
            this.sessionRows = sessionRows;
        }

        @Override
        public DWHDataDeleteResult deleteByPeriod(short year, short week) {
            calls++;
            this.year = year;
            this.week = week;
            return new DWHDataDeleteResult(periodRows);
        }

        @Override
        public DWHDataDeleteResult deleteByLoadSessionId(long loadSessionId) {
            calls++;
            this.loadSessionId = loadSessionId;
            return new DWHDataDeleteResult(sessionRows);
        }
    }

    private static final class FakeCDDataDeletionService extends CDDataDeletionService {
        private final long periodRows;
        private final long sessionRows;
        private int year;
        private int week;
        private long loadSessionId;
        private int calls;

        private FakeCDDataDeletionService(long periodRows, long sessionRows) {
            super(null, null);
            this.periodRows = periodRows;
            this.sessionRows = sessionRows;
        }

        @Override
        public DWHDataDeleteResult deleteByPeriod(int year, int week) {
            calls++;
            this.year = year;
            this.week = week;
            return new DWHDataDeleteResult(periodRows);
        }

        @Override
        public DWHDataDeleteResult deleteByLoadSessionId(long loadSessionId) {
            calls++;
            this.loadSessionId = loadSessionId;
            return new DWHDataDeleteResult(sessionRows);
        }
    }

    private static final class FakeCDEcomDeletionService extends CDEcomDeletionService {
        private final long periodRows;
        private final long sessionRows;
        private int year;
        private int week;
        private long loadSessionId;
        private int calls;

        private FakeCDEcomDeletionService(long periodRows, long sessionRows) {
            super(null, null);
            this.periodRows = periodRows;
            this.sessionRows = sessionRows;
        }

        @Override
        public DWHDataDeleteResult deleteByPeriod(int year, int week) {
            calls++;
            this.year = year;
            this.week = week;
            return new DWHDataDeleteResult(periodRows);
        }

        @Override
        public DWHDataDeleteResult deleteByLoadSessionId(long loadSessionId) {
            calls++;
            this.loadSessionId = loadSessionId;
            return new DWHDataDeleteResult(sessionRows);
        }
    }
}
