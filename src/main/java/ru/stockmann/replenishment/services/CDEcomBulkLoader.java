package ru.stockmann.replenishment.services;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomProcessResult;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomProcessor;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;

import javax.sql.DataSource;
import java.math.BigDecimal;

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
                result.totalRows(),
                result.loadedRows(),
                result.errorRows(),
                result.message()
        );
    }

    @Override
    protected DataFormatter createDataFormatter() {
        return new DataFormatter() {
            @Override
            public String formatRawCellContents(double value, int formatIndex, String formatString) {
                return rawNumericValue(value);
            }

            @Override
            public String formatRawCellContents(
                    double value,
                    int formatIndex,
                    String formatString,
                    boolean use1904Windowing
            ) {
                return rawNumericValue(value);
            }
        };
    }

    private static String rawNumericValue(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
