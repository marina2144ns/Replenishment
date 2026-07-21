package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomTargetRepository;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomTargetRow;
import ru.stockmann.replenishment.services.cddata.process.CDDataTargetRepository;
import ru.stockmann.replenishment.services.cddata.process.CDDataTargetRow;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.core.ExcelRowData;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDDataExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.WeeklyDataExcelLoadDefinition;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataTargetRepository;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataTargetRow;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.definitionColumns;

class DWHJdbcBindingSchemaContractTest {

    @Test
    void weeklyDataCommonRawInsertBindsLoadSessionExcelRowAndBusinessValuesInDefinitionOrder() throws Exception {
        assertRawBinding(new WeeklyDataExcelLoadDefinition());
    }

    @Test
    void cdDataCommonRawInsertBindsLoadSessionExcelRowAndBusinessValuesInDefinitionOrder() throws Exception {
        assertRawBinding(new CDDataExcelLoadDefinition());
    }

    @Test
    void cdecomCommonRawInsertBindsLoadSessionExcelRowAndBusinessValuesInDefinitionOrder() throws Exception {
        assertRawBinding(new CDEcomExcelLoadDefinition());
    }

    @Test
    void weeklyDataTargetInsertBindsParametersInInsertColumnOrder() {
        RecordingJdbc jdbc = new RecordingJdbc();

        new WeeklyDataTargetRepository(null).insertAll(jdbc.connection(), List.of(weeklyTargetRow()));

        assertEquals(26, jdbc.statement.calls.size());
        assertSetter(jdbc, 1, "setLong", 1);
        for (int parameter = 2; parameter <= 7; parameter++) {
            assertSetter(jdbc, parameter, "setShort", parameter);
        }
        for (int parameter = 8; parameter <= 14; parameter++) {
            assertSetter(jdbc, parameter, "setString", parameter);
        }
        for (int parameter = 15; parameter <= 21; parameter++) {
            assertSetter(jdbc, parameter, "setBigDecimal", parameter);
        }
        for (int parameter = 22; parameter <= 26; parameter++) {
            assertSetter(jdbc, parameter, "setString", parameter);
        }
        assertEquals(1, jdbc.statement.addBatchCalls);
        assertEquals(1, jdbc.statement.executeBatchCalls);
    }

    @Test
    void weeklyDataTargetInsertBindsNullSmallintFieldsAsSqlSmallint() {
        RecordingJdbc jdbc = new RecordingJdbc();

        new WeeklyDataTargetRepository(null).insertAll(jdbc.connection(), List.of(weeklyTargetRowWithNullSmallints()));

        assertEquals(26, jdbc.statement.calls.size());
        assertSetter(jdbc, 1, "setLong", 1);
        for (int parameter = 2; parameter <= 7; parameter++) {
            assertSetter(jdbc, parameter, "setNull", parameter);
            assertEquals(Types.SMALLINT, jdbc.statement.calls.get(parameter - 1).sqlType(), "parameter " + parameter);
        }
        assertEquals(1, jdbc.statement.addBatchCalls);
        assertEquals(1, jdbc.statement.executeBatchCalls);
    }

    @Test
    void cdDataTargetInsertBindsParametersInInsertColumnOrder() {
        RecordingJdbc jdbc = new RecordingJdbc();

        new CDDataTargetRepository(null).insertAll(jdbc.connection(), List.of(cdDataTargetRow()));

        assertEquals(38, jdbc.statement.calls.size());
        assertSetter(jdbc, 1, "setLong", 1);
        assertSetter(jdbc, 2, "setString", 2);
        assertSetter(jdbc, 3, "setInt", 3);
        assertSetter(jdbc, 4, "setInt", 4);
        assertSetter(jdbc, 5, "setInt", 5);
        assertSetter(jdbc, 6, "setDate", 6);
        for (int parameter = 7; parameter <= 17; parameter++) {
            assertSetter(jdbc, parameter, "setString", parameter);
        }
        assertSetter(jdbc, 18, "setLong", 18);
        assertSetter(jdbc, 19, "setString", 19);
        for (int parameter = 20; parameter <= 30; parameter++) {
            assertSetter(jdbc, parameter, "setBigDecimal", parameter);
        }
        assertSetter(jdbc, 31, "setInt", 31);
        for (int parameter = 32; parameter <= 38; parameter++) {
            assertSetter(jdbc, parameter, "setString", parameter);
        }
        assertEquals(1, jdbc.statement.addBatchCalls);
        assertEquals(1, jdbc.statement.executeBatchCalls);
    }

    @Test
    void cdecomTargetInsertBindsParametersInInsertColumnOrder() {
        RecordingJdbc jdbc = new RecordingJdbc();

        new CDEcomTargetRepository(null).insertAll(jdbc.connection(), List.of(cdecomTargetRow()));

        assertEquals(39, jdbc.statement.calls.size());
        assertSetter(jdbc, 1, "setLong", 1);
        assertSetter(jdbc, 2, "setString", 2);
        assertSetter(jdbc, 3, "setInt", 3);
        assertSetter(jdbc, 4, "setInt", 4);
        assertSetter(jdbc, 5, "setInt", 5);
        assertSetter(jdbc, 6, "setDate", 6);
        for (int parameter = 7; parameter <= 17; parameter++) {
            assertSetter(jdbc, parameter, "setString", parameter);
        }
        assertSetter(jdbc, 18, "setLong", 18);
        assertSetter(jdbc, 19, "setString", 19);
        for (int parameter = 20; parameter <= 29; parameter++) {
            assertSetter(jdbc, parameter, "setBigDecimal", parameter);
        }
        assertSetter(jdbc, 30, "setLong", 30);
        assertSetter(jdbc, 31, "setLong", 31);
        assertSetter(jdbc, 32, "setLong", 32);
        for (int parameter = 33; parameter <= 39; parameter++) {
            assertSetter(jdbc, parameter, "setString", parameter);
        }
        assertEquals(1, jdbc.statement.addBatchCalls);
        assertEquals(1, jdbc.statement.executeBatchCalls);
    }

