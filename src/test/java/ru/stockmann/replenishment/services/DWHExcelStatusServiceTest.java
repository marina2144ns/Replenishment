package ru.stockmann.replenishment.services;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionNotFoundException;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadStatusResult;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DWHExcelStatusServiceTest {

    @Test
    void returnsExistingSuccessRunningAndErrorSessions() {
        assertExistingStatus("SUCCESS");
        assertExistingStatus("RUNNING");
        assertExistingStatus("ERROR");
    }

    @Test
    void returnsPersistedDeleteCriteriaMetadataWithoutInterpretation() {
        Map<String, Object> row = row("SUCCESS");
        row.put("OperationType", "DELETE");
        row.put("OperationMode", "BY_CRITERIA");
        row.put("DeleteCriterion", "NAZVANIE_DEN");
        row.put("DeleteParameter1Name", "nazvanie");
        row.put("DeleteParameter1Value", "Main");
        row.put("DeleteParameter2Name", "den");
        row.put("DeleteParameter2Value", "15");
        row.put("DeletedRows", 27L);

        DWHExcelLoadStatusResult result =
                new DWHExcelStatusService(new FakeDataSource(row)).getStatus(10L);

        assertEquals("DELETE", result.operationType());
        assertEquals("BY_CRITERIA", result.operationMode());
        assertEquals("NAZVANIE_DEN", result.deleteCriterion());
        assertEquals("nazvanie", result.deleteParameter1Name());
        assertEquals("Main", result.deleteParameter1Value());
        assertEquals("den", result.deleteParameter2Name());
        assertEquals("15", result.deleteParameter2Value());
        assertEquals(27L, result.deletedRows());
        assertNull(result.deleteYear());
        assertNull(result.sourceLoadSessionId());
    }

    @Test
    void returnsNullableNumericDeleteMetadataWithoutNullToZeroConversion() {
        Map<String, Object> row = row("RUNNING");
        row.put("OperationType", "DELETE");
        row.put("OperationMode", "BY_LOAD_SESSION");
        row.put("SourceLoadSessionId", 10521L);

        DWHExcelLoadStatusResult result =
                new DWHExcelStatusService(new FakeDataSource(row)).getStatus(10L);

        assertEquals(10521L, result.sourceLoadSessionId());
        assertNull(result.deleteYear());
        assertNull(result.deleteWeek());
        assertNull(result.deleteMonth());
        assertNull(result.deletedRows());
    }

    @Test
    void missingSessionThrowsNotFoundException() {
        DWHExcelStatusService service = new DWHExcelStatusService(new FakeDataSource(null));

        DWHExcelLoadSessionNotFoundException exception = assertThrows(
                DWHExcelLoadSessionNotFoundException.class,
                () -> service.getStatus(404L)
        );

        assertEquals("Load session not found: 404", exception.getMessage());
    }

    private static void assertExistingStatus(String status) {
        DWHExcelStatusService service = new DWHExcelStatusService(new FakeDataSource(row(status)));

        DWHExcelLoadStatusResult result = service.getStatus(10L);

        assertEquals(10L, result.loadSessionId());
        assertEquals("CD_ECOM", result.loadTypeCode());
        assertEquals("CD ecom", result.serviceName());
        assertEquals("file.xlsx", result.fileName());
        assertEquals("/tmp/file.xlsx", result.filePath());
        assertEquals("LOAD", result.operationType());
        assertNull(result.operationMode());
        assertNull(result.deleteYear());
        assertNull(result.deleteWeek());
        assertNull(result.deleteMonth());
        assertNull(result.deleteYearText());
        assertNull(result.deleteMonthText());
        assertNull(result.sourceLoadSessionId());
        assertNull(result.deleteCriterion());
        assertNull(result.deleteParameter1Name());
        assertNull(result.deleteParameter1Value());
        assertNull(result.deleteParameter2Name());
        assertNull(result.deleteParameter2Value());
        assertNull(result.deletedRows());
        assertEquals(status, result.status());
        assertEquals("message", result.message());
        assertEquals("2026-01-01T10:00", result.startedAt());
        assertEquals("2026-01-01T10:05", result.finishedAt());
    }

    private static Map<String, Object> row(String status) {
        Map<String, Object> values = new HashMap<>();
        values.put("Id", 10L);
        values.put("LoadTypeCode", "CD_ECOM");
        values.put("ServiceName", "CD ecom");
        values.put("FileName", "file.xlsx");
        values.put("FilePath", "/tmp/file.xlsx");
        values.put("OperationType", "LOAD");
        values.put("OperationMode", null);
        values.put("DeleteYear", null);
        values.put("DeleteWeek", null);
        values.put("DeleteMonth", null);
        values.put("DeleteYearText", null);
        values.put("DeleteMonthText", null);
        values.put("SourceLoadSessionId", null);
        values.put("DeleteCriterion", null);
        values.put("DeleteParameter1Name", null);
        values.put("DeleteParameter1Value", null);
        values.put("DeleteParameter2Name", null);
        values.put("DeleteParameter2Value", null);
        values.put("DeletedRows", null);
        values.put("Status", status);
        values.put("Message", "message");
        values.put("StartedAt", Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 10, 0)));
        values.put("FinishedAt", Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 10, 5)));
        return values;
    }

    private static final class FakeDataSource implements DataSource {
        private final Map<String, Object> row;

        private FakeDataSource(Map<String, Object> row) {
            this.row = row;
        }

        @Override
        public Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            return preparedStatement(row);
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        @Override
        public <T> T unwrap(Class<T> iface) {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public java.io.PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return null;
        }
    }

    private static PreparedStatement preparedStatement(Map<String, Object> row) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("executeQuery".equals(method.getName())) {
                return resultSet(row);
            }
            return defaultValue(method.getReturnType());
        };

        return (PreparedStatement) Proxy.newProxyInstance(
                DWHExcelStatusServiceTest.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                handler
        );
    }

    private static ResultSet resultSet(Map<String, Object> row) {
        InvocationHandler handler = new InvocationHandler() {
            private boolean beforeFirst = true;
            private boolean lastWasNull;

            @Override
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                return switch (method.getName()) {
                    case "next" -> {
                        boolean hasNext = beforeFirst && row != null;
                        beforeFirst = false;
                        yield hasNext;
                    }
                    case "getInt" -> {
                        Object value = row.get((String) args[0]);
                        lastWasNull = value == null;
                        yield value == null ? 0 : ((Number) value).intValue();
                    }
                    case "getLong" -> {
                        Object value = row.get((String) args[0]);
                        lastWasNull = value == null;
                        yield value == null ? 0L : ((Number) value).longValue();
                    }
                    case "getString", "getTimestamp" -> {
                        Object value = row.get((String) args[0]);
                        lastWasNull = value == null;
                        yield value;
                    }
                    case "wasNull" -> lastWasNull;
                    default -> defaultValue(method.getReturnType());
                };
            }
        };

        return (ResultSet) Proxy.newProxyInstance(
                DWHExcelStatusServiceTest.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                handler
        );
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
