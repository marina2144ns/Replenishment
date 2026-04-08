package ru.stockmann.replenishment.models;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "ABCData")
public class ABCData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String skuTM;

    @Column(length = 150)
    private String section;

    @Column(length = 150)
    private String mfpDepartment;

    @Column(length = 150)
    private String merchandiseSubGroup;

    @Column(length = 50)
    private String skuItem;

    private int salesCurr;
    private int accumPercent;

    @Column(length = 1)
    private String abc;

    @Column(length = 1)
    private String abcno3_Units;
    @Column(length = 1)
    private String abcno3_Rev;
    @Column(length = 1)
    private String abcno6_Units;
    @Column(length = 1)
    private String abcno6_Rev;
    @Column(length = 1)
    private String abcno12_Units;
    @Column(length = 1)
    private String abcno12_Rev;

    private LocalDate supplyDate;

    @Column(columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDate loadDateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSkuTM() {
        return skuTM;
    }

    public void setSkuTM(String skuTM) {
        this.skuTM = skuTM;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getMfpDepartment() {
        return mfpDepartment;
    }

    public void setMfpDepartment(String mfpDepartment) {
        this.mfpDepartment = mfpDepartment;
    }

    public String getMerchandiseSubGroup() {
        return merchandiseSubGroup;
    }

    public void setMerchandiseSubGroup(String merchandiseSubGroup) {
        this.merchandiseSubGroup = merchandiseSubGroup;
    }

    public String getSkuItem() {
        return skuItem;
    }

    public void setSkuItem(String skuItem) {
        this.skuItem = skuItem;
    }

    public int getSalesCurr() {
        return salesCurr;
    }

    public void setSalesCurr(int salesCurr) {
        this.salesCurr = salesCurr;
    }

    public int getAccumPercent() {
        return accumPercent;
    }

    public void setAccumPercent(int accumPercent) {
        this.accumPercent = accumPercent;
    }

    public String getAbc() {
        return abc;
    }

    public void setAbc(String abc) {
        this.abc = abc;
    }

    public String getAbcno3_Units() {
        return abcno3_Units;
    }

    public void setAbcno3_Units(String abcno3_Units) {
        this.abcno3_Units = abcno3_Units;
    }

    public String getAbcno3_Rev() {
        return abcno3_Rev;
    }

    public void setAbcno3_Rev(String abcno3_Rev) {
        this.abcno3_Rev = abcno3_Rev;
    }

    public String getAbcno6_Units() {
        return abcno6_Units;
    }

    public void setAbcno6_Units(String abcno6_Units) {
        this.abcno6_Units = abcno6_Units;
    }

    public String getAbcno6_Rev() {
        return abcno6_Rev;
    }

    public void setAbcno6_Rev(String abcno6_Rev) {
        this.abcno6_Rev = abcno6_Rev;
    }

    public String getAbcno12_Units() {
        return abcno12_Units;
    }

    public void setAbcno12_Units(String abcno12_Units) {
        this.abcno12_Units = abcno12_Units;
    }

    public String getAbcno12_Rev() {
        return abcno12_Rev;
    }

    public void setAbcno12_Rev(String abcno12_Rev) {
        this.abcno12_Rev = abcno12_Rev;
    }

    public LocalDate getSupplyDate() {
        return supplyDate;
    }

    public void setSupplyDate(LocalDate supplyDate) {
        this.supplyDate = supplyDate;
    }

    public LocalDate getLoadDateTime() {
        return loadDateTime;
    }

    public void setLoadDateTime(LocalDate loadDateTime) {
        this.loadDateTime = loadDateTime;
    }

}
