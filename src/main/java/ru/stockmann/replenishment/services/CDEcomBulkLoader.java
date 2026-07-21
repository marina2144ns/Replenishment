package ru.stockmann.replenishment.services;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomProcessResult;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomProcessor;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;

import javax.sql.DataSource;

@Service
public class CDEcomBulkLoader extends AbstractDWHExcelLoader {

    private final CDEcomProcessor cdecomProcessor;

    public CDEcomBulkLoader(
            DataSource dataSource,
            CDEcomExcelLoadDefinition definition,
            CDEcomProcessor cdecomProcessor
    ) {
        super(dataSource, definition);
        this.cdecomProcessor = cdecomProcessor;
    }

    @Override
    protected DWHExcelLoadSessionResult processLoadSession(Long loadSessionId) {
        CDEcomProcessResult result = cdecomProcessor.process(loadSessionId);
        return new DWHExcelLoadSessionResult(
                loadSessionId,
                result.success(),
                result.message()
        );
    }
}
