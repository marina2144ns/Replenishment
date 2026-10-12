package ru.stockmann.replenishment.services.dwhexcelload.delete;

import java.util.List;

public record DWHDatasetFieldsResponse(
        String dataset,
        List<DWHDatasetField> fields
) {
}