    private static void assertRawBinding(DWHExcelLoadDefinition definition) throws SQLException {
        TestLoader loader = new TestLoader(definition);
        Map<String, String> values = new LinkedHashMap<>();
        for (var column : definition.columns()) {
            values.put(column.rawColumnName(), "value");
        }
        RecordingStatement statement = new RecordingStatement();

        loader.bind(statement.proxy(), 10L, new ExcelRowData(2, values));

        assertEquals(definition.columns().size() + 2, statement.calls.size());
        assertSetter(statement, 1, "setLong", 1);
        assertSetter(statement, 2, "setLong", 2);
        for (int parameter = 3; parameter <= definition.columns().size() + 2; parameter++) {
            assertSetter(statement, parameter, "setString", parameter);
        }
    }

    private static WeeklyDataTargetRow weeklyTargetRow() {
        BigDecimal decimal = new BigDecimal("1.23");
        return new WeeklyDataTargetRow(
                10L,
                (short) 2025,
                (short) 10,
                (short) 2025,
                (short) 10,
                (short) 2025,
                (short) 10,
                "SalesChannelBpo",
                "StoreRusBpo",
                "StoreRus",
                "MfpDivisionNew",
                "MfpDepartment",
                "SkuSeasonBudget",
                "TypeOfSales",
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                "MfpDivision",
                "Season",
                "Month",
                "Bundle",
                "Seasonality"
        );
    }

    private static WeeklyDataTargetRow weeklyTargetRowWithNullSmallints() {
        BigDecimal decimal = new BigDecimal("1.23");
        return new WeeklyDataTargetRow(
                10L,
                null,
                null,
                null,
                null,
                null,
                null,
                "SalesChannelBpo",
                "StoreRusBpo",
                "StoreRus",
                "MfpDivisionNew",
                "MfpDepartment",
                "SkuSeasonBudget",
                "TypeOfSales",
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                "MfpDivision",
                "Season",
                "Month",
                "Bundle",
                "Seasonality"
        );
    }

    private static CDDataTargetRow cdDataTargetRow() {
        BigDecimal decimal = new BigDecimal("1.23");
        return new CDDataTargetRow(
                10L,
                "nazvanie",
                2025,
                1,
                31,
                Date.valueOf("2025-01-31"),
                "salesChannel",
                "storeRus",
                "mfpDivision",
                "mfpDepartment",
                "mfpSubDepartment",
                "skuBrandType",
                "skuTm",
                "mfpNode",
                "section",
                "merchandiseSubGroup",
                "campaignSales",
                123L,
                "skuPhase",
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                100,
                "draiveryCd",
                "skuColorRus",
                "skuComposition",
                "skuSupplier",
                "skuName",
                "skuCollection",
                "skuComment"
        );
    }

    private static CDEcomTargetRow cdecomTargetRow() {
        BigDecimal decimal = new BigDecimal("1.23");
        return new CDEcomTargetRow(
                10L,
                "name",
                2025,
                1,
                31,
                Date.valueOf("2025-01-31"),
                "salesChannelBpo",
                "storeRus",
                "mfpDivision",
                "mfpDepartment",
                "mfpSubDepartment",
                "skuBrandType",
                "skuTm",
                "mfpNode",
                "section",
                "merchandiseSubGroup",
                "campaignSalesType",
                123L,
                "skuPhase",
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                decimal,
                100L,
                200L,
                300L,
                "cdDrivers",
                "skuSupplierModel",
                "skuComposition",
                "skuColorRussian",
                "skuName",
                "skuCommentBuyer",
                "skuCollection"
        );
    }

    private static void assertSetter(RecordingJdbc jdbc, int callNumber, String method, int parameterIndex) {
        assertSetter(jdbc.statement, callNumber, method, parameterIndex);
    }

    private static void assertSetter(RecordingStatement statement, int callNumber, String method, int parameterIndex) {
        RecordingStatement.Call call = statement.calls.get(callNumber - 1);
        assertEquals(method, call.method(), "call " + callNumber);
        assertEquals(parameterIndex, call.parameterIndex(), "call " + callNumber);
    }

    private static final class TestLoader extends AbstractDWHExcelLoader {

        private TestLoader(DWHExcelLoadDefinition definition) {
            super(null, definition);
        }

        private void bind(PreparedStatement ps, Long loadSessionId, ExcelRowData row) throws SQLException {
            bindRawRow(ps, loadSessionId, row);
        }
    }

    private static final class RecordingJdbc {
        private final RecordingStatement statement = new RecordingStatement();

        private Connection connection() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    return statement.proxy();
                }
                return defaultValue(method.getReturnType());
            };
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    handler
            );
        }
    }

    private static final class RecordingStatement {
        private final List<Call> calls = new java.util.ArrayList<>();
        private int addBatchCalls;
        private int executeBatchCalls;

        private PreparedStatement proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().startsWith("set")) {
                    Integer sqlType = args.length > 1 && args[1] instanceof Integer
                            ? (Integer) args[1]
                            : null;
                    calls.add(new Call(method.getName(), (Integer) args[0], sqlType));
                    return null;
                }
                if ("addBatch".equals(method.getName())) {
                    addBatchCalls++;
                    return null;
                }
                if ("executeBatch".equals(method.getName())) {
                    executeBatchCalls++;
                    return new int[0];
                }
                return defaultValue(method.getReturnType());
            };
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    handler
            );
        }

        record Call(String method, int parameterIndex, Integer sqlType) {
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
