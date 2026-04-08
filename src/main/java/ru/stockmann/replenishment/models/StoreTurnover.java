package ru.stockmann.replenishment.models;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "StoreTurnover")
public class StoreTurnover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;
    private LocalDate period;
    private String storeRus;
    private int remainingSum;
    private int remainingDays;
    private int salesQuantity;
    private int sales;
    private int asp;
    private int revenue;
    private int gp;
    private int discountTotal;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public LocalDate getPeriod() {
        return period;
    }

    public void setPeriod(LocalDate period) {
        this.period = period;
    }

    public String getStoreRus() {
        return storeRus;
    }

    public void setStoreRus(String storeRus) {
        this.storeRus = storeRus;
    }

    public int getRemainingSum() {
        return remainingSum;
    }

    public void setRemainingSum(int remainingSum) {
        this.remainingSum = remainingSum;
    }

    public int getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(int remainingDays) {
        this.remainingDays = remainingDays;
    }

    public int getSalesQuantity() {
        return salesQuantity;
    }

    public void setSalesQuantity(int salesQuantity) {
        this.salesQuantity = salesQuantity;
    }

    public int getSales() {
        return sales;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }

    public int getAsp() {
        return asp;
    }

    public void setAsp(int asp) {
        this.asp = asp;
    }

    public int getRevenue() {
        return revenue;
    }

    public void setRevenue(int revenue) {
        this.revenue = revenue;
    }

    public int getGp() {
        return gp;
    }

    public void setGp(int gp) {
        this.gp = gp;
    }

    public int getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(int discountTotal) {
        this.discountTotal = discountTotal;
    }
}
