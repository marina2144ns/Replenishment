package ru.stockmann.replenishment.services.weeklydata.process;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.util.ReflectionTestUtils;
import ru.stockmann.replenishment.services.WeeklyDataBulkLoader;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringJUnitConfig(classes = {
        WeeklyDataProcessContextTest.TestConfig.class,
        WeeklyDataProcessConfiguration.class,
        WeeklyDataBulkLoader.class
})
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class WeeklyDataProcessContextTest {

    @Autowired
    private WeeklyDataBulkLoader weeklyDataBulkLoader;

    @Autowired
    private WeeklyDataProcessor weeklyDataProcessor;

    @Autowired
    private WeeklyDataProcessConfiguration weeklyDataProcessConfiguration;

    @Autowired
    private WeeklyDataLoadSessionRepository weeklyDataLoadSessionRepository;

    @Autowired
    private WeeklyDataRawRepository weeklyDataRawRepository;

    @Autowired
    private WeeklyDataErrorRepository weeklyDataErrorRepository;

    @Autowired
    private WeeklyDataTargetRepository weeklyDataTargetRepository;

    @Autowired
    private WeeklyDataValidator weeklyDataValidator;

    @Autowired
    private WeeklyDataRowMapper weeklyDataRowMapper;

    @Test
    void weeklyDataProcessingBeansAreCreated() {
        assertNotNull(weeklyDataBulkLoader);
        assertNotNull(weeklyDataProcessor);
        assertNotNull(weeklyDataProcessConfiguration);
        assertNotNull(weeklyDataLoadSessionRepository);
        assertNotNull(weeklyDataRawRepository);
        assertNotNull(weeklyDataErrorRepository);
        assertNotNull(weeklyDataTargetRepository);
        assertNotNull(weeklyDataValidator);
        assertNotNull(weeklyDataRowMapper);
    }

    @Test
    void weeklyDataBulkLoaderIsWiredWithWeeklyDataProcessor() {
        Object wiredProcessor = ReflectionTestUtils.getField(weeklyDataBulkLoader, "weeklyDataProcessor");

        assertSame(weeklyDataProcessor, wiredProcessor);
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
