package ru.stockmann.replenishment.services;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomProcessResult;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomProcessor;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.ExcelRowData;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomBulkLoaderTest {

    private static final List<String> PRODUCTION_HEADERS = List.of(
            "название", "ГОД", "Сезон", "день", "дата", "Sales Channel_BPO", "StoreRUS",
            "MFP Division", "MFP Department", "MFP SubDepartment", "SKU Brand type", "SKU TM",
            "MFP Node", "Section", "Merchandise SubGroup", "Campaign Sales Type", "SKU StyleColor",
            "SKU Phase", "Заказ, шт", "Заказ, руб", "Найдено,шт", "Найдено,руб", "Sales, Pcs",
            "Sales, rub", "Revenue", "GP", "Cogs", "Sales Discount", "Plan, rub",
            "Stock Stores, Pcs", "Stock Stores, DDP", "Драйверы CD", "SKU Supplier model",
            "SKU Composition", "SKU Color Russian", "SKU Name", "SKU Comment (buyer)",
            "SKU Collection"
    );

    @Test
    void acceptsExactProductionHeader() {
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(
                null, new CDEcomExcelLoadDefinition(), fakeProcessor(true)
        );

        loader.validate(PRODUCTION_HEADERS.toArray(String[]::new));
    }

    @Test
    void rejectsEveryProductionHeaderContractMismatch() {
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(
                null, new CDEcomExcelLoadDefinition(), fakeProcessor(true)
        );
        String[] valid = PRODUCTION_HEADERS.toArray(String[]::new);

        String[] internalName = valid.clone();
        internalName[0] = "nazvanie";
        String[] wrongCase = valid.clone();
        wrongCase[1] = "Год";
        String[] missingSubGroupSpace = valid.clone();
        missingSubGroupSpace[14] = "MerchandiseSubGroup";
        String[] addedFoundSpace = valid.clone();
        addedFoundSpace[20] = "Найдено, шт";
        String[] swapped = valid.clone();
        swapped[10] = valid[11];
        swapped[11] = valid[10];
        String[] missingLast = Arrays.copyOf(valid, valid.length - 1);
        String[] extra = Arrays.copyOf(valid, valid.length + 1);
        extra[valid.length] = "Extra";
        String[] blank = valid.clone();
        blank[25] = "";
        String[] leadingSpace = valid.clone();
        leadingSpace[16] = " " + leadingSpace[16];
        String[] trailingSpace = valid.clone();
        trailingSpace[16] = trailingSpace[16] + " ";

        for (String[] invalid : List.of(
                internalName, wrongCase, missingSubGroupSpace, addedFoundSpace, swapped,
                missingLast, extra, blank, leadingSpace, trailingSpace
        )) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> loader.validate(invalid),
                    Arrays.toString(invalid)
            );
        }
    }

    @Test
    void cdecomBulkLoaderExtendsAbstractDwhLoaderAndUsesDefinition() {
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(null, new CDEcomExcelLoadDefinition(), fakeProcessor(true));

        assertInstanceOf(CDEcomBulkLoader.class, loader);
        assertEquals("CD_ECOM", loader.getDefinition().loadCode());
        assertEquals("dbo.CD_ecom_raw", loader.getDefinition().rawTableName());
    }

    @Test
    void rawInsertSqlUsesCommonLoadSessionAndExcelRowNum() {
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(null, new CDEcomExcelLoadDefinition(), fakeProcessor(true));

        String sql = loader.rawInsertSql();

        assertTrue(sql.startsWith("INSERT INTO dbo.CD_ecom_raw"));
        assertTrue(sql.contains("LoadSessionId, ExcelRowNum, name, year, season"));
        assertTrue(sql.contains("skuCollection"));
        assertFalse(sql.contains("CD_ecom_load_session"));
        assertFalse(sql.contains("CD_ecom_load_error"));
    }

    @Test
    void normalizeRowPreservesCdecomSpecificDateAndNumericRawRules() {
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(null, new CDEcomExcelLoadDefinition(), fakeProcessor(true));
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
    void normalizeRowDoesNotTruncateTextBeforeValidation() {
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(null, new CDEcomExcelLoadDefinition(), fakeProcessor(true));
        String longText = "x".repeat(256);
        String[] row = new String[38];
        row[0] = " " + longText + " ";

        ExcelRowData normalized = loader.normalize(2, row);

        assertEquals(longText, normalized.get("name"));
        assertEquals(256, normalized.get("name").length());
    }

    @Test
    void processLoadSessionCallsProcessorWithLoadSessionIdAndDoesNotUseProcedure() throws Exception {
        RecordingDataSource dataSource = new RecordingDataSource();
        FakeCDEcomProcessor processor = fakeProcessor(true);
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(dataSource, new CDEcomExcelLoadDefinition(), processor);

        DWHExcelLoadSessionResult result = loader.callProcessLoadSession(77L);

        assertTrue(result.success());
        assertEquals(77L, result.loadSessionId());
        assertEquals("OK", result.message());
        assertEquals(1, processor.calls);
        assertEquals(77L, processor.loadSessionId);
        assertEquals(0, dataSource.connections);
    }

    @Test
    void defaultProcedureFallbackIsNotAvailableForCdecom() {
        RecordingDataSource dataSource = new RecordingDataSource();
        TestCDEcomBulkLoader loader = new TestCDEcomBulkLoader(dataSource, new CDEcomExcelLoadDefinition(), fakeProcessor(true));

        assertThrows(UnsupportedOperationException.class, () -> loader.callDefaultProcedure(77L));
        assertEquals(0, dataSource.connections);
    }

    private static final class TestCDEcomBulkLoader extends CDEcomBulkLoader {

        private TestCDEcomBulkLoader(
                DataSource dataSource,
                CDEcomExcelLoadDefinition definition,
                CDEcomProcessor processor
        ) {
            super(dataSource, definition, processor);
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

        private void callDefaultProcedure(Long loadSessionId) throws Exception {
            callProcessProcedure(loadSessionId);
        }

        private void validate(String[] headers) {
            validateHeaderRow(headers);
        }
    }

    private static final class RecordingDataSource implements DataSource {

        private int connections;

        @Override
        public Connection getConnection() {
            connections++;
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
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

    private static FakeCDEcomProcessor fakeProcessor(boolean success) {
        return new FakeCDEcomProcessor(new CDEcomProcessResult(77L, success, 0, 0, 0, "OK"));
    }

    private static final class FakeCDEcomProcessor extends CDEcomProcessor {

        private final CDEcomProcessResult result;
        private int calls;
        private long loadSessionId;

        private FakeCDEcomProcessor(CDEcomProcessResult result) {
            super(null, null, null, null, null, null, null);
            this.result = result;
        }

        @Override
        public CDEcomProcessResult process(long loadSessionId) {
            this.calls++;
            this.loadSessionId = loadSessionId;
            return result;
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
