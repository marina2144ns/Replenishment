package ru.stockmann.replenishment.services.dwhexcelload.core;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDDataExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.WeeklyDataExcelLoadDefinition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractDWHExcelLoaderTest {

    @Test
    void commonCompletionPersistsStatisticsAndBuildsMessages() throws SQLException {
        RecordingStatement recording = new RecordingStatement();
        PreparedStatement statement = recording.proxy();
        Connection connection = (Connection) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> "prepareStatement".equals(method.getName())
                        ? statement : defaultValue(method.getReturnType()));
        DataSource dataSource = (DataSource) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{DataSource.class},
                (proxy, method, args) -> "getConnection".equals(method.getName())
                        ? connection : defaultValue(method.getReturnType()));

        TestLoader loader = new TestLoader(dataSource);
        DWHExcelLoadSessionResult result = new DWHExcelLoadSessionResult(
                10L, true, 7300L, 7300L, 0L, null);
        loader.finish(result, "SUCCESS");

        assertEquals(7300L, recording.values.get(1));
        assertEquals(7300L, recording.values.get(2));
        assertEquals(0L, recording.values.get(3));
        assertEquals("Success. Total raw rows: 7300. Loaded rows: 7300. Error rows: 0.",
                recording.values.get(4));
        assertEquals(10L, recording.values.get(5));
    }

    @Test
    void bindRawRowBindsExcelRowNumAsBigint() throws SQLException {
        TestLoader loader = new TestLoader();
        RecordingStatement statement = new RecordingStatement();
        PreparedStatement ps = statement.proxy();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("Value", "raw");

        loader.bind(ps, 10L, new ExcelRowData(2, values));

        assertEquals("setLong", statement.methods.get(1));
        assertEquals(2, statement.parameterIndexes.get(1));
        assertEquals(2L, statement.values.get(1));
        assertFalse(statement.methods.containsValue("setInt"));
    }

    @Test
    void weeklyDataTextValueLongerThanTargetIsNotTruncatedBeforeRawBinding() throws SQLException {
        TestLoader loader = new TestLoader(new WeeklyDataExcelLoadDefinition());
        String longText = "x".repeat(256);
        String[] row = new String[25];
        row[8] = " " + longText + " ";

        ExcelRowData normalized = loader.normalize(2, row);
        RecordingStatement statement = new RecordingStatement();

        loader.bind(statement.proxy(), 10L, normalized);

        assertEquals(longText, normalized.get("StoreRus"));
        assertEquals(256, ((String) statement.values.get(10)).length());
        assertEquals("setString", statement.methods.get(10));
    }

    @Test
    void cdDataTextValueLongerThanTargetIsNotTruncatedBeforeRawBinding() throws SQLException {
        TestLoader loader = new TestLoader(new CDDataExcelLoadDefinition());
        String longText = "x".repeat(256);
        String[] row = new String[37];
        row[0] = " " + longText + " ";

        ExcelRowData normalized = loader.normalize(2, row);
        RecordingStatement statement = new RecordingStatement();

        loader.bind(statement.proxy(), 20L, normalized);

        assertEquals(longText, normalized.get("nazvanie"));
        assertEquals(256, ((String) statement.values.get(2)).length());
        assertEquals("setString", statement.methods.get(2));
    }

    @Test
    void weeklyDataTextValueLongerThanRawStagingLimitIsNotTruncatedByJava() throws SQLException {
        TestLoader loader = new TestLoader(new WeeklyDataExcelLoadDefinition());
        String longText = "x".repeat(4001);
        String[] row = new String[25];
        row[8] = longText;

        ExcelRowData normalized = loader.normalize(2, row);
        RecordingStatement statement = new RecordingStatement();

        loader.bind(statement.proxy(), 10L, normalized);

        assertEquals(4001, normalized.get("StoreRus").length());
        assertEquals(4001, ((String) statement.values.get(10)).length());
    }

    @Test
    void cdDataTextValueLongerThanRawStagingLimitIsNotTruncatedByJava() throws SQLException {
        TestLoader loader = new TestLoader(new CDDataExcelLoadDefinition());
        String longText = "x".repeat(4001);
        String[] row = new String[37];
        row[0] = longText;

        ExcelRowData normalized = loader.normalize(2, row);
        RecordingStatement statement = new RecordingStatement();

        loader.bind(statement.proxy(), 20L, normalized);

        assertEquals(4001, normalized.get("nazvanie").length());
        assertEquals(4001, ((String) statement.values.get(2)).length());
    }

    @Test
    void weeklyDataBlankTextStillNormalizesToNull() {
        TestLoader loader = new TestLoader(new WeeklyDataExcelLoadDefinition());
        String[] row = new String[25];
        row[8] = "   ";

        ExcelRowData normalized = loader.normalize(2, row);

        assertNull(normalized.get("StoreRus"));
    }

    @Test
    void cdDataBlankTextStillNormalizesToNull() {
        TestLoader loader = new TestLoader(new CDDataExcelLoadDefinition());
        String[] row = new String[37];
        row[0] = "   ";

        ExcelRowData normalized = loader.normalize(2, row);

        assertNull(normalized.get("nazvanie"));
    }

    private static final class TestLoader extends AbstractDWHExcelLoader {

        private TestLoader() {
            super(null, new TestDefinition());
        }

        private TestLoader(DataSource dataSource) {
            super(dataSource, new TestDefinition());
        }

        private TestLoader(DWHExcelLoadDefinition definition) {
            super(null, definition);
        }

        private void bind(PreparedStatement ps, Long loadSessionId, ExcelRowData row) throws SQLException {
            bindRawRow(ps, loadSessionId, row);
        }

        private ExcelRowData normalize(int rowNum, String[] row) {
            return normalizeRow(rowNum, row);
        }

        private void finish(DWHExcelLoadSessionResult result, String status) {
            finishLoadSession(result.loadSessionId(), status, result.totalRows(), result.loadedRows(),
                    result.errorRows(), buildProcessingMessage(result.success(), result));
        }
    }

    private static final class TestDefinition implements DWHExcelLoadDefinition {

        @Override
        public DWHExcelLoadType loadType() {
            return DWHExcelLoadType.WEEKLY_DATA;
        }

        @Override
        public String rawTableName() {
            return "dbo.Test_raw";
        }

        @Override
        public String targetTableName() {
            return "dbo.Test";
        }

        @Override
        public String processProcedureName() {
            return "dbo.Test_ProcessLoadSession";
        }

        @Override
        public int expectedColumnCount() {
            return 1;
        }

        @Override
        public List<DWHExcelColumnSpec> columns() {
            return List.of(DWHExcelColumns.text(0, "Value", 255));
        }
    }

    private static final class RecordingStatement {
        private final Map<Integer, String> methods = new LinkedHashMap<>();
        private final Map<Integer, Integer> parameterIndexes = new LinkedHashMap<>();
        private final Map<Integer, Object> values = new LinkedHashMap<>();
        private int callIndex;

        private PreparedStatement proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().startsWith("set")) {
                    methods.put(callIndex, method.getName());
                    parameterIndexes.put(callIndex, (Integer) args[0]);
                    values.put(callIndex, args[1]);
                    callIndex++;
                    return null;
                }
                return defaultValue(method.getReturnType());
            };

            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    handler
            );
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }
}
