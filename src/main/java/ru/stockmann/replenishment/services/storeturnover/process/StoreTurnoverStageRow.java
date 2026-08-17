package ru.stockmann.replenishment.services.storeturnover.process;

import java.time.LocalDate;

public record StoreTurnoverStageRow(
        Long loadSessionId, Long excelRowNum,
        String sku, LocalDate period, String storeRus,
        Integer remainingSum, Integer remainingDays, Integer salesQuantity, Integer sales,
        Integer asp, Integer revenue, Integer gp, Integer discountTotal,
        Long rawRowId
) {}
