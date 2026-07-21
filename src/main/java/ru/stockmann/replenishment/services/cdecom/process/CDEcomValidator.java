package ru.stockmann.replenishment.services.cdecom.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHFieldValidator;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

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
        List<CDEcomValidationError> errors = new ArrayList<>();

        validateText(errors, row, "name", row.name());
        validateInteger(errors, row, "year", row.year());
        validateInteger(errors, row, "season", row.season());
        validateInteger(errors, row, "day", row.day());
        validateDate(errors, row, "data", row.data());
        validateText(errors, row, "salesChannelBpo", row.salesChannelBpo());
        validateText(errors, row, "storeRus", row.storeRus());
        validateText(errors, row, "mfpDivision", row.mfpDivision());
        validateText(errors, row, "mfpDepartment", row.mfpDepartment());
        validateText(errors, row, "mfpSubDepartment", row.mfpSubDepartment());
        validateText(errors, row, "skuBrandType", row.skuBrandType());
        validateText(errors, row, "skuTm", row.skuTm());
        validateText(errors, row, "mfpNode", row.mfpNode());
        validateText(errors, row, "section", row.section());
        validateText(errors, row, "merchandiseSubGroup", row.merchandiseSubGroup());
        validateText(errors, row, "campaignSalesType", row.campaignSalesType());
        validateRoundedLong(errors, row, "skuStyleColor", row.skuStyleColor());
        validateText(errors, row, "skuPhase", row.skuPhase());
        validateDecimal(errors, row, "orderPcs", row.orderPcs());
        validateDecimal(errors, row, "orderRub", row.orderRub());
        validateDecimal(errors, row, "foundPcs", row.foundPcs());
        validateDecimal(errors, row, "foundRub", row.foundRub());
        validateDecimal(errors, row, "salesPcs", row.salesPcs());
        validateDecimal(errors, row, "salesRub", row.salesRub());
        validateDecimal(errors, row, "revenue", row.revenue());
        validateDecimal(errors, row, "gp", row.gp());
        validateDecimal(errors, row, "cogs", row.cogs());
        validateDecimal(errors, row, "salesDiscount", row.salesDiscount());
        validateDirectLong(errors, row, "planRub", row.planRub());
        validateDirectLong(errors, row, "stockStoresPcs", row.stockStoresPcs());
        validateDirectLong(errors, row, "stockStoresDdp", row.stockStoresDdp());
        validateText(errors, row, "cdDrivers", row.cdDrivers());
        validateText(errors, row, "skuSupplierModel", row.skuSupplierModel());
        validateText(errors, row, "skuComposition", row.skuComposition());
        validateText(errors, row, "skuColorRussian", row.skuColorRussian());
        validateText(errors, row, "skuName", row.skuName());
        validateText(errors, row, "skuCommentBuyer", row.skuCommentBuyer());
        validateText(errors, row, "skuCollection", row.skuCollection());

        return new CDEcomValidationResult(row, errors);
    }

    private void validateInteger(List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value) {
        DWHParseResult<Integer> result = parser.parseInteger(value);
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private void validateDate(List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value) {
        DWHParseResult<?> result = parser.parseDate(value);
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private void validateRoundedLong(List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value) {
        DWHParseResult<Long> result = parser.parseRoundedLong(value);
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private void validateDecimal(List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value) {
        DWHParseResult<?> result = parser.parseDecimal(value);
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private void validateDirectLong(List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value) {
        DWHParseResult<Long> result = parser.parseDirectLong(value);
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private void validateText(List<CDEcomValidationError> errors, CDEcomRawRow row, String fieldName, String value) {
        if (!fieldValidator.isTextLengthValid(value, TEXT_MAX_LENGTH)) {
            errors.add(error(
                    row,
                    fieldName,
                    "TEXT_TOO_LONG",
                    "Value exceeds max length 255",
                    "RawId=" + row.id() + ". Value in [" + fieldName + "] exceeds target length 255: [" + value + "]"
            ));
        }
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
