package ru.stockmann.replenishment.services.dwhexcelload.delete;

import java.util.Map;

public record DWHUniversalDeleteRequest(
        String dataset,
        String requestedBy,
        Map<String, Object> filters,
        Boolean dryRun
) {

    public boolean isDryRun() {
        return Boolean.TRUE.equals(dryRun);
    }
}
