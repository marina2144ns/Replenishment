package ru.stockmann.replenishment.services.cddata.process;

import java.util.List;

public record CDDataValidationResult(
        CDDataRawRow row,
        List<CDDataValidationError> errors
) {

    public CDDataValidationResult {
        errors = List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
