package ru.stockmann.replenishment.services.cddata.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CDDataValidator {

    private static final String ERROR_LAYER = "VALIDATION";
    private static final int TEXT_MAX_LENGTH = 255;
    private static final int DECIMAL_PRECISION = 18;
    private static final int DECIMAL_SCALE = 2;

    private final DWHValueParser parser;

    public CDDataValidator() {
        this(new DWHValueParser());
    }

    public CDDataValidator(DWHValueParser parser) {
        this.parser = parser;
    }

    public CDDataValidationResult validate(CDDataRawRow row) {
        return new CDDataValidationResult(row, validateAndMap(row).errors());
    }

    public CDDataRowValidationResult validateAndMap(CDDataRawRow row) {
        List<CDDataValidationError> errors = new ArrayList<>();

        DWHParseResult<Integer> god = parseInteger(errors, row, "god", row.god(), true);
        DWHParseResult<Integer> sezon = parseInteger(errors, row, "sezon", row.sezon(), true);
        DWHParseResult<Integer> den = parseInteger(errors, row, "den", row.den(), true);
        DWHParseResult<LocalDate> data = parseDate(errors, row, "data", row.data());
        DWHParseResult<Long> skuStyleColor =
                parseLong(errors, row, "skuStyleColor", row.skuStyleColor());
        DWHParseResult<Integer> planRub = parseInteger(errors, row, "planRub", row.planRub(), false);

        DWHParseResult<BigDecimal> stockStartPcs =
                parseDecimal(errors, row, "stockStartPcs", row.stockStartPcs());
        DWHParseResult<BigDecimal> stockStartDd =
                parseDecimal(errors, row, "stockStartDd", row.stockStartDd());
        DWHParseResult<BigDecimal> salesPcs = parseDecimal(errors, row, "salesPcs", row.salesPcs());
        DWHParseResult<BigDecimal> salesRub = parseDecimal(errors, row, "salesRub", row.salesRub());
        DWHParseResult<BigDecimal> revenue = parseDecimal(errors, row, "revenue", row.revenue());
        DWHParseResult<BigDecimal> gp = parseDecimal(errors, row, "gp", row.gp());
        DWHParseResult<BigDecimal> cogs = parseDecimal(errors, row, "cogs", row.cogs());
        DWHParseResult<BigDecimal> salesFrpPrice =
                parseDecimal(errors, row, "salesFrpPrice", row.salesFrpPrice());
        DWHParseResult<BigDecimal> salesDiscount =
                parseDecimal(errors, row, "salesDiscount", row.salesDiscount());
        DWHParseResult<BigDecimal> stockStoresPcs =
                parseDecimal(errors, row, "stockStoresPcs", row.stockStoresPcs());
        DWHParseResult<BigDecimal> stockStoresDd =
                parseDecimal(errors, row, "stockStoresDd", row.stockStoresDd());

        String nazvanie = cleanText(errors, row, "nazvanie", row.nazvanie(), true);
        String salesChannel = cleanText(errors, row, "salesChannel", row.salesChannel(), false);
        String storeRus = cleanText(errors, row, "storeRus", row.storeRus(), false);
        String mfpDivision = cleanText(errors, row, "mfpDivision", row.mfpDivision(), false);
        String mfpDepartment = cleanText(errors, row, "mfpDepartment", row.mfpDepartment(), false);
        String mfpSubDepartment =
                cleanText(errors, row, "mfpSubDepartment", row.mfpSubDepartment(), false);
        String skuBrandType = cleanText(errors, row, "skuBrandType", row.skuBrandType(), false);
        String skuTm = cleanText(errors, row, "skuTm", row.skuTm(), false);
        String mfpNode = cleanText(errors, row, "mfpNode", row.mfpNode(), false);
        String section = cleanText(errors, row, "section", row.section(), false);
        String merchandiseSubGroup =
                cleanText(errors, row, "merchandiseSubGroup", row.merchandiseSubGroup(), false);
        String campaignSales = cleanText(errors, row, "campaignSales", row.campaignSales(), false);
        String skuPhase = cleanText(errors, row, "skuPhase", row.skuPhase(), false);
        String draiveryCd = cleanText(errors, row, "draiveryCd", row.draiveryCd(), false);
        String skuColorRus = cleanText(errors, row, "skuColorRus", row.skuColorRus(), false);
        String skuComposition = cleanText(errors, row, "skuComposition", row.skuComposition(), false);
        String skuSupplier = cleanText(errors, row, "skuSupplier", row.skuSupplier(), false);
        String skuName = cleanText(errors, row, "skuName", row.skuName(), false);
        String skuCollection = cleanText(errors, row, "skuCollection", row.skuCollection(), false);
        String skuComment = cleanText(errors, row, "skuComment", row.skuComment(), false);

        if (!errors.isEmpty()) {
            return new CDDataRowValidationResult(null, errors);
        }

        CDDataStageRow stageRow = new CDDataStageRow(
                row.loadSessionId(),
                row.excelRowNum(),
                nazvanie,
                god.value(),
                sezon.value(),
                den.value(),
                toSqlDate(data.value()),
                salesChannel,
                storeRus,
                mfpDivision,
                mfpDepartment,
                mfpSubDepartment,
                skuBrandType,
                skuTm,
                mfpNode,
                section,
                merchandiseSubGroup,
                campaignSales,
                skuStyleColor.value(),
                skuPhase,
                stockStartPcs.value(),
                stockStartDd.value(),
                salesPcs.value(),
                salesRub.value(),
                revenue.value(),
                gp.value(),
                cogs.value(),
                salesFrpPrice.value(),
                salesDiscount.value(),
                stockStoresPcs.value(),
                stockStoresDd.value(),
                planRub.value(),
                draiveryCd,
                skuColorRus,
                skuComposition,
                skuSupplier,
                skuName,
                skuCollection,
                skuComment,
                row.id()
        );
        return new CDDataRowValidationResult(stageRow, List.of());
    }

    private DWHParseResult<Integer> parseInteger(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value,
            boolean required
    ) {
        DWHParseResult<Integer> result = parser.parseInteger(value);
        if (required && result.success() && result.value() == null) {
            errors.add(requiredError(row, fieldName));
        } else {
            addParseError(errors, row, fieldName, value, result);
        }
        return result;
    }

    private DWHParseResult<Long> parseLong(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<Long> result = parser.parseLong(value);
        addParseError(errors, row, fieldName, value, result);
        return result;
    }

    private DWHParseResult<BigDecimal> parseDecimal(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<BigDecimal> result =
                parser.parseDecimal(value, DECIMAL_PRECISION, DECIMAL_SCALE);
        addParseError(errors, row, fieldName, value, result);
        return result;
    }

    private DWHParseResult<LocalDate> parseDate(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<LocalDate> result = parser.parseDate(value);
        addParseError(errors, row, fieldName, value, result);
        return result;
    }

    private void addParseError(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value,
            DWHParseResult<?> result
    ) {
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private String cleanText(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value,
            boolean required
    ) {
        String cleaned = parser.cleanText(value);
        if (required && cleaned == null) {
            errors.add(requiredError(row, fieldName));
            return null;
        }
        if (cleaned != null && cleaned.length() > TEXT_MAX_LENGTH) {
            errors.add(error(
                    row,
                    fieldName,
                    "TEXT_TOO_LONG",
                    "Value exceeds max length 255",
                    "Value exceeds max length 255 in field [" + fieldName + "]."
            ));
        }
        return cleaned;
    }

    private CDDataValidationError requiredError(CDDataRawRow row, String fieldName) {
        return error(
                row,
                fieldName,
                "REQUIRED_FIELD_EMPTY",
                "Required value is empty",
                "Required value is empty in field [" + fieldName + "]."
        );
    }

    private Date toSqlDate(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private CDDataValidationError parseError(
            CDDataRawRow row,
            String fieldName,
            String value,
            DWHParseResult<?> result
    ) {
        String reason = result.errorMessage();
        return error(
                row,
                fieldName,
                result.errorCode(),
                reason,
                "Invalid value in field [" + fieldName + "]: [" + value + "]. " + reason
        );
    }

    private CDDataValidationError error(
            CDDataRawRow row,
            String fieldName,
            String errorCode,
            String errorReason,
            String errorMessage
    ) {
        return new CDDataValidationError(
                row.loadSessionId(),
                row.id(),
                row.excelRowNum(),
                ERROR_LAYER,
                fieldName,
                errorCode,
                errorReason,
                "RawId=" + row.id() + ". " + errorMessage
        );
    }
}
