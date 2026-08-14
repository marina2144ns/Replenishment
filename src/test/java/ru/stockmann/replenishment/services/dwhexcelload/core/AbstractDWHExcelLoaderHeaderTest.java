package ru.stockmann.replenishment.services.dwhexcelload.core;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractDWHExcelLoaderHeaderTest {

    @TempDir
    Path tempDir;

    @Test
    void strictHeaderContractAcceptsOnlyExactNamesCountAndOrder() {
        TestLoader loader = new TestLoader(null);

        loader.validate("Year", "Month");
        assertHeaderError(loader, "Header mismatch", "YearRenamed", "Month");
        assertHeaderError(loader, "Header mismatch", "Month", "Year");
        assertHeaderError(loader, "Missing header", "Year");
        assertHeaderError(loader, "Header mismatch", "Year", "Inserted", "Month");
        assertHeaderError(loader, "Unexpected extra column", "Year", "Month", "Extra");
        assertHeaderError(loader, "Missing header", "Year", "");
        assertHeaderError(loader, "Header mismatch", "year", "Month");
        assertHeaderError(loader, "Header mismatch", " Year", "Month");
        assertHeaderError(loader, "Header mismatch", "Year ", "Month");
    }

    @Test
    void saxRejectsTrailingColumnBeforeAnyRawRowIsInserted() throws Exception {
        RecordingDataSource dataSource = new RecordingDataSource();
        TestLoader loader = new TestLoader(dataSource);
        Path file = workbook("extra.xlsx", 0,
                new String[]{"Year", "Month", "Extra"}, new String[]{"2026", "7"});

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> loader.read(file, 10L)
        );

        assertTrue(error.getMessage().contains("Unexpected extra column at column 3"));
        assertEquals(0, dataSource.rawRows);
        assertEquals(1, dataSource.rollbacks);
        assertEquals(0, dataSource.commits);
    }

    @Test
    void firstActualWorksheetRowIsHeaderEvenWhenPhysicalRowZeroIsAbsent() throws Exception {
        RecordingDataSource dataSource = new RecordingDataSource();
        TestLoader loader = new TestLoader(dataSource);
        Path file = workbook("offset-header.xlsx", 5,
                new String[]{"Year", "Month"}, new String[]{"2026", "7"});

        loader.read(file, 11L);

        assertEquals(1, dataSource.rawRows);
        assertEquals(7L, dataSource.lastRow.get(2));
        assertEquals("2026", dataSource.lastRow.get(3));
        assertEquals("7", dataSource.lastRow.get(4));
        assertEquals(1, dataSource.commits);
    }

    @Test
    void emptyFirstSheetFinishesExistingSessionAsErrorWithoutProcessorOrRawRows() throws Exception {
        RecordingDataSource dataSource = new RecordingDataSource();
        TestLoader loader = new TestLoader(dataSource);
        Path file = tempDir.resolve("empty.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             var output = Files.newOutputStream(file)) {
            workbook.createSheet("data");
            workbook.write(output);
        }

        assertThrows(RuntimeException.class, () -> loader.processAcceptedFile(12L, file.toString()));

        assertEquals(0, dataSource.rawRows);
        assertEquals(0, loader.processorCalls);
        assertEquals("ERROR", loader.finishedStatus);
        assertTrue(loader.finishedMessage.contains("Missing Excel header"));
        assertEquals("JAVA_LOAD_ERROR", loader.errorCode);
    }

    private void assertHeaderError(TestLoader loader, String message, String... headers) {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> loader.validate(headers)
        );
        assertTrue(error.getMessage().contains(message), error.getMessage());
    }

    private Path workbook(String name, int headerRowNumber, String[] headers, String[] data)
            throws Exception {
        Path file = tempDir.resolve(name);
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             var output = Files.newOutputStream(file)) {
            var sheet = workbook.createSheet("data");
            var header = sheet.createRow(headerRowNumber);
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }
            var row = sheet.createRow(headerRowNumber + 1);
            for (int index = 0; index < data.length; index++) {
                row.createCell(index).setCellValue(data[index]);
            }
            workbook.write(output);
        }
        return file;
    }

    private static final class TestLoader extends AbstractDWHExcelLoader {
        private int processorCalls;
        private String finishedStatus;
        private String finishedMessage;
        private String errorCode;

        private TestLoader(DataSource dataSource) {
            super(dataSource, new TestDefinition());
        }

        private void validate(String... headers) {
            validateHeaderRow(headers);
        }

        private void read(Path file, long loadSessionId) throws Exception {
            readAndInsertExcel(file.toString(), loadSessionId);
        }

        @Override
        protected DWHExcelLoadSessionResult processLoadSession(Long loadSessionId) {
            processorCalls++;
            return new DWHExcelLoadSessionResult(loadSessionId, true, "OK");
        }

        @Override
        protected void updateLoadSessionStatus(Long loadSessionId, String status, String message) {
        }

        @Override
        protected void finishLoadSession(Long loadSessionId, String status, String message) {
            finishedStatus = status;
            finishedMessage = message;
        }

        @Override
        protected void logLoadError(Long loadSessionId, DWHExcelErrorLayer errorLayer,
                Long excelRowNum, Long rawId, String fieldName, String errorCode,
                String errorReason, String errorMessage) {
            this.errorCode = errorCode;
        }
    }

    private static final class TestDefinition implements DWHExcelLoadDefinition {
        @Override public DWHExcelLoadType loadType() { return DWHExcelLoadType.WEEKLY_DATA; }
        @Override public String rawTableName() { return "dbo.Test_raw"; }
        @Override public String targetTableName() { return "dbo.Test"; }
        @Override public String processProcedureName() { return "unused"; }
        @Override public int expectedColumnCount() { return 2; }
        @Override public List<DWHExcelColumnSpec> columns() {
            return List.of(
                    DWHExcelColumns.text(0, "Year", 50),
                    DWHExcelColumns.text(1, "Month", 50)
            );
        }
    }

    private static final class RecordingDataSource implements DataSource {
        private int rawRows;
        private int commits;
        private int rollbacks;
        private Map<Integer, Object> lastRow = Map.of();

        @Override
        public Connection getConnection() {
            Map<Integer, Object> current = new LinkedHashMap<>();
            PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setLong", "setString" -> {
                            current.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "addBatch" -> {
                            rawRows++;
                            lastRow = new LinkedHashMap<>(current);
                            current.clear();
                            yield null;
                        }
                        case "executeBatch" -> new int[rawRows];
                        case "close" -> null;
                        default -> defaultValue(method.getReturnType());
                    }
            );
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "prepareStatement" -> statement;
                        case "commit" -> { commits++; yield null; }
                        case "rollback" -> { rollbacks++; yield null; }
                        case "setAutoCommit", "close" -> null;
                        default -> defaultValue(method.getReturnType());
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
        if (returnType == boolean.class) return false;
        if (returnType == int.class) return 0;
        if (returnType == long.class) return 0L;
        return null;
    }
}
