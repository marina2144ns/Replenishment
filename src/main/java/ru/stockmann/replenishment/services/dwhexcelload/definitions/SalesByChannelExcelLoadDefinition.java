package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.springframework.stereotype.Component;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumns;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelNullHandling;

import java.util.List;

@Component
public class SalesByChannelExcelLoadDefinition implements DWHExcelLoadDefinition {

    private static final int RAW_TEXT_LENGTH = 4000;

    @Override
    public DWHExcelLoadType loadType() {
        return DWHExcelLoadType.SALES_BY_CHANNEL;
    }

    @Override
    public String rawTableName() {
        return "dbo.SalesByChannel_raw";
    }

    @Override
    public String targetTableName() {
        return "dbo.SalesByChannel";
    }

    @Override
    public String processProcedureName() {
        throw new UnsupportedOperationException("SalesByChannel processing is not implemented yet");
    }

    @Override
    public int expectedColumnCount() {
        return 29;
    }

    @Override
    public int batchSize() {
        return 10_000;
    }

    @Override
    public List<DWHExcelColumnSpec> columns() {
        return List.of(
                text(0, "Сезон_Year", "seasonYear"),
                text(1, "Сезон (6м)", "season6m"),
                text(2, "Year_month", "yearMonth"),
                text(3, "Year_сезон", "yearSeason"),
                requiredText(4, "Year", "year"),
                requiredText(5, "Month", "month"),
                text(6, "Sales Channel type", "salesChannelType"),
                text(7, "StoreRUS", "storeRus"),
                text(8, "TypeOfSales", "typeOfSales"),
                text(9, "MFP Division", "mfpDivision"),
                text(10, "MFP Department", "mfpDepartment"),
                text(11, "Campaign Sales Type", "campaignSalesType"),
                text(12, "Seasonality", "seasonality"),
                text(13, "SKU Brand type", "skuBrandType"),
                integerMetric(14, "Sum([Sales Quantity])", "salesQuantity"),
                decimalMetric(15, "Sum([Sales Curr])", "salesCurr"),
                decimalMetric(16, "GM", "gm"),
                decimalMetric(17, "Discount TTL", "discountTtl"),
                decimalMetric(18, "Sum([Turnover Curr])", "turnoverCurr"),
                text(19, "SKU SeasonBudget", "skuSeasonBudget"),
                text(20, "StoreRus_BPO", "storeRusBpo"),
                text(21, "Sales Channel_BPO", "salesChannelBpo"),
                text(22, "MFP SubDepartment", "mfpSubDepartment"),
                text(23, "SKU TM", "skuTm"),
                text(24, "MFP Node", "mfpNode"),
                text(25, "Section", "section"),
                text(26, "MerchandiseSubGroup", "merchandiseSubGroup"),
                text(27, "SKU Phase", "skuPhase"),
                text(28, "SKU   Product Class", "skuProductClass")
        );
    }

    private static DWHExcelColumnSpec text(int index, String excelHeader, String rawColumn) {
        return text(index, excelHeader, rawColumn, false);
    }

    private static DWHExcelColumnSpec requiredText(int index, String excelHeader, String rawColumn) {
        return text(index, excelHeader, rawColumn, true);
    }

    private static DWHExcelColumnSpec integerMetric(int index, String excelHeader, String rawColumn) {
        return DWHExcelColumns.intNumber(
                index, excelHeader, rawColumn, rawColumn, RAW_TEXT_LENGTH,
                false, DWHExcelNullHandling.ZERO
        );
    }

    private static DWHExcelColumnSpec decimalMetric(int index, String excelHeader, String rawColumn) {
        return DWHExcelColumns.decimal(
                index, excelHeader, rawColumn, rawColumn, RAW_TEXT_LENGTH,
                false, DWHExcelNullHandling.ZERO
        );
    }

    private static DWHExcelColumnSpec text(
            int index,
            String excelHeader,
            String rawColumn,
            boolean required
    ) {
        return DWHExcelColumns.text(
                index,
                excelHeader,
                rawColumn,
                rawColumn,
                RAW_TEXT_LENGTH,
                required,
                DWHExcelNullHandling.KEEP_NULL
        );
    }
}
