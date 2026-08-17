package ru.stockmann.replenishment.services.dwhexcelload.core;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import javax.sql.DataSource;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Locale;

/** Shared streaming CSV source reader for the canonical DWH load lifecycle. */
public abstract class AbstractDWHCsvLoader extends AbstractDWHExcelLoader {

    private final char separator;

    protected AbstractDWHCsvLoader(
            DataSource dataSource,
            DWHExcelLoadDefinition definition,
            char separator
    ) {
        super(dataSource, definition);
        this.separator = separator;
    }

    @Override
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
        if (!filePath.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files (.csv) are allowed");
        }
    }

    @Override
    protected void readAndInsertExcel(String filePath, Long loadSessionId) throws Exception {
        int expectedColumns = definition.expectedColumnCount();
        int batchSize = definition.batchSize();
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement(buildRawInsertSql());
                     Reader input = Files.newBufferedReader(Path.of(filePath), StandardCharsets.UTF_8);
                     CSVReader csv = new CSVReaderBuilder(input)
                             .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                             .build()) {
                    String[] row;
                    long sourceRowNum = 0;
                    int inBatch = 0;
                    boolean headerProcessed = false;
                    while ((row = csv.readNext()) != null) {
                        sourceRowNum++;
                        if (!headerProcessed) {
                            if (row.length > 0 && row[0] != null && row[0].startsWith("\uFEFF")) {
                                row[0] = row[0].substring(1);
                            }
                            validateHeaderRow(row);
                            headerProcessed = true;
                            continue;
                        }
                        if (row.length != expectedColumns) {
                            throw new IllegalArgumentException(
                                    "CSV row " + sourceRowNum + " has " + row.length
                                            + " columns; expected " + expectedColumns
                            );
                        }
                        if (isEmpty(row)) {
                            continue;
                        }
                        bindRawRow(statement, loadSessionId, normalizeRow((int) sourceRowNum, row));
                        statement.addBatch();
                        inBatch++;
                        if (inBatch >= batchSize) {
                            statement.executeBatch();
                            inBatch = 0;
                        }
                    }
                    if (!headerProcessed) {
                        throw new IllegalArgumentException("Missing CSV header: file does not contain rows");
                    }
                    if (inBatch > 0) {
                        statement.executeBatch();
                    }
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        }
    }
}
