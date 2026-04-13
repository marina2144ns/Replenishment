package ru.stockmann.replenishment.services;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDDataExcelLoadDefinition;

import javax.sql.DataSource;

@Service
public class CDDataBulkLoader extends AbstractDWHExcelLoader {

    public CDDataBulkLoader(DataSource dataSource) {
        super(dataSource, new CDDataExcelLoadDefinition());
    }
}