package ru.stockmann.replenishment.services.dwhexcelload.core;

public record DWHExcelLoadStatusResult(
        Long loadSessionId,
        String loadTypeCode,
        String serviceName,
        String fileName,
        String filePath,
        String status,
        String message,
        String startedAt,
        String finishedAt
) {
}