package ru.stockmann.replenishment.services.storeturnover.process;

import java.util.List;

public record StoreTurnoverRowValidationResult(
        StoreTurnoverStageRow stageRow, List<StoreTurnoverValidationError> errors
) {
    public boolean valid() { return errors == null || errors.isEmpty(); }
}
