package ru.stockmann.replenishment.services.dwhexcelload.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DWHExcelAsyncLoadService {

    private static final Logger log = LoggerFactory.getLogger(DWHExcelAsyncLoadService.class);

    @Async("dwhExcelLoadExecutor")
    public void startAsync(
            AbstractDWHExcelLoader loader,
            Long loadSessionId,
            String filePath
    ) {
        String loadCode = loader != null && loader.getDefinition() != null
                ? loader.getDefinition().loadCode()
                : "UNKNOWN";

        log.info(
                "DWH Excel async load started. loadCode={}, loadSessionId={}, filePath={}",
                loadCode,
                loadSessionId,
                filePath
        );

        try {
            loader.processAcceptedFile(loadSessionId, filePath);

            log.info(
                    "DWH Excel async load finished. loadCode={}, loadSessionId={}",
                    loadCode,
                    loadSessionId
            );

        } catch (Exception e) {
            log.error(
                    "DWH Excel async load failed. loadCode={}, loadSessionId={}, filePath={}",
                    loadCode,
                    loadSessionId,
                    filePath,
                    e
            );
        }
    }
}