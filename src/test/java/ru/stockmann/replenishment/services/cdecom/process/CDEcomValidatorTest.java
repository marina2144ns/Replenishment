package ru.stockmann.replenishment.services.cdecom.process;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomValidatorTest {

    private final CDEcomValidator validator = new CDEcomValidator();

    @Test
    void emptyRowIsInvalid() {
        CDEcomValidationResult result = validator.validate(new RowBuilder().build());

        assertEquals(List.of("name", "year", "season", "day"), fields(result));
        assertTrue(result.errors().stream()
                .allMatch(error -> "REQUIRED_FIELD_EMPTY".equals(error.errorCode())));
        assertTrue(validator.validateAndMap(new RowBuilder().build()).stageRow() == null);
    }

    @Test
    void requiredIntegersRejectNullBlankWhitespaceSpecialNullAndInvalidValues() {
        for (String field : List.of("year", "season", "day")) {
            for (String value : new String[]{null, "", " ", "   ", "N/A", "NULL", "-"}) {
                CDEcomValidationResult result = validator.validate(requiredInteger(field, value));
                assertEquals(List.of(field), fields(result), field + "=" + value);
                assertEquals(List.of("REQUIRED_FIELD_EMPTY"), codes(result), field + "=" + value);
            }

            CDEcomValidationResult invalid = validator.validate(requiredInteger(field, "invalid"));
            assertEquals(List.of(field), fields(invalid));
            assertEquals(List.of("INVALID_INTEGER"), codes(invalid));
            assertTrue(validator.validate(requiredInteger(field, "123")).valid(), field);
        }
    }

    @Test
    void nameRejectsSupportedBlankRepresentationsAndAcceptsNonblankText() {
        for (String value : new String[]{
                null, "", " ", "   ", "\u00A0", "\u00A0\u00A0", "\u202F", "\u202F\u202F"
        }) {
            CDEcomValidationResult result = validator.validate(row().withName(value).build());
            assertEquals(List.of("name"), fields(result), "name=" + value);
            assertEquals(List.of("REQUIRED_FIELD_EMPTY"), codes(result), "name=" + value);
        }

        assertTrue(validator.validate(row().withName("Name").build()).valid());
    }

    @Test
    void fullyValidRowIsValid() {
        assertTrue(validator.validate(validRow()).valid());
        assertEquals(10L, validator.validateAndMap(validRow()).stageRow().rawRowId());
    }

    @Test
    void validatesIntegerFields() {
        CDEcomValidationResult result = validator.validate(row().withYear("12.5").withSeason("x").withDay("999999999999").build());

        assertEquals(List.of("year", "season", "day"), fields(result));
        assertEquals(List.of("INVALID_INTEGER", "INVALID_INTEGER", "NUMERIC_OVERFLOW"), codes(result));
    }

    @Test
    void validatesStoredProcedureDateFormats() {
        assertTrue(validator.validate(row().withData("31.01.2026").build()).valid());
        assertTrue(validator.validate(row().withData("31/01/2026").build()).valid());
        assertTrue(validator.validate(row().withData("2026-01-31").build()).valid());
        assertTrue(validator.validate(row().withData("1/31/26").build()).valid());
        assertTrue(validator.validate(row().withData("1/31/2026").build()).valid());

        CDEcomValidationResult result = validator.validate(row().withData("31-01-2026").build());

        assertEquals("data", result.errors().get(0).fieldName());
        assertEquals("INVALID_DATE", result.errors().get(0).errorCode());
    }

    @Test
    void acceptsNumericExcelDateNormalizedByDefinition() {
        String rawDate = new CDEcomExcelLoadDefinition()
                .columns()
                .get(4)
                .normalizer()
                .normalize("45658");

        assertEquals("01.01.2025", rawDate);
        assertTrue(validator.validate(row().withData(rawDate).build()).valid());
    }

    @Test
    void excelSerialDateAndTypedDateAreIndependentOfDefaultTimezone() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
            String east = normalizeExcelDate("45658");
            LocalDate eastDate = validator.validateAndMap(row().withData(east).build()).stageRow().data();

            TimeZone.setDefault(TimeZone.getTimeZone("America/Adak"));
            String west = normalizeExcelDate("45658");
            LocalDate westDate = validator.validateAndMap(row().withData(west).build()).stageRow().data();

            assertEquals("01.01.2025", east);
            assertEquals(east, west);
            assertEquals(LocalDate.of(2025, 1, 1), eastDate);
            assertEquals(eastDate, westDate);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void validateAndMapParsesDateOnlyOnceAndReturnsNormalErrorForInvalidDate() {
        CountingParser parser = new CountingParser();
        CDEcomValidator countingValidator = new CDEcomValidator(parser);

        CDEcomRowValidationResult valid =
                countingValidator.validateAndMap(row().withData("31.01.2025").build());
        CDEcomRowValidationResult invalid =
                countingValidator.validateAndMap(row().withData("invalid").build());

        assertTrue(valid.valid());
        assertEquals(LocalDate.of(2025, 1, 31), valid.stageRow().data());
        assertEquals(2, parser.dateCalls);
        assertFalse(invalid.valid());
        assertEquals("INVALID_DATE", invalid.errors().get(0).errorCode());
    }

    @Test
    void validatesTextLengthAndRetainsExcelRowNum() {
        CDEcomValidationResult result = validator.validate(row().withName("x".repeat(256)).build());

        assertEquals("name", result.errors().get(0).fieldName());
        assertEquals("TEXT_TOO_LONG", result.errors().get(0).errorCode());
        assertEquals(51L, result.errors().get(0).excelRowNum());
    }

    @Test
    void validatesRoundedSkuStyleColor() {
        assertTrue(validator.validate(row().withSkuStyleColor("12").build()).valid());
        assertTrue(validator.validate(row().withSkuStyleColor("12.4").build()).valid());
        assertTrue(validator.validate(row().withSkuStyleColor("12.5").build()).valid());
        assertTrue(validator.validate(row().withSkuStyleColor("12.6").build()).valid());
        assertTrue(validator.validate(row().withSkuStyleColor("-12.4").build()).valid());
        assertTrue(validator.validate(row().withSkuStyleColor("-12.5").build()).valid());
        assertTrue(validator.validate(row().withSkuStyleColor("-12.6").build()).valid());
        assertTrue(validator.validate(row().withSkuStyleColor("1 234,6").build()).valid());
        assertTrue(validator.validate(row().withSkuStyleColor("1.2E3").build()).valid());

        assertEquals("NUMERIC_OVERFLOW", validator.validate(row().withSkuStyleColor("9223372036854775807.5").build())
                .errors().get(0).errorCode());
        assertEquals("INVALID_BIGINT", validator.validate(row().withSkuStyleColor("NaN").build())
                .errors().get(0).errorCode());
    }

    @Test
    void validatesDecimalFields() {
        assertTrue(validator.validate(row().withOrderPcs("1 234,565").build()).valid());
        assertTrue(validator.validate(row().withOrderPcs("1.2E3").build()).valid());
        assertTrue(validator.validate(row().withOrderPcs("9999999999999999.994").build()).valid());
        assertTrue(validator.validate(row().withOrderPcs("-9999999999999999.994").build()).valid());

        assertEquals("NUMERIC_OVERFLOW", validator.validate(row().withOrderPcs("9999999999999999.995").build())
                .errors().get(0).errorCode());
        assertEquals("NUMERIC_OVERFLOW", validator.validate(row().withOrderPcs("-9999999999999999.995").build())
                .errors().get(0).errorCode());
        assertEquals("INVALID_DECIMAL", validator.validate(row().withOrderPcs("Infinity").build())
                .errors().get(0).errorCode());
    }

    @Test
    void validatesDirectBigintFieldsWithoutRounding() {
        assertTrue(validator.validate(row().withPlanRub("12").build()).valid());
        assertTrue(validator.validate(row().withPlanRub("12.0").build()).valid());
        assertTrue(validator.validate(row().withPlanRub("12.00").build()).valid());
        assertTrue(validator.validate(row().withPlanRub("-12.0").build()).valid());
        assertTrue(validator.validate(row().withStockStoresDdp("1e2").build()).valid());
        assertTrue(validator.validate(row().withPlanRub("9223372036854775807").build()).valid());
        assertEquals("INVALID_BIGINT", validator.validate(row().withPlanRub("12.5").build())
                .errors().get(0).errorCode());
        assertEquals("NUMERIC_OVERFLOW", validator.validate(row().withStockStoresPcs("9223372036854775808").build())
                .errors().get(0).errorCode());
    }

    @Test
    void everyNumericMetricMapsMissingAndRealZeroToTypedZero() {
        for (String field : metricFields()) {
            for (String value : new String[]{null, "", " ", "   ", "N/A", "NULL", "-", "0", "0.0"}) {
                CDEcomRowValidationResult result = validator.validateAndMap(rowWithMetric(field, value));

                assertTrue(result.valid(), field + "=" + value + ": " + result.errors());
                if (bigintMetrics().contains(field)) {
                    assertEquals(0L, bigintMetricValue(result.stageRow(), field), field + "=" + value);
                } else {
                    assertEquals(0, decimalMetricValue(result.stageRow(), field).compareTo(BigDecimal.ZERO),
                            field + "=" + value);
                }
            }
        }
    }

    @Test
    void everyNumericMetricParsesPositiveAndNegativeValues() {
        for (String field : metricFields()) {
            for (String value : List.of("12", "-12")) {
                CDEcomRowValidationResult result = validator.validateAndMap(rowWithMetric(field, value));

                assertTrue(result.valid(), field + "=" + value + ": " + result.errors());
                if (bigintMetrics().contains(field)) {
                    assertEquals(Long.valueOf(value), bigintMetricValue(result.stageRow(), field), field);
                } else {
                    assertEquals(0,
                            decimalMetricValue(result.stageRow(), field).compareTo(new BigDecimal(value)), field);
                }
            }
        }
    }

    @Test
    void everyNumericMetricRejectsInvalidNonblankInsteadOfDefaultingToZero() {
        for (String field : metricFields()) {
            for (String value : List.of("abc", "1x", "12abc")) {
                CDEcomRowValidationResult result = validator.validateAndMap(rowWithMetric(field, value));

                assertTrue(result.stageRow() == null, field + "=" + value);
                String code = bigintMetrics().contains(field) ? "INVALID_BIGINT" : "INVALID_DECIMAL";
                assertTrue(result.errors().stream().anyMatch(error ->
                        field.equals(error.fieldName()) && code.equals(error.errorCode())),
                        field + "=" + value + ": " + result.errors());
            }
        }
    }

    @Test
    void missingSkuStyleColorRemainsNullInsteadOfBecomingZero() {
        CDEcomRowValidationResult result = validator.validateAndMap(row().withSkuStyleColor(null).build());

        assertTrue(result.valid());
        assertEquals(null, result.stageRow().skuStyleColor());
    }

    @Test
    void collectsMultipleErrorsInStableOrderWithOneErrorPerField() {
        CDEcomValidationResult result = validator.validate(row()
                .withYear("x")
                .withData("bad")
                .withSkuStyleColor("bad")
                .withOrderPcs("bad")
                .withPlanRub("bad")
                .withName("x".repeat(256))
                .build());

        assertEquals(List.of("name", "year", "data", "skuStyleColor", "orderPcs", "planRub"), fields(result));
        assertEquals(6, result.errors().stream().map(CDEcomValidationError::fieldName).distinct().count());
    }

    private static List<String> fields(CDEcomValidationResult result) {
        return result.errors().stream().map(CDEcomValidationError::fieldName).toList();
    }

    private static List<String> codes(CDEcomValidationResult result) {
        return result.errors().stream().map(CDEcomValidationError::errorCode).toList();
    }

    private static RowBuilder row() {
        return new RowBuilder()
                .withName("Name")
                .withYear("2026")
                .withSeason("1")
                .withDay("31");
    }

    private static CDEcomRawRow requiredInteger(String field, String value) {
        RowBuilder builder = row();
        return switch (field) {
            case "year" -> builder.withYear(value).build();
            case "season" -> builder.withSeason(value).build();
            case "day" -> builder.withDay(value).build();
            default -> throw new IllegalArgumentException(field);
        };
    }

    private static String normalizeExcelDate(String value) {
        return new CDEcomExcelLoadDefinition().columns().get(4).normalizer().normalize(value);
    }

    private static List<String> metricFields() {
        return List.of(
                "orderPcs", "orderRub", "foundPcs", "foundRub", "salesPcs", "salesRub",
                "revenue", "gp", "cogs", "salesDiscount", "planRub", "stockStoresPcs",
                "stockStoresDdp"
        );
    }

    private static List<String> bigintMetrics() {
        return List.of("planRub", "stockStoresPcs", "stockStoresDdp");
    }

    private static CDEcomRawRow rowWithMetric(String field, String value) {
        RowBuilder builder = row();
        return switch (field) {
            case "orderPcs" -> builder.withOrderPcs(value).build();
            case "orderRub" -> builder.withOrderRub(value).build();
            case "foundPcs" -> builder.withFoundPcs(value).build();
            case "foundRub" -> builder.withFoundRub(value).build();
            case "salesPcs" -> builder.withSalesPcs(value).build();
            case "salesRub" -> builder.withSalesRub(value).build();
            case "revenue" -> builder.withRevenue(value).build();
            case "gp" -> builder.withGp(value).build();
            case "cogs" -> builder.withCogs(value).build();
            case "salesDiscount" -> builder.withSalesDiscount(value).build();
            case "planRub" -> builder.withPlanRub(value).build();
            case "stockStoresPcs" -> builder.withStockStoresPcs(value).build();
            case "stockStoresDdp" -> builder.withStockStoresDdp(value).build();
            default -> throw new IllegalArgumentException(field);
        };
    }

    private static BigDecimal decimalMetricValue(CDEcomStageRow row, String field) {
        return switch (field) {
            case "orderPcs" -> row.orderPcs();
            case "orderRub" -> row.orderRub();
            case "foundPcs" -> row.foundPcs();
            case "foundRub" -> row.foundRub();
            case "salesPcs" -> row.salesPcs();
            case "salesRub" -> row.salesRub();
            case "revenue" -> row.revenue();
            case "gp" -> row.gp();
            case "cogs" -> row.cogs();
            case "salesDiscount" -> row.salesDiscount();
            default -> throw new IllegalArgumentException(field);
        };
    }

    private static Long bigintMetricValue(CDEcomStageRow row, String field) {
        return switch (field) {
            case "planRub" -> row.planRub();
            case "stockStoresPcs" -> row.stockStoresPcs();
            case "stockStoresDdp" -> row.stockStoresDdp();
            default -> throw new IllegalArgumentException(field);
        };
    }

    private static final class CountingParser extends CDEcomValueParser {
        private int dateCalls;
        @Override
        DWHParseResult<LocalDate> parseDate(String value) {
            dateCalls++;
            return super.parseDate(value);
        }
    }

    private static CDEcomRawRow validRow() {
        return row()
                .withName("Name")
                .withYear("2026")
                .withSeason("1")
                .withDay("31")
                .withData("31.01.2026")
                .withSkuStyleColor("12.5")
                .withOrderPcs("1,235")
                .withPlanRub("123")
                .withStockStoresPcs("456")
                .build();
    }

    private static final class RowBuilder {
        private String name;
        private String year;
        private String season;
        private String day;
        private String data;
        private String skuStyleColor;
        private String orderPcs;
        private String orderRub;
        private String foundPcs;
        private String foundRub;
        private String salesPcs;
        private String salesRub;
        private String revenue;
        private String gp;
        private String cogs;
        private String salesDiscount;
        private String planRub;
        private String stockStoresPcs;
        private String stockStoresDdp;

        private RowBuilder withName(String value) { this.name = value; return this; }
        private RowBuilder withYear(String value) { this.year = value; return this; }
        private RowBuilder withSeason(String value) { this.season = value; return this; }
        private RowBuilder withDay(String value) { this.day = value; return this; }
        private RowBuilder withData(String value) { this.data = value; return this; }
        private RowBuilder withSkuStyleColor(String value) { this.skuStyleColor = value; return this; }
        private RowBuilder withOrderPcs(String value) { this.orderPcs = value; return this; }
        private RowBuilder withOrderRub(String value) { this.orderRub = value; return this; }
        private RowBuilder withFoundPcs(String value) { this.foundPcs = value; return this; }
        private RowBuilder withFoundRub(String value) { this.foundRub = value; return this; }
        private RowBuilder withSalesPcs(String value) { this.salesPcs = value; return this; }
        private RowBuilder withSalesRub(String value) { this.salesRub = value; return this; }
        private RowBuilder withRevenue(String value) { this.revenue = value; return this; }
        private RowBuilder withGp(String value) { this.gp = value; return this; }
        private RowBuilder withCogs(String value) { this.cogs = value; return this; }
        private RowBuilder withSalesDiscount(String value) { this.salesDiscount = value; return this; }
        private RowBuilder withPlanRub(String value) { this.planRub = value; return this; }
        private RowBuilder withStockStoresPcs(String value) { this.stockStoresPcs = value; return this; }
        private RowBuilder withStockStoresDdp(String value) { this.stockStoresDdp = value; return this; }

        private CDEcomRawRow build() {
            return new CDEcomRawRow(
                    10L, 20L, 51L,
                    name, year, season, day, data,
                    null, null, null, null, null, null, null, null, null, null, null,
                    skuStyleColor, null,
                    orderPcs, orderRub, foundPcs, foundRub, salesPcs, salesRub,
                    revenue, gp, cogs, salesDiscount,
                    planRub, stockStoresPcs, stockStoresDdp,
                    null, null, null, null, null, null, null
            );
        }
    }
}
