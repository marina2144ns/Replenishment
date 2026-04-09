package ru.stockmann.replenishment.services;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import ru.stockmann.replenishment.models.CDDataLoadResult;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Locale;

@Service
public class CDDataBulkLoader {

    private static final int BATCH = 10_000;
    private static final int COLUMN_COUNT = 37;

    private static final Logger log = LoggerFactory.getLogger(CDDataBulkLoader.class);

    private final DataSource dataSource;

    public CDDataBulkLoader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CDDataLoadResult bulkLoad(String filePath) {

        final int[] lastRowNum = {0};
        final int[] parsedRows = {0};
        final int[] emptyRows = {0};
        final int[] stagedRows = {0};

        // СОЗДАЕМ СЕССИЮ ОБРАБОТКИ
        MethodSessionResult result = createLoadSession(filePath);
        Long loadSessionId = result.loadSessionId();
        if (!result.success()) {
            return CDDataLoadResult.error(
                    loadSessionId,
                    result.message()
            );
        }

        // ПРОВЕРЯЕМ ФАЙЛ
        if (filePath == null || filePath.isBlank()) {
            log.warn("Load session {}: filePath - {} - is empty", loadSessionId, filePath);
            if (loadSessionId != null) {
                finishLoadSession(loadSessionId, "ERROR", "filePath is empty");
            }
            return CDDataLoadResult.error(loadSessionId, "filePath is empty");
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            log.warn("Load session {}: file - {} - does not exist", loadSessionId, filePath);
            if (loadSessionId != null) {
                finishLoadSession(loadSessionId, "ERROR", "file does not exist");
            }
            return CDDataLoadResult.error(loadSessionId, "file does not exist: " + filePath);
        }

        if (!Files.isReadable(path)) {
            log.warn("Load session {}: file - {} - is not readable", loadSessionId, filePath);
            if (loadSessionId != null) {
                finishLoadSession(loadSessionId, "ERROR", "file is not readable");
            }
            return CDDataLoadResult.error(loadSessionId, "file is not readable: " + filePath);
        }

        String lower = filePath.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx")) {
            log.warn("Load session {}: file - {} - invalid file extension", loadSessionId, filePath);
            if (loadSessionId != null) {
                finishLoadSession(loadSessionId, "ERROR", "invalid file extension");
            }
            return CDDataLoadResult.error(loadSessionId, "Only Excel files (.xlsx) are allowed");
        }

