package ru.stockmann.replenishment.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CD_ecom_load_session")
public class CDEcomLoadSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "FileName")
    private String fileName;

    @Column(name = "FilePath")
    private String filePath;

    @Column(name = "Status")
    private String status;

    @Column(name = "Message")
    private String message;

    @Column(name = "StartedAt", insertable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "FinishedAt")
    private LocalDateTime finishedAt;

    @Column(name = "RowsTotal")
    private Integer rowsTotal;

    @Column(name = "RowsLoaded")
    private Integer rowsLoaded;

    @Column(name = "RowsWithError")
    private Integer rowsWithError;

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getRowsTotal() {
        return rowsTotal;
    }

    public void setRowsTotal(Integer rowsTotal) {
        this.rowsTotal = rowsTotal;
    }

    public Integer getRowsLoaded() {
        return rowsLoaded;
    }

    public void setRowsLoaded(Integer rowsLoaded) {
        this.rowsLoaded = rowsLoaded;
    }

    public Integer getRowsWithError() {
        return rowsWithError;
    }

    public void setRowsWithError(Integer rowsWithError) {
        this.rowsWithError = rowsWithError;
    }
}