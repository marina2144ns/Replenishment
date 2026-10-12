package ru.stockmann.replenishment.services.dwhexcelload.delete;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.stockmann.replenishment.controllers.DWHApiExceptionHandler;
import ru.stockmann.replenishment.controllers.DWHDataController;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSession;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSessionRepository;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDDataExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.SalesByChannelExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.StoreTurnoverExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.WeeklyDataExcelLoadDefinition;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DWHUniversalDeletionServiceTest {

    private final DWHDatasetRegistry registry = new DWHDatasetRegistry(List.of(
            new CDDataExcelLoadDefinition(),
            new CDEcomExcelLoadDefinition(),
            new SalesByChannelExcelLoadDefinition(),
            new StoreTurnoverExcelLoadDefinition(),
            new WeeklyDataExcelLoadDefinition()
    ));

    @Test
    void metadataComesFromRegisteredDefinitions() {
        RecordingJdbc jdbc = new RecordingJdbc();
        DWHUniversalDeletionService service = service(jdbc, new Sessions());

        assertEquals(List.of(
                "CD_DATA", "CD_ECOM", "SALES_BY_CHANNEL", "STORE_TURNOVER", "WEEKLY_DATA"
        ), service.datasets().datasets().stream().map(DWHDatasetSummary::code).toList());

        DWHDatasetFieldsResponse cdData = service.fields("CD_DATA");
        DWHDatasetField planRub = cdData.fields().stream()
                .filter(field -> field.name().equals("plan_rub"))
                .findFirst().orElseThrow();
        assertEquals("Plan, rub", planRub.displayName());
        assertEquals(DWHExcelValueKind.DECIMAL, planRub.type());

        DWHDatasetField weeklyYear = service.fields("weekly_data").fields().stream()
                .filter(field -> field.name().equals("Year"))
                .findFirst().orElseThrow();
        assertEquals("Year", weeklyYear.displayName());
        assertEquals(DWHExcelValueKind.INT, weeklyYear.type());
    }

    @Test
    void validatesDatasetFieldsEmptyFiltersAndValueKinds() {
        DWHUniversalDeletionService service = service(new RecordingJdbc(), new Sessions());

        assertCode("INVALID_DATASET", () -> service.fields("ABC_DATA"));
        assertCode("EMPTY_DELETE_FILTER", () -> service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", null, Map.of(), false), null
        ));
        assertCode("EMPTY_DELETE_FILTER", () -> service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", null, null, false), null
        ));
        assertCode("INVALID_DELETE_FIELD", () -> service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", null, Map.of("x] OR 1=1 --", 1), false), null
        ));
        assertCode("INVALID_FILTER_VALUE", () -> service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", null, Map.of("god", "abc"), false), null
        ));
        assertCode("INVALID_FILTER_VALUE", () -> service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", null, Map.of("god", 2.5), false), null
        ));
        assertCode("INVALID_FILTER_VALUE", () -> service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", null, Map.of("plan_rub", "100.50"), false), null
        ));
        assertCode("INVALID_FILTER_VALUE", () -> service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", null, Map.of("store_rus", 10), false), null
        ));
        assertCode("INVALID_FILTER_VALUE", () -> service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", null, Map.of("data", "06.10.2026"), false), null
        ));
    }

    @Test
    void parameterizesAllSupportedTypesPersistsCriteriaAndSupportsNull() {
        RecordingJdbc jdbc = new RecordingJdbc();
        jdbc.updateCount = 18425L;
        Sessions sessions = new Sessions();
        DWHUniversalDeletionService service = service(jdbc, sessions);
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("god", 2026);
        filters.put("sku_style_color", 1234567890123L);
        filters.put("plan_rub", new BigDecimal("150000.50"));
        filters.put("data", "2026-10-06");
        filters.put("store_rus", "' OR 1=1 --");
        filters.put("sku_comment", null);

        DWHUniversalDeleteResult result = service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", "one-c-user", filters, false),
                "one-c-user"
        );

        assertEquals(991L, result.sessionId());
        assertEquals("CD_DATA", result.dataset());
        assertEquals(18425L, result.deletedRows());
        assertEquals("SUCCESS", result.status());
        assertFalse(result.dryRun());
        assertEquals(18425L, sessions.deletedRows);
        assertEquals("one-c-user", sessions.created.requestedBy());
        assertTrue(sessions.created.deleteCriteria().contains("\"god\":2026"));
        assertTrue(sessions.created.deleteCriteria().contains("\"sku_comment\":null"));
        assertTrue(jdbc.sql.startsWith("DELETE FROM [dbo].[CD_data] WHERE "));
        assertTrue(jdbc.sql.contains("[sku_comment] IS NULL"));
        assertFalse(jdbc.sql.contains("' OR 1=1 --"));
        assertEquals(List.of(
                "setInt:1:2026",
                "setLong:2:1234567890123",
                "setBigDecimal:3:150000.50",
                "setDate:4:2026-10-06",
                "setString:5:' OR 1=1 --"
        ), jdbc.bindings);
        assertTrue(jdbc.committed);
    }

    @Test
    void httpBodyRequestedByReachesDeletionSessionAndUrlValueIsIgnored() throws Exception {
        RecordingJdbc jdbc = new RecordingJdbc();
        jdbc.updateCount = 5L;
        Sessions sessions = new Sessions();
        MockMvc mvc = mvc(service(jdbc, sessions));

        mvc.perform(post("/data/v1.0/delete")
                        .queryParam("requestedBy", "UrlUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataset": "CD_DATA",
                                  "requestedBy": "TestUser",
                                  "filters": {"god": 2026, "sezon": 2},
                                  "dryRun": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedRows").value(5));

        assertEquals("TestUser", sessions.created.requestedBy());
    }

    @Test
    void httpUrlOnlyRequestedByIsIgnoredAndNullIsStored() throws Exception {
        RecordingJdbc jdbc = new RecordingJdbc();
        Sessions sessions = new Sessions();

        mvc(service(jdbc, sessions)).perform(post("/data/v1.0/delete")
                        .queryParam("requestedBy", "UrlOnlyUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dataset": "CD_DATA",
                                  "filters": {"god": 2026}
                                }
                                """))
                .andExpect(status().isOk());

        assertNull(sessions.created.requestedBy());
    }

    @Test
    void dryRunCountsWithoutCreatingDeletionSession() {
        RecordingJdbc jdbc = new RecordingJdbc();
        jdbc.count = 77L;
        Sessions sessions = new Sessions();
        DWHUniversalDeletionService service = service(jdbc, sessions);

        DWHUniversalDeleteResult result = service.execute(
                new DWHUniversalDeleteRequest(
                        "WEEKLY_DATA", "dry-run-user", Map.of("Year", 2026, "Week", 40), true
                ),
                "ignored-for-dry-run"
        );

        assertNull(result.sessionId());
        assertEquals(77L, result.matchedRows());
        assertEquals(0L, result.deletedRows());
        assertEquals("DRY_RUN", result.status());
        assertTrue(result.dryRun());
        assertNull(sessions.created);
        assertTrue(jdbc.sql.startsWith("SELECT COUNT_BIG(*) FROM [dbo].[Weekly_data]"));
    }

    @Test
    void failedDeleteRollsBackAndMarksSessionError() {
        RecordingJdbc jdbc = new RecordingJdbc();
        jdbc.failure = new SQLException("database unavailable");
        Sessions sessions = new Sessions();
        DWHUniversalDeletionService service = service(jdbc, sessions);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.execute(
                new DWHUniversalDeleteRequest("CD_DATA", null, Map.of("god", 2026), false), null
        ));

        assertTrue(exception.getMessage().contains("Failed to delete rows"));
        assertTrue(jdbc.rolledBack);
        assertEquals(991L, sessions.errorSessionId);
        assertTrue(sessions.errorMessage.contains("Failed to delete rows"));
    }

    private DWHUniversalDeletionService service(RecordingJdbc jdbc, Sessions sessions) {
        return new DWHUniversalDeletionService(
                jdbc.dataSource(), registry, new DWHUniversalDeletionRepository(),
                sessions, new ObjectMapper()
        );
    }

    private MockMvc mvc(DWHUniversalDeletionService service) {
        return MockMvcBuilders.standaloneSetup(new DWHDataController(service))
                .setControllerAdvice(new DWHApiExceptionHandler())
                .build();
    }

    private static void assertCode(String code, Runnable operation) {
        DWHDeleteRequestException exception = assertThrows(
                DWHDeleteRequestException.class, operation::run
        );
        assertEquals(code, exception.code());
    }

    private static final class Sessions extends DWHDeletionSessionRepository {
        private DWHDeletionSession created;
        private long deletedRows;
        private long errorSessionId;
        private String errorMessage;

        private Sessions() {
            super(null);
        }

        @Override
        public long create(DWHDeletionSession session) {
            this.created = session;
            return 991L;
        }

        @Override
        public void completeSuccess(Connection connection, long sessionId, long deletedRows) {
            assertEquals(991L, sessionId);
            this.deletedRows = deletedRows;
        }

        @Override
        public void completeError(long sessionId, String message) {
            this.errorSessionId = sessionId;
            this.errorMessage = message;
        }
    }

    private static final class RecordingJdbc {
        private String sql;
        private final List<String> bindings = new ArrayList<>();
        private long updateCount;
        private long count;
        private SQLException failure;
        private boolean committed;
        private boolean rolledBack;

        private DataSource dataSource() {
            return (DataSource) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{DataSource.class},
                    (proxy, method, args) -> "getConnection".equals(method.getName())
                            ? connection()
                            : defaultValue(method.getReturnType())
            );
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "prepareStatement" -> {
                            sql = (String) args[0];
                            bindings.clear();
                            yield statement();
                        }
                        case "getAutoCommit" -> true;
                        case "commit" -> {
                            committed = true;
                            yield null;
                        }
                        case "rollback" -> {
                            rolledBack = true;
                            yield null;
                        }
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }

        private PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("set")) {
                            bindings.add(method.getName() + ":" + args[0] + ":" + args[1]);
                            return null;
                        }
                        if ("executeLargeUpdate".equals(method.getName())) {
                            if (failure != null) {
                                throw failure;
                            }
                            return updateCount;
                        }
                        if ("executeQuery".equals(method.getName())) {
                            if (failure != null) {
                                throw failure;
                            }
                            return resultSet();
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private ResultSet resultSet() {
            class State { boolean next; }
            State state = new State();
            return (ResultSet) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> {
                            boolean value = !state.next;
                            state.next = true;
                            yield value;
                        }
                        case "getLong" -> count;
                        default -> defaultValue(method.getReturnType());
                    }
            );
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == int.class) return 0;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }
}
