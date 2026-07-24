package ru.stockmann.replenishment.services.salesbychannel;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.SalesByChannelExcelLoadDefinition;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

@Service
public class SalesByChannelBulkLoader extends AbstractDWHExcelLoader {

    public SalesByChannelBulkLoader(
            DataSource dataSource,
            SalesByChannelExcelLoadDefinition definition
    ) {
        super(dataSource, definition);
    }

    @Override
    protected void validateHeaderRow(String[] headerValues) {
        List<DWHExcelColumnSpec> columns = definition.columns();
        for (int index = 0; index < columns.size(); index++) {
            String expected = columns.get(index).excelColumnName();
            String actual = headerValues[index];
            if (!Objects.equals(expected, actual)) {
                throw new IllegalArgumentException(
                        "Invalid SalesByChannel header at column " + (index + 1)
                                + ": expected [" + expected + "], actual [" + actual + "]"
                );
            }
        }
    }

    @Override
    protected DWHExcelLoadSessionResult processLoadSession(Long loadSessionId) {
        return new DWHExcelLoadSessionResult(
                loadSessionId,
                true,
                "SalesByChannel raw load completed"
        );
    }
}
