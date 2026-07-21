package ru.stockmann.replenishment.services;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.AbstractDataSource;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataProcessResult;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataProcessor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyDataBulkLoaderTest {

    @Test
    void processLoadSessionCallsProcessorWithSameLoadSessionId() {
        RecordingDataSource dataSource = new RecordingDataSource();
        FakeWeeklyDataProcessor processor = new FakeWeeklyDataProcessor(new WeeklyDataProcessResult(
                100L,
                true,
                2,
                2,
                0,
                "WeeklyData load session processed successfully"
        ));
        WeeklyDataBulkLoader loader = new WeeklyDataBulkLoader(dataSource, processor);

        DWHExcelLoadSessionResult result = loader.processLoadSession(100L);

        assertEquals(1, processor.calls);
        assertEquals(100L, processor.loadSessionId);
        assertTrue(result.success());
        assertEquals("WeeklyData load session processed successfully", result.message());
        assertEquals(0, dataSource.connections);
    }

    @Test
    void defaultProcedureFallbackIsNotAvailableForWeeklyData() {
        RecordingDataSource dataSource = new RecordingDataSource();
        FakeWeeklyDataProcessor processor = new FakeWeeklyDataProcessor(new WeeklyDataProcessResult(
                100L,
                true,
                0,
                0,
                0,
                "OK"
        ));
        TestWeeklyDataBulkLoader loader = new TestWeeklyDataBulkLoader(dataSource, processor);

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> loader.callDefaultProcedure(100L)
        );

        assertTrue(exception.getMessage().contains("WeeklyData processing is implemented in Java"));
        assertEquals(0, dataSource.connections);
    }

    private static final class FakeWeeklyDataProcessor extends WeeklyDataProcessor {

        private final WeeklyDataProcessResult result;
        private int calls;
        private long loadSessionId;

        private FakeWeeklyDataProcessor(WeeklyDataProcessResult result) {
            super(null, null, null, null, null, null, null);
            this.result = result;
        }

        @Override
        public WeeklyDataProcessResult process(long loadSessionId) {
            this.calls++;
            this.loadSessionId = loadSessionId;
            return result;
        }
    }

    private static final class RecordingDataSource extends AbstractDataSource {

        private int connections;

        @Override
        public Connection getConnection() throws SQLException {
            connections++;
            throw new SQLException("WeeklyDataBulkLoader processLoadSession must not use DataSource");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            connections++;
            throw new SQLException("WeeklyDataBulkLoader processLoadSession must not use DataSource");
        }
    }

    private static final class TestWeeklyDataBulkLoader extends WeeklyDataBulkLoader {

        private TestWeeklyDataBulkLoader(DataSource dataSource, WeeklyDataProcessor processor) {
            super(dataSource, processor);
        }

        private void callDefaultProcedure(Long loadSessionId) throws Exception {
            callProcessProcedure(loadSessionId);
        }
    }
}
