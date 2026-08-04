package ru.stockmann.replenishment.services.cddata.process;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDDataRowMapperTest {

    private final CDDataRowMapper mapper = new CDDataRowMapper();

    @Test
    void mapsValidRawRowToTargetRow() {
        CDDataTargetRow target = mapper.toTargetRow(validRow());

        assertEquals(20L, target.loadSessionId());
        assertEquals("Name", target.nazvanie());
        assertEquals(2025, target.god());
        assertEquals(1, target.sezon());
        assertEquals(31, target.den());
        assertEquals(Date.valueOf("2025-01-31"), target.data());
        assertEquals("Online", target.salesChannel());
        assertEquals("Store", target.storeRus());
        assertEquals("Division", target.mfpDivision());
        assertEquals("Department", target.mfpDepartment());
        assertEquals("SubDepartment", target.mfpSubDepartment());
        assertEquals("Brand", target.skuBrandType());
        assertEquals("TM", target.skuTm());
        assertEquals("Node", target.mfpNode());
        assertEquals("Section", target.section());
        assertEquals("Group", target.merchandiseSubGroup());
        assertEquals("Campaign", target.campaignSales());
        assertEquals(9223372036854775807L, target.skuStyleColor());
        assertEquals("Phase", target.skuPhase());
        assertEquals(new BigDecimal("12.35"), target.stockStartPcs());
        assertEquals(new BigDecimal("1234.56"), target.stockStartDd());
        assertEquals(new BigDecimal("0.00"), target.salesPcs());
        assertEquals(new BigDecimal("100.00"), target.salesRub());
        assertEquals(new BigDecimal("200.10"), target.revenue());
        assertEquals(new BigDecimal("-12.34"), target.gp());
        assertEquals(new BigDecimal("5.00"), target.cogs());
        assertEquals(new BigDecimal("99.99"), target.salesFrpPrice());
        assertEquals(new BigDecimal("10.00"), target.salesDiscount());
        assertEquals(new BigDecimal("3.00"), target.stockStoresPcs());
        assertEquals(new BigDecimal("4.00"), target.stockStoresDd());
        assertEquals(123, target.planRub());
        assertEquals("Driver", target.draiveryCd());
        assertEquals("Color", target.skuColorRus());
        assertEquals("Composition", target.skuComposition());
        assertEquals("Supplier", target.skuSupplier());
        assertEquals("Sku Name", target.skuName());
        assertEquals("Collection", target.skuCollection());
        assertEquals("Comment", target.skuComment());
        assertEquals(10L, target.rawRowId());
    }

    @Test
    void mapsExcelSerialDateToSqlDate() {
        CDDataTargetRow target = mapper.toTargetRow(rowBuilder()
                .data("61")
                .build());

        assertEquals(Date.valueOf("1900-03-01"), target.data());
    }

    @Test
    void mapsNullableAndSpecialNullValuesToNull() {
        CDDataTargetRow target = mapper.toTargetRow(rowBuilder()
                .nazvanie("  ")
                .god("N/A")
                .data("NULL")
                .skuStyleColor("-")
                .stockStartPcs("--")
                .planRub(null)
                .salesChannel("\u00A0")
                .build());

        assertNull(target.nazvanie());
        assertNull(target.god());
        assertNull(target.data());
        assertNull(target.skuStyleColor());
        assertNull(target.stockStartPcs());
        assertNull(target.planRub());
        assertNull(target.salesChannel());
    }

    @Test
    void invalidIntegerExceptionContainsStructuredParserDetails() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> mapper.toTargetRow(rowBuilder()
                        .god("12.5")
                        .build())
        );

        assertTrue(exception.getMessage().contains("field [god]"));
        assertTrue(exception.getMessage().contains("INVALID_INTEGER"));
        assertTrue(exception.getMessage().contains("originalValue=[12.5]"));
    }

    @Test
    void invalidDateExceptionContainsStructuredParserDetails() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> mapper.toTargetRow(rowBuilder()
                        .data("31.02.2025")
                        .build())
        );

        assertTrue(exception.getMessage().contains("field [data]"));
        assertTrue(exception.getMessage().contains("INVALID_DATE"));
        assertTrue(exception.getMessage().contains("31.02.2025"));
    }

    private static CDDataRawRow validRow() {
        return rowBuilder()
                .nazvanie(" Name ")
                .god("2025")
                .sezon("1")
                .den("31")
                .data("2025-01-31")
                .salesChannel("Online")
                .storeRus("Store")
                .mfpDivision("Division")
                .mfpDepartment("Department")
                .mfpSubDepartment("SubDepartment")
                .skuBrandType("Brand")
                .skuTm("TM")
                .mfpNode("Node")
                .section("Section")
                .merchandiseSubGroup("Group")
                .campaignSales("Campaign")
                .skuStyleColor("9223372036854775807")
                .skuPhase("Phase")
                .stockStartPcs("12.345")
                .stockStartDd("1 234,56")
                .salesPcs("0")
                .salesRub("100")
                .revenue("200.10")
                .gp("-12.34")
                .cogs("5")
                .salesFrpPrice("99.99")
                .salesDiscount("10")
                .stockStoresPcs("3")
                .stockStoresDd("4")
                .planRub("123")
                .draiveryCd("Driver")
                .skuColorRus("Color")
                .skuComposition("Composition")
                .skuSupplier("Supplier")
                .skuName("Sku Name")
                .skuCollection("Collection")
                .skuComment("Comment")
                .build();
    }

    private static CDDataRawRowBuilder rowBuilder() {
        return new CDDataRawRowBuilder();
    }

    private static final class CDDataRawRowBuilder {
        private String nazvanie;
        private String god;
        private String sezon;
        private String den;
        private String data;
        private String salesChannel;
        private String storeRus;
        private String mfpDivision;
        private String mfpDepartment;
        private String mfpSubDepartment;
        private String skuBrandType;
        private String skuTm;
        private String mfpNode;
        private String section;
        private String merchandiseSubGroup;
        private String campaignSales;
        private String skuStyleColor;
        private String skuPhase;
        private String stockStartPcs;
        private String stockStartDd;
        private String salesPcs;
        private String salesRub;
        private String revenue;
        private String gp;
        private String cogs;
        private String salesFrpPrice;
        private String salesDiscount;
        private String stockStoresPcs;
        private String stockStoresDd;
        private String planRub;
        private String draiveryCd;
        private String skuColorRus;
        private String skuComposition;
        private String skuSupplier;
        private String skuName;
        private String skuCollection;
        private String skuComment;

        CDDataRawRowBuilder nazvanie(String value) {
            this.nazvanie = value;
            return this;
        }

        CDDataRawRowBuilder god(String value) {
            this.god = value;
            return this;
        }

        CDDataRawRowBuilder sezon(String value) {
            this.sezon = value;
            return this;
        }

        CDDataRawRowBuilder den(String value) {
            this.den = value;
            return this;
        }

        CDDataRawRowBuilder data(String value) {
            this.data = value;
            return this;
        }

        CDDataRawRowBuilder salesChannel(String value) {
            this.salesChannel = value;
            return this;
        }

        CDDataRawRowBuilder storeRus(String value) {
            this.storeRus = value;
            return this;
        }

        CDDataRawRowBuilder mfpDivision(String value) {
            this.mfpDivision = value;
            return this;
        }

        CDDataRawRowBuilder mfpDepartment(String value) {
            this.mfpDepartment = value;
            return this;
        }

        CDDataRawRowBuilder mfpSubDepartment(String value) {
            this.mfpSubDepartment = value;
            return this;
        }

        CDDataRawRowBuilder skuBrandType(String value) {
            this.skuBrandType = value;
            return this;
        }

        CDDataRawRowBuilder skuTm(String value) {
            this.skuTm = value;
            return this;
        }

        CDDataRawRowBuilder mfpNode(String value) {
            this.mfpNode = value;
            return this;
        }

        CDDataRawRowBuilder section(String value) {
            this.section = value;
            return this;
        }

        CDDataRawRowBuilder merchandiseSubGroup(String value) {
            this.merchandiseSubGroup = value;
            return this;
        }

        CDDataRawRowBuilder campaignSales(String value) {
            this.campaignSales = value;
            return this;
        }

        CDDataRawRowBuilder skuStyleColor(String value) {
            this.skuStyleColor = value;
            return this;
        }

        CDDataRawRowBuilder skuPhase(String value) {
            this.skuPhase = value;
            return this;
        }

        CDDataRawRowBuilder stockStartPcs(String value) {
            this.stockStartPcs = value;
            return this;
        }

        CDDataRawRowBuilder stockStartDd(String value) {
            this.stockStartDd = value;
            return this;
        }

        CDDataRawRowBuilder salesPcs(String value) {
            this.salesPcs = value;
            return this;
        }

        CDDataRawRowBuilder salesRub(String value) {
            this.salesRub = value;
            return this;
        }

        CDDataRawRowBuilder revenue(String value) {
            this.revenue = value;
            return this;
        }

        CDDataRawRowBuilder gp(String value) {
            this.gp = value;
            return this;
        }

        CDDataRawRowBuilder cogs(String value) {
            this.cogs = value;
            return this;
        }

        CDDataRawRowBuilder salesFrpPrice(String value) {
            this.salesFrpPrice = value;
            return this;
        }

        CDDataRawRowBuilder salesDiscount(String value) {
            this.salesDiscount = value;
            return this;
        }

        CDDataRawRowBuilder stockStoresPcs(String value) {
            this.stockStoresPcs = value;
            return this;
        }

        CDDataRawRowBuilder stockStoresDd(String value) {
            this.stockStoresDd = value;
            return this;
        }

        CDDataRawRowBuilder planRub(String value) {
            this.planRub = value;
            return this;
        }

        CDDataRawRowBuilder draiveryCd(String value) {
            this.draiveryCd = value;
            return this;
        }

        CDDataRawRowBuilder skuColorRus(String value) {
            this.skuColorRus = value;
            return this;
        }

        CDDataRawRowBuilder skuComposition(String value) {
            this.skuComposition = value;
            return this;
        }

        CDDataRawRowBuilder skuSupplier(String value) {
            this.skuSupplier = value;
            return this;
        }

        CDDataRawRowBuilder skuName(String value) {
            this.skuName = value;
            return this;
        }

        CDDataRawRowBuilder skuCollection(String value) {
            this.skuCollection = value;
            return this;
        }

        CDDataRawRowBuilder skuComment(String value) {
            this.skuComment = value;
            return this;
        }

        CDDataRawRow build() {
            return new CDDataRawRow(
                    10L,
                    20L,
                    30L,
                    nazvanie,
                    god,
                    sezon,
                    den,
                    data,
                    salesChannel,
                    storeRus,
                    mfpDivision,
                    mfpDepartment,
                    mfpSubDepartment,
                    skuBrandType,
                    skuTm,
                    mfpNode,
                    section,
                    merchandiseSubGroup,
                    campaignSales,
                    skuStyleColor,
                    skuPhase,
                    stockStartPcs,
                    stockStartDd,
                    salesPcs,
                    salesRub,
                    revenue,
                    gp,
                    cogs,
                    salesFrpPrice,
                    salesDiscount,
                    stockStoresPcs,
                    stockStoresDd,
                    planRub,
                    draiveryCd,
                    skuColorRus,
                    skuComposition,
                    skuSupplier,
                    skuName,
                    skuCollection,
                    skuComment
            );
        }
    }
}
