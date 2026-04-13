package ru.stockmann.replenishment.services.dwhexcelload.core;

import java.util.List;


public interface DWHExcelLoadDefinition {

    DWHExcelLoadType loadType();

    default String loadCode() {
        return loadType().code();
    }

    default String serviceName() {
        return loadType().displayName();
    }

    String rawTableName();

    String targetTableName();

    String processProcedureName();

    int expectedColumnCount();

    default int batchSize() {
        return 20_000;
    }

    List<DWHExcelColumnSpec> columns();
}