package ru.stockmann.replenishment.services.cdecom.process;

import java.util.List;

public record CDEcomValidationResult(
        CDEcomRawRow row,
        List<CDEcomValidationError> errors
) {

    public CDEcomValidationResult {
        errors = List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
