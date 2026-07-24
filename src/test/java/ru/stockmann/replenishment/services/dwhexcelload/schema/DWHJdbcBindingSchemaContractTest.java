package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.core.ExcelRowData;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDDataExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.WeeklyDataExcelLoadDefinition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
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
