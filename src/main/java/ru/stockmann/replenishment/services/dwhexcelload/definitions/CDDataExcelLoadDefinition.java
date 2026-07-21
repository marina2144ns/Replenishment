package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import ru.stockmann.replenishment.services.dwhexcelload.core.*;


import java.util.List;

public class CDDataExcelLoadDefinition implements DWHExcelLoadDefinition {

    private static final int RAW_TEXT_LENGTH = 4000;

    @Override
    public DWHExcelLoadType loadType() {
        return DWHExcelLoadType.CD_DATA;
    }

    @Override
    public String rawTableName() {
        return "dbo.CD_data_raw";
    }

    @Override
    public String targetTableName() {
        return "dbo.CD_data";
    }

    @Override
    public String processProcedureName() {
        throw new UnsupportedOperationException("CDData processing is implemented in Java");
    }

    @Override
    public int expectedColumnCount() {
        return 37;
    }
    @Override
    public int batchSize() {
        return 10_000;
    }

    @Override
    public List<DWHExcelColumnSpec> columns() {
        return List.of(
                DWHExcelColumns.text(0,  "nazvanie", RAW_TEXT_LENGTH),

                DWHExcelColumns.intNumber(1, "god", 50, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(2, "sezon", 50, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(3, "den", 50, DWHExcelNullHandling.KEEP_NULL),

                DWHExcelColumns.date(4, "data", 50),

                DWHExcelColumns.text(5,  "sales_channel", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(6,  "store_rus", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(7,  "mfp_division", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(8,  "mfp_department", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(9,  "mfp_sub_department", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(10, "sku_brand_type", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(11, "sku_tm", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(12, "mfp_node", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(13, "section", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(14, "merchandise_sub_group", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(15, "campaign_sales", RAW_TEXT_LENGTH),

                DWHExcelColumns.intNumber(16, "sku_style_color", 50, DWHExcelNullHandling.KEEP_NULL),

                DWHExcelColumns.text(17, "sku_phase", RAW_TEXT_LENGTH),

                DWHExcelColumns.decimal(18, "stock_start_pcs", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(19, "stock_start_dd", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(20, "sales_pcs", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(21, "sales_rub", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(22, "revenue", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(23, "gp", 50, DWHExcelNullHandling.ZERO),

                DWHExcelColumns.decimalFloatValidation(
                        24,
                        "cogs",
                        50,
                        DWHExcelNullHandling.ZERO
                ),

                DWHExcelColumns.decimal(25, "sales_frp_price", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(26, "sales_discount", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(27, "stock_stores_pcs", 50, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(28, "stock_stores_dd", 50, DWHExcelNullHandling.ZERO),

                DWHExcelColumns.intNumber(29, "plan_rub", 50, DWHExcelNullHandling.KEEP_NULL),

                DWHExcelColumns.text(30, "draivery_cd", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(31, "sku_color_rus", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(32, "sku_composition", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(33, "sku_supplier", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(34, "sku_name", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(35, "sku_collection", RAW_TEXT_LENGTH),
                DWHExcelColumns.text(36, "sku_comment", RAW_TEXT_LENGTH)
        );
    }


}
