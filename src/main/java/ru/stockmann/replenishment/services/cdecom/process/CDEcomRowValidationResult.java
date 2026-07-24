package ru.stockmann.replenishment.services.cdecom.process;

import java.util.List;

public record CDEcomRowValidationResult(
        CDEcomStageRow stageRow,
        List<CDEcomValidationError> errors
) {
    public CDEcomRowValidationResult {
        errors = List.copyOf(errors);
        if ((stageRow == null) == errors.isEmpty()) {
            throw new IllegalArgumentException("Result must contain either a stage row or validation errors");
        }
    }

    public boolean valid() {
        return stageRow != null;
    }
}
