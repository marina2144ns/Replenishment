package ru.stockmann.replenishment.storeturnover.models;

public class ABCDataLoadRequest {
    // Полный путь к CSV; пример: C:\\data\\abc.csv
    private String filePath;
    // "3U","3R","6U","6R","12U","12R"
    private String month;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
}
