package ru.stockmann.replenishment.services.salesbychannel.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

public class SalesByChannelErrorRepository {

    private static final Logger log = LoggerFactory.getLogger(SalesByChannelErrorRepository.class);
    private static final String LOAD_TYPE_CODE = "SALES_BY_CHANNEL";
    private final DataSource dataSource;

    public SalesByChannelErrorRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.DWH_Excel_Load_Error
                WHERE LoadSessionId = ?
                  AND LoadTypeCode = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, loadSessionId);
            ps.setString(2, LOAD_TYPE_CODE);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete SalesByChannel errors. loadSessionId=" + loadSessionId, e
            );
        }
    }

    public void insertBatch(
            Connection connection,
            long loadSessionId,
            List<SalesByChannelValidationError> errors
    ) {
        if (errors == null || errors.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO dbo.DWH_Excel_Load_Error
                (
                    LoadSessionId, LoadTypeCode, ErrorLayer, ExcelRowNum, RawId,
                    FieldName, ErrorCode, ErrorReason, ErrorMessage
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        long startedAt = System.nanoTime();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (SalesByChannelValidationError error : errors) {
                if (error.loadSessionId() != loadSessionId) {
                    throw new IllegalArgumentException(
                            "SalesByChannel error belongs to another load session. expected="
                                    + loadSessionId + ", actual=" + error.loadSessionId()
                    );
                }
                ps.setLong(1, error.loadSessionId());
                ps.setString(2, LOAD_TYPE_CODE);
                ps.setString(3, error.errorLayer());
                nullableLong(ps, 4, error.excelRowNum());
                nullableLong(ps, 5, error.rawId());
                nullableString(ps, 6, error.fieldName());
                nullableString(ps, 7, error.errorCode());
                nullableString(ps, 8, error.errorReason());
                ps.setString(9, error.errorMessage());
                ps.addBatch();
            }
            int[] counts = ps.executeBatch();
            validateCounts(counts, errors.size(), loadSessionId);
            ps.clearBatch();
            log.info("SalesByChannel error chunk inserted. loadSessionId={}, chunkSize={}, elapsedMs={}, "
                            + "updateCountsLength={}",
                    loadSessionId, errors.size(), elapsedMs(startedAt), counts.length);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to insert SalesByChannel errors. loadSessionId=" + loadSessionId, e
            );
        }
    }

    public void insertBestEffort(SalesByChannelValidationError error) {
        try (Connection connection = dataSource.getConnection()) {
            insertBatch(connection, error.loadSessionId(), List.of(error));
        } catch (RuntimeException | SQLException ignored) {
        }
    }

    private void validateCounts(int[] counts, int expected, long loadSessionId) {
        if (counts == null || counts.length != expected) {
            throw new IllegalStateException(
                    "Unexpected SalesByChannel error update counts. loadSessionId=" + loadSessionId
            );
        }
        for (int count : counts) {
            if (count == Statement.EXECUTE_FAILED) {
                throw new IllegalStateException(
                        "SalesByChannel error batch contains failed statement. loadSessionId=" + loadSessionId
                );
            }
        }
    }

    private void nullableLong(PreparedStatement ps, int index, Long value) throws SQLException {
        if (value == null) ps.setNull(index, Types.BIGINT); else ps.setLong(index, value);
    }

    private void nullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) ps.setNull(index, Types.NVARCHAR); else ps.setString(index, value);
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }
}
