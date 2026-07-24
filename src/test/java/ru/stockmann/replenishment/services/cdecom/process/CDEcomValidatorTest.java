package ru.stockmann.replenishment.services.cdecom.process;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;

import java.time.LocalDate;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomValidatorTest {

    private final CDEcomValidator validator = new CDEcomValidator();

    @Test
    void emptyRowIsValid() {
        assertTrue(validator.validate(row().build()).valid());
    }

    @Test
    void fullyValidRowIsValid() {
        assertTrue(validator.validate(validRow()).valid());
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
        return new RowBuilder();
    }

    private static String normalizeExcelDate(String value) {
        return new CDEcomExcelLoadDefinition().columns().get(4).normalizer().normalize(value);
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
        private RowBuilder withPlanRub(String value) { this.planRub = value; return this; }
        private RowBuilder withStockStoresPcs(String value) { this.stockStoresPcs = value; return this; }
        private RowBuilder withStockStoresDdp(String value) { this.stockStoresDdp = value; return this; }

        private CDEcomRawRow build() {
            return new CDEcomRawRow(
                    10L, 20L, 51L,
                    name, year, season, day, data,
                    null, null, null, null, null, null, null, null, null, null, null,
                    skuStyleColor, null,
                    orderPcs, null, null, null, null, null, null, null, null, null,
                    planRub, stockStoresPcs, stockStoresDdp,
                    null, null, null, null, null, null, null
            );
        }
    }
}
