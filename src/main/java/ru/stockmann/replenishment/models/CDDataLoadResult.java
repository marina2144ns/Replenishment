package ru.stockmann.replenishment.models;

public record CDDataLoadResult(Long loadSessionId, String status, String message) {

    public static CDDataLoadResult ok(Long loadSessionId) {
        return new CDDataLoadResult(loadSessionId, "OK", null);
    }

    public static CDDataLoadResult error(Long loadSessionId, String message) {
        return new CDDataLoadResult(loadSessionId, "ERROR", message);
    }
}