package ru.stockmann.replenishment.services.dwhexcelload.core;

public class DWHExcelLoadSessionNotFoundException extends RuntimeException {

    public DWHExcelLoadSessionNotFoundException(Long loadSessionId) {
        super("Load session not found: " + loadSessionId);
    }
}
