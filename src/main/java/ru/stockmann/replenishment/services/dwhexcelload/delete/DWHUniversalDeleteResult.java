package ru.stockmann.replenishment.services.dwhexcelload.delete;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DWHUniversalDeleteResult(
        Long sessionId,
        String dataset,
        Long matchedRows,
        long deletedRows,
        boolean dryRun,
        String status
) {

    public static DWHUniversalDeleteResult dryRun(String dataset, long matchedRows) {
        return new DWHUniversalDeleteResult(
                null, dataset, matchedRows, 0, true, "DRY_RUN"
        );
    }

    public static DWHUniversalDeleteResult deleted(
            long sessionId,
            String dataset,
            long deletedRows
    ) {
        return new DWHUniversalDeleteResult(
                sessionId, dataset, null, deletedRows, false, "SUCCESS"
        );
    }
}
