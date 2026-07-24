package ru.stockmann.replenishment.services.cddata.process;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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

class CDDataRepositoryTest {

    @Test
    void loadSessionRepositoryChecksLoadSessionIdAndCdDataLoadTypeCode() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);
        statement.resultSet = resultSet(Map.of());

        boolean exists = new CDDataLoadSessionRepository(new RecordingDataSource(connection).proxy())
                .existsById(20L);

        assertTrue(exists);
        assertTrue(connection.sql.contains("FROM dbo.DWH_Excel_Load_Session"));
        assertTrue(connection.sql.contains("WHERE Id = ?"));
        assertTrue(connection.sql.contains("AND LoadTypeCode = ?"));
        assertEquals(20L, statement.values.get(1));
        assertEquals("CD_DATA", statement.values.get(2));
    }

    @Test
    void rawRepositoryReadsRowsByLoadSessionIdOrderedById() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);
        statement.resultSet = resultSet(rawData(30L));

        List<CDDataRawRow> rows = new CDDataRawRepository(null)
                .findByLoadSessionId(connection.proxy(), 20L);

        assertEquals(1, rows.size());
        CDDataRawRow row = rows.get(0);
        assertEquals(10L, row.id());
        assertEquals(20L, row.loadSessionId());
        assertEquals(30L, row.excelRowNum());
        assertRawStrings(row);
        assertTrue(connection.sql.contains("FROM dbo.CD_data_raw"));
        assertTrue(connection.sql.contains("WHERE LoadSessionId = ?"));
        assertTrue(connection.sql.contains("ORDER BY Id ASC"));
        assertEquals(20L, statement.values.get(1));
    }

    @Test
    void rawRepositoryMapsNullableExcelRowNum() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);
        statement.resultSet = resultSet(rawData(null));

        CDDataRawRow row = new CDDataRawRepository(null)
                .findByLoadSessionId(connection.proxy(), 20L)
                .get(0);

        assertNull(row.excelRowNum());
    }

    @Test
    void errorRepositoryDeletesByLoadSessionIdAndLoadTypeCode() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);

        new CDDataErrorRepository(null).deleteByLoadSessionId(connection.proxy(), 20L);

        assertTrue(connection.sql.contains("DELETE FROM dbo.DWH_Excel_Load_Error"));
        assertTrue(connection.sql.contains("WHERE LoadSessionId = ?"));
        assertTrue(connection.sql.contains("AND LoadTypeCode = ?"));
        assertEquals(20L, statement.values.get(1));
        assertEquals("CD_DATA", statement.values.get(2));
        assertTrue(statement.executeUpdateCalled);
    }

    @Test
    void errorRepositoryInsertsLoadTypeCodeAndNullableFields() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);

        CDDataValidationError error = new CDDataValidationError(
                20L,
                null,
                null,
                "VALIDATION",
                null,
                "INVALID_INTEGER",
                null,
                "message"
        );

        new CDDataErrorRepository(null).insertAll(connection.proxy(), List.of(error));

        assertTrue(connection.sql.contains("INSERT INTO dbo.DWH_Excel_Load_Error"));
        assertEquals(20L, statement.values.get(1));
        assertEquals("CD_DATA", statement.values.get(2));
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
    void errorRepositoryValidatesBatchUpdateCountsStrictly() {
        RecordingPreparedStatement wrongLength = new RecordingPreparedStatement();
        wrongLength.forcedUpdateCounts = new int[0];
        RecordingConnection wrongLengthConnection = new RecordingConnection(wrongLength);

        assertThrows(IllegalStateException.class, () ->
                new CDDataErrorRepository(null).insertBatch(
                        wrongLengthConnection.proxy(), 20L, List.of(validationError())
                ));

        RecordingPreparedStatement failed = new RecordingPreparedStatement();
        failed.forcedUpdateCounts = new int[]{Statement.EXECUTE_FAILED};
        RecordingConnection failedConnection = new RecordingConnection(failed);

        assertThrows(IllegalStateException.class, () ->
                new CDDataErrorRepository(null).insertBatch(
                        failedConnection.proxy(), 20L, List.of(validationError())
                ));
    }

    @Test
    void errorRepositoryInsertAllEmptyListDoesNothing() {
        RecordingPreparedStatement statement = new RecordingPreparedStatement();
        RecordingConnection connection = new RecordingConnection(statement);

        new CDDataErrorRepository(null).insertAll(connection.proxy(), List.of());

        assertFalse(connection.prepareStatementCalled);
    }

    private static Map<String, Object> rawData(Long excelRowNum) {
        Map<String, Object> row = new HashMap<>();
        row.put("Id", 10L);
        row.put("LoadSessionId", 20L);
        row.put("ExcelRowNum", excelRowNum);
        row.put("nazvanie", "Name");
        row.put("god", "2025");
        row.put("sezon", "1");
        row.put("den", "31");
        row.put("data", "2025-01-31");
        row.put("sales_channel", "Online");
        row.put("store_rus", "Store");
        row.put("mfp_division", "Division");
        row.put("mfp_department", "Department");
        row.put("mfp_sub_department", "SubDepartment");
        row.put("sku_brand_type", "Brand");
        row.put("sku_tm", "TM");
        row.put("mfp_node", "Node");
        row.put("section", "Section");
        row.put("merchandise_sub_group", "Group");
        row.put("campaign_sales", "Campaign");
        row.put("sku_style_color", "123456789");
        row.put("sku_phase", "Phase");
        row.put("stock_start_pcs", "12.35");
        row.put("stock_start_dd", "1234.56");
        row.put("sales_pcs", "0");
        row.put("sales_rub", "100");
        row.put("revenue", "200.10");
        row.put("gp", "-12.34");
        row.put("cogs", "5");
        row.put("sales_frp_price", "99.99");
        row.put("sales_discount", "10");
        row.put("stock_stores_pcs", "3");
        row.put("stock_stores_dd", "4");
        row.put("plan_rub", "123");
        row.put("draivery_cd", "Driver");
        row.put("sku_color_rus", "Color");
        row.put("sku_composition", "Composition");
        row.put("sku_supplier", "Supplier");
        row.put("sku_name", "Sku Name");
        row.put("sku_collection", "Collection");
        row.put("sku_comment", "Comment");
        return row;
    }

    private static void assertRawStrings(CDDataRawRow row) {
        assertEquals("Name", row.nazvanie());
        assertEquals("2025", row.god());
        assertEquals("1", row.sezon());
        assertEquals("31", row.den());
        assertEquals("2025-01-31", row.data());
        assertEquals("Online", row.salesChannel());
        assertEquals("Store", row.storeRus());
        assertEquals("Division", row.mfpDivision());
        assertEquals("Department", row.mfpDepartment());
        assertEquals("SubDepartment", row.mfpSubDepartment());
        assertEquals("Brand", row.skuBrandType());
        assertEquals("TM", row.skuTm());
        assertEquals("Node", row.mfpNode());
        assertEquals("Section", row.section());
        assertEquals("Group", row.merchandiseSubGroup());
        assertEquals("Campaign", row.campaignSales());
        assertEquals("123456789", row.skuStyleColor());
        assertEquals("Phase", row.skuPhase());
        assertEquals("12.35", row.stockStartPcs());
        assertEquals("1234.56", row.stockStartDd());
        assertEquals("0", row.salesPcs());
        assertEquals("100", row.salesRub());
        assertEquals("200.10", row.revenue());
        assertEquals("-12.34", row.gp());
        assertEquals("5", row.cogs());
        assertEquals("99.99", row.salesFrpPrice());
        assertEquals("10", row.salesDiscount());
        assertEquals("3", row.stockStoresPcs());
        assertEquals("4", row.stockStoresDd());
        assertEquals("123", row.planRub());
        assertEquals("Driver", row.draiveryCd());
        assertEquals("Color", row.skuColorRus());
        assertEquals("Composition", row.skuComposition());
        assertEquals("Supplier", row.skuSupplier());
        assertEquals("Sku Name", row.skuName());
        assertEquals("Collection", row.skuCollection());
        assertEquals("Comment", row.skuComment());
    }

    private static CDDataValidationError validationError() {
        return new CDDataValidationError(
                20L, 10L, 30L, "VALIDATION", "god",
                "INVALID_INTEGER", "reason", "message"
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
        private int[] forcedUpdateCounts;
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
                    if (forcedUpdateCounts != null) {
                        return java.util.Arrays.copyOf(forcedUpdateCounts, forcedUpdateCounts.length);
                    }
                    int[] updateCounts = new int[addBatchCalls];
                    java.util.Arrays.fill(updateCounts, 1);
                    return updateCounts;
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
