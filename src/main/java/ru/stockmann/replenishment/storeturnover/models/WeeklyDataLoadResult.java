package ru.stockmann.replenishment.storeturnover.models;

public record WeeklyDataLoadResult(Long loadSessionId, String status, String message) {

    public static WeeklyDataLoadResult ok(Long loadSessionId) {
        return new WeeklyDataLoadResult(loadSessionId, "OK", null);
    }

    public static WeeklyDataLoadResult error(Long loadSessionId, String message) {
        return new WeeklyDataLoadResult(loadSessionId, "ERROR", message);
    }
}