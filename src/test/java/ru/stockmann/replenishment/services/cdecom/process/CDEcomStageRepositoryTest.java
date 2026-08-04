package ru.stockmann.replenishment.services.cdecom.process;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomStageRepositoryTest {

    @Test
    void insertsAllColumnsAndBindsLocalDateAndExcelRowNumber() {
        RecordingJdbc jdbc = new RecordingJdbc(new int[]{1});

        new CDEcomStageRepository().insertBatch(jdbc.connection(), 10L, List.of(stageRow(25L)));

        assertTrue(jdbc.sql.contains("INSERT INTO dbo.CD_ecom_stage"));
        assertFalse(jdbc.sql.contains("SELECT *"));
        assertEquals(41, jdbc.calls.size());
        assertEquals("setLong:1:10", jdbc.calls.get(0));
        assertEquals("setLong:2:25", jdbc.calls.get(1));
        assertEquals("setInt:4:2025", jdbc.calls.get(3));
        assertEquals("setObject:7:2025-01-31:" + Types.DATE, jdbc.calls.get(6));
        assertEquals("setLong:19:123", jdbc.calls.get(18));
        assertEquals("setBigDecimal:21:1.25", jdbc.calls.get(20));
        assertEquals("setLong:41:99", jdbc.calls.get(40));
        assertEquals(1, jdbc.addBatchCalls);
        assertEquals(1, jdbc.executeBatchCalls);
        assertEquals(1, jdbc.clearBatchCalls);
    }

    @Test
    void nullExcelRowNumberUsesBigintAndCountsAreStrict() {
        RecordingJdbc nullable = new RecordingJdbc(new int[]{1});
        new CDEcomStageRepository().insertBatch(nullable.connection(), 10L, List.of(stageRow(null)));
        assertEquals("setNull:2:" + Types.BIGINT, nullable.calls.get(1));

        assertThrows(IllegalStateException.class, () -> new CDEcomStageRepository().insertBatch(
                new RecordingJdbc(new int[0]).connection(), 10L, List.of(stageRow(25L))
        ));
        assertThrows(IllegalStateException.class, () -> new CDEcomStageRepository().insertBatch(
                new RecordingJdbc(new int[]{Statement.EXECUTE_FAILED}).connection(),
                10L, List.of(stageRow(25L))
        ));
    }

    @Test
    void rejectsAnotherLoadSession() {
        assertThrows(IllegalArgumentException.class, () -> new CDEcomStageRepository().insertBatch(
                new RecordingJdbc(new int[]{1}).connection(), 11L, List.of(stageRow(25L))
        ));
    }

    private static CDEcomStageRow stageRow(Long excelRowNum) {
        BigDecimal decimal = new BigDecimal("1.25");
        return new CDEcomStageRow(
                10L, excelRowNum, "name", 2025, 1, 31, LocalDate.of(2025, 1, 31),
                "channel", "store", "division", "department", "subDepartment", "brand", "tm",
                "node", "section", "group", "campaign", 123L, "phase",
                decimal, decimal, decimal, decimal, decimal, decimal, decimal, decimal, decimal,
                decimal, 100L, 200L, 300L, "drivers", "supplier", "composition", "color", "sku",
                "comment", "collection", 99L
        );
    }

    private static final class RecordingJdbc {
        final int[] updateCounts;
        final List<String> calls = new ArrayList<>();
        String sql;
        int addBatchCalls;
        int executeBatchCalls;
        int clearBatchCalls;
        RecordingJdbc(int[] updateCounts) { this.updateCounts = updateCounts.clone(); }
        Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql = (String) args[0];
                            return statement();
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
        PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("set")) {
                            String call = method.getName() + ":" + args[0] + ":" + args[1];
                            if ("setObject".equals(method.getName())) call += ":" + args[2];
                            calls.add(call);
                            return null;
                        }
                        if ("addBatch".equals(method.getName())) { addBatchCalls++; return null; }
                        if ("executeBatch".equals(method.getName())) {
                            executeBatchCalls++;
                            return Arrays.copyOf(updateCounts, updateCounts.length);
                        }
                        if ("clearBatch".equals(method.getName())) { clearBatchCalls++; return null; }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        return 0;
    }
}
