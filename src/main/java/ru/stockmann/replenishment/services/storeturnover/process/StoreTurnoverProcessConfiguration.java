package ru.stockmann.replenishment.services.storeturnover.process;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class StoreTurnoverProcessConfiguration {
    static final int CHUNK_SIZE=1_000;
    @Bean StoreTurnoverLoadSessionRepository storeTurnoverLoadSessionRepository(DataSource ds){return new StoreTurnoverLoadSessionRepository(ds);}
    @Bean StoreTurnoverRawRepository storeTurnoverRawRepository(DataSource ds){return new StoreTurnoverRawRepository(ds,CHUNK_SIZE);}
    @Bean StoreTurnoverStageRepository storeTurnoverStageRepository(){return new StoreTurnoverStageRepository();}
    @Bean StoreTurnoverErrorRepository storeTurnoverErrorRepository(DataSource ds){return new StoreTurnoverErrorRepository(ds);}
    @Bean StoreTurnoverTargetRepository storeTurnoverTargetRepository(){return new StoreTurnoverTargetRepository();}
    @Bean StoreTurnoverValidator storeTurnoverValidator(){return new StoreTurnoverValidator();}
    @Bean StoreTurnoverDeletionRepository storeTurnoverDeletionRepository(){return new StoreTurnoverDeletionRepository();}
    @Bean StoreTurnoverDeletionService storeTurnoverDeletionService(DataSource ds,StoreTurnoverDeletionRepository r){return new StoreTurnoverDeletionService(ds,r);}
    @Bean StoreTurnoverProcessor storeTurnoverProcessor(DataSource ds,StoreTurnoverLoadSessionRepository s,StoreTurnoverRawRepository r,StoreTurnoverStageRepository st,StoreTurnoverErrorRepository e,StoreTurnoverTargetRepository t,StoreTurnoverValidator v){return new StoreTurnoverProcessor(ds,s,r,st,e,t,v);}
}
