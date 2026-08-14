package ru.stockmann.replenishment.services.salesbychannel.process;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesByChannelValidatorTest {

    private final SalesByChannelValidator validator = new SalesByChannelValidator();

    @Test
    void mapsAllTwentyNineFieldsWithoutMixingSimilarColumns() {
        SalesByChannelRowValidationResult result = validator.validateAndMap(validRow());
        SalesByChannelStageRow row = result.stageRow();

        assertTrue(result.valid());
        assertEquals(List.of(
                "seasonYear", "season6m", "yearMonth", "yearSeason", "2025", "April",
                "channelType", "StoreRUS", "typeOfSales", "division", "department",
                "campaign", "seasonality", "brand"
        ), List.of(row.seasonYear(), row.season6m(), row.yearMonth(), row.yearSeason(),
                row.year(), row.month(), row.salesChannelType(), row.storeRus(), row.typeOfSales(),
                row.mfpDivision(), row.mfpDepartment(), row.campaignSalesType(),
                row.seasonality(), row.skuBrandType()));
        assertEquals(-2, row.salesQuantity());
        assertEquals(new BigDecimal("12.35"), row.salesCurr());
        assertEquals(new BigDecimal("-2.35"), row.gm());
        assertEquals(new BigDecimal("3.00"), row.discountTtl());
        assertEquals(new BigDecimal("4.00"), row.turnoverCurr());
        assertEquals(List.of(
                "budget", "StoreRus_BPO", "Sales Channel_BPO", "subDepartment", "SKU TM",
                "MFP Node", "section", "group", "phase", "productClass"
        ), List.of(row.skuSeasonBudget(), row.storeRusBpo(), row.salesChannelBpo(),
                row.mfpSubDepartment(), row.skuTm(), row.mfpNode(), row.section(),
                row.merchandiseSubGroup(), row.skuPhase(), row.skuProductClass()));
        assertEquals(17L, row.excelRowNum());
        assertEquals(8L, row.rawRowId());
    }

    @Test
    void blankNullableStringsBecomeNullAndStringZeroSurvives() {
        SalesByChannelRawRow raw = withTextValues(" ", "\t", "0");

        SalesByChannelStageRow row = validator.validateAndMap(raw).stageRow();

        assertNull(row.seasonYear());
        assertNull(row.season6m());
        assertEquals("0", row.salesChannelType());
    }

    @Test
    void yearAndMonthAreRequiredButAreNotParsedAsNumbers() {
        SalesByChannelRawRow textual = withYearMonth("FY2025", "April");
        assertTrue(validator.validateAndMap(textual).valid());

        SalesByChannelRowValidationResult invalid = validator.validateAndMap(withYearMonth(" ", null));
        assertFalse(invalid.valid());
        assertEquals(List.of("year", "month"),
                invalid.errors().stream().map(SalesByChannelValidationError::fieldName).toList());
        assertTrue(invalid.errors().stream()
                .allMatch(error -> "REQUIRED_FIELD_EMPTY".equals(error.errorCode())));
    }

    @Test
    void validatesEveryConfiguredTextLengthAfterTrimming() {
        String[] fields = {
                "seasonYear", "season6m", "yearMonth", "yearSeason", "year", "month",
                "salesChannelType", "storeRus", "typeOfSales", "mfpDivision", "mfpDepartment",
                "campaignSalesType", "seasonality", "skuBrandType", "skuSeasonBudget",
                "storeRusBpo", "salesChannelBpo", "mfpSubDepartment", "skuTm", "mfpNode",
                "section", "merchandiseSubGroup", "skuPhase", "skuProductClass"
        };
        for (String field : fields) {
            SalesByChannelRowValidationResult result = validator.validateAndMap(tooLong(field));
            assertTrue(result.errors().stream().anyMatch(error ->
                            field.equals(error.fieldName()) && "TEXT_TOO_LONG".equals(error.errorCode())),
                    "Expected length error for " + field);
        }
    }

    @Test
    void integerBlankDefaultsToZeroAndNegativeIsAllowed() {
        assertEquals(0, validator.validateAndMap(withSalesQuantity(" ")).stageRow().salesQuantity());
        assertEquals(-42, validator.validateAndMap(withSalesQuantity("-42")).stageRow().salesQuantity());
        assertEquals(0, validator.validateAndMap(withSalesQuantity("0")).stageRow().salesQuantity());
    }

    @Test
    void fractionalInvalidAndOverflowIntegersAreRejected() {
        assertError(withSalesQuantity("1.5"), "salesQuantity", "INVALID_INTEGER");
        assertError(withSalesQuantity("2147483648"), "salesQuantity", "NUMERIC_OVERFLOW");
    }

    @Test
    void decimalBlankDefaultsToZeroAndHalfUpRoundingAndNegativeAreSupported() {
        SalesByChannelStageRow blank = validator.validateAndMap(withDecimals(null, " ", "", "\t")).stageRow();
        assertEquals(new BigDecimal("0.00"), blank.salesCurr());
        assertEquals(new BigDecimal("0.00"), blank.gm());
        assertEquals(new BigDecimal("0.00"), blank.discountTtl());
        assertEquals(new BigDecimal("0.00"), blank.turnoverCurr());

        SalesByChannelStageRow values =
                validator.validateAndMap(withDecimals("1.005", "-1.005", "2,345", "-0.004")).stageRow();
        assertEquals(new BigDecimal("1.01"), values.salesCurr());
        assertEquals(new BigDecimal("-1.01"), values.gm());
        assertEquals(new BigDecimal("2.35"), values.discountTtl());
        assertEquals(new BigDecimal("0.00"), values.turnoverCurr());
    }

    @Test
    void invalidAndOverflowDecimalsAreRejected() {
        assertError(withDecimals("abc", "0", "0", "0"), "salesCurr", "INVALID_DECIMAL");
        assertError(withDecimals("10000000000000000", "0", "0", "0"),
                "salesCurr", "NUMERIC_OVERFLOW");
    }

    @Test
    void everyMetricMapsMissingNullMarkersAndRealZeroToTypedZero() {
        for (String field : metricFields()) {
            for (String value : new String[]{null, "", " ", "   ", "N/A", "NULL", "-", "0", "0.0"}) {
                SalesByChannelRowValidationResult result = validator.validateAndMap(withMetric(field, value));

                assertTrue(result.valid(), field + "=" + value + ": " + result.errors());
                if ("salesQuantity".equals(field)) {
                    assertEquals(0, result.stageRow().salesQuantity(), field + "=" + value);
                } else {
                    assertEquals(0, decimalMetric(result.stageRow(), field).compareTo(BigDecimal.ZERO),
                            field + "=" + value);
                }
            }
        }
    }

    @Test
    void everyMetricParsesPositiveAndNegativeValues() {
        for (String field : metricFields()) {
            for (String value : List.of("12", "-12")) {
                SalesByChannelRowValidationResult result = validator.validateAndMap(withMetric(field, value));

                assertTrue(result.valid(), field + "=" + value + ": " + result.errors());
                if ("salesQuantity".equals(field)) {
                    assertEquals(Integer.valueOf(value), result.stageRow().salesQuantity(), field);
                } else {
                    assertEquals(0, decimalMetric(result.stageRow(), field).compareTo(new BigDecimal(value)), field);
                }
            }
        }
    }

    @Test
    void everyMetricRejectsInvalidNonblankInsteadOfDefaultingToZero() {
        for (String field : metricFields()) {
            for (String value : List.of("abc", "1x", "12abc")) {
                SalesByChannelRowValidationResult result = validator.validateAndMap(withMetric(field, value));

                assertFalse(result.valid(), field + "=" + value);
                String code = "salesQuantity".equals(field) ? "INVALID_INTEGER" : "INVALID_DECIMAL";
                assertTrue(result.errors().stream().anyMatch(error ->
                        field.equals(error.fieldName()) && code.equals(error.errorCode())),
                        field + "=" + value + ": " + result.errors());
            }
        }
    }

    @Test
    void textualYearMonthAcceptFiscalTextAndLeadingZeroExactly() {
        SalesByChannelStageRow fiscal = validator.validateAndMap(withYearMonth("FY2025", "April")).stageRow();
        SalesByChannelStageRow leadingZero = validator.validateAndMap(withYearMonth("2026", "08")).stageRow();

        assertEquals("FY2025", fiscal.year());
        assertEquals("April", fiscal.month());
        assertEquals("2026", leadingZero.year());
        assertEquals("08", leadingZero.month());
    }

    @Test
    void collectsAllErrorsAndNeverReturnsPartiallyTypedStageRow() {
        SalesByChannelRawRow raw = new SalesByChannelRawRow(
                8L, 10L, 17L,
                "x".repeat(51), null, null, null, null, " ",
                null, null, null, null, null, null, null, null,
                "1.5", "bad", "0", "0", "0",
                null, null, null, null, null, null, null, null, null, null
        );

        SalesByChannelRowValidationResult result = validator.validateAndMap(raw);

        assertFalse(result.valid());
        assertNull(result.stageRow());
        assertEquals(5, result.errors().size());
        assertTrue(result.errors().stream().allMatch(error ->
                error.rawId() == 8L && error.excelRowNum() == 17L
                        && error.loadSessionId() == 10L && "VALIDATION".equals(error.errorLayer())));
    }

    private void assertError(SalesByChannelRawRow raw, String field, String code) {
        SalesByChannelRowValidationResult result = validator.validateAndMap(raw);
        assertFalse(result.valid());
        assertNull(result.stageRow());
        assertTrue(result.errors().stream().anyMatch(error ->
                field.equals(error.fieldName()) && code.equals(error.errorCode())));
    }

    private SalesByChannelRawRow validRow() {
        return new SalesByChannelRawRow(
                8L, 10L, 17L,
                " seasonYear ", "season6m", "yearMonth", "yearSeason", "2025", "April",
                "channelType", "StoreRUS", "typeOfSales", "division", "department",
                "campaign", "seasonality", "brand", "-2", "12.345", "-2.345",
                "3", "4", "budget", "StoreRus_BPO", "Sales Channel_BPO",
                "subDepartment", "SKU TM", "MFP Node", "section", "group", "phase",
                "productClass"
        );
    }

    private SalesByChannelRawRow withTextValues(String seasonYear, String season6m, String channel) {
        SalesByChannelRawRow row = validRow();
        return copy(row, seasonYear, season6m, row.yearMonth(), row.yearSeason(), row.year(), row.month(),
                channel, row.storeRus(), row.typeOfSales(), row.mfpDivision(), row.mfpDepartment(),
                row.campaignSalesType(), row.seasonality(), row.skuBrandType(), row.salesQuantity(),
                row.salesCurr(), row.gm(), row.discountTtl(), row.turnoverCurr(), row.skuSeasonBudget(),
                row.storeRusBpo(), row.salesChannelBpo(), row.mfpSubDepartment(), row.skuTm(),
                row.mfpNode(), row.section(), row.merchandiseSubGroup(), row.skuPhase(),
                row.skuProductClass());
    }

    private SalesByChannelRawRow withYearMonth(String year, String month) {
        SalesByChannelRawRow row = validRow();
        return copy(row, row.seasonYear(), row.season6m(), row.yearMonth(), row.yearSeason(), year, month,
                row.salesChannelType(), row.storeRus(), row.typeOfSales(), row.mfpDivision(),
                row.mfpDepartment(), row.campaignSalesType(), row.seasonality(), row.skuBrandType(),
                row.salesQuantity(), row.salesCurr(), row.gm(), row.discountTtl(), row.turnoverCurr(),
                row.skuSeasonBudget(), row.storeRusBpo(), row.salesChannelBpo(), row.mfpSubDepartment(),
                row.skuTm(), row.mfpNode(), row.section(), row.merchandiseSubGroup(), row.skuPhase(),
                row.skuProductClass());
    }

    private SalesByChannelRawRow withSalesQuantity(String value) {
        SalesByChannelRawRow row = validRow();
        return copy(row, row.seasonYear(), row.season6m(), row.yearMonth(), row.yearSeason(), row.year(),
                row.month(), row.salesChannelType(), row.storeRus(), row.typeOfSales(), row.mfpDivision(),
                row.mfpDepartment(), row.campaignSalesType(), row.seasonality(), row.skuBrandType(),
                value, row.salesCurr(), row.gm(), row.discountTtl(), row.turnoverCurr(),
                row.skuSeasonBudget(), row.storeRusBpo(), row.salesChannelBpo(), row.mfpSubDepartment(),
                row.skuTm(), row.mfpNode(), row.section(), row.merchandiseSubGroup(), row.skuPhase(),
                row.skuProductClass());
    }

    private SalesByChannelRawRow withDecimals(String sales, String gm, String discount, String turnover) {
        SalesByChannelRawRow row = validRow();
        return copy(row, row.seasonYear(), row.season6m(), row.yearMonth(), row.yearSeason(), row.year(),
                row.month(), row.salesChannelType(), row.storeRus(), row.typeOfSales(), row.mfpDivision(),
                row.mfpDepartment(), row.campaignSalesType(), row.seasonality(), row.skuBrandType(),
                row.salesQuantity(), sales, gm, discount, turnover, row.skuSeasonBudget(),
                row.storeRusBpo(), row.salesChannelBpo(), row.mfpSubDepartment(), row.skuTm(),
                row.mfpNode(), row.section(), row.merchandiseSubGroup(), row.skuPhase(),
                row.skuProductClass());
    }

    private static List<String> metricFields() {
        return List.of("salesQuantity", "salesCurr", "gm", "discountTtl", "turnoverCurr");
    }

    private SalesByChannelRawRow withMetric(String field, String value) {
        SalesByChannelRawRow row = validRow();
        return copy(row, row.seasonYear(), row.season6m(), row.yearMonth(), row.yearSeason(), row.year(),
                row.month(), row.salesChannelType(), row.storeRus(), row.typeOfSales(), row.mfpDivision(),
                row.mfpDepartment(), row.campaignSalesType(), row.seasonality(), row.skuBrandType(),
                "salesQuantity".equals(field) ? value : row.salesQuantity(),
                "salesCurr".equals(field) ? value : row.salesCurr(),
                "gm".equals(field) ? value : row.gm(),
                "discountTtl".equals(field) ? value : row.discountTtl(),
                "turnoverCurr".equals(field) ? value : row.turnoverCurr(),
                row.skuSeasonBudget(), row.storeRusBpo(), row.salesChannelBpo(), row.mfpSubDepartment(),
                row.skuTm(), row.mfpNode(), row.section(), row.merchandiseSubGroup(), row.skuPhase(),
                row.skuProductClass());
    }

    private static BigDecimal decimalMetric(SalesByChannelStageRow row, String field) {
        return switch (field) {
            case "salesCurr" -> row.salesCurr();
            case "gm" -> row.gm();
            case "discountTtl" -> row.discountTtl();
            case "turnoverCurr" -> row.turnoverCurr();
            default -> throw new IllegalArgumentException(field);
        };
    }

    private SalesByChannelRawRow tooLong(String field) {
        SalesByChannelRawRow row = validRow();
        int max = List.of("seasonYear", "season6m", "yearMonth", "yearSeason", "year", "month",
                "seasonality", "skuSeasonBudget").contains(field) ? 50 : 100;
        String value = "x".repeat(max + 1);
        String[] text = {
                row.seasonYear(), row.season6m(), row.yearMonth(), row.yearSeason(), row.year(), row.month(),
                row.salesChannelType(), row.storeRus(), row.typeOfSales(), row.mfpDivision(),
                row.mfpDepartment(), row.campaignSalesType(), row.seasonality(), row.skuBrandType(),
                row.skuSeasonBudget(), row.storeRusBpo(), row.salesChannelBpo(), row.mfpSubDepartment(),
                row.skuTm(), row.mfpNode(), row.section(), row.merchandiseSubGroup(),
                row.skuPhase(), row.skuProductClass()
        };
        String[] names = {
                "seasonYear", "season6m", "yearMonth", "yearSeason", "year", "month",
                "salesChannelType", "storeRus", "typeOfSales", "mfpDivision", "mfpDepartment",
                "campaignSalesType", "seasonality", "skuBrandType", "skuSeasonBudget",
                "storeRusBpo", "salesChannelBpo", "mfpSubDepartment", "skuTm", "mfpNode",
                "section", "merchandiseSubGroup", "skuPhase", "skuProductClass"
        };
        for (int i = 0; i < names.length; i++) if (names[i].equals(field)) text[i] = value;
        return copy(row, text[0], text[1], text[2], text[3], text[4], text[5], text[6], text[7],
                text[8], text[9], text[10], text[11], text[12], text[13], row.salesQuantity(),
                row.salesCurr(), row.gm(), row.discountTtl(), row.turnoverCurr(), text[14], text[15],
                text[16], text[17], text[18], text[19], text[20], text[21], text[22], text[23]);
    }

    private SalesByChannelRawRow copy(
            SalesByChannelRawRow row, String seasonYear, String season6m, String yearMonth,
            String yearSeason, String year, String month, String salesChannelType, String storeRus,
            String typeOfSales, String mfpDivision, String mfpDepartment, String campaignSalesType,
            String seasonality, String skuBrandType, String salesQuantity, String salesCurr, String gm,
            String discountTtl, String turnoverCurr, String skuSeasonBudget, String storeRusBpo,
            String salesChannelBpo, String mfpSubDepartment, String skuTm, String mfpNode,
            String section, String merchandiseSubGroup, String skuPhase, String skuProductClass
    ) {
        return new SalesByChannelRawRow(
                row.id(), row.loadSessionId(), row.excelRowNum(), seasonYear, season6m, yearMonth,
                yearSeason, year, month, salesChannelType, storeRus, typeOfSales, mfpDivision,
                mfpDepartment, campaignSalesType, seasonality, skuBrandType, salesQuantity,
                salesCurr, gm, discountTtl, turnoverCurr, skuSeasonBudget, storeRusBpo,
                salesChannelBpo, mfpSubDepartment, skuTm, mfpNode, section,
                merchandiseSubGroup, skuPhase, skuProductClass
        );
    }
}
