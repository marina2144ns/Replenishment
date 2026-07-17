package ru.stockmann.replenishment.services.cddata.process;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class CDDataProcessConfiguration {

    @Bean
    public CDDataLoadSessionRepository cdDataLoadSessionRepository(DataSource dataSource) {
        return new CDDataLoadSessionRepository(dataSource);
    }

    @Bean
    public CDDataRawRepository cdDataRawRepository(DataSource dataSource) {
        return new CDDataRawRepository(dataSource);
    }

    @Bean
    public CDDataTargetRepository cdDataTargetRepository(DataSource dataSource) {
        return new CDDataTargetRepository(dataSource);
    }

    @Bean
    public CDDataErrorRepository cdDataErrorRepository(DataSource dataSource) {
        return new CDDataErrorRepository(dataSource);
    }

    @Bean
    public CDDataValidator cdDataValidator() {
        return new CDDataValidator();
    }

    @Bean
    public CDDataRowMapper cdDataRowMapper() {
        return new CDDataRowMapper();
    }

    @Bean
    public CDDataProcessor cdDataProcessor(
            DataSource dataSource,
            CDDataLoadSessionRepository loadSessionRepository,
            CDDataRawRepository rawRepository,
            CDDataTargetRepository targetRepository,
            CDDataErrorRepository errorRepository,
            CDDataValidator validator,
            CDDataRowMapper mapper
    ) {
        return new CDDataProcessor(
                dataSource,
                loadSessionRepository,
                rawRepository,
                targetRepository,
                errorRepository,
                validator,
                mapper
        );
    }
}
