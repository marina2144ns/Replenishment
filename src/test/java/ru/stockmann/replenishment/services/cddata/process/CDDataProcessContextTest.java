package ru.stockmann.replenishment.services.cddata.process;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;
import ru.stockmann.replenishment.services.CDDataBulkLoader;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringJUnitConfig(classes = {
        CDDataProcessContextTest.TestConfig.class,
        CDDataProcessConfiguration.class,
        CDDataBulkLoader.class
})
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class CDDataProcessContextTest {

    @Autowired
    private CDDataBulkLoader cdDataBulkLoader;

    @Autowired
    private CDDataProcessor cdDataProcessor;

    @Autowired
    private CDDataProcessConfiguration cdDataProcessConfiguration;

    @Autowired
    private CDDataLoadSessionRepository cdDataLoadSessionRepository;

    @Autowired
    private CDDataRawRepository cdDataRawRepository;

    @Autowired
    private CDDataTargetRepository cdDataTargetRepository;

    @Autowired
    private CDDataErrorRepository cdDataErrorRepository;

    @Autowired
    private CDDataValidator cdDataValidator;

    @Autowired
    private CDDataRowMapper cdDataRowMapper;

    @Test
    void cdDataProcessingBeansAreCreated() {
        assertNotNull(cdDataBulkLoader);
        assertNotNull(cdDataProcessor);
        assertNotNull(cdDataProcessConfiguration);
        assertNotNull(cdDataLoadSessionRepository);
        assertNotNull(cdDataRawRepository);
        assertNotNull(cdDataTargetRepository);
        assertNotNull(cdDataErrorRepository);
        assertNotNull(cdDataValidator);
        assertNotNull(cdDataRowMapper);
    }

    @Test
    void cdDataBulkLoaderIsWiredWithCDDataProcessor() {
        Object wiredProcessor = ReflectionTestUtils.getField(cdDataBulkLoader, "cdDataProcessor");

        assertSame(cdDataProcessor, wiredProcessor);
    }

    static class TestConfig {

        @Bean
        DataSource dataSource() {
            return new AbstractDataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    throw new SQLException("Test DataSource must not be used");
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException {
                    throw new SQLException("Test DataSource must not be used");
                }
            };
        }
    }
}
