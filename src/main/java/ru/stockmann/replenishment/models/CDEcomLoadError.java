package ru.stockmann.replenishment.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CD_ecom_load_error")
public class CDEcomLoadError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "LoadSessionId")
    private Long loadSessionId;

    @Column(name = "RawId")
    private Long rawId;

    @Column(name = "ErrorAt", insertable = false, updatable = false)
    private LocalDateTime errorAt;

    @Column(name = "Stage")
    private String stage;

    @Column(name = "ErrorMessage")
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public Long getLoadSessionId() {
        return loadSessionId;
    }

    public void setLoadSessionId(Long loadSessionId) {
        this.loadSessionId = loadSessionId;
    }

    public Long getRawId() {
        return rawId;
    }

    public void setRawId(Long rawId) {
        this.rawId = rawId;
    }

    public LocalDateTime getErrorAt() {
        return errorAt;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}