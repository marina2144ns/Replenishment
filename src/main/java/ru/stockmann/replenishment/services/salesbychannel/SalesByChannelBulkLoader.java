package ru.stockmann.replenishment.services.salesbychannel;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.SalesByChannelExcelLoadDefinition;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelProcessResult;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelProcessor;

import javax.sql.DataSource;

@Service
public class SalesByChannelBulkLoader extends AbstractDWHExcelLoader {

    private final SalesByChannelProcessor processor;

    public SalesByChannelBulkLoader(
            DataSource dataSource,
            SalesByChannelExcelLoadDefinition definition,
            SalesByChannelProcessor processor
    ) {
        super(dataSource, definition);
        this.processor = processor;
    }

    @Override
    protected String applyNullHandling(DWHExcelColumnSpec column, String value) {
        // SalesByChannel RAW preserves source absence; ZERO is applied in typed validation.
        return value;
    }

    @Override
    protected DWHExcelLoadSessionResult processLoadSession(Long loadSessionId) {
        SalesByChannelProcessResult result = processor.process(loadSessionId);
        return new DWHExcelLoadSessionResult(
                loadSessionId,
                result.success(),
                result.message()
        );
    }
}
