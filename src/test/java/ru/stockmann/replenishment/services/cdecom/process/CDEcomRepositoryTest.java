package ru.stockmann.replenishment.services.cdecom.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomRepositoryTest {

    @Test
    void loadSessionRepositoryChecksLoadSessionIdAndCdecomLoadTypeCode() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);
        statement.resultSet = resultSet(Map.of());

        boolean exists = new CDEcomLoadSessionRepository(new RecordingDataSource(connection).proxy())
                .existsById(20L);

        assertTrue(exists);
        assertTrue(connection.sql.contains("FROM dbo.DWH_Excel_Load_Session"));
        assertTrue(connection.sql.contains("WHERE Id = ?"));
        assertTrue(connection.sql.contains("AND LoadTypeCode = ?"));
        assertEquals(20L, statement.values.get(1));
        assertEquals("CD_ECOM", statement.values.get(2));
    }

    @Test
    void rawRepositoryReadsRowsByLoadSessionIdOrderedById() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);
        statement.resultSet = resultSet(rawData(30L));

        List<CDEcomRawRow> rows = new CDEcomRawRepository(null)
                .findByLoadSessionId(connection.proxy(), 20L);

        assertEquals(1, rows.size());
        CDEcomRawRow row = rows.get(0);
        assertEquals(10L, row.id());
        assertEquals(20L, row.loadSessionId());
        assertEquals(30L, row.excelRowNum());
        assertRawStrings(row);
        assertTrue(connection.sql.contains("FROM dbo.CD_ecom_raw"));
        assertTrue(connection.sql.contains("WHERE LoadSessionId = ?"));
        assertTrue(connection.sql.contains("ORDER BY Id ASC"));
        assertEquals(20L, statement.values.get(1));
    }

    @Test
    void rawRepositoryMapsNullableExcelRowNum() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);
        statement.resultSet = resultSet(rawData(null));

        CDEcomRawRow row = new CDEcomRawRepository(null)
                .findByLoadSessionId(connection.proxy(), 20L)
                .get(0);

        assertNull(row.excelRowNum());
    }

    @Test
    void errorRepositoryDeletesByLoadSessionIdAndLoadTypeCode() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);

        new CDEcomErrorRepository(null).deleteByLoadSessionId(connection.proxy(), 20L);

        assertTrue(connection.sql.contains("DELETE FROM dbo.DWH_Excel_Load_Error"));
        assertTrue(connection.sql.contains("WHERE LoadSessionId = ?"));
        assertTrue(connection.sql.contains("AND LoadTypeCode = ?"));
        assertEquals(20L, statement.values.get(1));
        assertEquals("CD_ECOM", statement.values.get(2));
        assertTrue(statement.executeUpdateCalled);
    }

    @Test
    void errorRepositoryInsertsLoadTypeCodeAndNullableFields() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);

        CDEcomValidationError error = new CDEcomValidationError(
                20L,
                null,
                null,
                "VALIDATION",
                null,
                "INVALID_INTEGER",
                null,
                "message"
        );

        new CDEcomErrorRepository(null).insertAll(connection.proxy(), List.of(error));

        assertTrue(connection.sql.contains("INSERT INTO dbo.DWH_Excel_Load_Error"));
        assertEquals(20L, statement.values.get(1));
        assertEquals("CD_ECOM", statement.values.get(2));
        assertEquals("VALIDATION", statement.values.get(3));
        assertEquals(Types.BIGINT, statement.nullTypes.get(4));
        assertEquals(Types.BIGINT, statement.nullTypes.get(5));
        assertEquals(Types.NVARCHAR, statement.nullTypes.get(6));
        assertEquals("INVALID_INTEGER", statement.values.get(7));
        assertEquals(Types.NVARCHAR, statement.nullTypes.get(8));
        assertEquals("message", statement.values.get(9));
        assertTrue(statement.executeBatchCalled);
    }

    @Test
    void errorRepositoryInsertAllEmptyListDoesNothing() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);

        new CDEcomErrorRepository(null).insertAll(connection.proxy(), List.of());

        assertFalse(connection.prepareStatementCalled);
    }

    private static Map<String, Object> rawData(Long excelRowNum) {
        Map<String, Object> row = new HashMap<>();
        row.put("Id", 10L);
        row.put("LoadSessionId", 20L);
        row.put("ExcelRowNum", excelRowNum);
        row.put("name", "Name");
        row.put("year", "2025");
        row.put("season", "1");
        row.put("day", "31");
        row.put("data", "2025-01-31");
        row.put("salesChannelBpo", "Online");
        row.put("storeRus", "Store");
        row.put("mfpDivision", "Division");
        row.put("mfpDepartment", "Department");
        row.put("mfpSubDepartment", "SubDepartment");
        row.put("skuBrandType", "Brand");
        row.put("skuTm", "TM");
        row.put("mfpNode", "Node");
        row.put("section", "Section");
        row.put("merchandiseSubGroup", "Group");
        row.put("campaignSalesType", "Campaign");
        row.put("skuStyleColor", "123456789");
        row.put("skuPhase", "Phase");
        row.put("orderPcs", "12.35");
        row.put("orderRub", "1234.56");
        row.put("foundPcs", "0");
        row.put("foundRub", "1");
        row.put("salesPcs", "2");
        row.put("salesRub", "100");
        row.put("revenue", "200.10");
        row.put("gp", "-12.34");
        row.put("cogs", "5");
        row.put("salesDiscount", "10");
        row.put("planRub", "123");
        row.put("stockStoresPcs", "3");
        row.put("stockStoresDdp", "4");
        row.put("cdDrivers", "Driver");
        row.put("skuColorRussian", "Color");
        row.put("skuComposition", "Composition");
        row.put("skuSupplierModel", "Supplier");
        row.put("skuName", "Sku Name");
        row.put("skuCollection", "Collection");
        row.put("skuCommentBuyer", "Comment");
        return row;
    }

    private static void assertRawStrings(CDEcomRawRow row) {
        assertEquals("Name", row.name());
        assertEquals("2025", row.year());
        assertEquals("1", row.season());
        assertEquals("31", row.day());
        assertEquals("2025-01-31", row.data());
        assertEquals("Online", row.salesChannelBpo());
        assertEquals("Store", row.storeRus());
        assertEquals("Division", row.mfpDivision());
        assertEquals("Department", row.mfpDepartment());
        assertEquals("SubDepartment", row.mfpSubDepartment());
        assertEquals("Brand", row.skuBrandType());
        assertEquals("TM", row.skuTm());
        assertEquals("Node", row.mfpNode());
        assertEquals("Section", row.section());
        assertEquals("Group", row.merchandiseSubGroup());
        assertEquals("Campaign", row.campaignSalesType());
        assertEquals("123456789", row.skuStyleColor());
        assertEquals("Phase", row.skuPhase());
        assertEquals("12.35", row.orderPcs());
        assertEquals("1234.56", row.orderRub());
        assertEquals("0", row.foundPcs());
        assertEquals("1", row.foundRub());
        assertEquals("2", row.salesPcs());
        assertEquals("100", row.salesRub());
        assertEquals("200.10", row.revenue());
        assertEquals("-12.34", row.gp());
        assertEquals("5", row.cogs());
        assertEquals("10", row.salesDiscount());
        assertEquals("3", row.stockStoresPcs());
        assertEquals("4", row.stockStoresDdp());
        assertEquals("123", row.planRub());
        assertEquals("Driver", row.cdDrivers());
        assertEquals("Color", row.skuColorRussian());
        assertEquals("Composition", row.skuComposition());
        assertEquals("Supplier", row.skuSupplierModel());
        assertEquals("Sku Name", row.skuName());
        assertEquals("Collection", row.skuCollection());
        assertEquals("Comment", row.skuCommentBuyer());
    }

    private static CDEcomTargetRow targetRow() {
        return targetRowWithLoadSessionId(20L);
    }

    private static CDEcomTargetRow targetRowWithLoadSessionId(Long loadSessionId) {
        return new CDEcomTargetRow(
                loadSessionId,
                "Name",
                2025,
                1,
                31,
                Date.valueOf("2025-01-31"),
                "Online",
                "Store",
                "Division",
                "Department",
                "SubDepartment",
                "Brand",
                "TM",
                "Node",
                "Section",
                "Group",
                "Campaign",
                123456789L,
                "Phase",
                new BigDecimal("12.35"),
                new BigDecimal("1234.56"),
                new BigDecimal("0.00"),
                new BigDecimal("100.00"),
                new BigDecimal("200.10"),
                new BigDecimal("-12.34"),
                new BigDecimal("5.00"),
                new BigDecimal("99.99"),
                new BigDecimal("10.00"),
                new BigDecimal("11.00"),
                123L,
                456L,
                789L,
                "Driver",
                "Supplier",
                "Composition",
                "Color",
                "Sku Name",
                "Comment",
                "Collection",
                99L
        );
    }

    private static CDEcomTargetRow emptyTargetRow() {
        return new CDEcomTargetRow(
                20L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private static ResultSet resultSet(Map<String, Object> row) {
        class State {
            boolean consumed;
            boolean wasNull;
        }
        State state = new State();
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("next".equals(methodName)) {
                if (!state.consumed) {
                    state.consumed = true;
                    return true;
                }
                return false;
            }
            if ("getLong".equals(methodName)) {
                Object value = row.get((String) args[0]);
                state.wasNull = value == null;
                return value == null ? 0L : value;
            }
            if ("getString".equals(methodName)) {
                Object value = row.get((String) args[0]);
                state.wasNull = value == null;
                return value;
            }
            if ("wasNull".equals(methodName)) {
                return state.wasNull;
            }
            return defaultValue(method.getReturnType());
        };
        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                handler
        );
    }

    private static final class RecordingPreparedStatement {
        private final Map<Integer, Object> values = new HashMap<>();
        private final Map<Integer, Integer> nullTypes = new HashMap<>();
        private int addBatchCalls;
        private boolean executeBatchCalled;
        private boolean executeUpdateCalled;
        private ResultSet resultSet;

        PreparedStatement proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                String methodName = method.getName();
                if ("setLong".equals(methodName)
                        || "setInt".equals(methodName)
                        || "setString".equals(methodName)
                        || "setBigDecimal".equals(methodName)
                        || "setDate".equals(methodName)) {
                    values.put((Integer) args[0], args[1]);
                    return null;
                }
                if ("setNull".equals(methodName)) {
                    nullTypes.put((Integer) args[0], (Integer) args[1]);
                    return null;
                }
                if ("addBatch".equals(methodName)) {
                    addBatchCalls++;
                    return null;
                }
                if ("executeBatch".equals(methodName)) {
                    executeBatchCalled = true;
                    int[] counts = new int[addBatchCalls];
                    java.util.Arrays.fill(counts, 1);
                    return counts;
                }
                if ("executeUpdate".equals(methodName)) {
                    executeUpdateCalled = true;
                    return 1;
                }
                if ("executeQuery".equals(methodName)) {
                    return resultSet;
                }
                return defaultValue(method.getReturnType());
            };
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    handler
            );
        }
    }

    private static final class RecordingConnection {
        private final RecordingPreparedStatement statement;
        private boolean prepareStatementCalled;
        private String sql;

        private RecordingConnection(RecordingPreparedStatement statement) {
            this.statement = statement;
        }

        Connection proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    prepareStatementCalled = true;
                    sql = (String) args[0];
                    return statement.proxy();
                }
                return defaultValue(method.getReturnType());
            };
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    handler
            );
        }
    }

    private static final class RecordingDataSource {
        private final RecordingConnection connection;
        private int getConnectionCalls;

        private RecordingDataSource(RecordingConnection connection) {
            this.connection = connection;
        }

        DataSource proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("getConnection".equals(method.getName())) {
                    getConnectionCalls++;
                    return connection.proxy();
                }
                return defaultValue(method.getReturnType());
            };
            return (DataSource) Proxy.newProxyInstance(
                    DataSource.class.getClassLoader(),
                    new Class<?>[]{DataSource.class},
                    handler
            );
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0F;
        }
        if (returnType == Double.TYPE) {
            return 0D;
        }
        if (returnType == Character.TYPE) {
            return '\0';
        }
        return null;
    }
}
