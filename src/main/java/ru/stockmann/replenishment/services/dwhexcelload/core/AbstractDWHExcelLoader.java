package ru.stockmann.replenishment.services.dwhexcelload.core;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractDWHExcelLoader {

    private static final Logger log = LoggerFactory.getLogger(AbstractDWHExcelLoader.class);

    protected final DataSource dataSource;
    protected final DWHExcelLoadDefinition definition;

    protected AbstractDWHExcelLoader(DataSource dataSource, DWHExcelLoadDefinition definition) {
        this.dataSource = dataSource;
        this.definition = definition;
    }

    public DWHExcelLoadResult acceptFile(String filePath) {

        Long loadSessionId = null;

        try {
            DWHExcelLoadSessionResult sessionResult = createLoadSession(filePath);
            loadSessionId = sessionResult.loadSessionId();

            if (!sessionResult.success()) {
                return DWHExcelLoadResult.error(loadSessionId, sessionResult.message());
            }

            validateFileBasic(filePath);

            updateLoadSessionStatus(
                    loadSessionId,
                    DWHExcelLoadStatus.QUEUED.name(),
                    "File accepted for processing"
            );

            return DWHExcelLoadResult.ok(
                    loadSessionId,
                    definition.serviceName() + " file accepted for processing"
            );

        } catch (Exception e) {
            String errorText = buildErrorText(e);

            log.error("AcceptFile failed. loadSessionId={}, filePath={}", loadSessionId, filePath, e);

            if (loadSessionId != null) {
                logLoadError(
                        loadSessionId,
                        DWHExcelErrorLayer.JAVA,
                        null,
                        null,
                        null,
                        "JAVA_LOAD_ERROR",
                        "Java load failed",
                        errorText
                );

                updateLoadSessionStatus(
                        loadSessionId,
                        "ERROR",
                        errorText
                );
            }

            return DWHExcelLoadResult.error(loadSessionId, errorText);
        }
    }

    protected void updateLoadSessionStatus(
            Long loadSessionId,
            String status,
            String message
    ) {
        String sql = """
            UPDATE dbo.DWH_Excel_Load_Session
            SET
                Status = ?,
                Message = ?
            WHERE Id = ?
            """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, message);
            ps.setLong(3, loadSessionId);

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to update load session status. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void processAcceptedFile(Long loadSessionId, String filePath) {

        if (loadSessionId == null) {
            throw new IllegalArgumentException("loadSessionId is required");
        }

        try {
            updateLoadSessionStatus(
                    loadSessionId,
                    DWHExcelLoadStatus.RUNNING.name(),
                    "Processing started"
            );

            readAndInsertExcel(filePath, loadSessionId);

            try (Connection conn = dataSource.getConnection();
                 CallableStatement stmt = conn.prepareCall("{call " + definition.processProcedureName() + "(?)}")) {

                stmt.setLong(1, loadSessionId);

                boolean hasResult = stmt.execute();

                if (!hasResult) {
                    throw new RuntimeException("Procedure returned no result");
                }

                try (ResultSet rs = stmt.getResultSet()) {

                    if (!rs.next()) {
                        throw new RuntimeException("Procedure returned empty result");
                    }

                    boolean success = rs.getBoolean("Success");
                    String message = rs.getString("Message");

                    if (success) {
                        finishLoadSession(
                                loadSessionId,
                                DWHExcelLoadStatus.SUCCESS.name(),
                                message
                        );
                    } else {
                        finishLoadSession(
                                loadSessionId,
                                DWHExcelLoadStatus.ERROR.name(),
                                message
                        );
                    }
                }

            } catch (Exception ex) {
                finishLoadSession(
                        loadSessionId,
                        DWHExcelLoadStatus.ERROR.name(),
                        "Fatal error: " + ex.getMessage()
                );
                throw new RuntimeException(ex);
            }

        } catch (Exception e) {
            String errorText = buildErrorText(e);

            log.error(
                    "ProcessAcceptedFile failed. loadSessionId={}, filePath={}",
                    loadSessionId,
                    filePath,
                    e
            );

            logLoadError(
                    loadSessionId,
                    DWHExcelErrorLayer.JAVA,
                    null,
                    null,
                    null,
                    "JAVA_LOAD_ERROR",
                    "Java load failed",
                    errorText
            );

            finishLoadSession(
                    loadSessionId,
                    DWHExcelLoadStatus.ERROR.name(),
                    errorText
            );

            throw new RuntimeException(
                    "Failed to process accepted file. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    protected String buildErrorText(Throwable e) {
        if (e == null) {
            return "Fatal error: unknown exception";
        }

        String className = e.getClass().getSimpleName();
        String message = e.getMessage();

        if (message == null || message.isBlank()) {
            return "Fatal error: " + className;
        }

        return "Fatal error: " + className + ": " + message;
    }

    protected void validateDefinition() {
        if (definition == null) {
            throw new IllegalStateException("Load definition is null");
        }

        if (definition.columns() == null || definition.columns().isEmpty()) {
            throw new IllegalStateException(
                    "Load definition columns are empty for load type " + definition.loadCode()
            );
        }

        if (definition.expectedColumnCount() != definition.columns().size()) {
            throw new IllegalStateException(
                    "Definition column count mismatch for load type " + definition.loadCode()
                            + ": expectedColumnCount=" + definition.expectedColumnCount()
                            + ", actualColumns=" + definition.columns().size()
            );
        }
    }

    protected void validateFileBasic(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("filePath is empty");
        }

        Path path = Path.of(filePath);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("file does not exist: " + filePath);
        }

        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("file is not readable: " + filePath);
        }

        String lower = filePath.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only Excel files (.xlsx) are allowed");
        }
    }

    protected void readAndInsertExcel(String filePath, Long loadSessionId) throws Exception {
        final int columnCount = definition.expectedColumnCount();
        final int batchSize = definition.batchSize();

        String sql = buildRawInsertSql();

        final int[] parsedRows = {0};
        final int[] emptyRows = {0};
        final int[] stagedRows = {0};
        final int[] lastRowNum = {0};

        Connection connection = null;

        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(sql);
                 OPCPackage pkg = OPCPackage.open(Path.of(filePath).toFile())) {

                XSSFReader reader = new XSSFReader(pkg);
                StylesTable styles = reader.getStylesTable();
                ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
                DataFormatter formatter = new DataFormatter();
                XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();

                if (!sheets.hasNext()) {
                    throw new IllegalArgumentException("Excel file does not contain sheets");
                }

                final int[] inBatch = {0};

                XSSFSheetXMLHandler.SheetContentsHandler handler = new XSSFSheetXMLHandler.SheetContentsHandler() {
                    private String[] currentRow;
                    private int currentRowNum = -1;

                    @Override
                    public void startRow(int rowNum) {
                        currentRowNum = rowNum;
                        lastRowNum[0] = rowNum;
                        currentRow = new String[columnCount];
                    }

                    @Override
                    public void endRow(int rowNum) {
                        parsedRows[0]++;

                        if (rowNum == 0) {
                            return; // header
                        }

                        if (isEmpty(currentRow)) {
                            emptyRows[0]++;
                            return;
                        }

                        ExcelRowData row = normalizeRow(currentRowNum, currentRow);

                        try {
                            bindRawRow(ps, loadSessionId, row);
                            ps.addBatch();
                            inBatch[0]++;

                            if (inBatch[0] >= batchSize) {
                                ps.executeBatch();
                                stagedRows[0] += inBatch[0];
                                inBatch[0] = 0;
                            }
                        } catch (SQLException e) {
                            throw new RuntimeException(
                                    "SQL error on Excel row " + (currentRowNum + 1) + ": " + e.getMessage(), e
                            );
                        }
                    }

                    @Override
                    public void cell(String cellReference, String formattedValue,
                                     org.apache.poi.xssf.usermodel.XSSFComment comment) {
                        int col = columnIndex(cellReference);
                        if (col >= 0 && col < columnCount) {
                            currentRow[col] = formattedValue;
                        }
                    }

                    @Override
                    public void headerFooter(String text, boolean isHeader, String tagName) {
                    }
                };

                XMLReader parser = XMLHelper.newXMLReader();
                parser.setContentHandler(new XSSFSheetXMLHandler(
                        styles, null, strings, handler, formatter, false
                ));

                try (InputStream sheetStream = sheets.next()) {
                    parser.parse(new InputSource(sheetStream));
                }

                if (inBatch[0] > 0) {
                    ps.executeBatch();
                    stagedRows[0] += inBatch[0];
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        log.info("Load {} summary: lastRow={}, parsedRows={}, emptyRows={}, stagedRows={}",
                definition.loadCode(), lastRowNum[0], parsedRows[0], emptyRows[0], stagedRows[0]);
    }

    protected ExcelRowData normalizeRow(int rowNum, String[] rowValues) {
        Map<String, String> values = new LinkedHashMap<>();

        for (DWHExcelColumnSpec col : definition.columns()) {

            String raw = col.excelIndex() < rowValues.length
                    ? rowValues[col.excelIndex()]
                    : null;

            String normalized = col.normalizer() != null
                    ? col.normalizer().normalize(raw)
                    : raw;

            normalized = applyNullHandling(col, normalized);

            values.put(col.rawColumnName(), normalized);
        }

        return new ExcelRowData(rowNum, values);
    }

    protected String applyNullHandling(DWHExcelColumnSpec col, String value) {
        if (value != null) {
            return value;
        }

        if (col.nullHandling() == null) {
            return null;
        }

        return switch (col.nullHandling()) {
            case KEEP_NULL -> null;
            case ZERO -> "0";
        };
    }

    protected void bindRawRow(PreparedStatement ps, Long loadSessionId, ExcelRowData row) throws SQLException {
        ps.setLong(1, loadSessionId);
        ps.setInt(2, row.rowNum());

        int paramIndex = 3;
        for (DWHExcelColumnSpec column : definition.columns()) {
            ps.setString(paramIndex++, row.get(column.rawColumnName()));
        }
    }

    protected String buildRawInsertSql() {
        String columns = definition.columns().stream()
                .map(DWHExcelColumnSpec::rawColumnName)
                .collect(Collectors.joining(", "));

        String placeholders = definition.columns().stream()
                .map(c -> "?")
                .collect(Collectors.joining(", "));

        return "INSERT INTO " + definition.rawTableName()
                + " (LoadSessionId, ExcelRowNum, " + columns + ") VALUES (?, ?, " + placeholders + ")";
    }

    protected void callProcessProcedure(Long loadSessionId) throws SQLException {
        String sql = "EXEC " + definition.processProcedureName() + " @LoadSessionId = ?";

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            ps.execute();
        }
    }

    protected DWHExcelLoadSessionResult createLoadSession(String filePath) {
        Long loadSessionId = null;
        Connection c = null;

        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            String fileName = filePath != null ? Path.of(filePath).getFileName().toString() : null;

            String sql = """
                INSERT INTO dbo.DWH_Excel_Load_Session
                (
                    LoadTypeCode,
                    ServiceName,
                    FileName,
                    FilePath,
                    Status
                )
                OUTPUT INSERTED.Id
                VALUES (?, ?, ?, ?, ?)
                """;

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, definition.loadCode());
                ps.setString(2, definition.serviceName());
                ps.setString(3, fileName);
                ps.setString(4, filePath);
                ps.setString(5, DWHExcelLoadStatus.QUEUED.name());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        loadSessionId = rs.getLong(1);
                    } else {
                        c.rollback();
                        return DWHExcelLoadSessionResult.error(null, "Failed to create load session");
                    }
                }
            }

            c.commit();
            return DWHExcelLoadSessionResult.ok(loadSessionId);
        } catch (Exception ex) {
            rollbackQuietly(c);
            return DWHExcelLoadSessionResult.error(loadSessionId, "Fatal error: " + ex.getMessage());
        } finally {
            closeQuietly(c);
        }
    }
    protected void finishLoadSession(Long loadSessionId, String status, String message) {
        if (loadSessionId == null) {
            return;
        }

        Connection c = null;

        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            String sql = """
                UPDATE dbo.DWH_Excel_Load_Session
                SET Status = ?,
                    FinishedAt = SYSDATETIME(),
                    Message = ?
                WHERE Id = ?
                """;

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, message);
                ps.setLong(3, loadSessionId);
                ps.executeUpdate();
            }

            c.commit();
        } catch (Exception ex) {
            rollbackQuietly(c);
            log.error("finishLoadSession error. loadSessionId={}", loadSessionId, ex);
        } finally {
            closeQuietly(c);
        }
    }


    protected void safeFinishWithError(Long loadSessionId, String errorText) {
        try {
            finishLoadSession(loadSessionId, DWHExcelLoadStatus.ERROR.name(), errorText);
        } catch (Exception ignored) {
        }
    }

    protected static boolean isEmpty(String[] row) {
        if (row == null) {
            return true;
        }

        for (String s : row) {
            if (s != null && !s.isBlank()) {
                return false;
            }
        }

        return true;
    }

    protected static int columnIndex(String cellReference) {
        if (cellReference == null || cellReference.isBlank()) {
            return -1;
        }

        int col = 0;
        for (int i = 0; i < cellReference.length(); i++) {
            char ch = cellReference.charAt(i);
            if (Character.isDigit(ch)) {
                break;
            }
            col = col * 26 + (Character.toUpperCase(ch) - 'A' + 1);
        }

        return col - 1;
    }

    protected static void rollbackQuietly(Connection c) {
        if (c != null) {
            try {
                c.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    protected static void closeQuietly(Connection c) {
        if (c != null) {
            try {
                c.close();
            } catch (SQLException ignored) {
            }
        }
    }

    protected void logLoadError(
            Long loadSessionId,
            DWHExcelErrorLayer errorLayer,
            Integer excelRowNum,
            Long rawId,
            String fieldName,
            String errorCode,
            String errorReason,
            String errorMessage
    ) {
        String sql = """
            INSERT INTO dbo.DWH_Excel_Load_Error
            (
                LoadSessionId,
                LoadTypeCode,
                ErrorLayer,
                ExcelRowNum,
                RawId,
                FieldName,
                ErrorCode,
                ErrorReason,
                ErrorMessage
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, loadSessionId);
            ps.setString(2, definition.loadCode());
            ps.setString(3, errorLayer.name());

            if (excelRowNum != null) ps.setInt(4, excelRowNum); else ps.setNull(4, java.sql.Types.INTEGER);
            if (rawId != null) ps.setLong(5, rawId); else ps.setNull(5, java.sql.Types.BIGINT);

            ps.setString(6, fieldName);
            ps.setString(7, errorCode);
            ps.setString(8, errorReason);
            ps.setString(9, errorMessage);

            ps.executeUpdate();
        } catch (Exception ex) {
            log.error("Failed to log load error. loadSessionId={}", loadSessionId, ex);
        }
    }

    public DWHExcelLoadDefinition getDefinition() {
        return definition;
    }

}