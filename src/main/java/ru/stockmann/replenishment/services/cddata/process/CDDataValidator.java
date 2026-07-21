package ru.stockmann.replenishment.services.cddata.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHFieldValidator;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CDDataValidator {

    private static final String ERROR_LAYER = "VALIDATION";
    private static final int TEXT_MAX_LENGTH = 255;
    private static final int DECIMAL_PRECISION = 18;
    private static final int DECIMAL_SCALE = 2;

    private final DWHValueParser parser;
    private final DWHFieldValidator fieldValidator;

    public CDDataValidator() {
        this(new DWHValueParser());
    }

    public CDDataValidator(DWHValueParser parser) {
        this.parser = parser;
        this.fieldValidator = new DWHFieldValidator(parser);
    }

    public CDDataValidationResult validate(CDDataRawRow row) {
        List<CDDataValidationError> errors = new ArrayList<>();

        validateInteger(errors, row, "god", row.god());
        validateInteger(errors, row, "sezon", row.sezon());
        validateInteger(errors, row, "den", row.den());
        validateDate(errors, row, "data", row.data());
        validateLong(errors, row, "skuStyleColor", row.skuStyleColor());
        validateInteger(errors, row, "planRub", row.planRub());

        validateDecimal(errors, row, "stockStartPcs", row.stockStartPcs());
        validateDecimal(errors, row, "stockStartDd", row.stockStartDd());
        validateDecimal(errors, row, "salesPcs", row.salesPcs());
        validateDecimal(errors, row, "salesRub", row.salesRub());
        validateDecimal(errors, row, "revenue", row.revenue());
        validateDecimal(errors, row, "gp", row.gp());
        validateDecimal(errors, row, "cogs", row.cogs());
        validateDecimal(errors, row, "salesFrpPrice", row.salesFrpPrice());
        validateDecimal(errors, row, "salesDiscount", row.salesDiscount());
        validateDecimal(errors, row, "stockStoresPcs", row.stockStoresPcs());
        validateDecimal(errors, row, "stockStoresDd", row.stockStoresDd());

        validateText(errors, row, "nazvanie", row.nazvanie());
        validateText(errors, row, "salesChannel", row.salesChannel());
        validateText(errors, row, "storeRus", row.storeRus());
        validateText(errors, row, "mfpDivision", row.mfpDivision());
        validateText(errors, row, "mfpDepartment", row.mfpDepartment());
        validateText(errors, row, "mfpSubDepartment", row.mfpSubDepartment());
        validateText(errors, row, "skuBrandType", row.skuBrandType());
        validateText(errors, row, "skuTm", row.skuTm());
        validateText(errors, row, "mfpNode", row.mfpNode());
        validateText(errors, row, "section", row.section());
        validateText(errors, row, "merchandiseSubGroup", row.merchandiseSubGroup());
        validateText(errors, row, "campaignSales", row.campaignSales());
        validateText(errors, row, "skuPhase", row.skuPhase());
        validateText(errors, row, "draiveryCd", row.draiveryCd());
        validateText(errors, row, "skuColorRus", row.skuColorRus());
        validateText(errors, row, "skuComposition", row.skuComposition());
        validateText(errors, row, "skuSupplier", row.skuSupplier());
        validateText(errors, row, "skuName", row.skuName());
        validateText(errors, row, "skuCollection", row.skuCollection());
        validateText(errors, row, "skuComment", row.skuComment());

        return new CDDataValidationResult(row, errors);
    }

    private void validateInteger(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<Integer> result = parser.parseInteger(value);
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private void validateLong(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<Long> result = parser.parseLong(value);
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private void validateDecimal(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(
                value,
                DECIMAL_PRECISION,
                DECIMAL_SCALE
        );
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private void validateDate(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value
    ) {
        DWHParseResult<LocalDate> result = parser.parseDate(value);
        if (!result.success()) {
            errors.add(parseError(row, fieldName, value, result));
        }
    }

    private void validateText(
            List<CDDataValidationError> errors,
            CDDataRawRow row,
            String fieldName,
            String value
    ) {
        if (!fieldValidator.isTextLengthValid(value, TEXT_MAX_LENGTH)) {
            errors.add(error(
                    row,
                    fieldName,
                    "TEXT_TOO_LONG",
                    "Value exceeds max length 255",
                    "Value exceeds max length 255 in field [" + fieldName + "]."
            ));
        }
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
