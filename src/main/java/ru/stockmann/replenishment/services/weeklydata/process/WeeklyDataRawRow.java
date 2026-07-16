package ru.stockmann.replenishment.services.weeklydata.process;

public record WeeklyDataRawRow(
        long loadSessionId,
        long rawId,
        Long excelRowNum,
        String year21,
        String week21,
        String yearCorr,
        String weekCorr,
        String year,
        String week,
        String salesChannelBpo,
        String storeRusBpo,
        String storeRus,
        String mfpDivisionNew,
        String mfpDepartment,
        String skuSeasonBudget,
        String typeOfSales,
        String totalStockPcs,
        String totalStockDdp,
        String salesPcs,
        String salesRub,
        String revenue,
        String gp,
        String discountTotalRub,
        String mfpDivision,
        String season,
        String month,
        String bundle,
        String seasonality
) {
}
