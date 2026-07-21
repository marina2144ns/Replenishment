package ru.stockmann.replenishment.services.cdecom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import ru.stockmann.replenishment.services.CDEcomBulkLoader;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringJUnitConfig(classes = {
        CDEcomProcessContextTest.TestConfig.class,
        CDEcomExcelLoadDefinition.class,
        CDEcomBulkLoader.class
})
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class CDEcomProcessContextTest {

    @Autowired
    private CDEcomBulkLoader cdecomBulkLoader;

    @Autowired
    private CDEcomExcelLoadDefinition cdecomExcelLoadDefinition;

    @Test
    void cdecomCommonLoadBeansAreCreated() {
        assertNotNull(cdecomBulkLoader);
        assertNotNull(cdecomExcelLoadDefinition);
    }

    @Test
    void cdecomBulkLoaderIsWiredWithDefinitionBean() {
        assertSame(cdecomExcelLoadDefinition, cdecomBulkLoader.getDefinition());
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
