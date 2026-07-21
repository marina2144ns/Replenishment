package ru.stockmann.replenishment.services.dwhexcelload.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AbstractDWHExcelLoaderTest {

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

    private static final class TestLoader extends AbstractDWHExcelLoader {

        private TestLoader() {
            super(null, new TestDefinition());
        }

        private void bind(PreparedStatement ps, Long loadSessionId, ExcelRowData row) throws SQLException {
            bindRawRow(ps, loadSessionId, row);
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
