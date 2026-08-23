package ru.stockmann.replenishment.services.storeturnover.process;

import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHParseResult;
import ru.stockmann.replenishment.services.dwhexcelload.validation.DWHValueParser;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StoreTurnoverValidator {
    private static final DateTimeFormatter PERIOD =
            DateTimeFormatter.ofPattern("MM.uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);
    private final DWHValueParser parser = new DWHValueParser();

    public StoreTurnoverRowValidationResult validateAndMap(StoreTurnoverRawRow row) {
        List<StoreTurnoverValidationError> errors = new ArrayList<>();
        String sku = requiredText(errors, row, "sku", row.sku(), 255);
        LocalDate period = period(errors, row, row.period());
        String storeRus = optionalText(errors, row, "storeRus", row.storeRus(), 255);
        Integer remainingSum = integer(errors, row, "remainingSum", row.remainingSum());
        Integer remainingDays = integer(errors, row, "remainingDays", row.remainingDays());
        Integer salesQuantity = integer(errors, row, "salesQuantity", row.salesQuantity());
        Integer sales = integer(errors, row, "sales", row.sales());
        Integer asp = integer(errors, row, "asp", row.asp());
        Integer revenue = integer(errors, row, "revenue", row.revenue());
        Integer gp = integer(errors, row, "gp", row.gp());
        Integer discountTotal = integer(errors, row, "discountTotal", row.discountTotal());
        if (!errors.isEmpty()) return new StoreTurnoverRowValidationResult(null, errors);
        return new StoreTurnoverRowValidationResult(new StoreTurnoverStageRow(
                row.loadSessionId(), row.excelRowNum(), sku, period, storeRus,
                remainingSum, remainingDays, salesQuantity, sales, asp, revenue, gp,
                discountTotal, row.id()), List.of());
    }

    private String requiredText(List<StoreTurnoverValidationError> errors, StoreTurnoverRawRow row,
                                String field, String raw, int maxLength) {
        String value = parser.cleanText(raw);
        if (value == null || parser.isSpecialNull(value)) {
            value = null;
            errors.add(error(row, field, "REQUIRED_FIELD_EMPTY", "Required value is empty"));
        }
        else if (value.length() > maxLength) errors.add(error(row, field, "TEXT_TOO_LONG", "Value exceeds max length " + maxLength));
        return value;
    }

    private String optionalText(List<StoreTurnoverValidationError> errors, StoreTurnoverRawRow row,
                                String field, String raw, int maxLength) {
        String value = parser.cleanText(raw);
        if (value == null || parser.isSpecialNull(value)) {
            return null;
        }
        if (value.length() > maxLength) {
            errors.add(error(row, field, "TEXT_TOO_LONG", "Value exceeds max length " + maxLength));
        }
        return value;
    }

    private LocalDate period(List<StoreTurnoverValidationError> errors, StoreTurnoverRawRow row, String raw) {
        String value = parser.cleanText(raw);
        if (value == null || parser.isSpecialNull(value)) {
            errors.add(error(row, "period", "REQUIRED_FIELD_EMPTY", "Required value is empty"));
            return null;
        }
        try {
            return YearMonth.parse(value, PERIOD).atDay(1);
        } catch (DateTimeParseException e) {
            errors.add(error(row, "period", "INVALID_DATE", "Expected period format MM.yyyy"));
            return null;
        }
    }

    private Integer integer(List<StoreTurnoverValidationError> errors, StoreTurnoverRawRow row,
                            String field, String raw) {
        if (parser.cleanText(raw) == null) return 0;
        DWHParseResult<Integer> parsed = parser.parseInteger(raw);
        if (!parsed.success()) {
            errors.add(error(row, field, parsed.errorCode(), "Invalid INTEGER value"));
            return null;
        }
        return parsed.value() == null ? 0 : parsed.value();
    }

    private StoreTurnoverValidationError error(StoreTurnoverRawRow row, String field,
                                               String code, String reason) {
        return new StoreTurnoverValidationError(row.loadSessionId(), row.id(), row.excelRowNum(),
                "VALIDATION", field, code, reason,
                "RawId=" + row.id() + ". " + reason + " in field [" + field + "].");
    }
}
