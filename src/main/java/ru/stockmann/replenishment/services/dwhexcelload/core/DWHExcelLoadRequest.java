package ru.stockmann.replenishment.services.dwhexcelload.core;

public class DWHExcelLoadRequest {

    private String filePath;
    private String requestedBy;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = DWHRequestedBy.normalizeAndValidate(requestedBy);
    }
}
