package ru.stockmann.replenishment.models;

public record CDEcomLoadResult(
        Long loadSessionId,
        String status,
        String message
) {

    public static CDEcomLoadResult ok(Long loadSessionId) {
        return new CDEcomLoadResult(loadSessionId, "OK", null);
    }

    public static CDEcomLoadResult error(Long loadSessionId, String message) {
        return new CDEcomLoadResult(loadSessionId, "ERROR", message);
    }
}