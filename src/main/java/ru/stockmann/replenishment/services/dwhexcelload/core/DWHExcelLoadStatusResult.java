package ru.stockmann.replenishment.services.dwhexcelload.core;

public record DWHExcelLoadStatusResult(
        Long loadSessionId,
        String loadTypeCode,
        String serviceName,
        String fileName,
        String filePath,
        String operationType,
        String operationMode,
        Integer deleteYear,
        Integer deleteWeek,
        Integer deleteMonth,
        String deleteYearText,
        String deleteMonthText,
        Long sourceLoadSessionId,
        String deleteCriterion,
        String deleteParameter1Name,
        String deleteParameter1Value,
        String deleteParameter2Name,
        String deleteParameter2Value,
        Long deletedRows,
        String status,
        String message,
        String startedAt,
        String finishedAt
) {
}
