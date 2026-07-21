package ru.stockmann.replenishment.services.cdecom.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

public class CDEcomRowMapper {

    private final CDEcomValueParser parser;

    public CDEcomRowMapper() {
        this(new CDEcomValueParser());
    }

    CDEcomRowMapper(CDEcomValueParser parser) {
        this.parser = parser;
    }

    public CDEcomTargetRow toTargetRow(CDEcomRawRow row) {
        return new CDEcomTargetRow(
                row.loadSessionId(),
                parser.cleanText(row.name()),
                parseInteger(row.year(), "year"),
                parseInteger(row.season(), "season"),
                parseInteger(row.day(), "day"),
                parseDate(row.data(), "data"),
                parser.cleanText(row.salesChannelBpo()),
                parser.cleanText(row.storeRus()),
                parser.cleanText(row.mfpDivision()),
                parser.cleanText(row.mfpDepartment()),
                parser.cleanText(row.mfpSubDepartment()),
                parser.cleanText(row.skuBrandType()),
                parser.cleanText(row.skuTm()),
                parser.cleanText(row.mfpNode()),
                parser.cleanText(row.section()),
                parser.cleanText(row.merchandiseSubGroup()),
                parser.cleanText(row.campaignSalesType()),
                parseRoundedLong(row.skuStyleColor(), "skuStyleColor"),
                parser.cleanText(row.skuPhase()),
                parseDecimal(row.orderPcs(), "orderPcs"),
                parseDecimal(row.orderRub(), "orderRub"),
                parseDecimal(row.foundPcs(), "foundPcs"),
                parseDecimal(row.foundRub(), "foundRub"),
                parseDecimal(row.salesPcs(), "salesPcs"),
                parseDecimal(row.salesRub(), "salesRub"),
                parseDecimal(row.revenue(), "revenue"),
                parseDecimal(row.gp(), "gp"),
                parseDecimal(row.cogs(), "cogs"),
                parseDecimal(row.salesDiscount(), "salesDiscount"),
                parseDirectLong(row.planRub(), "planRub"),
                parseDirectLong(row.stockStoresPcs(), "stockStoresPcs"),
                parseDirectLong(row.stockStoresDdp(), "stockStoresDdp"),
                parser.cleanText(row.cdDrivers()),
                parser.cleanText(row.skuSupplierModel()),
                parser.cleanText(row.skuComposition()),
                parser.cleanText(row.skuColorRussian()),
                parser.cleanText(row.skuName()),
                parser.cleanText(row.skuCommentBuyer()),
                parser.cleanText(row.skuCollection())
        );
    }

    private Integer parseInteger(String value, String fieldName) {
        DWHParseResult<Integer> result = parser.parseInteger(value);
        if (!result.success()) {
            throw invalidValue(fieldName, result);
        }
        return result.value();
    }

    private Date parseDate(String value, String fieldName) {
        DWHParseResult<LocalDate> result = parser.parseDate(value);
        if (!result.success()) {
            throw invalidValue(fieldName, result);
        }
        return result.value() == null ? null : Date.valueOf(result.value());
    }

    private Long parseRoundedLong(String value, String fieldName) {
        DWHParseResult<Long> result = parser.parseRoundedLong(value);
        if (!result.success()) {
            throw invalidValue(fieldName, result);
        }
        return result.value();
    }

    private BigDecimal parseDecimal(String value, String fieldName) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(value);
        if (!result.success()) {
            throw invalidValue(fieldName, result);
        }
        return result.value();
    }

    private Long parseDirectLong(String value, String fieldName) {
        DWHParseResult<Long> result = parser.parseDirectLong(value);
        if (!result.success()) {
            throw invalidValue(fieldName, result);
        }
        return result.value();
    }

    private IllegalStateException invalidValue(String fieldName, DWHParseResult<?> result) {
        return new IllegalStateException(
                "Invalid value in field [" + fieldName + "]"
                        + ", errorCode=" + result.errorCode()
                        + ", originalValue=[" + result.originalValue() + "]: "
                        + result.errorMessage()
        );
    }
}
