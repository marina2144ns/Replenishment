package ru.stockmann.replenishment.services;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionResult;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.WeeklyDataExcelLoadDefinition;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataProcessResult;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataProcessor;

import javax.sql.DataSource;

@Service
public class WeeklyDataBulkLoader extends AbstractDWHExcelLoader {

    private final WeeklyDataProcessor weeklyDataProcessor;

    public WeeklyDataBulkLoader(DataSource dataSource, WeeklyDataProcessor weeklyDataProcessor) {
        super(dataSource, new WeeklyDataExcelLoadDefinition());
        this.weeklyDataProcessor = weeklyDataProcessor;
    }

    @Override
    protected DWHExcelLoadSessionResult processLoadSession(Long loadSessionId) {
        WeeklyDataProcessResult result = weeklyDataProcessor.process(loadSessionId);
        return new DWHExcelLoadSessionResult(
                loadSessionId,
                result.success(),
                result.message()
        );
    }
}
