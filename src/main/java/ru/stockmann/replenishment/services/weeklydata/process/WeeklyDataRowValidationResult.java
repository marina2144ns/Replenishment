package ru.stockmann.replenishment.services.weeklydata.process;

import java.util.List;

public record WeeklyDataRowValidationResult(
        WeeklyDataStageRow stageRow,
        List<WeeklyDataValidationError> errors
) {

    public WeeklyDataRowValidationResult {
        errors = List.copyOf(errors);
        if ((stageRow == null) == errors.isEmpty()) {
            throw new IllegalArgumentException("Result must contain either a stage row or validation errors");
        }
    }

    public boolean valid() {
        return stageRow != null;
    }
}
