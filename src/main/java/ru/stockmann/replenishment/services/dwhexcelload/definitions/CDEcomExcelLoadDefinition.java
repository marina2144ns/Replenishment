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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class CDEcomExcelLoadDefinition implements DWHExcelLoadDefinition {

    private static final DateTimeFormatter RAW_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

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
        return "dbo.usp_CDEcom_ProcessLoadSession";
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
                DWHExcelColumns.text(0, "name", 255),
                DWHExcelColumns.intNumber(1, "year", 50, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(2, "season", 50, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(3, "day", 50, DWHExcelNullHandling.KEEP_NULL),
                date(4, "data", 50),
                DWHExcelColumns.text(5, "salesChannelBpo", 255),
                DWHExcelColumns.text(6, "storeRus", 255),
                DWHExcelColumns.text(7, "mfpDivision", 255),
                DWHExcelColumns.text(8, "mfpDepartment", 255),
                DWHExcelColumns.text(9, "mfpSubDepartment", 255),
                DWHExcelColumns.text(10, "skuBrandType", 255),
                DWHExcelColumns.text(11, "skuTm", 255),
                DWHExcelColumns.text(12, "mfpNode", 255),
                DWHExcelColumns.text(13, "section", 255),
                DWHExcelColumns.text(14, "merchandiseSubGroup", 255),
                DWHExcelColumns.text(15, "campaignSalesType", 255),
                DWHExcelColumns.decimal(16, "skuStyleColor", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(17, "skuPhase", 255),
                DWHExcelColumns.decimal(18, "orderPcs", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(19, "orderRub", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(20, "foundPcs", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(21, "foundRub", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(22, "salesPcs", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(23, "salesRub", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(24, "revenue", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(25, "gp", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(26, "cogs", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.decimal(27, "salesDiscount", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(28, "planRub", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(29, "stockStoresPcs", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.intNumber(30, "stockStoresDdp", 100, DWHExcelNullHandling.KEEP_NULL),
                DWHExcelColumns.text(31, "cdDrivers", 255),
                DWHExcelColumns.text(32, "skuSupplierModel", 255),
                DWHExcelColumns.text(33, "skuComposition", 255),
                DWHExcelColumns.text(34, "skuColorRussian", 255),
                DWHExcelColumns.text(35, "skuName", 255),
                DWHExcelColumns.text(36, "skuCommentBuyer", 255),
                DWHExcelColumns.text(37, "skuCollection", 255)
        );
    }

    private static DWHExcelColumnSpec date(int index, String name, int rawLength) {
        return new DWHExcelColumnSpec(
                index,
                name,
                name,
                name,
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
                    java.util.Date javaDate = DateUtil.getJavaDate(serial);
                    return javaDate.toInstant()
                            .atZone(ZoneId.systemDefault())
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
