package ru.stockmann.replenishment.services.weeklydata.process;

import java.math.BigDecimal;

public record WeeklyDataStageRow(
        long loadSessionId,
        Long excelRowNum,
        Short year21,
        Short week21,
        Short yearCorr,
        Short weekCorr,
        Short year,
        Short week,
        String salesChannelBpo,
        String storeRusBpo,
        String storeRus,
        String mfpDivisionNew,
        String mfpDepartment,
        String skuSeasonBudget,
        String typeOfSales,
        BigDecimal totalStockPcs,
        BigDecimal totalStockDdp,
        BigDecimal salesPcs,
        BigDecimal salesRub,
        BigDecimal revenue,
        BigDecimal gp,
        BigDecimal discountTotalRub,
        String mfpDivision,
        String season,
        String month,
        String bundle,
        String seasonality,
        Long rawRowId
) {
}
