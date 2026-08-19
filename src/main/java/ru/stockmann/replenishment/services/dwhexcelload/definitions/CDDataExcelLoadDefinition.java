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
                DWHExcelColumns.text(
                        0, "название", "nazvanie", "nazvanie", RAW_TEXT_LENGTH,
                        true, DWHExcelNullHandling.KEEP_NULL
                ),

                DWHExcelColumns.intNumber(
                        1, "ГОД", "god", "god", 50, true, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        2, "Сезон", "sezon", "sezon", 50, true, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        3, "день", "den", "den", 50, true, DWHExcelNullHandling.KEEP_NULL
                ),

                DWHExcelColumns.date(
                        4, "дата", "data", "data", 50,
                        false, DWHExcelNullHandling.KEEP_NULL
                ),

                DWHExcelColumns.text(5, "Sales Channel_BPO", "sales_channel", "sales_channel",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(6, "StoreRUS", "store_rus", "store_rus",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(7, "MFP Division", "mfp_division", "mfp_division",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(8, "MFP Department", "mfp_department", "mfp_department",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(9, "MFP SubDepartment", "mfp_sub_department", "mfp_sub_department",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(10, "SKU Brand type", "sku_brand_type", "sku_brand_type",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(11, "SKU TM", "sku_tm", "sku_tm",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(12, "MFP Node", "mfp_node", "mfp_node",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(13, "Section", "section", "section",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(14, "MerchandiseSubGroup", "merchandise_sub_group",
                        "merchandise_sub_group", RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(15, "Campaign Sales Type", "campaign_sales", "campaign_sales",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),

                DWHExcelColumns.intNumber(16, "SKU StyleColor", "sku_style_color", "sku_style_color",
                        50, false, DWHExcelNullHandling.KEEP_NULL),

                DWHExcelColumns.text(17, "SKU Phase", "sku_phase", "sku_phase",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),

                DWHExcelColumns.decimal(18, "Stock Start, pcs", "stock_start_pcs", "stock_start_pcs",
                        50, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(19, "Stock Start, DDP", "stock_start_dd", "stock_start_dd",
                        50, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(20, "Sales, Pcs", "sales_pcs", "sales_pcs",
                        50, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(21, "Sales, rub", "sales_rub", "sales_rub",
                        50, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(22, "Revenue", "revenue", "revenue",
                        50, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(23, "GP", "gp", "gp",
                        50, false, DWHExcelNullHandling.ZERO),

                DWHExcelColumns.decimalFloatValidation(
                        24, "Cogs", "cogs", "cogs", 50,
                        false, DWHExcelNullHandling.ZERO
                ),

                DWHExcelColumns.decimal(25, "Sales FRP Price, rub", "sales_frp_price", "sales_frp_price",
                        50, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(26, "Sales Discount", "sales_discount", "sales_discount",
                        50, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(27, "Stock Stores, Pcs", "stock_stores_pcs", "stock_stores_pcs",
                        50, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(28, "Stock Stores, DDP", "stock_stores_dd", "stock_stores_dd",
                        50, false, DWHExcelNullHandling.ZERO),

                DWHExcelColumns.intNumber(29, "Plan, rub", "plan_rub", "plan_rub",
                        50, false, DWHExcelNullHandling.ZERO),

                DWHExcelColumns.text(30, "Драйверы CD", "draivery_cd", "draivery_cd",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(31, "SKU Color Russian", "sku_color_rus", "sku_color_rus",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(32, "SKU Composition", "sku_composition", "sku_composition",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(33, "SKU Supplier model", "sku_supplier", "sku_supplier",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(34, "SKU Name", "sku_name", "sku_name",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(35, "SKU Collection", "sku_collection", "sku_collection",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(36, "SKU Comment (buyer)", "sku_comment", "sku_comment",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL)
        );
    }


}
