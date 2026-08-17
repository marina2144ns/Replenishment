package ru.stockmann.replenishment.services.storeturnover;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHCsvLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.StoreTurnoverExcelLoadDefinition;
import ru.stockmann.replenishment.services.storeturnover.process.StoreTurnoverProcessResult;
import ru.stockmann.replenishment.services.storeturnover.process.StoreTurnoverProcessor;

import javax.sql.DataSource;

@Service
public class StoreTurnoverBulkLoader extends AbstractDWHCsvLoader {
    private final StoreTurnoverProcessor processor;

    public StoreTurnoverBulkLoader(DataSource dataSource, StoreTurnoverExcelLoadDefinition definition,
                                   StoreTurnoverProcessor processor) {
        super(dataSource, definition, ';');
        this.processor = processor;
    }

    @Override
    protected String applyNullHandling(DWHExcelColumnSpec column, String value) {
        return value;
    }

    @Override
    protected DWHExcelLoadSessionResult processLoadSession(Long loadSessionId) {
        StoreTurnoverProcessResult result = processor.process(loadSessionId);
        return new DWHExcelLoadSessionResult(loadSessionId, result.success(), result.message());
    }
}
