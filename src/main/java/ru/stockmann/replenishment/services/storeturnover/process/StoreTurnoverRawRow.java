package ru.stockmann.replenishment.services.storeturnover.process;

public record StoreTurnoverRawRow(
        Long id, Long loadSessionId, Long excelRowNum,
        String sku, String period, String storeRus,
        String remainingSum, String remainingDays, String salesQuantity, String sales,
        String asp, String revenue, String gp, String discountTotal
) {}
