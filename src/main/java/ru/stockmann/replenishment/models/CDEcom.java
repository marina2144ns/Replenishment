package ru.stockmann.replenishment.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "CD_ecom")
public class CDEcom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "LoadSessionId")
    private Long loadSessionId;

    @Column(name = "name")
    private String name;

    @Column(name = "year")
    private Integer year;

    @Column(name = "season")
    private Integer season;

    @Column(name = "day")
    private Integer day;

    @Column(name = "data")
    private LocalDate data;

    @Column(name = "salesChannelBpo")
    private String salesChannelBpo;

    @Column(name = "storeRus")
    private String storeRus;

    @Column(name = "mfpDivision")
    private String mfpDivision;

    @Column(name = "mfpDepartment")
    private String mfpDepartment;

    @Column(name = "mfpSubDepartment")
    private String mfpSubDepartment;

    @Column(name = "skuBrandType")
    private String skuBrandType;

    @Column(name = "skuTm")
    private String skuTm;

    @Column(name = "mfpNode")
    private String mfpNode;

    @Column(name = "section")
    private String section;

    @Column(name = "merchandiseSubGroup")
    private String merchandiseSubGroup;

    @Column(name = "campaignSalesType")
    private String campaignSalesType;

    @Column(name = "skuStyleColor")
    private Long skuStyleColor;

    @Column(name = "skuPhase")
    private String skuPhase;

    @Column(name = "orderPcs", precision = 18, scale = 2)
    private BigDecimal orderPcs;

    @Column(name = "orderRub", precision = 18, scale = 2)
    private BigDecimal orderRub;

    @Column(name = "foundPcs", precision = 18, scale = 2)
    private BigDecimal foundPcs;

    @Column(name = "foundRub", precision = 18, scale = 2)
    private BigDecimal foundRub;

    @Column(name = "salesPcs", precision = 18, scale = 2)
    private BigDecimal salesPcs;

    @Column(name = "salesRub", precision = 18, scale = 2)
    private BigDecimal salesRub;

    @Column(name = "revenue", precision = 18, scale = 2)
    private BigDecimal revenue;

    @Column(name = "gp", precision = 18, scale = 2)
    private BigDecimal gp;

    @Column(name = "cogs", precision = 18, scale = 2)
    private BigDecimal cogs;

    @Column(name = "salesDiscount", precision = 18, scale = 2)
    private BigDecimal salesDiscount;

    @Column(name = "planRub")
    private Long planRub;

    @Column(name = "stockStoresPcs")
    private Long stockStoresPcs;

    @Column(name = "stockStoresDdp")
    private Long stockStoresDdp;

    @Column(name = "cdDrivers")
    private String cdDrivers;

    @Column(name = "skuSupplierModel")
    private String skuSupplierModel;

    @Column(name = "skuComposition")
    private String skuComposition;

    @Column(name = "skuColorRussian")
    private String skuColorRussian;

    @Column(name = "skuName")
    private String skuName;

    @Column(name = "skuCommentBuyer")
    private String skuCommentBuyer;

    @Column(name = "skuCollection")
    private String skuCollection;

    @Column(name = "CreatedAt", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLoadSessionId() {
        return loadSessionId;
    }

    public void setLoadSessionId(Long loadSessionId) {
        this.loadSessionId = loadSessionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getSalesChannelBpo() {
        return salesChannelBpo;
    }

    public void setSalesChannelBpo(String salesChannelBpo) {
        this.salesChannelBpo = salesChannelBpo;
    }

    public String getStoreRus() {
        return storeRus;
    }

    public void setStoreRus(String storeRus) {
        this.storeRus = storeRus;
    }

    public String getMfpDivision() {
        return mfpDivision;
    }

    public void setMfpDivision(String mfpDivision) {
        this.mfpDivision = mfpDivision;
    }

    public String getMfpDepartment() {
        return mfpDepartment;
    }

    public void setMfpDepartment(String mfpDepartment) {
        this.mfpDepartment = mfpDepartment;
    }

    public String getMfpSubDepartment() {
        return mfpSubDepartment;
    }

    public void setMfpSubDepartment(String mfpSubDepartment) {
        this.mfpSubDepartment = mfpSubDepartment;
    }

    public String getSkuBrandType() {
        return skuBrandType;
    }

    public void setSkuBrandType(String skuBrandType) {
        this.skuBrandType = skuBrandType;
    }

    public String getSkuTm() {
        return skuTm;
    }

    public void setSkuTm(String skuTm) {
        this.skuTm = skuTm;
    }

    public String getMfpNode() {
        return mfpNode;
    }

    public void setMfpNode(String mfpNode) {
        this.mfpNode = mfpNode;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getMerchandiseSubGroup() {
        return merchandiseSubGroup;
    }

    public void setMerchandiseSubGroup(String merchandiseSubGroup) {
        this.merchandiseSubGroup = merchandiseSubGroup;
    }

    public String getCampaignSalesType() {
        return campaignSalesType;
    }

    public void setCampaignSalesType(String campaignSalesType) {
        this.campaignSalesType = campaignSalesType;
    }

    public Long getSkuStyleColor() {
        return skuStyleColor;
    }

    public void setSkuStyleColor(Long skuStyleColor) {
        this.skuStyleColor = skuStyleColor;
    }

    public String getSkuPhase() {
        return skuPhase;
    }

    public void setSkuPhase(String skuPhase) {
        this.skuPhase = skuPhase;
    }

    public BigDecimal getOrderPcs() {
        return orderPcs;
    }

    public void setOrderPcs(BigDecimal orderPcs) {
        this.orderPcs = orderPcs;
    }

    public BigDecimal getOrderRub() {
        return orderRub;
    }

    public void setOrderRub(BigDecimal orderRub) {
        this.orderRub = orderRub;
    }

    public BigDecimal getFoundPcs() {
        return foundPcs;
    }

    public void setFoundPcs(BigDecimal foundPcs) {
        this.foundPcs = foundPcs;
    }

    public BigDecimal getFoundRub() {
        return foundRub;
    }

    public void setFoundRub(BigDecimal foundRub) {
        this.foundRub = foundRub;
    }

    public BigDecimal getSalesPcs() {
        return salesPcs;
    }

    public void setSalesPcs(BigDecimal salesPcs) {
        this.salesPcs = salesPcs;
    }

    public BigDecimal getSalesRub() {
        return salesRub;
    }

    public void setSalesRub(BigDecimal salesRub) {
        this.salesRub = salesRub;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getGp() {
        return gp;
    }

    public void setGp(BigDecimal gp) {
        this.gp = gp;
    }

    public BigDecimal getCogs() {
        return cogs;
    }

    public void setCogs(BigDecimal cogs) {
        this.cogs = cogs;
    }

    public BigDecimal getSalesDiscount() {
        return salesDiscount;
    }

    public void setSalesDiscount(BigDecimal salesDiscount) {
        this.salesDiscount = salesDiscount;
    }

    public Long getPlanRub() {
        return planRub;
    }

    public void setPlanRub(Long planRub) {
        this.planRub = planRub;
    }

    public Long getStockStoresPcs() {
        return stockStoresPcs;
    }

    public void setStockStoresPcs(Long stockStoresPcs) {
        this.stockStoresPcs = stockStoresPcs;
    }

    public Long getStockStoresDdp() {
        return stockStoresDdp;
    }

    public void setStockStoresDdp(Long stockStoresDdp) {
        this.stockStoresDdp = stockStoresDdp;
    }

    public String getCdDrivers() {
        return cdDrivers;
    }

    public void setCdDrivers(String cdDrivers) {
        this.cdDrivers = cdDrivers;
    }

    public String getSkuSupplierModel() {
        return skuSupplierModel;
    }

    public void setSkuSupplierModel(String skuSupplierModel) {
        this.skuSupplierModel = skuSupplierModel;
    }

    public String getSkuComposition() {
        return skuComposition;
    }

    public void setSkuComposition(String skuComposition) {
        this.skuComposition = skuComposition;
    }

    public String getSkuColorRussian() {
        return skuColorRussian;
    }

    public void setSkuColorRussian(String skuColorRussian) {
        this.skuColorRussian = skuColorRussian;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getSkuCommentBuyer() {
        return skuCommentBuyer;
    }

    public void setSkuCommentBuyer(String skuCommentBuyer) {
        this.skuCommentBuyer = skuCommentBuyer;
    }

    public String getSkuCollection() {
        return skuCollection;
    }

    public void setSkuCollection(String skuCollection) {
        this.skuCollection = skuCollection;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}