package ru.stockmann.replenishment.models;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Weekly_data_raw")
public class WeeklyDataRaw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String year21;
    private String week21;

    private String yearCorr;
    private String weekCorr;

    private String year;
    private String week;

    private String salesChannelBpo;
    private String storeRusBpo;
    private String storeRus;

    private String mfpDivisionNew;
    private String mfpDepartment;

    private String skuSeasonBudget;
    private String typeOfSales;

    private String totalStockPcs;
    private String totalStockDdp;

    private String salesPcs;
    private String salesRub;

    private String revenue;
    private String gp;
    private String discountTotalRub;

    private String mfpDivision;
    private String season;
    private String month;
    private String bundle;
    private String seasonality;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getYear21() {
        return year21;
    }

    public void setYear21(String year21) {
        this.year21 = year21;
    }

    public String getWeek21() {
        return week21;
    }

    public void setWeek21(String week21) {
        this.week21 = week21;
    }

    public String getYearCorr() {
        return yearCorr;
    }

    public void setYearCorr(String yearCorr) {
        this.yearCorr = yearCorr;
    }

    public String getWeekCorr() {
        return weekCorr;
    }

    public void setWeekCorr(String weekCorr) {
        this.weekCorr = weekCorr;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getWeek() {
        return week;
    }

    public void setWeek(String week) {
        this.week = week;
    }

    public String getSalesChannelBpo() {
        return salesChannelBpo;
    }

    public void setSalesChannelBpo(String salesChannelBpo) {
        this.salesChannelBpo = salesChannelBpo;
    }

    public String getStoreRusBpo() {
        return storeRusBpo;
    }

    public void setStoreRusBpo(String storeRusBpo) {
        this.storeRusBpo = storeRusBpo;
    }

    public String getStoreRus() {
        return storeRus;
    }

    public void setStoreRus(String storeRus) {
        this.storeRus = storeRus;
    }

    public String getMfpDivisionNew() {
        return mfpDivisionNew;
    }

    public void setMfpDivisionNew(String mfpDivisionNew) {
        this.mfpDivisionNew = mfpDivisionNew;
    }

    public String getMfpDepartment() {
        return mfpDepartment;
    }

    public void setMfpDepartment(String mfpDepartment) {
        this.mfpDepartment = mfpDepartment;
    }

    public String getSkuSeasonBudget() {
        return skuSeasonBudget;
    }

    public void setSkuSeasonBudget(String skuSeasonBudget) {
        this.skuSeasonBudget = skuSeasonBudget;
    }

    public String getTypeOfSales() {
        return typeOfSales;
    }

    public void setTypeOfSales(String typeOfSales) {
        this.typeOfSales = typeOfSales;
    }

    public String getTotalStockPcs() {
        return totalStockPcs;
    }

    public void setTotalStockPcs(String totalStockPcs) {
        this.totalStockPcs = totalStockPcs;
    }

    public String getTotalStockDdp() {
        return totalStockDdp;
    }

    public void setTotalStockDdp(String totalStockDdp) {
        this.totalStockDdp = totalStockDdp;
    }

    public String getSalesPcs() {
        return salesPcs;
    }

    public void setSalesPcs(String salesPcs) {
        this.salesPcs = salesPcs;
    }

    public String getSalesRub() {
        return salesRub;
    }

    public void setSalesRub(String salesRub) {
        this.salesRub = salesRub;
    }

    public String getRevenue() {
        return revenue;
    }

    public void setRevenue(String revenue) {
        this.revenue = revenue;
    }

    public String getGp() {
        return gp;
    }

    public void setGp(String gp) {
        this.gp = gp;
    }

    public String getDiscountTotalRub() {
        return discountTotalRub;
    }

    public void setDiscountTotalRub(String discountTotalRub) {
        this.discountTotalRub = discountTotalRub;
    }

    public String getMfpDivision() {
        return mfpDivision;
    }

    public void setMfpDivision(String mfpDivision) {
        this.mfpDivision = mfpDivision;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getSeasonality() {
        return seasonality;
    }

    public void setSeasonality(String seasonality) {
        this.seasonality = seasonality;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
