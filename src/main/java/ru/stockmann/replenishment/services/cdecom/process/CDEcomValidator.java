package ru.stockmann.replenishment.services.cdecom.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHFieldValidator;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CDEcomValidator {

    private static final String ERROR_LAYER = "VALIDATION";
    private static final int TEXT_MAX_LENGTH = 255;

    private final CDEcomValueParser parser;
    private final DWHFieldValidator fieldValidator;

    public CDEcomValidator() {
        this(new CDEcomValueParser());
    }

    CDEcomValidator(CDEcomValueParser parser) {
        this.parser = parser;
        this.fieldValidator = new DWHFieldValidator(new DWHValueParser());
    }

    public CDEcomValidationResult validate(CDEcomRawRow row) {
        return new CDEcomValidationResult(row, validateAndMap(row).errors());
    }

    public CDEcomRowValidationResult validateAndMap(CDEcomRawRow row) {
        List<CDEcomValidationError> errors = new ArrayList<>();

        String name = cleanText(errors, row, "name", row.name(), true);
        DWHParseResult<Integer> year = parseInteger(errors, row, "year", row.year(), true);
        DWHParseResult<Integer> season = parseInteger(errors, row, "season", row.season(), true);
        DWHParseResult<Integer> day = parseInteger(errors, row, "day", row.day(), true);
        DWHParseResult<LocalDate> data = parseDate(errors, row, "data", row.data());
        String salesChannelBpo = cleanText(errors, row, "salesChannelBpo", row.salesChannelBpo(), false);
        String storeRus = cleanText(errors, row, "storeRus", row.storeRus(), false);
        String mfpDivision = cleanText(errors, row, "mfpDivision", row.mfpDivision(), false);
        String mfpDepartment = cleanText(errors, row, "mfpDepartment", row.mfpDepartment(), false);
        String mfpSubDepartment = cleanText(errors, row, "mfpSubDepartment", row.mfpSubDepartment(), false);
        String skuBrandType = cleanText(errors, row, "skuBrandType", row.skuBrandType(), false);
        String skuTm = cleanText(errors, row, "skuTm", row.skuTm(), false);
        String mfpNode = cleanText(errors, row, "mfpNode", row.mfpNode(), false);
        String section = cleanText(errors, row, "section", row.section(), false);
        String merchandiseSubGroup =
                cleanText(errors, row, "merchandiseSubGroup", row.merchandiseSubGroup(), false);
        String campaignSalesType =
                cleanText(errors, row, "campaignSalesType", row.campaignSalesType(), false);
        DWHParseResult<Long> skuStyleColor =
                parseRoundedLong(errors, row, "skuStyleColor", row.skuStyleColor());
        String skuPhase = cleanText(errors, row, "skuPhase", row.skuPhase(), false);
        DWHParseResult<BigDecimal> orderPcs = parseDecimal(errors, row, "orderPcs", row.orderPcs());
        DWHParseResult<BigDecimal> orderRub = parseDecimal(errors, row, "orderRub", row.orderRub());
        DWHParseResult<BigDecimal> foundPcs = parseDecimal(errors, row, "foundPcs", row.foundPcs());
        DWHParseResult<BigDecimal> foundRub = parseDecimal(errors, row, "foundRub", row.foundRub());
        DWHParseResult<BigDecimal> salesPcs = parseDecimal(errors, row, "salesPcs", row.salesPcs());
        DWHParseResult<BigDecimal> salesRub = parseDecimal(errors, row, "salesRub", row.salesRub());
        DWHParseResult<BigDecimal> revenue = parseDecimal(errors, row, "revenue", row.revenue());
        DWHParseResult<BigDecimal> gp = parseDecimal(errors, row, "gp", row.gp());
        DWHParseResult<BigDecimal> cogs = parseDecimal(errors, row, "cogs", row.cogs());
        DWHParseResult<BigDecimal> salesDiscount =
                parseDecimal(errors, row, "salesDiscount", row.salesDiscount());
        DWHParseResult<Long> planRub = parseDirectLong(errors, row, "planRub", row.planRub());
        DWHParseResult<Long> stockStoresPcs =
                parseDirectLong(errors, row, "stockStoresPcs", row.stockStoresPcs());
        DWHParseResult<Long> stockStoresDdp =
                parseDirectLong(errors, row, "stockStoresDdp", row.stockStoresDdp());
        String cdDrivers = cleanText(errors, row, "cdDrivers", row.cdDrivers(), false);
        String skuSupplierModel =
                cleanText(errors, row, "skuSupplierModel", row.skuSupplierModel(), false);
        String skuComposition = cleanText(errors, row, "skuComposition", row.skuComposition(), false);
        String skuColorRussian =
                cleanText(errors, row, "skuColorRussian", row.skuColorRussian(), false);
        String skuName = cleanText(errors, row, "skuName", row.skuName(), false);
        String skuCommentBuyer =
                cleanText(errors, row, "skuCommentBuyer", row.skuCommentBuyer(), false);
        String skuCollection = cleanText(errors, row, "skuCollection", row.skuCollection(), false);

        if (!errors.isEmpty()) {
            return new CDEcomRowValidationResult(null, errors);
        }

        return new CDEcomRowValidationResult(new CDEcomStageRow(
                row.loadSessionId(), row.excelRowNum(), name, year.value(), season.value(), day.value(),
                data.value(), salesChannelBpo, storeRus, mfpDivision, mfpDepartment, mfpSubDepartment,
                skuBrandType, skuTm, mfpNode, section, merchandiseSubGroup, campaignSalesType,
                skuStyleColor.value(), skuPhase, decimalValueOrZero(orderPcs), decimalValueOrZero(orderRub),
                decimalValueOrZero(foundPcs), decimalValueOrZero(foundRub), decimalValueOrZero(salesPcs),
                decimalValueOrZero(salesRub), decimalValueOrZero(revenue), decimalValueOrZero(gp),
                decimalValueOrZero(cogs), decimalValueOrZero(salesDiscount), longValueOrZero(planRub),
                longValueOrZero(stockStoresPcs), longValueOrZero(stockStoresDdp),
                cdDrivers, skuSupplierModel, skuComposition, skuColorRussian,
                skuName, skuCommentBuyer, skuCollection, row.id()
        ), List.of());
    }

    private DWHParseResult<Integer> parseInteger(
            List<CDEcomValidationError> errors,
            CDEcomRawRow row,
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

    private DWHParseResult<LocalDate> parseDate(
            List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value
    ) {
        DWHParseResult<LocalDate> result = parser.parseDate(value);
        addParseError(errors, row, fieldName, value, result);
        return result;
    }

    private DWHParseResult<Long> parseRoundedLong(
            List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value
    ) {
        DWHParseResult<Long> result = parser.parseRoundedLong(value);
        addParseError(errors, row, fieldName, value, result);
        return result;
    }

    private DWHParseResult<BigDecimal> parseDecimal(
            List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value
    ) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(value);
        addParseError(errors, row, fieldName, value, result);
        return result;
    }

    private DWHParseResult<Long> parseDirectLong(
            List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value
    ) {
        DWHParseResult<Long> result = parser.parseDirectLong(value);
        addParseError(errors, row, fieldName, value, result);
        return result;
    }

    private void addParseError(
            List<CDEcomValidationError> errors,
            CDEcomRawRow row,
            String fieldName,
            String value,
            DWHParseResult<?> result
    ) {
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private String cleanText(
            List<CDEcomValidationError> errors,
            CDEcomRawRow row,
            String fieldName,
            String value,
            boolean required
    ) {
        if (!fieldValidator.isTextLengthValid(value, TEXT_MAX_LENGTH)) {
            errors.add(error(
                    row,
                    fieldName,
                    "TEXT_TOO_LONG",
                    "Value exceeds max length 255",
                    "RawId=" + row.id() + ". Value in [" + fieldName + "] exceeds target length 255: [" + value + "]"
            ));
        }
        String cleaned = parser.cleanText(value);
        if (required && cleaned == null) {
            errors.add(requiredError(row, fieldName));
        }
        return cleaned;
    }

    private CDEcomValidationError requiredError(CDEcomRawRow row, String fieldName) {
        return error(
                row,
                fieldName,
                "REQUIRED_FIELD_EMPTY",
                "Required value is empty",
                "Required value is empty in field [" + fieldName + "]."
        );
    }

    private BigDecimal decimalValueOrZero(DWHParseResult<BigDecimal> result) {
        return result.value() != null ? result.value() : BigDecimal.ZERO;
    }

    private Long longValueOrZero(DWHParseResult<Long> result) {
        return result.value() != null ? result.value() : 0L;
    }

    private CDEcomValidationError parseError(
            CDEcomRawRow row,
            String fieldName,
            String value,
            DWHParseResult<?> result
    ) {
        return error(
                row,
                fieldName,
                result.errorCode(),
                result.errorMessage(),
                "RawId=" + row.id() + ". Invalid value in field [" + fieldName + "]: [" + value + "]. "
                        + result.errorMessage()
        );
    }

    private CDEcomValidationError error(
            CDEcomRawRow row,
            String fieldName,
            String errorCode,
            String errorReason,
            String errorMessage
    ) {
        return new CDEcomValidationError(
                row.loadSessionId(),
                row.id(),
                row.excelRowNum(),
                ERROR_LAYER,
                fieldName,
                errorCode,
                trim(errorReason, 500),
                trim(errorMessage, 4000)
        );
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
