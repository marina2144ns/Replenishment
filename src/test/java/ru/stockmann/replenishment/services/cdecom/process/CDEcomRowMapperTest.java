package ru.stockmann.replenishment.services.cdecom.process;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.CDEcomExcelLoadDefinition;

import java.math.BigDecimal;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDEcomRowMapperTest {

    private final CDEcomRowMapper mapper = new CDEcomRowMapper();

    @Test
    void mapsAllSupportedTypes() {
        CDEcomTargetRow row = mapper.toTargetRow(validRow());

        assertEquals(20L, row.loadSessionId());
        assertEquals("Name", row.name());
        assertEquals(2026, row.year());
        assertEquals(1, row.season());
        assertEquals(31, row.day());
        assertEquals(Date.valueOf("2026-01-31"), row.data());
        assertEquals(13L, row.skuStyleColor());
        assertEquals(new BigDecimal("1234.57"), row.orderPcs());
        assertEquals(123L, row.planRub());
        assertEquals(456L, row.stockStoresPcs());
        assertEquals(789L, row.stockStoresDdp());
        assertEquals("Collection", row.skuCollection());
    }

    @Test
    void mapsNumericExcelDateNormalizedByDefinition() {
        String rawDate = new CDEcomExcelLoadDefinition()
                .columns()
                .get(4)
                .normalizer()
                .normalize("45658.75");

        CDEcomTargetRow row = mapper.toTargetRow(validRowWithData(rawDate));

        assertEquals("01.01.2025", rawDate);
        assertEquals(Date.valueOf("2025-01-01"), row.data());
    }

    @Test
    void mapsBlankAndSpecialNullValuesToNull() {
        CDEcomTargetRow row = mapper.toTargetRow(new CDEcomRawRow(
                10L, 20L, 51L, " ", " ", null, null, "N/A",
                null, null, null, null, null, null, null, null, null, null,
                null, "-", null, "NULL", null, null, null, null, null, null, null, null,
                null, "NA", null, null, null, null, null, null, null, null, null
        ));

        assertNull(row.name());
        assertNull(row.year());
        assertNull(row.data());
        assertNull(row.skuStyleColor());
        assertNull(row.orderPcs());
        assertNull(row.planRub());
    }

    @Test
    void mapsDirectBigintDecimalsOnlyWhenMathematicallyIntegral() {
        CDEcomTargetRow row = mapper.toTargetRow(validRow("2026", "12", "1", "12.00", "-12.0", "1e2"));

        assertEquals(12L, row.planRub());
        assertEquals(-12L, row.stockStoresPcs());
        assertEquals(100L, row.stockStoresDdp());

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> mapper.toTargetRow(validRow("2026", "12", "1", "12.5", "1", "1"))
        );

        assertTrue(ex.getMessage().contains("field [planRub]"));
        assertTrue(ex.getMessage().contains("INVALID_BIGINT"));
        assertTrue(ex.getMessage().contains("originalValue=[12.5]"));
    }

    @Test
    void mapsSkuStyleColorWithHalfUpRounding() {
        assertEquals(-12L, mapper.toTargetRow(validRow("2026", "-12.4", "1", "1", "1", "1")).skuStyleColor());
        assertEquals(-13L, mapper.toTargetRow(validRow("2026", "-12.5", "1", "1", "1", "1")).skuStyleColor());
        assertEquals(-13L, mapper.toTargetRow(validRow("2026", "-12.6", "1", "1", "1", "1")).skuStyleColor());
        assertEquals(12L, mapper.toTargetRow(validRow("2026", "12.4", "1", "1", "1", "1")).skuStyleColor());
        assertEquals(13L, mapper.toTargetRow(validRow("2026", "12.5", "1", "1", "1", "1")).skuStyleColor());
        assertEquals(13L, mapper.toTargetRow(validRow("2026", "12.6", "1", "1", "1", "1")).skuStyleColor());
    }

    @Test
    void mapsDecimalBoundariesAfterRounding() {
        assertEquals(
                new BigDecimal("9999999999999999.99"),
                mapper.toTargetRow(validRow("2026", "12", "9999999999999999.994", "1", "1", "1")).orderPcs()
        );
        assertEquals(
                new BigDecimal("-9999999999999999.99"),
                mapper.toTargetRow(validRow("2026", "12", "-9999999999999999.994", "1", "1", "1")).orderPcs()
        );
    }

    @Test
    void mapperExceptionContainsFieldAndOriginalValue() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> mapper.toTargetRow(validRow("bad-year"))
        );

        assertTrue(ex.getMessage().contains("field [year]"));
        assertTrue(ex.getMessage().contains("originalValue=[bad-year]"));
        assertTrue(ex.getMessage().contains("INVALID_INTEGER"));
    }

    private static CDEcomRawRow validRow() {
        return validRow("2026");
    }

    private static CDEcomRawRow validRow(String year) {
        return validRow(year, "12.5", "1 234,565", "123", "456", "789");
    }

    private static CDEcomRawRow validRowWithData(String data) {
        return validRow("2026", data, "12.5", "1 234,565", "123", "456", "789");
    }

    private static CDEcomRawRow validRow(
            String year,
            String skuStyleColor,
            String orderPcs,
            String planRub,
            String stockStoresPcs,
            String stockStoresDdp
    ) {
        return validRow(year, "31.01.2026", skuStyleColor, orderPcs, planRub, stockStoresPcs, stockStoresDdp);
    }

    private static CDEcomRawRow validRow(
            String year,
            String data,
            String skuStyleColor,
            String orderPcs,
            String planRub,
            String stockStoresPcs,
            String stockStoresDdp
    ) {
        return new CDEcomRawRow(
                10L, 20L, 51L, " Name ", year, "1", "31", data,
                "Online", "Store", "Division", "Department", "SubDepartment", "Brand", "TM",
                "Node", "Section", "Group", "Campaign", skuStyleColor, "Phase",
                orderPcs, "1", "2", "3", "4", "5", "6", "7", "8",
                "9", planRub, stockStoresPcs, stockStoresDdp,
                "Drivers", "Model", "Composition", "Color", "Sku Name", "Comment", "Collection"
        );
    }
}
