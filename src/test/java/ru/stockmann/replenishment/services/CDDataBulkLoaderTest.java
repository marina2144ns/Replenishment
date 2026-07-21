package ru.stockmann.replenishment.services;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.AbstractDataSource;
import ru.stockmann.replenishment.services.cddata.process.CDDataProcessResult;
import ru.stockmann.replenishment.services.cddata.process.CDDataProcessor;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDDataBulkLoaderTest {

    @Test
    void processLoadSessionCallsProcessorWithSameLoadSessionId() {
        RecordingDataSource dataSource = new RecordingDataSource();
        FakeCDDataProcessor processor = new FakeCDDataProcessor(new CDDataProcessResult(
                100L,
                true,
                2,
                2,
                0,
                "CDData load session processed successfully"
        ));
        CDDataBulkLoader loader = new CDDataBulkLoader(dataSource, processor);

        DWHExcelLoadSessionResult result = loader.processLoadSession(100L);

        assertEquals(1, processor.calls);
        assertEquals(100L, processor.loadSessionId);
        assertTrue(result.success());
        assertEquals("CDData load session processed successfully", result.message());
        assertEquals(0, dataSource.connections);
    }

    @Test
    void defaultProcedureFallbackIsNotAvailableForCdData() {
        RecordingDataSource dataSource = new RecordingDataSource();
        FakeCDDataProcessor processor = new FakeCDDataProcessor(new CDDataProcessResult(
                100L,
                true,
                0,
                0,
                0,
                "OK"
        ));
        TestCDDataBulkLoader loader = new TestCDDataBulkLoader(dataSource, processor);

        UnsupportedOperationException exception = org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> loader.callDefaultProcedure(100L)
        );

        assertTrue(exception.getMessage().contains("CDData processing is implemented in Java"));
        assertEquals(0, dataSource.connections);
    }

    @Test
    void validationFailureResultIsReturnedAsFailure() {
        RecordingDataSource dataSource = new RecordingDataSource();
        FakeCDDataProcessor processor = new FakeCDDataProcessor(new CDDataProcessResult(
                100L,
                false,
                2,
                0,
                3,
                "Validation failed"
        ));
        CDDataBulkLoader loader = new CDDataBulkLoader(dataSource, processor);

        DWHExcelLoadSessionResult result = loader.processLoadSession(100L);

        assertFalse(result.success());
        assertEquals("Validation failed", result.message());
        assertEquals(0, dataSource.connections);
    }

    @Test
    void processingFailureResultIsReturnedAsFailure() {
        RecordingDataSource dataSource = new RecordingDataSource();
        FakeCDDataProcessor processor = new FakeCDDataProcessor(new CDDataProcessResult(
                100L,
                false,
                1,
                0,
                1,
                "raw failed"
        ));
        CDDataBulkLoader loader = new CDDataBulkLoader(dataSource, processor);

        DWHExcelLoadSessionResult result = loader.processLoadSession(100L);

        assertFalse(result.success());
        assertEquals("raw failed", result.message());
        assertEquals(0, dataSource.connections);
    }

    private static final class FakeCDDataProcessor extends CDDataProcessor {

        private final CDDataProcessResult result;
        private int calls;
        private long loadSessionId;

        private FakeCDDataProcessor(CDDataProcessResult result) {
            super(null, null, null, null, null, null, null);
            this.result = result;
        }

        @Override
        public CDDataProcessResult process(long loadSessionId) {
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
            throw new SQLException("CDDataBulkLoader processLoadSession must not use DataSource");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            connections++;
            throw new SQLException("CDDataBulkLoader processLoadSession must not use DataSource");
        }
    }

    private static final class TestCDDataBulkLoader extends CDDataBulkLoader {

        private TestCDDataBulkLoader(DataSource dataSource, CDDataProcessor processor) {
            super(dataSource, processor);
        }

        private void callDefaultProcedure(Long loadSessionId) throws Exception {
            callProcessProcedure(loadSessionId);
        }
    }
}
