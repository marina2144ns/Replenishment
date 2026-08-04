package ru.stockmann.replenishment.services.cddata.process;

import java.math.BigDecimal;
import java.sql.Date;

public record CDDataStageRow(
        long loadSessionId,
        Long excelRowNum,
        String nazvanie,
        Integer god,
        Integer sezon,
        Integer den,
        Date data,
        String salesChannel,
        String storeRus,
        String mfpDivision,
        String mfpDepartment,
        String mfpSubDepartment,
        String skuBrandType,
        String skuTm,
        String mfpNode,
        String section,
        String merchandiseSubGroup,
        String campaignSales,
        Long skuStyleColor,
        String skuPhase,
        BigDecimal stockStartPcs,
        BigDecimal stockStartDd,
        BigDecimal salesPcs,
        BigDecimal salesRub,
        BigDecimal revenue,
        BigDecimal gp,
        BigDecimal cogs,
        BigDecimal salesFrpPrice,
        BigDecimal salesDiscount,
        BigDecimal stockStoresPcs,
        BigDecimal stockStoresDd,
        Integer planRub,
        String draiveryCd,
        String skuColorRus,
        String skuComposition,
        String skuSupplier,
        String skuName,
        String skuCollection,
        String skuComment,
        Long rawRowId
) {
}
