package ru.stockmann.replenishment.services;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class ABCBulkLoader {

    private static final int BATCH = 10_000; // размер пачки вставок в STG

    private final DataSource dataSource;

    public ABCBulkLoader(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** CSV -> STG JDBC batch -> MERGE */
    public LoadResult bulkLoad(String filePath, String timePeriod) {
        List<String> errors = new ArrayList<>();
        long stagedRows = 0;

        // базовые проверки файла
        try {
            if (filePath == null || filePath.isBlank())
                return LoadResult.error("filePath is empty");
            Path p = Path.of(filePath);
            if (!Files.exists(p))
                return LoadResult.error("file does not exist: " + filePath);
            if (!Files.isReadable(p))
                return LoadResult.error("file is not readable: " + filePath);
        } catch (Exception ex) {
            return LoadResult.error("failed to access file: " + ex.getMessage());
        }

        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);

            // 0) проверка наличия STG
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM sys.objects WHERE object_id = OBJECT_ID('dbo.ABCData_STG') AND type = 'U'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return LoadResult.error("Table dbo.ABCData_STG does not exist. Create it first.");
                    }
                }
            }

            // 1) очистка STG: сначала пробуем TRUNCATE, если нет прав — DELETE
            try (Statement st = c.createStatement()) {
                try {
                    st.execute("TRUNCATE TABLE dbo.ABCData_STG");
                } catch (SQLException ex) {
                    st.executeUpdate("DELETE FROM dbo.ABCData_STG");
                }
            }

            // 2) вставка CSV -> STG порциями
            String sql = """
                    INSERT INTO dbo.ABCData_STG
                    (SkuTM, Section, MFPDepartment, MerchandiseSubGroup, SKUItem,
                     SalesCurrRaw, AccumPercentRaw, ABC, ABCNO, SupplyDateRaw)
                    VALUES (?,?,?,?,?,?,?,?,?,?)
                    """;

            int inBatch = 0;
            try (BufferedReader br = Files.newBufferedReader(Path.of(filePath), StandardCharsets.UTF_8);
                 CSVReader reader = new CSVReaderBuilder(br)
                         .withCSVParser(new CSVParserBuilder().withSeparator(';').build())
                         .build();
                 PreparedStatement ps = c.prepareStatement(sql)) {

                String[] row;
                // пропускаем заголовок
                reader.readNext();

                while ((row = reader.readNext()) != null) {
                    // ожидаем порядок колонок:
                    // 0 SkuTM; 1 Section; 2 MFPDepartment; 3 MerchandiseSubGroup; 4 SKUItem;
                    // 5 SalesCurr; 6 AccumPercent; 7 ABC; 8 ABCNO; 9 SupplyDate
                    ps.setString(1,  trimTo(safe(row,0), 150));
                    ps.setString(2,  trimTo(safe(row,1), 150));
                    ps.setString(3,  trimTo(safe(row,2), 150));
                    ps.setString(4,  trimTo(safe(row,3), 150));
                    ps.setString(5,  trimTo(safe(row,4),  50));
                    ps.setString(6,  digitsOnly(safe(row,5)));
                    ps.setString(7,  digitsOnly(safe(row,6)));
                    ps.setString(8,  safe(row,7));
                    ps.setString(9,  safe(row,8));
                    ps.setString(10, safe(row,9));

                    ps.addBatch();
                    inBatch++;
                    if (inBatch >= BATCH) {
                        ps.executeBatch();
                        c.commit();
                        stagedRows += inBatch;
                        inBatch = 0;
                    }
                }

                if (inBatch > 0) {
                    ps.executeBatch();
                    c.commit();
                    stagedRows += inBatch;
                }
            }

            // 3) MERGE через хранимую процедуру
            try (CallableStatement cs = c.prepareCall("{ call dbo.usp_ABCData_Merge(?) }")) {
                cs.setString(1, timePeriod);
                cs.execute();
            }
            c.commit();

            return LoadResult.ok(stagedRows);

        } catch (SQLException ex) {
            errors.add("SQL error: " + ex.getMessage());
            return new LoadResult("ERROR", stagedRows, errors);
        } catch (Exception ex) {
            errors.add("Fatal error: " + ex.getMessage());
            return new LoadResult("ERROR", stagedRows, errors);
        }
    }

    private static String safe(String[] row, int idx) {
        return (row != null && idx < row.length) ? row[idx] : null;
    }

    private static String trimTo(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    /** Компактный DTO результата */
    public record LoadResult(String status, long stagedRows, List<String> errors) {
        public static LoadResult ok(long stagedRows) {
            return new LoadResult("OK", stagedRows, List.of());
        }
        public static LoadResult error(String message) {
            return new LoadResult("ERROR", 0, List.of(message));
        }
        public String status() { return status; }
    }

    private static String digitsOnly(String s) {
        if (s == null) return null;
        String cleaned = s.replaceAll("[^\\d]", ""); // оставляем только цифры
        return cleaned.isEmpty() ? null : cleaned;
    }

}
