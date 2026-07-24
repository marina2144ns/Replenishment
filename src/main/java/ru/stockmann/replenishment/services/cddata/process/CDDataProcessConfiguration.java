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
    public CDDataTargetRepository cdDataTargetRepository() {
        return new CDDataTargetRepository();
    }

    @Bean
    public CDDataErrorRepository cdDataErrorRepository(DataSource dataSource) {
        return new CDDataErrorRepository(dataSource);
    }

    @Bean
    public CDDataStageRepository cdDataStageRepository() {
        return new CDDataStageRepository();
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
            CDDataErrorRepository errorRepository,
            CDDataStageRepository stageRepository,
            CDDataTargetRepository targetRepository,
            CDDataValidator validator
    ) {
        return new CDDataProcessor(
                dataSource,
                loadSessionRepository,
                rawRepository,
                errorRepository,
                stageRepository,
                targetRepository,
                validator
        );
    }
}
