package ru.stockmann.replenishment.services.dwhexcelload.core;

import java.util.List;

public interface DWHExcelLoadDefinition {

    String loadCode();

    String serviceName();

    String rawTableName();

    String targetTableName();

    String loadSessionTableName();

    String loadErrorTableName();

    String processProcedureName();

    int expectedColumnCount();

    default int batchSize() {
        return 20_000;
    }

    List<DWHExcelColumnSpec> columns();
}