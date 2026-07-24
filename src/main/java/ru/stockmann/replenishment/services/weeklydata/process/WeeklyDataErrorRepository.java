package ru.stockmann.replenishment.services.weeklydata.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class WeeklyDataErrorRepository {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDataErrorRepository.class);
    private static final String LOAD_TYPE_CODE = "WEEKLY_DATA";
    private static final int PROGRESS_INTERVAL = 5_000;

    private final DataSource dataSource;

    public WeeklyDataErrorRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void deleteByLoadSessionId(long loadSessionId) {
        try (Connection connection = dataSource.getConnection()) {
            deleteByLoadSessionId(connection, loadSessionId);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete WeeklyData load errors. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void deleteByLoadSessionId(Connection connection, long loadSessionId) {
        String sql = """
                DELETE FROM dbo.DWH_Excel_Load_Error
                WHERE LoadSessionId = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setLong(1, loadSessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to delete WeeklyData load errors. loadSessionId=" + loadSessionId,
                    e
            );
        }
    }

    public void insertAll(List<WeeklyDataValidationError> errors) {
        try (Connection connection = dataSource.getConnection()) {
            if (errors != null && !errors.isEmpty()) {
                insertBatch(connection, errors.get(0).loadSessionId(), errors);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert WeeklyData load errors", e);
        }
    }

    public void insertBatch(
            Connection connection,
            long loadSessionId,
            List<WeeklyDataValidationError> errors
    ) {
        if (errors == null || errors.isEmpty()) {
            return;
        }

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

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            long bindStartedAt = System.nanoTime();
            int processed = 0;
            for (WeeklyDataValidationError error : errors) {
                ps.setLong(1, error.loadSessionId());
                ps.setString(2, LOAD_TYPE_CODE);
                ps.setString(3, error.errorLayer());
                setNullableBigInt(ps, 4, error.excelRowNum());
                ps.setLong(5, error.rawId());
                ps.setString(6, error.fieldName());
                ps.setString(7, error.errorCode());
                ps.setString(8, error.errorReason());
                ps.setString(9, error.errorMessage());
                ps.addBatch();
                processed++;
                if (processed % PROGRESS_INTERVAL == 0) {
                    log.info("WeeklyData error batch binding progress. loadSessionId={}, processedErrors={}, "
                                    + "totalErrors={}, elapsedMs={}",
                            loadSessionId, processed, errors.size(), elapsedMs(bindStartedAt));
                }
            }
            log.info("WeeklyData error batch binding finished. loadSessionId={}, errors={}, elapsedMs={}",
                    loadSessionId, processed, elapsedMs(bindStartedAt));

            long executeStartedAt = System.nanoTime();
            log.info("WeeklyData error executeBatch started. loadSessionId={}, errors={}",
                    loadSessionId, errors.size());
            int[] updateCounts = ps.executeBatch();
            validateUpdateCounts(updateCounts, errors.size(), loadSessionId);
            ps.clearBatch();
            log.info("WeeklyData error executeBatch finished. loadSessionId={}, errors={}, elapsedMs={}, "
                            + "updateCountsLength={}",
                    loadSessionId, errors.size(), elapsedMs(executeStartedAt), updateCounts.length);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert WeeklyData load errors", e);
        }
    }

    private void validateUpdateCounts(int[] updateCounts, int expected, long loadSessionId) {
        if (updateCounts == null || updateCounts.length != expected) {
            throw new IllegalStateException(
                    "Unexpected WeeklyData error update counts. loadSessionId=" + loadSessionId
                            + ", expected=" + expected
                            + ", actual=" + (updateCounts == null ? "null" : updateCounts.length)
            );
        }
        for (int updateCount : updateCounts) {
            if (updateCount == Statement.EXECUTE_FAILED) {
                throw new IllegalStateException(
                        "WeeklyData error batch contains failed statement. loadSessionId=" + loadSessionId
                );
            }
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private void setNullableBigInt(PreparedStatement ps, int parameterIndex, Long value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, java.sql.Types.BIGINT);
        } else {
            ps.setLong(parameterIndex, value);
        }
    }
}
