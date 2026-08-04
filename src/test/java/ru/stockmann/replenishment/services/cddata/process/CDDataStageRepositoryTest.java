package ru.stockmann.replenishment.services.cddata.process;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDDataStageRepositoryTest {

    @Test
    void insertsEveryStageColumnAndBindsTypedValuesIncludingExcelRowNumber() {
        RecordingJdbc jdbc = new RecordingJdbc(new int[]{1});

        new CDDataStageRepository().insertBatch(jdbc.connection(), 10L, List.of(stageRow(25L)));

        assertTrue(jdbc.sql.contains("INSERT INTO dbo.CD_data_stage"));
        assertFalse(jdbc.sql.contains("SELECT *"));
        assertEquals(40, jdbc.calls.size());
        assertEquals("setLong:1:10", jdbc.calls.get(0));
        assertEquals("setLong:2:25", jdbc.calls.get(1));
        assertEquals("setInt:4:2025", jdbc.calls.get(3));
        assertEquals("setDate:7:2025-01-31", jdbc.calls.get(6));
        assertEquals("setLong:19:123", jdbc.calls.get(18));
        assertEquals("setBigDecimal:21:1.25", jdbc.calls.get(20));
        assertEquals("setLong:40:99", jdbc.calls.get(39));
        assertEquals(1, jdbc.addBatchCalls);
        assertEquals(1, jdbc.executeBatchCalls);
        assertEquals(1, jdbc.clearBatchCalls);
    }

    @Test
    void nullableExcelRowNumberUsesBigintBinding() {
        RecordingJdbc jdbc = new RecordingJdbc(new int[]{1});

        new CDDataStageRepository().insertBatch(jdbc.connection(), 10L, List.of(stageRow(null)));

        assertEquals("setNull:2:" + Types.BIGINT, jdbc.calls.get(1));
    }

    @Test
    void validatesUpdateCountsStrictly() {
        RecordingJdbc wrongLength = new RecordingJdbc(new int[0]);
        assertThrows(IllegalStateException.class, () ->
                new CDDataStageRepository().insertBatch(
                        wrongLength.connection(), 10L, List.of(stageRow(25L))
                ));

        RecordingJdbc failed = new RecordingJdbc(new int[]{Statement.EXECUTE_FAILED});
        assertThrows(IllegalStateException.class, () ->
                new CDDataStageRepository().insertBatch(
                        failed.connection(), 10L, List.of(stageRow(25L))
                ));
    }

    @Test
    void rejectsRowsFromAnotherLoadSession() {
        RecordingJdbc jdbc = new RecordingJdbc(new int[]{1});

        assertThrows(IllegalArgumentException.class, () ->
                new CDDataStageRepository().insertBatch(
                        jdbc.connection(), 11L, List.of(stageRow(25L))
                ));
    }

    private static CDDataStageRow stageRow(Long excelRowNum) {
        BigDecimal decimal = new BigDecimal("1.25");
        return new CDDataStageRow(
                10L, excelRowNum, "name", 2025, 1, 31, Date.valueOf("2025-01-31"),
                "channel", "store", "division", "department", "subDepartment", "brand", "tm",
                "node", "section", "group", "campaign", 123L, "phase",
                decimal, decimal, decimal, decimal, decimal, decimal, decimal, decimal, decimal,
                decimal, decimal, 100, "drivers", "color", "composition", "supplier", "sku",
                "collection", "comment", 99L
        );
    }

    private static final class RecordingJdbc {
        private final int[] updateCounts;
        private final List<String> calls = new ArrayList<>();
        private String sql;
        private int addBatchCalls;
        private int executeBatchCalls;
        private int clearBatchCalls;

        private RecordingJdbc(int[] updateCounts) {
            this.updateCounts = Arrays.copyOf(updateCounts, updateCounts.length);
        }

        Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql = (String) args[0];
                            return statement();
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("set")) {
                            calls.add(method.getName() + ":" + args[0] + ":" + args[1]);
                            return null;
                        }
                        if ("addBatch".equals(method.getName())) {
                            addBatchCalls++;
                            return null;
                        }
                        if ("executeBatch".equals(method.getName())) {
                            executeBatchCalls++;
                            return Arrays.copyOf(updateCounts, updateCounts.length);
                        }
                        if ("clearBatch".equals(method.getName())) {
                            clearBatchCalls++;
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        return 0;
    }
}
