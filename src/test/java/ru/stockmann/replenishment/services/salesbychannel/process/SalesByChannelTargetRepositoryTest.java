package ru.stockmann.replenishment.services.salesbychannel.process;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesByChannelTargetRepositoryTest {

    @Test
    void reprocessingCurrentSessionDeletesOnlyThatSessionBeforePublishingItsStageRows() {
        RecordingJdbc jdbc = new RecordingJdbc(5, 3);

        int published = new SalesByChannelTargetRepository()
                .publishFromStage(jdbc.connection(), 100L);

        assertEquals(3, published);
        assertEquals(2, jdbc.sql.size());
        String delete = normalize(jdbc.sql.get(0));
        assertEquals("DELETE FROM DBO.SALESBYCHANNEL WHERE LOADSESSIONID = ?", delete);

        String insert = normalize(jdbc.sql.get(1));
        assertTrue(insert.contains("INSERT INTO DBO.SALESBYCHANNEL"));
        assertTrue(insert.contains("FROM DBO.SALESBYCHANNEL_STAGE"));
        assertTrue(insert.contains("WHERE LOADSESSIONID = ?"));
        assertFalse(insert.contains("SELECT *"));
        assertFalse(insert.contains("EXCELROWNUM"));
        assertFalse(insert.contains("CREATEDAT"));
        assertFalse(insert.contains("VALUES ("));
        assertEquals(List.of(100L, 100L), jdbc.parameters);
    }

    @Test
    void publishingAnotherSessionForSameYearMonthDoesNotDeletePreviousSession() {
        RecordingJdbc jdbc = new RecordingJdbc(0, 1);

        new SalesByChannelTargetRepository().publishFromStage(jdbc.connection(), 101L);

        String delete = normalize(jdbc.sql.get(0));
        assertEquals("DELETE FROM DBO.SALESBYCHANNEL WHERE LOADSESSIONID = ?", delete);
        assertFalse(delete.contains("[YEAR]"));
        assertFalse(delete.contains("[MONTH]"));
        assertFalse(delete.contains("SALESBYCHANNEL_STAGE"));
        assertEquals(List.of(101L, 101L), jdbc.parameters);
    }

    @Test
    void publishingSessionCannotDeleteOtherSessionsOrBusinessPeriods() {
        RecordingJdbc jdbc = new RecordingJdbc(0, 2);

        new SalesByChannelTargetRepository().publishFromStage(jdbc.connection(), 101L);

        String delete = normalize(jdbc.sql.get(0));
        assertTrue(delete.contains("WHERE LOADSESSIONID = ?"));
        assertFalse(delete.contains("JOIN"));
        assertFalse(delete.contains("SELECT DISTINCT"));
        assertFalse(delete.contains("[YEAR]"));
        assertFalse(delete.contains("[MONTH]"));
    }

    @Test
    void insertAndSelectHaveExactlyTheSameThirtyOneColumnsInOrder() {
        RecordingJdbc jdbc = new RecordingJdbc(0, 1);
        new SalesByChannelTargetRepository().publishFromStage(jdbc.connection(), 10L);
        String sql = jdbc.sql.get(1).replaceAll("\\s+", " ").trim();

        String insertColumns = between(sql, "(", ") SELECT");
        String selectColumns = between(sql, "SELECT", " FROM dbo.SalesByChannel_stage");

        assertEquals(columns(insertColumns), columns(selectColumns));
        assertEquals(List.of(
                "LoadSessionId", "seasonYear", "season6m", "yearMonth", "yearSeason",
                "[year]", "[month]", "salesChannelType", "storeRus", "typeOfSales",
                "mfpDivision", "mfpDepartment", "campaignSalesType", "seasonality",
                "skuBrandType", "salesQuantity", "salesCurr", "gm", "discountTtl",
                "turnoverCurr", "skuSeasonBudget", "storeRusBpo", "salesChannelBpo",
                "mfpSubDepartment", "skuTm", "mfpNode", "section", "merchandiseSubGroup",
                "skuPhase", "skuProductClass", "RawRowId"
        ), columns(insertColumns));

        int storeRus = columns(insertColumns).indexOf("storeRus");
        int storeRusBpo = columns(insertColumns).indexOf("storeRusBpo");
        int skuTm = columns(insertColumns).indexOf("skuTm");
        int mfpNode = columns(insertColumns).indexOf("mfpNode");
        assertTrue(storeRus >= 0 && storeRusBpo >= 0 && storeRus != storeRusBpo);
        assertTrue(skuTm >= 0 && mfpNode >= 0 && skuTm != mfpNode);
    }

    private String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toUpperCase();
    }

    private String between(String value, String start, String end) {
        int from = value.indexOf(start) + start.length();
        return value.substring(from, value.indexOf(end, from));
    }

    private List<String> columns(String value) {
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(column -> !column.isEmpty())
                .toList();
    }

    private static final class RecordingJdbc {
        private final int[] updateCounts;
        private final List<String> sql = new ArrayList<>();
        private final List<Long> parameters = new ArrayList<>();
        private int statementIndex;

        private RecordingJdbc(int... updateCounts) {
            this.updateCounts = updateCounts;
        }

        Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql.add((String) args[0]);
                            int index = statementIndex++;
                            return statement(index);
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        PreparedStatement statement(int index) {
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if ("setLong".equals(method.getName())) {
                            assertEquals(1, args[0]);
                            parameters.add((Long) args[1]);
                            return null;
                        }
                        if ("executeUpdate".equals(method.getName())) return updateCounts[index];
                        return defaultValue(method.getReturnType());
                    });
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
