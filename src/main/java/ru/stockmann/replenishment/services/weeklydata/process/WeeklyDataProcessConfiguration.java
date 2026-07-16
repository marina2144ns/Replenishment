package ru.stockmann.replenishment.services.weeklydata.process;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class WeeklyDataProcessConfiguration {

    @Bean
    public WeeklyDataLoadSessionRepository weeklyDataLoadSessionRepository(DataSource dataSource) {
        return new WeeklyDataLoadSessionRepository(dataSource);
    }

    @Bean
    public WeeklyDataRawRepository weeklyDataRawRepository(DataSource dataSource) {
        return new WeeklyDataRawRepository(dataSource);
    }

    @Bean
    public WeeklyDataErrorRepository weeklyDataErrorRepository(DataSource dataSource) {
        return new WeeklyDataErrorRepository(dataSource);
    }

    @Bean
    public WeeklyDataTargetRepository weeklyDataTargetRepository(DataSource dataSource) {
        return new WeeklyDataTargetRepository(dataSource);
    }

    @Bean
    public WeeklyDataValidator weeklyDataValidator() {
        return new WeeklyDataValidator();
    }

    @Bean
    public WeeklyDataRowMapper weeklyDataRowMapper() {
        return new WeeklyDataRowMapper();
    }

    @Bean
    public WeeklyDataProcessor weeklyDataProcessor(
            DataSource dataSource,
            WeeklyDataLoadSessionRepository loadSessionRepository,
            WeeklyDataRawRepository rawRepository,
            WeeklyDataErrorRepository errorRepository,
            WeeklyDataTargetRepository targetRepository,
            WeeklyDataValidator validator,
            WeeklyDataRowMapper mapper
    ) {
        return new WeeklyDataProcessor(
                dataSource,
                loadSessionRepository,
                rawRepository,
                errorRepository,
                targetRepository,
                validator,
                mapper
        );
    }
}
