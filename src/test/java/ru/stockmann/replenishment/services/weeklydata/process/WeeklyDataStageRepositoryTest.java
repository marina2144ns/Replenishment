package ru.stockmann.replenishment.services.weeklydata.process;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyDataStageRepositoryTest {

    @Test
    void insertSqlAndBindingsMatchStageContractIncludingExcelRowNumber() {
        RecordingJdbc jdbc = new RecordingJdbc(new int[]{1});

        new WeeklyDataStageRepository(null).insertBatch(jdbc.connection(), 10L, List.of(stageRow()));

        assertTrue(jdbc.sql.contains("INSERT INTO dbo.Weekly_data_stage"));
        assertEquals(27, jdbc.calls.size());
        assertEquals("setLong:1:10", jdbc.calls.get(0));
        assertEquals("setLong:2:25", jdbc.calls.get(1));
        assertEquals("setShort:7:2025", jdbc.calls.get(6));
        assertEquals("setShort:8:10", jdbc.calls.get(7));
        assertEquals(1, jdbc.addBatchCalls);
        assertEquals(1, jdbc.executeBatchCalls);
        assertEquals(1, jdbc.clearBatchCalls);
    }

    @Test
    void nullableExcelRowNumberUsesBigintSqlType() {
        RecordingJdbc jdbc = new RecordingJdbc(new int[]{1});
        WeeklyDataStageRow row = withExcelRowNum(stageRow(), null);

        new WeeklyDataStageRepository(null).insertBatch(jdbc.connection(), 10L, List.of(row));

        assertEquals("setNull:2:" + Types.BIGINT, jdbc.calls.get(1));
    }

    @Test
    void rejectsWrongUpdateCountLengthAndFailedStatements() {
        RecordingJdbc shortCounts = new RecordingJdbc(new int[0]);
        assertThrows(IllegalStateException.class, () ->
                new WeeklyDataStageRepository(null).insertBatch(
                        shortCounts.connection(), 10L, List.of(stageRow())
                ));

        RecordingJdbc failed = new RecordingJdbc(new int[]{Statement.EXECUTE_FAILED});
        assertThrows(IllegalStateException.class, () ->
                new WeeklyDataStageRepository(null).insertBatch(
                        failed.connection(), 10L, List.of(stageRow())
                ));
    }

    private static WeeklyDataStageRow stageRow() {
        BigDecimal value = new BigDecimal("1.25");
        return new WeeklyDataStageRow(
                10L, 25L, null, null, null, null, (short) 2025, (short) 10,
                "channel", "storeBpo", "store", "divisionNew", "department", "seasonBudget", "type",
                value, value, value, value, value, value, value,
                "division", "season", "month", "bundle", "seasonality"
        );
    }

    private static WeeklyDataStageRow withExcelRowNum(WeeklyDataStageRow row, Long excelRowNum) {
        return new WeeklyDataStageRow(
                row.loadSessionId(), excelRowNum, row.year21(), row.week21(), row.yearCorr(), row.weekCorr(),
                row.year(), row.week(), row.salesChannelBpo(), row.storeRusBpo(), row.storeRus(),
                row.mfpDivisionNew(), row.mfpDepartment(), row.skuSeasonBudget(), row.typeOfSales(),
                row.totalStockPcs(), row.totalStockDdp(), row.salesPcs(), row.salesRub(), row.revenue(),
                row.gp(), row.discountTotalRub(), row.mfpDivision(), row.season(), row.month(),
                row.bundle(), row.seasonality()
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

        private Connection connection() {
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
