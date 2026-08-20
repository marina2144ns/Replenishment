package ru.stockmann.replenishment.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelErrorLayer;
import ru.stockmann.replenishment.services.dwhexcelload.core.ExcelRowData;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.SalesByChannelExcelLoadDefinition;
import ru.stockmann.replenishment.services.salesbychannel.SalesByChannelBulkLoader;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelProcessResult;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelProcessor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesByChannelBulkLoaderTest {

    private static final List<String> PRODUCTION_HEADERS = List.of(
            "Сезон_Year", "Сезон (6м)", "Year_month", "Year_сезон", "Year", "Month",
            "Sales Channel type", "StoreRUS", "TypeOfSales", "MFP Division", "MFP Department",
            "Campaign Sales Type", "Seasonality", "SKU Brand type", "Sum([Sales Quantity])",
            "Sum([Sales Curr])", "GM", "Discount TTL", "Sum([Turnover Curr])",
            "SKU SeasonBudget", "StoreRus_BPO", "Sales Channel_BPO", "MFP SubDepartment",
            "SKU TM", "MFP Node", "Section", "MerchandiseSubGroup", "SKU Phase",
            "SKU   Product Class"
    );

    @TempDir
    Path tempDir;

    @Test
    void rawInsertUsesAllColumnsInDefinitionOrder() {
        TestLoader loader = new TestLoader(null);
        String sql = loader.rawInsertSql();

        assertTrue(sql.startsWith("INSERT INTO dbo.SalesByChannel_raw"));
        assertTrue(sql.contains("(LoadSessionId, ExcelRowNum, seasonYear, season6m"));
        assertTrue(sql.contains("skuPhase, skuProductClass)"));
        assertEquals(31, sql.chars().filter(ch -> ch == '?').count());
    }

    @Test
    void rawNormalizationPreservesTextNumbersZerosAndNulls() {
        TestLoader loader = new TestLoader(null);
        String[] values = new String[29];
        values[14] = "0";
        values[15] = "123.4567";
        values[16] = null;

        ExcelRowData row = loader.normalize(12, values);

        assertEquals("0", row.get("salesQuantity"));
        assertEquals("123.4567", row.get("salesCurr"));
        assertNull(row.get("gm"));
        assertEquals(12, row.rowNum());
    }

    @Test
    void acceptsExactProductionHeader() {
        TestLoader loader = new TestLoader(null);

        loader.validateHeaders(PRODUCTION_HEADERS.toArray(String[]::new));
    }

    @Test
    void rejectsEveryProductionHeaderContractMismatch() {
        TestLoader loader = new TestLoader(null);
        String[] valid = PRODUCTION_HEADERS.toArray(String[]::new);

        String[] translatedSeason = valid.clone();
        translatedSeason[0] = "Season_Year";
        String[] translatedYearSeason = valid.clone();
        translatedYearSeason[3] = "Year_season";
        String[] missingMetricBrackets = valid.clone();
        missingMetricBrackets[14] = "Sum(Sales Quantity)";
        String[] wrongProductClassSpacing = valid.clone();
        wrongProductClassSpacing[28] = "SKU Product Class";
        String[] swapped = valid.clone();
        swapped[6] = valid[7];
        swapped[7] = valid[6];
        String[] missingLast = Arrays.copyOf(valid, valid.length - 1);
        String[] extra = Arrays.copyOf(valid, valid.length + 1);
        extra[valid.length] = "Extra";
        String[] blank = valid.clone();
        blank[12] = "";
        String[] leadingSpace = valid.clone();
        leadingSpace[20] = " " + leadingSpace[20];
        String[] trailingSpace = valid.clone();
        trailingSpace[20] = trailingSpace[20] + " ";

        for (String[] invalid : List.of(
                translatedSeason, translatedYearSeason, missingMetricBrackets,
                wrongProductClassSpacing, swapped, missingLast, extra, blank,
                leadingSpace, trailingSpace
        )) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> loader.validateHeaders(invalid),
                    Arrays.toString(invalid)
            );
        }
    }

    @Test
    void processHookCallsJavaProcessorExactlyOnceAndDoesNotCallProcedure() {
        RecordingDataSource dataSource = new RecordingDataSource();
        FakeProcessor processor = new FakeProcessor(new SalesByChannelProcessResult(
                44L, true, 3, 3, 3, 0, "published"
        ));
        TestLoader loader = new TestLoader(dataSource, processor);

        DWHExcelLoadSessionResult result = loader.process(44L);

        assertTrue(result.success());
        assertEquals(44L, result.loadSessionId());
        assertEquals("published", result.message());
        assertEquals(1, processor.calls);
        assertEquals(44L, processor.loadSessionId);
        assertEquals(0, dataSource.connectionCalls);
        assertThrows(UnsupportedOperationException.class, () -> loader.defaultProcedure(44L));
        assertEquals(0, dataSource.connectionCalls);
    }

    @Test
    void processHookPreservesEmptyAndTechnicalFailureMessages() {
        FakeProcessor emptyProcessor = new FakeProcessor(new SalesByChannelProcessResult(
                45L, false, 0, 0, 0, 0,
                "Publication is impossible because stage contains no rows"
        ));
        DWHExcelLoadSessionResult empty =
                new TestLoader(null, emptyProcessor).process(45L);
        assertFalse(empty.success());
        assertEquals("Publication is impossible because stage contains no rows", empty.message());
        assertEquals(1, emptyProcessor.calls);

        FakeProcessor technicalProcessor = new FakeProcessor(new SalesByChannelProcessResult(
                46L, false, 2, 2, 0, 1, "target insert failed"
        ));
        DWHExcelLoadSessionResult technical =
                new TestLoader(null, technicalProcessor).process(46L);
        assertFalse(technical.success());
        assertEquals("target insert failed", technical.message());
        assertEquals(1, technicalProcessor.calls);
    }

    @Test
    void commonSessionCreationUsesSalesByChannelLoadType() {
        SessionRecordingDataSource dataSource = new SessionRecordingDataSource();
        TestLoader loader = new TestLoader(dataSource);

        DWHExcelLoadSessionResult result = loader.createSession("/tmp/source.xlsx");

        assertTrue(result.success());
        assertEquals(73L, result.loadSessionId());
        assertEquals("SALES_BY_CHANNEL", dataSource.parameters.get(1));
        assertEquals("SalesByChannel", dataSource.parameters.get(2));
        assertEquals("source.xlsx", dataSource.parameters.get(3));
        assertEquals("/tmp/source.xlsx", dataSource.parameters.get(4));
        assertEquals("QUEUED", dataSource.parameters.get(5));
        assertEquals(1, dataSource.commits);
    }

    @Test
    void commonEmptyRowRuleKeepsZeroButSkipsBlankRows() {
        assertTrue(TestLoader.empty(new String[29]));
        assertTrue(TestLoader.empty(new String[]{" ", null}));
        assertFalse(TestLoader.empty(new String[]{"0", null}));
    }

    @Test
    void streamsFirstSheetAndBatchesOriginalValuesIntoRaw() throws Exception {
        BatchRecordingDataSource dataSource = new BatchRecordingDataSource();
        TestLoader loader = new TestLoader(dataSource);
        Path workbookPath = tempDir.resolve("sales-by-channel.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("data");
            var header = sheet.createRow(0);
            String[] headers = loader.expectedHeaders();
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }

            var first = sheet.createRow(1);
            first.createCell(14).setCellValue(0);
            first.createCell(15).setCellFormula("1.23456+0");
            workbook.getCreationHelper().createFormulaEvaluator().evaluateFormulaCell(first.getCell(15));
            sheet.createRow(2).createCell(0).setBlank();
            sheet.createRow(3).createCell(0).setCellValue("Ofline");

            try (var output = Files.newOutputStream(workbookPath)) {
                workbook.write(output);
            }
        }

        loader.read(workbookPath, 91L);

        assertEquals(2, dataSource.rows.size());
        Map<Integer, Object> first = dataSource.rows.get(0);
        assertEquals(91L, first.get(1));
        assertEquals(2L, first.get(2));
        assertEquals("0", first.get(17));
        assertEquals("1.23456", first.get(18));
        assertNull(first.get(19));
        assertEquals(4L, dataSource.rows.get(1).get(2));
        assertEquals("Ofline", dataSource.rows.get(1).get(3));
        assertEquals(1, dataSource.commits);
        assertEquals(0, dataSource.rollbacks);
    }

    @Test
    void runtimeCallsProcessorOnlyAfterRawInsertAndFinishesSessionWithProcessorResult() {
        List<String> events = new ArrayList<>();
        EventProcessor processor = new EventProcessor(events, new SalesByChannelProcessResult(
                55L, true, 2, 2, 2, 0, "published 2 rows"
        ));
        FlowLoader loader = new FlowLoader(processor, events, false);

        loader.processAcceptedFile(55L, "/tmp/source.xlsx");

        assertEquals(List.of(
                "status:RUNNING",
                "raw",
                "processor:55",
                "finish:SUCCESS:published 2 rows"
        ), events);
        assertEquals(1, processor.calls);
    }

    @Test
    void validationOrEmptyFailureBecomesSessionErrorWithoutSecondProcessorCall() {
        List<String> events = new ArrayList<>();
        EventProcessor processor = new EventProcessor(events, new SalesByChannelProcessResult(
                56L, false, 1, 0, 0, 1, "Validation failed; target was not changed"
        ));
        FlowLoader loader = new FlowLoader(processor, events, false);

        loader.processAcceptedFile(56L, "/tmp/source.xlsx");

        assertEquals(List.of(
                "status:RUNNING",
                "raw",
                "processor:56",
                "finish:ERROR:Validation failed; target was not changed"
        ), events);
        assertEquals(1, processor.calls);
    }

    @Test
    void rawFailurePreventsProcessorInvocationAndMarksSessionError() {
        List<String> events = new ArrayList<>();
        EventProcessor processor = new EventProcessor(events, new SalesByChannelProcessResult(
                57L, true, 1, 1, 1, 0, "unused"
        ));
        FlowLoader loader = new FlowLoader(processor, events, true);

        assertThrows(RuntimeException.class,
                () -> loader.processAcceptedFile(57L, "/tmp/source.xlsx"));

        assertEquals(0, processor.calls);
        assertEquals("status:RUNNING", events.get(0));
        assertEquals("raw", events.get(1));
        assertTrue(events.stream().anyMatch(event -> event.startsWith("finish:ERROR:")));
    }

    private static final class TestLoader extends SalesByChannelBulkLoader {

        private TestLoader(DataSource dataSource) {
            this(dataSource, new FakeProcessor(new SalesByChannelProcessResult(
                    1L, true, 0, 0, 0, 0, "OK"
            )));
        }

        private TestLoader(DataSource dataSource, SalesByChannelProcessor processor) {
            super(dataSource, new SalesByChannelExcelLoadDefinition(), processor);
        }

        private String rawInsertSql() {
            return buildRawInsertSql();
        }

        private ExcelRowData normalize(int rowNum, String[] values) {
            return normalizeRow(rowNum, values);
        }

        private String[] expectedHeaders() {
            return definition.columns().stream()
                    .map(column -> column.excelColumnName())
                    .toArray(String[]::new);
        }

        private void validateHeaders(String[] values) {
            validateHeaderRow(values);
        }

        private DWHExcelLoadSessionResult process(long loadSessionId) {
            return processLoadSession(loadSessionId);
        }

        private void defaultProcedure(long loadSessionId) throws Exception {
            callProcessProcedure(loadSessionId);
        }

        private static boolean empty(String[] values) {
            return isEmpty(values);
        }

        private void read(Path path, long loadSessionId) throws Exception {
            readAndInsertExcel(path.toString(), loadSessionId);
        }

        private DWHExcelLoadSessionResult createSession(String path) {
            return createLoadSession(path);
        }
    }

    private static final class FakeProcessor extends SalesByChannelProcessor {
        private final SalesByChannelProcessResult result;
        private int calls;
        private long loadSessionId;

        private FakeProcessor(SalesByChannelProcessResult result) {
            super(null, null, null, null, null, null, null, 1_000);
            this.result = result;
        }

        @Override
        public SalesByChannelProcessResult process(long loadSessionId) {
            calls++;
            this.loadSessionId = loadSessionId;
            return result;
        }
    }

    private static final class EventProcessor extends SalesByChannelProcessor {
        private final List<String> events;
        private final SalesByChannelProcessResult result;
        private int calls;

        private EventProcessor(List<String> events, SalesByChannelProcessResult result) {
            super(null, null, null, null, null, null, null, 1_000);
            this.events = events;
            this.result = result;
        }

        @Override
        public SalesByChannelProcessResult process(long loadSessionId) {
            calls++;
            events.add("processor:" + loadSessionId);
            return result;
        }
    }

    private static final class FlowLoader extends SalesByChannelBulkLoader {
        private final List<String> events;
        private final boolean failRaw;

        private FlowLoader(EventProcessor processor, List<String> events, boolean failRaw) {
            super(null, new SalesByChannelExcelLoadDefinition(), processor);
            this.events = events;
            this.failRaw = failRaw;
        }

        @Override
        protected void readAndInsertExcel(String filePath, Long loadSessionId) {
            events.add("raw");
            if (failRaw) {
                throw new RuntimeException("raw failed");
            }
        }

        @Override
        protected void updateLoadSessionStatus(Long loadSessionId, String status, String message) {
            events.add("status:" + status);
        }

        @Override
        protected void finishLoadSession(Long loadSessionId, String status, String message) {
            events.add("finish:" + status + ":" + message);
        }

        @Override
        protected void logLoadError(
                Long loadSessionId,
                DWHExcelErrorLayer errorLayer,
                Long excelRowNum,
                Long rawId,
                String fieldName,
                String errorCode,
                String errorReason,
                String errorMessage
        ) {
            events.add("loadError:" + errorCode);
        }
    }

    private static final class SessionRecordingDataSource implements DataSource {
        private final Map<Integer, Object> parameters = new LinkedHashMap<>();
        private int commits;

        @Override
        public Connection getConnection() {
            ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    new java.lang.reflect.InvocationHandler() {
                        private boolean next = true;

                        @Override
                        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                            if ("next".equals(method.getName())) {
                                boolean value = next;
                                next = false;
                                return value;
                            }
                            if ("getLong".equals(method.getName())) {
                                return 73L;
                            }
                            return defaultValue(method.getReturnType());
                        }
                    }
            );
            PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("set")) {
                            parameters.put((Integer) args[0], args[1]);
                            return null;
                        }
                        if ("executeQuery".equals(method.getName())) {
                            return resultSet;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            return statement;
                        }
                        if ("commit".equals(method.getName())) {
                            commits++;
                            return null;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        @Override public Connection getConnection(String username, String password) { return getConnection(); }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() { return null; }
    }

    private static final class RecordingDataSource implements DataSource {
        private int connectionCalls;

        @Override
        public Connection getConnection() {
            connectionCalls++;
            return null;
        }

        @Override public Connection getConnection(String username, String password) { return getConnection(); }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() { return null; }
    }

    private static final class BatchRecordingDataSource implements DataSource {
        private final java.util.List<Map<Integer, Object>> rows = new ArrayList<>();
        private int commits;
        private int rollbacks;

        @Override
        public Connection getConnection() {
            final Map<Integer, Object> current = new LinkedHashMap<>();
            PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setLong", "setString" -> {
                                current.put((Integer) args[0], args[1]);
                                return null;
                            }
                            case "addBatch" -> {
                                rows.add(new LinkedHashMap<>(current));
                                current.clear();
                                return null;
                            }
                            case "executeBatch" -> {
                                return new int[rows.size()];
                            }
                            case "close" -> {
                                return null;
                            }
                            default -> {
                                return defaultValue(method.getReturnType());
                            }
                        }
                    }
            );
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "prepareStatement" -> {
                                return statement;
                            }
                            case "commit" -> {
                                commits++;
                                return null;
                            }
                            case "rollback" -> {
                                rollbacks++;
                                return null;
                            }
                            case "setAutoCommit", "close" -> {
                                return null;
                            }
                            default -> {
                                return defaultValue(method.getReturnType());
                            }
                        }
                    }
            );
        }

        @Override public Connection getConnection(String username, String password) { return getConnection(); }
        @Override public <T> T unwrap(Class<T> iface) { return null; }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
        @Override public java.io.PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(java.io.PrintWriter out) { }
        @Override public void setLoginTimeout(int seconds) { }
        @Override public int getLoginTimeout() { return 0; }
        @Override public java.util.logging.Logger getParentLogger() { return null; }
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
