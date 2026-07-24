package ru.stockmann.replenishment.services.salesbychannel.process;

import java.math.BigDecimal;

public record SalesByChannelStageRow(
        Long loadSessionId,
        Long excelRowNum,
        String seasonYear,
        String season6m,
        String yearMonth,
        String yearSeason,
        String year,
        String month,
        String salesChannelType,
        String storeRus,
        String typeOfSales,
        String mfpDivision,
        String mfpDepartment,
        String campaignSalesType,
        String seasonality,
        String skuBrandType,
        Integer salesQuantity,
        BigDecimal salesCurr,
        BigDecimal gm,
        BigDecimal discountTtl,
        BigDecimal turnoverCurr,
        String skuSeasonBudget,
        String storeRusBpo,
        String salesChannelBpo,
        String mfpSubDepartment,
        String skuTm,
        String mfpNode,
        String section,
        String merchandiseSubGroup,
        String skuPhase,
        String skuProductClass
) {
}
