package ru.stockmann.replenishment.services.dwhexcelload.delete;

import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;

public record DWHDatasetField(
        String name,
        String displayName,
        DWHExcelValueKind type
) {
}
