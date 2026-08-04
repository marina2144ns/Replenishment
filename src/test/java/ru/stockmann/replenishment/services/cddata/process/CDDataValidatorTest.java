package ru.stockmann.replenishment.services.cddata.process;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDDataValidatorTest {

    private final CDDataValidator validator = new CDDataValidator();

    @Test
    void fullyEmptyRowIsValid() {
        CDDataValidationResult result = validator.validate(emptyRow());

        assertTrue(result.valid());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void validRowIsValid() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .god("2025")
                .sezon("1")
                .den("31")
                .data("2025-01-31")
                .skuStyleColor("9223372036854775807")
                .planRub("123")
                .stockStartPcs("12.345")
                .stockStartDd("1 234,56")
                .salesPcs("0")
                .salesRub("100")
                .revenue("200.10")
                .gp("-12.34")
                .cogs("5")
                .salesFrpPrice("99.99")
                .salesDiscount("10")
                .stockStoresPcs("3")
                .stockStoresDd("4")
                .nazvanie("Name")
                .build());

        assertTrue(result.valid());
    }

    @Test
    void multipleInvalidFieldsReturnMultipleErrors() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .god("12.5")
                .data("31.02.2025")
                .salesPcs("abc")
                .nazvanie("a".repeat(256))
                .build());

        assertEquals(4, result.errors().size());
        assertError(result.errors().get(0), "god", "INVALID_INTEGER");
        assertError(result.errors().get(1), "data", "INVALID_DATE");
        assertError(result.errors().get(2), "salesPcs", "INVALID_DECIMAL");
        assertError(result.errors().get(3), "nazvanie", "TEXT_TOO_LONG");
    }

    @Test
    void errorsFollowStableSqlFieldOrder() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .skuComment("a".repeat(256))
                .god("bad")
                .stockStoresDd("bad")
                .skuStyleColor("bad")
                .salesChannel("a".repeat(256))
                .build());

        assertEquals(List.of(
                "god",
                "skuStyleColor",
                "stockStoresDd",
                "salesChannel",
                "skuComment"
        ), result.errors().stream().map(CDDataValidationError::fieldName).toList());
    }

    @Test
    void fractionalIntegerIsInvalidInteger() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .god("12.5")
                .build());

        assertSingleError(result, "god", "INVALID_INTEGER");
    }

    @Test
    void integerOverflowKeepsParserReason() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .god("2147483648")
                .build());

        CDDataValidationError error = singleError(result);
        assertError(error, "god", "NUMERIC_OVERFLOW");
        assertTrue(error.errorReason().contains("out of range"));
        assertTrue(error.errorMessage().contains("god"));
        assertTrue(error.errorMessage().contains("2147483648"));
        assertTrue(error.errorMessage().contains(error.errorReason()));
    }

    @Test
    void skuStyleColorOutsideBigintIsNumericOverflow() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .skuStyleColor("9223372036854775808")
                .build());

        assertSingleError(result, "skuStyleColor", "NUMERIC_OVERFLOW");
    }

    @Test
    void decimalWithRoundingIsValid() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .stockStartPcs("12.345")
                .build());

        assertTrue(result.valid());
    }

    @Test
    void decimalOutsidePrecisionAfterRoundingIsNumericOverflow() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .stockStartPcs("9999999999999999.995")
                .build());

        assertSingleError(result, "stockStartPcs", "NUMERIC_OVERFLOW");
    }

    @Test
    void invalidCalendarDateIsInvalidDate() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .data("31.02.2025")
                .build());

        assertSingleError(result, "data", "INVALID_DATE");
    }

    @Test
    void americanDateIsInvalidDate() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .data("12/25/2025")
                .build());

        assertSingleError(result, "data", "INVALID_DATE");
    }

    @Test
    void dateOutOfRangeKeepsParserCodeAndReason() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .data("60001")
                .build());

        CDDataValidationError error = singleError(result);
        assertError(error, "data", "DATE_OUT_OF_RANGE");
        assertTrue(error.errorReason().contains("out of range"));
        assertTrue(error.errorMessage().contains("data"));
        assertTrue(error.errorMessage().contains("60001"));
        assertTrue(error.errorMessage().contains(error.errorReason()));
    }

    @Test
    void excelSerialDateIsValid() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .data("61")
                .build());

        assertTrue(result.valid());
    }

    @Test
    void specialNullIsValidForNullableField() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .god("N/A")
                .stockStartPcs("-")
                .data("NULL")
                .build());

        assertTrue(result.valid());
    }

    @Test
    void textLength255IsValid() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .nazvanie("a".repeat(255))
                .build());

        assertTrue(result.valid());
    }

    @Test
    void textLength255IsValidForAllTextFields() {
        for (String fieldName : textFields()) {
            CDDataValidationResult result = validator.validate(rowWithTextField(fieldName, "a".repeat(255)));

            assertTrue(
                    result.errors().stream().noneMatch(error -> fieldName.equals(error.fieldName())),
                    "Expected no text length error for " + fieldName + ", actual errors: " + result.errors()
            );
        }
    }

    @Test
    void textLength256ReturnsTextTooLong() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .nazvanie("a".repeat(256))
                .build());

        assertSingleError(result, "nazvanie", "TEXT_TOO_LONG");
    }

    @Test
    void textLength4000ReturnsSafeTextTooLongMessage() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .nazvanie("a".repeat(4000))
                .build());

        CDDataValidationError error = singleError(result);
        assertError(error, "nazvanie", "TEXT_TOO_LONG");
        assertTrue(error.errorReason().length() <= 500);
        assertTrue(error.errorMessage().length() <= 4000);
        assertTrue(error.errorMessage().contains("RawId=10"));
        assertTrue(error.errorMessage().contains("nazvanie"));
        assertTrue(error.errorMessage().contains("max length 255"));
    }

    @Test
    void textLength256ReturnsTextTooLongForAllTextFieldsAndKeepsExcelRowNum() {
        for (String fieldName : textFields()) {
            CDDataValidationResult result = validator.validate(rowWithTextField(fieldName, "a".repeat(256)));

            CDDataValidationError error = result.errors().stream()
                    .filter(e -> fieldName.equals(e.fieldName()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("TEXT_TOO_LONG", error.errorCode(), fieldName);
            assertEquals(30L, error.excelRowNum(), fieldName);
        }
    }

    @Test
    void oneColumnCreatesAtMostOneError() {
        CDDataValidationResult result = validator.validate(rowBuilder()
                .nazvanie("a".repeat(256))
                .build());

        assertEquals(1, result.errors().size());
        assertSingleError(result, "nazvanie", "TEXT_TOO_LONG");
    }

    @Test
    void validateAndMapParsesEachTypedFieldOnceAndReusesValues() {
        CountingParser parser = new CountingParser();
        CDDataValidator typedValidator = new CDDataValidator(parser);
        CDDataRawRow row = rowBuilder()
                .god("2025")
                .sezon("1")
                .den("31")
                .data("2025-01-31")
                .skuStyleColor("123")
                .planRub("100")
                .stockStartPcs("1.255")
                .stockStartDd("2")
                .salesPcs("3")
                .salesRub("4")
                .revenue("5")
                .gp("6")
                .cogs("7")
                .salesFrpPrice("8")
                .salesDiscount("9")
                .stockStoresPcs("10")
                .stockStoresDd("11")
                .build();

        CDDataRowValidationResult result = typedValidator.validateAndMap(row);
        assertEquals(10L, result.stageRow().rawRowId());

        assertTrue(result.valid());
        assertEquals(4, parser.integerCalls);
        assertEquals(1, parser.longCalls);
        assertEquals(11, parser.decimalCalls);
        assertEquals(1, parser.dateCalls);
        assertEquals(2025, result.stageRow().god());
        assertEquals(LocalDate.of(2025, 1, 31), result.stageRow().data().toLocalDate());
        assertEquals(new BigDecimal("1.26"), result.stageRow().stockStartPcs());
        assertEquals(30L, result.stageRow().excelRowNum());
    }

    private static CDDataRawRow emptyRow() {
        return rowBuilder().build();
    }

    private static final class CountingParser extends DWHValueParser {
        private int integerCalls;
        private int longCalls;
        private int decimalCalls;
        private int dateCalls;

        @Override
        public DWHParseResult<Integer> parseInteger(String value) {
            integerCalls++;
            return super.parseInteger(value);
        }

        @Override
        public DWHParseResult<Long> parseLong(String value) {
            longCalls++;
            return super.parseLong(value);
        }

        @Override
        public DWHParseResult<BigDecimal> parseDecimal(String value, int precision, int scale) {
            decimalCalls++;
            return super.parseDecimal(value, precision, scale);
        }

        @Override
        public DWHParseResult<LocalDate> parseDate(String value) {
            dateCalls++;
            return super.parseDate(value);
        }
    }

    private static CDDataRawRowBuilder rowBuilder() {
        return new CDDataRawRowBuilder();
    }

    private static List<String> textFields() {
        return List.of(
                "nazvanie",
                "salesChannel",
                "storeRus",
                "mfpDivision",
                "mfpDepartment",
                "mfpSubDepartment",
                "skuBrandType",
                "skuTm",
                "mfpNode",
                "section",
                "merchandiseSubGroup",
                "campaignSales",
                "skuPhase",
                "draiveryCd",
                "skuColorRus",
                "skuComposition",
                "skuSupplier",
                "skuName",
                "skuCollection",
                "skuComment"
        );
    }

    private static CDDataRawRow rowWithTextField(String fieldName, String value) {
        return new CDDataRawRow(
                10L,
                20L,
                30L,
                "nazvanie".equals(fieldName) ? value : null,
                null,
                null,
                null,
                null,
                "salesChannel".equals(fieldName) ? value : null,
                "storeRus".equals(fieldName) ? value : null,
                "mfpDivision".equals(fieldName) ? value : null,
                "mfpDepartment".equals(fieldName) ? value : null,
                "mfpSubDepartment".equals(fieldName) ? value : null,
                "skuBrandType".equals(fieldName) ? value : null,
                "skuTm".equals(fieldName) ? value : null,
                "mfpNode".equals(fieldName) ? value : null,
                "section".equals(fieldName) ? value : null,
                "merchandiseSubGroup".equals(fieldName) ? value : null,
                "campaignSales".equals(fieldName) ? value : null,
                null,
                "skuPhase".equals(fieldName) ? value : null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "draiveryCd".equals(fieldName) ? value : null,
                "skuColorRus".equals(fieldName) ? value : null,
                "skuComposition".equals(fieldName) ? value : null,
                "skuSupplier".equals(fieldName) ? value : null,
                "skuName".equals(fieldName) ? value : null,
                "skuCollection".equals(fieldName) ? value : null,
                "skuComment".equals(fieldName) ? value : null
        );
    }

    private static void assertSingleError(
            CDDataValidationResult result,
            String fieldName,
            String errorCode
    ) {
        assertError(singleError(result), fieldName, errorCode);
    }

    private static CDDataValidationError singleError(CDDataValidationResult result) {
        assertEquals(1, result.errors().size());
        return result.errors().get(0);
    }

    private static void assertError(
            CDDataValidationError error,
            String fieldName,
            String errorCode
    ) {
        assertEquals(fieldName, error.fieldName());
        assertEquals(errorCode, error.errorCode());
    }

    private static final class CDDataRawRowBuilder {
        private String nazvanie;
        private String god;
        private String sezon;
        private String den;
        private String data;
        private String salesChannel;
        private String storeRus;
        private String mfpDivision;
        private String mfpDepartment;
        private String mfpSubDepartment;
        private String skuBrandType;
        private String skuTm;
        private String mfpNode;
        private String section;
        private String merchandiseSubGroup;
        private String campaignSales;
        private String skuStyleColor;
        private String skuPhase;
        private String stockStartPcs;
        private String stockStartDd;
        private String salesPcs;
        private String salesRub;
        private String revenue;
        private String gp;
        private String cogs;
        private String salesFrpPrice;
        private String salesDiscount;
        private String stockStoresPcs;
        private String stockStoresDd;
        private String planRub;
        private String draiveryCd;
        private String skuColorRus;
        private String skuComposition;
        private String skuSupplier;
        private String skuName;
        private String skuCollection;
        private String skuComment;

        CDDataRawRowBuilder nazvanie(String value) {
            this.nazvanie = value;
            return this;
        }

        CDDataRawRowBuilder god(String value) {
            this.god = value;
            return this;
        }

        CDDataRawRowBuilder sezon(String value) {
            this.sezon = value;
            return this;
        }

        CDDataRawRowBuilder den(String value) {
            this.den = value;
            return this;
        }

        CDDataRawRowBuilder data(String value) {
            this.data = value;
            return this;
        }

        CDDataRawRowBuilder salesChannel(String value) {
            this.salesChannel = value;
            return this;
        }

        CDDataRawRowBuilder skuStyleColor(String value) {
            this.skuStyleColor = value;
            return this;
        }

        CDDataRawRowBuilder stockStartPcs(String value) {
            this.stockStartPcs = value;
            return this;
        }

        CDDataRawRowBuilder stockStartDd(String value) {
            this.stockStartDd = value;
            return this;
        }

        CDDataRawRowBuilder salesPcs(String value) {
            this.salesPcs = value;
            return this;
        }

        CDDataRawRowBuilder salesRub(String value) {
            this.salesRub = value;
            return this;
        }

        CDDataRawRowBuilder revenue(String value) {
            this.revenue = value;
            return this;
        }

        CDDataRawRowBuilder gp(String value) {
            this.gp = value;
            return this;
        }

        CDDataRawRowBuilder cogs(String value) {
            this.cogs = value;
            return this;
        }

        CDDataRawRowBuilder salesFrpPrice(String value) {
            this.salesFrpPrice = value;
            return this;
        }

        CDDataRawRowBuilder salesDiscount(String value) {
            this.salesDiscount = value;
            return this;
        }

        CDDataRawRowBuilder stockStoresPcs(String value) {
            this.stockStoresPcs = value;
            return this;
        }

        CDDataRawRowBuilder stockStoresDd(String value) {
            this.stockStoresDd = value;
            return this;
        }

        CDDataRawRowBuilder planRub(String value) {
            this.planRub = value;
            return this;
        }

        CDDataRawRowBuilder skuComment(String value) {
            this.skuComment = value;
            return this;
        }

        CDDataRawRow build() {
            return new CDDataRawRow(
                    10L,
                    20L,
                    30L,
                    nazvanie,
                    god,
                    sezon,
                    den,
                    data,
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
                    skuStyleColor,
                    skuPhase,
                    stockStartPcs,
                    stockStartDd,
                    salesPcs,
                    salesRub,
                    revenue,
                    gp,
                    cogs,
                    salesFrpPrice,
                    salesDiscount,
                    stockStoresPcs,
                    stockStoresDd,
                    planRub,
                    draiveryCd,
                    skuColorRus,
                    skuComposition,
                    skuSupplier,
                    skuName,
                    skuCollection,
                    skuComment
            );
        }
    }
}
