package ru.stockmann.replenishment.services.cddata.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

public class CDDataRowMapper {

    private static final int DECIMAL_PRECISION = 18;
    private static final int DECIMAL_SCALE = 2;

    private final DWHValueParser parser;

    public CDDataRowMapper() {
        this(new DWHValueParser());
    }

    public CDDataRowMapper(DWHValueParser parser) {
        this.parser = parser;
    }

    public CDDataTargetRow toTargetRow(CDDataRawRow row) {
        return new CDDataTargetRow(
                row.loadSessionId(),
                parser.cleanText(row.nazvanie()),
                parseInteger(row.god(), "god"),
                parseInteger(row.sezon(), "sezon"),
                parseInteger(row.den(), "den"),
                parseDate(row.data(), "data"),
                parser.cleanText(row.salesChannel()),
                parser.cleanText(row.storeRus()),
                parser.cleanText(row.mfpDivision()),
                parser.cleanText(row.mfpDepartment()),
                parser.cleanText(row.mfpSubDepartment()),
                parser.cleanText(row.skuBrandType()),
                parser.cleanText(row.skuTm()),
                parser.cleanText(row.mfpNode()),
                parser.cleanText(row.section()),
                parser.cleanText(row.merchandiseSubGroup()),
                parser.cleanText(row.campaignSales()),
                parseLong(row.skuStyleColor(), "skuStyleColor"),
                parser.cleanText(row.skuPhase()),
                parseDecimal(row.stockStartPcs(), "stockStartPcs"),
                parseDecimal(row.stockStartDd(), "stockStartDd"),
                parseDecimal(row.salesPcs(), "salesPcs"),
                parseDecimal(row.salesRub(), "salesRub"),
                parseDecimal(row.revenue(), "revenue"),
                parseDecimal(row.gp(), "gp"),
                parseDecimal(row.cogs(), "cogs"),
                parseDecimal(row.salesFrpPrice(), "salesFrpPrice"),
                parseDecimal(row.salesDiscount(), "salesDiscount"),
                parseDecimal(row.stockStoresPcs(), "stockStoresPcs"),
                parseDecimal(row.stockStoresDd(), "stockStoresDd"),
                parseInteger(row.planRub(), "planRub"),
                parser.cleanText(row.draiveryCd()),
                parser.cleanText(row.skuColorRus()),
                parser.cleanText(row.skuComposition()),
                parser.cleanText(row.skuSupplier()),
                parser.cleanText(row.skuName()),
                parser.cleanText(row.skuCollection()),
                parser.cleanText(row.skuComment())
        );
    }

    private Integer parseInteger(String value, String fieldName) {
        DWHParseResult<Integer> result = parser.parseInteger(value);
        if (!result.success()) {
            throw invalidValue(fieldName, result);
        }
        return result.value();
    }

    private Long parseLong(String value, String fieldName) {
        DWHParseResult<Long> result = parser.parseLong(value);
        if (!result.success()) {
            throw invalidValue(fieldName, result);
        }
        return result.value();
    }

    private BigDecimal parseDecimal(String value, String fieldName) {
        DWHParseResult<BigDecimal> result = parser.parseDecimal(
                value,
                DECIMAL_PRECISION,
                DECIMAL_SCALE
        );
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

        LocalDate localDate = result.value();
        return localDate == null
                ? null
                : Date.valueOf(localDate);
    }

    private IllegalStateException invalidValue(
            String fieldName,
            DWHParseResult<?> result
    ) {
        return new IllegalStateException(
                "Invalid value in field [" + fieldName + "]"
                        + ", errorCode=" + result.errorCode()
                        + ", originalValue=[" + result.originalValue() + "]: "
                        + result.errorMessage()
        );
    }
}
