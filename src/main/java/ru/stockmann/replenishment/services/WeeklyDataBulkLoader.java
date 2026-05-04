package ru.stockmann.replenishment.services;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.WeeklyDataExcelLoadDefinition;

import javax.sql.DataSource;

@Service
public class WeeklyDataBulkLoader extends AbstractDWHExcelLoader {

    public WeeklyDataBulkLoader(DataSource dataSource) {
        super(dataSource, new WeeklyDataExcelLoadDefinition());
    }
}