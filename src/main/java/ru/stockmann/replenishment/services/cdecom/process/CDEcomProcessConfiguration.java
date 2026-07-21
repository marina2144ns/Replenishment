package ru.stockmann.replenishment.services.cdecom.process;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class CDEcomProcessConfiguration {

    @Bean
    public CDEcomLoadSessionRepository cdecomLoadSessionRepository(DataSource dataSource) {
        return new CDEcomLoadSessionRepository(dataSource);
    }

    @Bean
    public CDEcomRawRepository cdecomRawRepository(DataSource dataSource) {
        return new CDEcomRawRepository(dataSource);
    }

    @Bean
    public CDEcomTargetRepository cdecomTargetRepository(DataSource dataSource) {
        return new CDEcomTargetRepository(dataSource);
    }

    @Bean
    public CDEcomErrorRepository cdecomErrorRepository(DataSource dataSource) {
        return new CDEcomErrorRepository(dataSource);
    }

    @Bean
    public CDEcomValidator cdecomValidator() {
        return new CDEcomValidator();
    }

    @Bean
    public CDEcomRowMapper cdecomRowMapper() {
        return new CDEcomRowMapper();
    }

    @Bean
    public CDEcomProcessor cdecomProcessor(
            DataSource dataSource,
            CDEcomLoadSessionRepository loadSessionRepository,
            CDEcomRawRepository rawRepository,
            CDEcomTargetRepository targetRepository,
            CDEcomErrorRepository errorRepository,
            CDEcomValidator validator,
            CDEcomRowMapper mapper
    ) {
        return new CDEcomProcessor(
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
