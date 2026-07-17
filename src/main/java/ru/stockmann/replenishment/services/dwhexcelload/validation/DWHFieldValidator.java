package ru.stockmann.replenishment.services.dwhexcelload.validation;

public class DWHFieldValidator {

    private final DWHValueParser parser;

    public DWHFieldValidator() {
        this(new DWHValueParser());
    }

    public DWHFieldValidator(DWHValueParser parser) {
        this.parser = parser;
    }

    public boolean isRequiredPresent(String value) {
        String cleaned = parser.cleanText(value);
        return cleaned != null && !parser.isSpecialNull(cleaned);
    }

    public boolean isTextLengthValid(String value, int maxLength) {
        String cleaned = parser.cleanText(value);
        return cleaned == null || cleaned.length() <= maxLength;
    }

    public boolean isInRange(Short value, int min, int max) {
        return value == null || (value >= min && value <= max);
    }
}
