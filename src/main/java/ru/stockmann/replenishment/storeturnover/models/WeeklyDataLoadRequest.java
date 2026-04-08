package ru.stockmann.replenishment.storeturnover.models;

public class WeeklyDataLoadRequest {

    // Полный путь к Excel; пример: C:\\data\\weekly_data.xslx
    private String filePath;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}