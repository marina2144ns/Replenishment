package ru.stockmann.replenishment.services;

import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.AbstractDWHExcelLoader;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;

import javax.sql.DataSource;

@Service
public class CDEcomBulkLoader extends AbstractDWHExcelLoader {

    public CDEcomBulkLoader(DataSource dataSource, CDEcomExcelLoadDefinition definition) {
        super(dataSource, definition);
    }
}
