package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;
import ru.stockmann.replenishment.services.dwhexcelload.normalizers.DWHExcelNormalizers;

import java.util.List;

public class CDDataExcelLoadDefinition implements DWHExcelLoadDefinition {

    @Override
    public String loadCode() {
        return "CD_DATA";
    }

    @Override
    public String serviceName() {
        return "CD data";
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
    public String loadSessionTableName() {
        return "dbo.CD_data_Load_session";
    }

    @Override
    public String loadErrorTableName() {
        return "dbo.CD_data_Load_error";
    }

    @Override
    public String processProcedureName() {
        return "dbo.usp_CDData_ProcessLoadSession";
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
                text255(0,  "nazvanie"),
                int50(1,   "god"),
                int50(2,   "sezon"),
                int50(3,   "den"),
                date50(4,  "data"),

                text255(5,  "sales_channel"),
                text255(6,  "store_rus"),
                text255(7,  "mfp_division"),
                text255(8,  "mfp_department"),
                text255(9,  "mfp_sub_department"),
                text255(10, "sku_brand_type"),
                text255(11, "sku_tm"),
                text255(12, "mfp_node"),
                text255(13, "section"),
                text255(14, "merchandise_sub_group"),
                text255(15, "campaign_sales"),

                int50(16, "sku_style_color"),
                text255(17, "sku_phase"),

                decimal50(18, "stock_start_pcs"),
                decimal50(19, "stock_start_dd"),
                decimal50(20, "sales_pcs"),
                decimal50(21, "sales_rub"),
                decimal50(22, "revenue"),
                decimal50(23, "gp"),
                decimal50FloatValidation50(24, "cogs"),
                decimal50(25, "sales_frp_price"),
                decimal50(26, "sales_discount"),
                decimal50(27, "stock_stores_pcs"),
                decimal50(28, "stock_stores_dd"),

                int50(29, "plan_rub"),

                text255(30, "draivery_cd"),
                text255(31, "sku_color_rus"),
                text255(32, "sku_composition"),
                text255(33, "sku_supplier"),
                text255(34, "sku_name"),
                text255(35, "sku_collection"),
                text255(36, "sku_comment")
        );
    }

    private static DWHExcelColumnSpec text255(int index, String name) {
        return new DWHExcelColumnSpec(
                index,
                name,
                name,
                name,
                DWHExcelValueKind.TEXT,
                255,
                false,
                DWHExcelNormalizers.TEXT,
                "trim + empty->null",
                "NULLIF(LTRIM(RTRIM(...)), '') + LEN<=255"
        );
    }

    private static DWHExcelColumnSpec int50(int index, String name) {
        return new DWHExcelColumnSpec(
                index,
                name,
                name,
                name,
                DWHExcelValueKind.INT,
                50,
                false,
                DWHExcelNormalizers.NUMERIC_TEXT,
                "trim numeric text + remove NBSP/narrow NBSP",
                "TRY_CONVERT(INT)"
        );
    }

    private static DWHExcelColumnSpec decimal50(int index, String name) {
        return new DWHExcelColumnSpec(
                index,
                name,
                name,
                name,
                DWHExcelValueKind.DECIMAL,
                50,
                false,
                DWHExcelNormalizers.NUMERIC_TEXT,
                "trim numeric text + remove NBSP/narrow NBSP",
                "REPLACE(','->'.') + TRY_CONVERT(DECIMAL(18,2))"
        );
    }

    /**
     * В текущей proc поле cogs валидируется через TRY_CONVERT(FLOAT),
     * хотя в target оно DECIMAL(18,2). Оставляю это в note как есть,
     * чтобы definition пока отражал текущую реальность.
     */
    private static DWHExcelColumnSpec decimal50FloatValidation50(int index, String name) {
        return new DWHExcelColumnSpec(
                index,
                name,
                name,
                name,
                DWHExcelValueKind.DECIMAL,
                50,
                false,
                DWHExcelNormalizers.NUMERIC_TEXT,
                "trim numeric text + remove NBSP/narrow NBSP",
                "REPLACE(','->'.') + TRY_CONVERT(FLOAT) in validation; target DECIMAL(18,2)"
        );
    }

    private static DWHExcelColumnSpec date50(int index, String name) {
        return new DWHExcelColumnSpec(
                index,
                name,
                name,
                name,
                DWHExcelValueKind.DATE,
                50,
                false,
                DWHExcelNormalizers.DATE_TO_ISO,
                "trim + normalize to yyyy-MM-dd if possible",
                "TRY_CONVERT(DATE, current proc uses style 1)"
        );
    }
}