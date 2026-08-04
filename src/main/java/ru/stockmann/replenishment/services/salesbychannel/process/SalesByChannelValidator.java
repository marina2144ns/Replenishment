package ru.stockmann.replenishment.services.salesbychannel.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SalesByChannelValidator {

    private static final String ERROR_LAYER = "VALIDATION";
    private static final int DECIMAL_PRECISION = 18;
    private static final int DECIMAL_SCALE = 2;
    private static final BigDecimal ZERO_DECIMAL = new BigDecimal("0.00");

    private final DWHValueParser parser;

    public SalesByChannelValidator() {
        this(new DWHValueParser());
    }

    SalesByChannelValidator(DWHValueParser parser) {
        this.parser = parser;
    }

    public SalesByChannelRowValidationResult validateAndMap(SalesByChannelRawRow row) {
        List<SalesByChannelValidationError> errors = new ArrayList<>();

        String seasonYear = text(errors, row, "seasonYear", row.seasonYear(), 50, false);
        String season6m = text(errors, row, "season6m", row.season6m(), 50, false);
        String yearMonth = text(errors, row, "yearMonth", row.yearMonth(), 50, false);
        String yearSeason = text(errors, row, "yearSeason", row.yearSeason(), 50, false);
        String year = text(errors, row, "year", row.year(), 50, true);
        String month = text(errors, row, "month", row.month(), 50, true);
        String salesChannelType =
                text(errors, row, "salesChannelType", row.salesChannelType(), 100, false);
        String storeRus = text(errors, row, "storeRus", row.storeRus(), 100, false);
        String typeOfSales = text(errors, row, "typeOfSales", row.typeOfSales(), 100, false);
        String mfpDivision = text(errors, row, "mfpDivision", row.mfpDivision(), 100, false);
        String mfpDepartment =
                text(errors, row, "mfpDepartment", row.mfpDepartment(), 100, false);
        String campaignSalesType =
                text(errors, row, "campaignSalesType", row.campaignSalesType(), 100, false);
        String seasonality = text(errors, row, "seasonality", row.seasonality(), 50, false);
        String skuBrandType = text(errors, row, "skuBrandType", row.skuBrandType(), 100, false);
        String skuSeasonBudget =
                text(errors, row, "skuSeasonBudget", row.skuSeasonBudget(), 50, false);
        String storeRusBpo = text(errors, row, "storeRusBpo", row.storeRusBpo(), 100, false);
        String salesChannelBpo =
                text(errors, row, "salesChannelBpo", row.salesChannelBpo(), 100, false);
        String mfpSubDepartment =
                text(errors, row, "mfpSubDepartment", row.mfpSubDepartment(), 100, false);
        String skuTm = text(errors, row, "skuTm", row.skuTm(), 100, false);
        String mfpNode = text(errors, row, "mfpNode", row.mfpNode(), 100, false);
        String section = text(errors, row, "section", row.section(), 100, false);
        String merchandiseSubGroup =
                text(errors, row, "merchandiseSubGroup", row.merchandiseSubGroup(), 100, false);
        String skuPhase = text(errors, row, "skuPhase", row.skuPhase(), 100, false);
        String skuProductClass =
                text(errors, row, "skuProductClass", row.skuProductClass(), 100, false);

        Integer salesQuantity = integer(errors, row, "salesQuantity", row.salesQuantity());
        BigDecimal salesCurr = decimal(errors, row, "salesCurr", row.salesCurr());
        BigDecimal gm = decimal(errors, row, "gm", row.gm());
        BigDecimal discountTtl = decimal(errors, row, "discountTtl", row.discountTtl());
        BigDecimal turnoverCurr = decimal(errors, row, "turnoverCurr", row.turnoverCurr());

        if (!errors.isEmpty()) {
            return new SalesByChannelRowValidationResult(null, errors);
        }

        return new SalesByChannelRowValidationResult(new SalesByChannelStageRow(
                row.loadSessionId(), row.excelRowNum(),
                seasonYear, season6m, yearMonth, yearSeason, year, month,
                salesChannelType, storeRus, typeOfSales, mfpDivision, mfpDepartment,
                campaignSalesType, seasonality, skuBrandType, salesQuantity,
                salesCurr, gm, discountTtl, turnoverCurr, skuSeasonBudget,
                storeRusBpo, salesChannelBpo, mfpSubDepartment, skuTm, mfpNode,
                section, merchandiseSubGroup, skuPhase, skuProductClass, row.id()
        ), List.of());
    }

    private String text(
            List<SalesByChannelValidationError> errors,
            SalesByChannelRawRow row,
            String field,
            String value,
            int maxLength,
            boolean required
    ) {
        String cleaned = cleanText(value);
        if (required && cleaned == null) {
            errors.add(error(row, field, "REQUIRED_FIELD_EMPTY", "Required value is empty"));
        }
        if (cleaned != null && cleaned.length() > maxLength) {
            errors.add(error(
                    row, field, "TEXT_TOO_LONG", "Value exceeds max length " + maxLength
            ));
        }
        return cleaned;
    }

    private Integer integer(
            List<SalesByChannelValidationError> errors,
            SalesByChannelRawRow row,
            String field,
            String value
    ) {
        if (cleanText(value) == null) {
            return 0;
        }
        DWHParseResult<Integer> result = parser.parseInteger(value);
        if (!result.success() || result.value() == null) {
            String code = result.success() ? "INVALID_INTEGER" : result.errorCode();
            String reason = "NUMERIC_OVERFLOW".equals(code)
                    ? "INTEGER value is out of range"
                    : "Invalid INTEGER value";
            errors.add(error(row, field, code, reason));
            return null;
        }
        return result.value();
    }

    private BigDecimal decimal(
            List<SalesByChannelValidationError> errors,
            SalesByChannelRawRow row,
            String field,
            String value
    ) {
        if (cleanText(value) == null) {
            return ZERO_DECIMAL;
        }
        DWHParseResult<BigDecimal> result =
                parser.parseDecimal(value, DECIMAL_PRECISION, DECIMAL_SCALE);
        if (!result.success() || result.value() == null) {
            String code = result.success() ? "INVALID_DECIMAL" : result.errorCode();
            String reason = "NUMERIC_OVERFLOW".equals(code)
                    ? "DECIMAL(18,2) value is out of range"
                    : "Invalid DECIMAL value";
            errors.add(error(row, field, code, reason));
            return null;
        }
        return result.value();
    }

    private SalesByChannelValidationError error(
            SalesByChannelRawRow row,
            String field,
            String code,
            String reason
    ) {
        return new SalesByChannelValidationError(
                row.loadSessionId(), row.id(), row.excelRowNum(), ERROR_LAYER,
                field, code, trim(reason, 500),
                trim("RawId=" + row.id() + ". " + reason + " in field [" + field + "].", 4000)
        );
    }

    private String cleanText(String value) {
        String cleaned = parser.cleanText(value);
        return cleaned == null || cleaned.isBlank() ? null : cleaned;
    }

    private static String trim(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
