package ru.stockmann.replenishment.services.dwhexcelload.core;

public record DWHDeletionSession(
        DWHExcelLoadType loadType,
        DWHDeletionOperationMode operationMode,
        Integer deleteYear,
        Integer deleteWeek,
        Long sourceLoadSessionId
) {

    public static DWHDeletionSession byPeriod(
            DWHExcelLoadType loadType,
            int year,
            int week
    ) {
        return new DWHDeletionSession(loadType, DWHDeletionOperationMode.BY_PERIOD,
                year, week, null);
    }

    public static DWHDeletionSession byLoadSession(
            DWHExcelLoadType loadType,
            long sourceLoadSessionId
    ) {
        return new DWHDeletionSession(loadType, DWHDeletionOperationMode.BY_LOAD_SESSION,
                null, null, sourceLoadSessionId);
    }
}
