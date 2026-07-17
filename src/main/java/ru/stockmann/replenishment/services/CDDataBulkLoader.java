package ru.stockmann.replenishment.services;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.cddata.process.CDDataProcessResult;
import ru.stockmann.replenishment.services.cddata.process.CDDataProcessor;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDDataExcelLoadDefinition;

import javax.sql.DataSource;

@Service
public class CDDataBulkLoader extends AbstractDWHExcelLoader {

    private final CDDataProcessor cdDataProcessor;

    public CDDataBulkLoader(DataSource dataSource, CDDataProcessor cdDataProcessor) {
        super(dataSource, new CDDataExcelLoadDefinition());
        this.cdDataProcessor = cdDataProcessor;
    }

    @Override
    protected DWHExcelLoadSessionResult processLoadSession(Long loadSessionId) {
        CDDataProcessResult result = cdDataProcessor.process(loadSessionId);
        return new DWHExcelLoadSessionResult(
                loadSessionId,
                result.success(),
                result.message()
        );
    }
}
