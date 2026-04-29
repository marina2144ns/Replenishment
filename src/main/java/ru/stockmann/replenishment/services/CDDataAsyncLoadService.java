package ru.stockmann.replenishment.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CDDataAsyncLoadService {

    private static final Logger log = LoggerFactory.getLogger(CDDataAsyncLoadService.class);

    private final CDDataBulkLoader bulkLoader;

    public CDDataAsyncLoadService(CDDataBulkLoader bulkLoader) {
        this.bulkLoader = bulkLoader;
    }

    @Async("cdDataLoadExecutor")
    public void startAsync(Long loadSessionId, String filePath) {
        log.info(
                "CDData async load started. loadSessionId={}, filePath={}",
                loadSessionId,
                filePath
        );

        try {
            bulkLoader.processAcceptedFile(loadSessionId, filePath);

            log.info(
                    "CDData async load finished. loadSessionId={}",
                    loadSessionId
            );

        } catch (Exception e) {
            log.error(
                    "CDData async load failed. loadSessionId={}, filePath={}",
                    loadSessionId,
                    filePath,
                    e
            );
        }
    }
}