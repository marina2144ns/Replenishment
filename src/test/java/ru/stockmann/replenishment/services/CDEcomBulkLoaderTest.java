package ru.stockmann.replenishment.services;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.ExcelRowData;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomBulkLoaderTest {

    @Test
    void cdecomBulkLoaderExtendsAbstractDwhLoaderAndUsesDefinition() {
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(null, new CDEcomExcelLoadDefinition());

        assertInstanceOf(CDEcomBulkLoader.class, loader);
        assertEquals("CD_ECOM", loader.getDefinition().loadCode());
        assertEquals("dbo.CD_ecom_raw", loader.getDefinition().rawTableName());
    }

    @Test
    void rawInsertSqlUsesCommonLoadSessionAndExcelRowNum() {
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(null, new CDEcomExcelLoadDefinition());

        String sql = loader.rawInsertSql();

        assertTrue(sql.startsWith("INSERT INTO dbo.CD_ecom_raw"));
        assertTrue(sql.contains("LoadSessionId, ExcelRowNum, name, year, season"));
        assertTrue(sql.contains("skuCollection"));
        assertFalse(sql.contains("CD_ecom_load_session"));
        assertFalse(sql.contains("CD_ecom_load_error"));
    }

    @Test
    void normalizeRowPreservesCdecomSpecificDateAndNumericRawRules() {
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(null, new CDEcomExcelLoadDefinition());
        String[] row = new String[38];
        row[0] = " name ";
        row[4] = "1/2/2026";
        row[18] = " 1\u00A0234,50 ";

        ExcelRowData normalized = loader.normalize(2, row);

        assertEquals("name", normalized.get("name"));
        assertEquals("01.02.2026", normalized.get("data"));
        assertEquals("1 234,50", normalized.get("orderPcs"));
    }

    @Test
    void processLoadSessionCallsCdecomProcedureWithLoadSessionId() throws Exception {
        RecordingDataSource dataSource = new RecordingDataSource();
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(dataSource, new CDEcomExcelLoadDefinition());

        DWHExcelLoadSessionResult result = loader.callProcessLoadSession(77L);

        assertTrue(result.success());
        assertEquals(77L, result.loadSessionId());
        assertEquals("OK", result.message());
        assertEquals("{call dbo.usp_CDEcom_ProcessLoadSession(?)}", dataSource.sql);
        assertEquals(77L, dataSource.loadSessionId);
        assertFalse(dataSource.sql.contains("CD_ecom_load_session"));
        assertFalse(dataSource.sql.contains("CD_ecom_load_error"));
    }

    private static final class TestCDEcomBulkLoader extends CDEcomBulkLoader {

        private TestCDEcomBulkLoader(DataSource dataSource, CDEcomExcelLoadDefinition definition) {
            super(dataSource, definition);
        }

        private String rawInsertSql() {
            return buildRawInsertSql();
        }

        private ExcelRowData normalize(int rowNum, String[] row) {
            return normalizeRow(rowNum, row);
        }

        private DWHExcelLoadSessionResult callProcessLoadSession(Long loadSessionId) throws Exception {
            return processLoadSession(loadSessionId);
        }
    }

    private static final class RecordingDataSource implements DataSource {

        private String sql;
        private long loadSessionId;

        @Override
        public Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareCall".equals(method.getName())) {
                            sql = (String) args[0];
                            return callableStatement();
                        }
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private CallableStatement callableStatement() {
            return (CallableStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{CallableStatement.class},
                    (proxy, method, args) -> {
                        if ("setLong".equals(method.getName())) {
                            loadSessionId = (Long) args[1];
                            return null;
                        }
                        if ("execute".equals(method.getName())) {
                            return true;
                        }
                        if ("getResultSet".equals(method.getName())) {
                            return resultSet();
                        }
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private ResultSet resultSet() {
            return (ResultSet) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> {
                        if ("next".equals(method.getName())) {
                            return true;
                        }
                        if ("getBoolean".equals(method.getName())) {
                            return true;
                        }
                        if ("getString".equals(method.getName())) {
                            return "OK";
                        }
                        if ("close".equals(method.getName())) {
                            return null;
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

    private static Object defaultValue(Class<?> returnType) {
        return Map.of(
                boolean.class, false,
                int.class, 0,
                long.class, 0L
        ).get(returnType);
    }
}
