package ru.stockmann.replenishment.services.salesbychannel.process;

import java.util.List;

public record SalesByChannelRowValidationResult(
        SalesByChannelStageRow stageRow,
        List<SalesByChannelValidationError> errors
) {
    public boolean valid() {
        return errors == null || errors.isEmpty();
    }
}
