package ru.stockmann.replenishment.services.salesbychannel.process;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class SalesByChannelProcessConfiguration {

    public static final int DEFAULT_CHUNK_SIZE = 1_000;

    @Bean
    public SalesByChannelLoadSessionRepository salesByChannelLoadSessionRepository(DataSource dataSource) {
        return new SalesByChannelLoadSessionRepository(dataSource);
    }

    @Bean
    public SalesByChannelRawRepository salesByChannelRawRepository(DataSource dataSource) {
        return new SalesByChannelRawRepository(dataSource, DEFAULT_CHUNK_SIZE);
    }

    @Bean
    public SalesByChannelStageRepository salesByChannelStageRepository() {
        return new SalesByChannelStageRepository();
    }

    @Bean
    public SalesByChannelErrorRepository salesByChannelErrorRepository(DataSource dataSource) {
        return new SalesByChannelErrorRepository(dataSource);
    }

    @Bean
    public SalesByChannelTargetRepository salesByChannelTargetRepository() {
        return new SalesByChannelTargetRepository();
    }

    @Bean
    public SalesByChannelValidator salesByChannelValidator() {
        return new SalesByChannelValidator();
    }

    @Bean
    public SalesByChannelProcessor salesByChannelProcessor(
            DataSource dataSource,
            SalesByChannelLoadSessionRepository sessionRepository,
            SalesByChannelRawRepository rawRepository,
            SalesByChannelStageRepository stageRepository,
            SalesByChannelErrorRepository errorRepository,
            SalesByChannelTargetRepository targetRepository,
            SalesByChannelValidator validator
    ) {
        return new SalesByChannelProcessor(
                dataSource, sessionRepository, rawRepository, stageRepository,
                errorRepository, targetRepository, validator, DEFAULT_CHUNK_SIZE
        );
    }
}
