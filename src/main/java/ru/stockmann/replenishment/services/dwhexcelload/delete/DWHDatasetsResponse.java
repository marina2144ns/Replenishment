package ru.stockmann.replenishment.services.dwhexcelload.delete;

import java.util.List;

public record DWHDatasetsResponse(List<DWHDatasetSummary> datasets) {
}
