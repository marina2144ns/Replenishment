package ru.stockmann.replenishment.services.dwhexcelload.core;

public record DWHDeletionSession(
        DWHExcelLoadType loadType,
        DWHDeletionOperationMode operationMode,
        Integer deleteYear,
        Integer deleteWeek,
        Long sourceLoadSessionId,
        String deleteCriterion,
        String deleteParameter1Name,
        String deleteParameter1Value,
        String deleteParameter2Name,
        String deleteParameter2Value
) {

    public static DWHDeletionSession byPeriod(
            DWHExcelLoadType loadType,
            int year,
            int week
    ) {
        return new DWHDeletionSession(loadType, DWHDeletionOperationMode.BY_PERIOD,
                year, week, null, null, null, null, null, null);
    }

    public static DWHDeletionSession byLoadSession(
            DWHExcelLoadType loadType,
            long sourceLoadSessionId
    ) {
        return new DWHDeletionSession(loadType, DWHDeletionOperationMode.BY_LOAD_SESSION,
                null, null, sourceLoadSessionId, null, null, null, null, null);
    }

    public static DWHDeletionSession byCriteria(
            DWHExcelLoadType loadType,
            String criterion,
            String parameter1Name,
            String parameter1Value,
            String parameter2Name,
            String parameter2Value
    ) {
        return new DWHDeletionSession(loadType, DWHDeletionOperationMode.BY_CRITERIA,
                null, null, null, criterion,
                parameter1Name, parameter1Value, parameter2Name, parameter2Value);
    }
}
