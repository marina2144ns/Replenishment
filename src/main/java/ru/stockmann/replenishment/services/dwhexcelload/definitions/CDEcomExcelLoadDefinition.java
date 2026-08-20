package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.apache.poi.ss.usermodel.DateUtil;
import org.springframework.stereotype.Component;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumns;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelNullHandling;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;
import ru.stockmann.replenishment.services.dwhexcelload.normalizers.DWHExcelNormalizers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class CDEcomExcelLoadDefinition implements DWHExcelLoadDefinition {

    private static final DateTimeFormatter RAW_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int RAW_TEXT_LENGTH = 4000;

    @Override
    public DWHExcelLoadType loadType() {
        return DWHExcelLoadType.CD_ECOM;
    }

    @Override
    public String rawTableName() {
        return "dbo.CD_ecom_raw";
    }

    @Override
    public String targetTableName() {
        return "dbo.CD_ecom";
    }

    @Override
    public String processProcedureName() {
        throw new UnsupportedOperationException("CDEcom processing is implemented in Java");
    }

    @Override
    public int expectedColumnCount() {
        return 38;
    }

    @Override
    public int batchSize() {
        return 10_000;
    }

    @Override
    public List<DWHExcelColumnSpec> columns() {
        return List.of(
                DWHExcelColumns.text(
                        0, "название", "name", "name", RAW_TEXT_LENGTH,
                        true, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        1, "ГОД", "year", "year", 50, true, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        2, "Сезон", "season", "season", 50, true, DWHExcelNullHandling.KEEP_NULL
                ),
                DWHExcelColumns.intNumber(
                        3, "день", "day", "day", 50, true, DWHExcelNullHandling.KEEP_NULL
                ),
                date(4, "дата", "data", "data", 50),
                DWHExcelColumns.text(5, "Sales Channel_BPO", "salesChannelBpo", "salesChannelBpo",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(6, "StoreRUS", "storeRus", "storeRus",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(7, "MFP Division", "mfpDivision", "mfpDivision",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(8, "MFP Department", "mfpDepartment", "mfpDepartment",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(9, "MFP SubDepartment", "mfpSubDepartment", "mfpSubDepartment",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(10, "SKU Brand type", "skuBrandType", "skuBrandType",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(11, "SKU TM", "skuTm", "skuTm",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(12, "MFP Node", "mfpNode", "mfpNode",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(13, "Section", "section", "section",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(14, "Merchandise SubGroup", "merchandiseSubGroup", "merchandiseSubGroup",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(15, "Campaign Sales Type", "campaignSalesType", "campaignSalesType",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(16, "SKU StyleColor", "skuStyleColor", "skuStyleColor",
                        100, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(17, "SKU Phase", "skuPhase", "skuPhase",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(18, "Заказ, шт", "orderPcs", "orderPcs",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(19, "Заказ, руб", "orderRub", "orderRub",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(20, "Найдено,шт", "foundPcs", "foundPcs",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(21, "Найдено,руб", "foundRub", "foundRub",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(22, "Sales, Pcs", "salesPcs", "salesPcs",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(23, "Sales, rub", "salesRub", "salesRub",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(24, "Revenue", "revenue", "revenue",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(25, "GP", "gp", "gp",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(26, "Cogs", "cogs", "cogs",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.decimal(27, "Sales Discount", "salesDiscount", "salesDiscount",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.intNumber(28, "Plan, rub", "planRub", "planRub",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.intNumber(29, "Stock Stores, Pcs", "stockStoresPcs", "stockStoresPcs",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.intNumber(30, "Stock Stores, DDP", "stockStoresDdp", "stockStoresDdp",
                        100, false, DWHExcelNullHandling.ZERO),
                DWHExcelColumns.text(31, "Драйверы CD", "cdDrivers", "cdDrivers",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(32, "SKU Supplier model", "skuSupplierModel", "skuSupplierModel",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(33, "SKU Composition", "skuComposition", "skuComposition",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(34, "SKU Color Russian", "skuColorRussian", "skuColorRussian",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(35, "SKU Name", "skuName", "skuName",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(36, "SKU Comment (buyer)", "skuCommentBuyer", "skuCommentBuyer",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(37, "SKU Collection", "skuCollection", "skuCollection",
                        RAW_TEXT_LENGTH, false, DWHExcelNullHandling.KEEP_NULL)
        );
    }

    private static DWHExcelColumnSpec date(
            int index,
            String excelColumnName,
            String rawColumnName,
            String targetColumnName,
            int rawLength
    ) {
        return new DWHExcelColumnSpec(
                index,
                excelColumnName,
                rawColumnName,
                targetColumnName,
                DWHExcelValueKind.DATE,
                rawLength,
                false,
                CDEcomExcelLoadDefinition::normalizeDateForRaw,
                DWHExcelNullHandling.KEEP_NULL,
                "trim + normalize displayed date to dd.MM.yyyy if possible",
                "TRY_CONVERT(DATE, 104/103/23/1/101)"
        );
    }

    private static String normalizeDateForRaw(String value) {
        String v = DWHExcelNormalizers.TRIM_TO_NULL.normalize(value);
        if (v == null) {
            return null;
        }

        LocalDate parsedDate = tryParseDisplayedDate(v);
        if (parsedDate != null) {
            return parsedDate.format(RAW_DATE_FORMAT);
        }

        if (isNumeric(v)) {
            try {
                double serial = Double.parseDouble(v.replace(',', '.'));
                if (DateUtil.isValidExcelDate(serial)) {
                    return DateUtil.getLocalDateTime(serial)
                            .toLocalDate()
                            .format(RAW_DATE_FORMAT);
                }
            } catch (Exception ignored) {
            }
        }

        return v;
    }

    private static LocalDate tryParseDisplayedDate(String value) {
        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("d.M.yyyy"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                DateTimeFormatter.ofPattern("d.M.yy"),
                DateTimeFormatter.ofPattern("dd.MM.yy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yy"),
                DateTimeFormatter.ofPattern("dd/MM/yy"),
                DateTimeFormatter.ofPattern("M/d/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("M/d/yy"),
                DateTimeFormatter.ofPattern("MM/dd/yy"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    private static boolean isNumeric(String value) {
        return value.matches("[-+]?\\d+(?:[\\.,]\\d+)?");
    }
}
