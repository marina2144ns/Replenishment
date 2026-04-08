package ru.stockmann.replenishment.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Weekly_data")
public class WeeklyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Short year21;
    private Short week21;

    private Short yearCorr;
    private Short weekCorr;

    private Short year;
    private Short week;

    private String salesChannelBpo;
    private String storeRusBpo;
    private String storeRus;

    private String mfpDivisionNew;
    private String mfpDepartment;

    private String skuSeasonBudget;
    private String typeOfSales;

    private Integer totalStockPcs;
    private Integer totalStockDdp;

    private Integer salesPcs;
    private Integer salesRub;

    private Integer revenue;
    private Integer gp;
    private Integer discountTotalRub;

    private String mfpDivision;
    private String season;
    private String month;
    private String bundle;
    private String seasonality;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public Short getYear21() {
        return year21;
    }

    public void setYear21(Short year21) {
        this.year21 = year21;
    }

    public Short getWeek21() {
        return week21;
    }

    public void setWeek21(Short week21) {
        this.week21 = week21;
    }

    public Short getYearCorr() {
        return yearCorr;
    }

    public void setYearCorr(Short yearCorr) {
        this.yearCorr = yearCorr;
    }

    public Short getWeekCorr() {
        return weekCorr;
    }

    public void setWeekCorr(Short weekCorr) {
        this.weekCorr = weekCorr;
    }

    public Short getYear() {
        return year;
    }

    public void setYear(Short year) {
        this.year = year;
    }

    public Short getWeek() {
        return week;
    }

    public void setWeek(Short week) {
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

    public Integer getTotalStockPcs() {
        return totalStockPcs;
    }

    public void setTotalStockPcs(Integer totalStockPcs) {
        this.totalStockPcs = totalStockPcs;
    }

    public Integer getTotalStockDdp() {
        return totalStockDdp;
    }

    public void setTotalStockDdp(Integer totalStockDdp) {
        this.totalStockDdp = totalStockDdp;
    }

    public Integer getSalesPcs() {
        return salesPcs;
    }

    public void setSalesPcs(Integer salesPcs) {
        this.salesPcs = salesPcs;
    }

    public Integer getSalesRub() {
        return salesRub;
    }

    public void setSalesRub(Integer salesRub) {
        this.salesRub = salesRub;
    }

    public Integer getRevenue() {
        return revenue;
    }

    public void setRevenue(Integer revenue) {
        this.revenue = revenue;
    }

    public Integer getGp() {
        return gp;
    }

    public void setGp(Integer gp) {
        this.gp = gp;
    }

    public Integer getDiscountTotalRub() {
        return discountTotalRub;
    }

    public void setDiscountTotalRub(Integer discountTotalRub) {
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
