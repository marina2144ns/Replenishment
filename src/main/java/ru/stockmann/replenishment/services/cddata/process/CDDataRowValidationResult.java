package ru.stockmann.replenishment.services.cddata.process;

import java.util.List;

public record CDDataRowValidationResult(
        CDDataStageRow stageRow,
        List<CDDataValidationError> errors
) {

    public CDDataRowValidationResult {
        errors = List.copyOf(errors);
        if ((stageRow == null) == errors.isEmpty()) {
            throw new IllegalArgumentException("Result must contain either a stage row or validation errors");
        }
    }

    public boolean valid() {
        return stageRow != null;
    }
}
