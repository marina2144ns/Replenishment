package ru.stockmann.replenishment.models;

public class CDDataLoadRequest {

    // Полный путь к Excel; пример: C:\\data\\cd_data.xlsx
    private String filePath;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}