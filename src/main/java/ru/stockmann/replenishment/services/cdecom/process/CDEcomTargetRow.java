package ru.stockmann.replenishment.services.cdecom.process;

import java.math.BigDecimal;
import java.sql.Date;

public record CDEcomTargetRow(
        Long loadSessionId,
        String name,
        Integer year,
        Integer season,
        Integer day,
        Date data,
        String salesChannelBpo,
        String storeRus,
        String mfpDivision,
        String mfpDepartment,
        String mfpSubDepartment,
        String skuBrandType,
        String skuTm,
        String mfpNode,
        String section,
        String merchandiseSubGroup,
        String campaignSalesType,
        Long skuStyleColor,
        String skuPhase,
        BigDecimal orderPcs,
        BigDecimal orderRub,
        BigDecimal foundPcs,
        BigDecimal foundRub,
        BigDecimal salesPcs,
        BigDecimal salesRub,
        BigDecimal revenue,
        BigDecimal gp,
        BigDecimal cogs,
        BigDecimal salesDiscount,
        Long planRub,
        Long stockStoresPcs,
        Long stockStoresDdp,
        String cdDrivers,
        String skuSupplierModel,
        String skuComposition,
        String skuColorRussian,
        String skuName,
        String skuCommentBuyer,
        String skuCollection,
        Long rawRowId
) {
}