        String sql = """
                INSERT INTO dbo.CD_data_raw
                (LoadSessionId,
                 nazvanie, god, sezon, den, data,
                 sales_channel, store_rus, mfp_division, mfp_department, mfp_sub_department,
                 sku_brand_type, sku_tm, mfp_node, section, merchandise_sub_group, campaign_sales,
                 sku_style_color, sku_phase,
                 stock_start_pcs, stock_start_dd,
                 sales_pcs, sales_rub, revenue, gp, cogs,
                 sales_frp_price, sales_discount,
                 stock_stores_pcs, stock_stores_dd,
                 plan_rub,
                 draivery_cd, sku_color_rus, sku_composition, sku_supplier, sku_name, sku_collection, sku_comment)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;

        Connection c = null;
        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement(sql);
                 OPCPackage pkg = OPCPackage.open(Path.of(filePath).toFile())) {

                XSSFReader reader = new XSSFReader(pkg);
                StylesTable styles = reader.getStylesTable();
                ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
                DataFormatter formatter = new DataFormatter();

                XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
                if (!sheets.hasNext()) {
                    try {
                        c.rollback();
                    } catch (SQLException ignored) {
                    }
                    log.error("Load session {}: file - {}. Excel file does not contain sheets", loadSessionId, filePath);
                    if (loadSessionId != null) {
                        finishLoadSession(loadSessionId, "ERROR", "Excel file does not contain sheets");
                    }
                    return CDDataLoadResult.error(loadSessionId, "Excel file does not contain sheets");
                }

                final int[] inBatch = {0};

                XSSFSheetXMLHandler.SheetContentsHandler handler = new XSSFSheetXMLHandler.SheetContentsHandler() {
                    private String[] currentRow;
                    private int currentRowNum = -1;

                    @Override
                    public void startRow(int rowNum) {
                        currentRowNum = rowNum;
                        lastRowNum[0] = rowNum;
                        currentRow = new String[COLUMN_COUNT];
                    }

                    @Override
                    public void endRow(int rowNum) {
                        parsedRows[0]++;

                        if (rowNum % 10000 == 0) {
                            log.info("Parsed Excel row: {}", rowNum);
                        }

                        // 0-я строка = заголовок
                        if (rowNum == 0) {
                            return;
                        }

                        if (isEmpty(currentRow)) {
                            emptyRows[0]++;
                            if (rowNum % 10000 == 0) {
                                log.warn("Empty row detected at {}", rowNum);
                            }
                            return;
                        }

                        try {
                            ps.setLong(1, loadSessionId);

                            for (int i = 0; i < COLUMN_COUNT; i++) {
                                ps.setString(i + 2, trimTo(currentRow[i], dbSize(i)));
                            }

                            ps.addBatch();
                            inBatch[0]++;

                            if (inBatch[0] >= BATCH) {
                                ps.executeBatch();
                                stagedRows[0] += inBatch[0];
                                inBatch[0] = 0;
                            }

                        } catch (SQLException e) {
                            throw new RuntimeException("SQL error on Excel row " + (currentRowNum + 1) + ": " + e.getMessage(), e);
                        }
                    }

                    @Override
                    public void cell(String cellReference, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
                        int col = columnIndex(cellReference);
                        if (col >= 0 && col < COLUMN_COUNT) {
                            currentRow[col] = normalize(formattedValue);
                        }
                    }

                    @Override
                    public void headerFooter(String text, boolean isHeader, String tagName) {
                        // не нужно
                    }
                };

                XMLReader parser = XMLHelper.newXMLReader();
                parser.setContentHandler(new XSSFSheetXMLHandler(styles, null, strings, handler, formatter, false));

                try (InputStream sheetStream = sheets.next()) {
                    parser.parse(new InputSource(sheetStream));
                }

                if (inBatch[0] > 0) {
                    ps.executeBatch();
                    stagedRows[0] += inBatch[0];
                }

                c.commit();
            }

            log.info("=== LOAD SUMMARY ===");
            log.info("Last parsed Excel row = {}", lastRowNum[0]);
            log.info("Total parsed rows (callbacks) = {}", parsedRows[0]);
            log.info("Empty rows skipped = {}", emptyRows[0]);
            log.info("Inserted rows = {}", stagedRows[0]);
            log.info("====================");

        } catch (SQLException ex) {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                }
            }
            log.error("Load session {}: file - {}. SQL error ", loadSessionId, filePath, ex);
            if (loadSessionId != null) {
                finishLoadSession(loadSessionId, "ERROR", "SQL error after processing " + stagedRows[0] + " rows: " + ex.getMessage());
            }
            return CDDataLoadResult.error(loadSessionId, "SQL error after processing " + stagedRows[0] + " rows: " + ex.getMessage());
        } catch (Exception ex) {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                }
            }
            log.error("Load session {}: file - {}. Fatal error ", loadSessionId, filePath, ex);
            if (loadSessionId != null) {
                finishLoadSession(loadSessionId, "ERROR", "Fatal error after processing " + stagedRows[0] + " rows: " + ex.getMessage());
            }
            return CDDataLoadResult.error(loadSessionId, "Fatal error after processing " + stagedRows[0] + " rows: " + ex.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException ignored) {
                }
            }
        }

        // ЗАКРЫВАЕМ СЕССИЮ
        result = finishLoadSession(loadSessionId, "SUCCESS", "OK");
        if (!result.success()) {
            return CDDataLoadResult.error(
                    loadSessionId,
                    result.message()
            );
        }
        return CDDataLoadResult.ok(loadSessionId);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static boolean isEmpty(String[] row) {
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

    private static int columnIndex(String cellReference) {
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

    private static int dbSize(int idx) {
        return switch (idx) {
            case 0 -> 255;  // nazvanie
            case 1, 2, 3, 4 -> 50; // god, sezon, den, data
            case 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 -> 255; // text columns
            case 16 -> 50;  // sku_style_color
            case 17 -> 255; // sku_phase
            case 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29 -> 50; // numeric staging as string
            case 30, 31, 32, 33, 34, 35, 36 -> 255; // text columns
            default -> 255;
        };
    }

    private static String trimTo(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    public record MethodSessionResult(
            Long loadSessionId,
            boolean success,
            String message
    ) {
        public static MethodSessionResult ok(Long loadSessionId) {
            return new MethodSessionResult(loadSessionId, true, null);
        }

        public static MethodSessionResult error(Long loadSessionId, String message) {
            return new MethodSessionResult(loadSessionId, false, message);
        }
    }

    private MethodSessionResult createLoadSession(String filePath) {
        Long loadSessionId = null;
        Connection c = null;

        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            String fileName = filePath != null ? Path.of(filePath).getFileName().toString() : null;

            try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO dbo.CD_data_Load_session (FileName, FilePath, Status)
                OUTPUT INSERTED.Id
                VALUES (?, ?, 'RUNNING')
                """)) {
                ps.setString(1, fileName);
                ps.setString(2, filePath);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        loadSessionId = rs.getLong(1);
                    } else {
                        log.error("Failed to create load session");
                        return MethodSessionResult.error(loadSessionId, "Failed to create load session");
                    }
                }
            }

            c.commit();
        } catch (Exception ex) {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                }
            }
            log.error("Fatal error creating load session ", ex);
            return MethodSessionResult.error(loadSessionId, "Fatal error: " + ex.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException ignored) {
                }
            }
        }

        return MethodSessionResult.ok(loadSessionId);
    }

    private MethodSessionResult finishLoadSession(Long loadSessionId, String status, String message) {

        if (loadSessionId == null) {
            log.error("finishLoadSession called with null loadSessionId. Status={}, message={}", status, message);
            return MethodSessionResult.error(null, "loadSessionId is null");
        }

        Connection c = null;

        try {
            c = dataSource.getConnection();
            c.setAutoCommit(false);

            try (PreparedStatement ps = c.prepareStatement("""
                UPDATE dbo.CD_data_Load_session
                SET Status = ?, FinishedAt = SYSDATETIME(), Message = ?
                WHERE Id = ?
                """)) {
                ps.setString(1, status);
                ps.setString(2, message);
                ps.setLong(3, loadSessionId);

                int updated = ps.executeUpdate();
                if (updated != 1) {
                    log.error("finishLoadSession: session {} not found or not updated", loadSessionId);
                    c.rollback();
                    return MethodSessionResult.error(loadSessionId, "Session not found or not updated");
                }
            }

            c.commit();
        } catch (Exception ex) {
            if (c != null) {
                try {
                    c.rollback();
                } catch (SQLException ignored) {
                }
            }
            log.error("finishLoadSession FATAL ERROR session " + loadSessionId + "  ", ex);
            return MethodSessionResult.error(loadSessionId, "Fatal error closing session: " + ex.getMessage());
        } finally {
            if (c != null) {
                try {
                    c.close();
                } catch (SQLException ignored) {
                }
            }
        }

        return MethodSessionResult.ok(loadSessionId);
    }
}