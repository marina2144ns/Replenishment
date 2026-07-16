package ru.stockmann.replenishment.services.weeklydata.process;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WeeklyDataRowMapperTest {

    private final WeeklyDataRowMapper mapper = new WeeklyDataRowMapper();

    @Test
    void validRowMapsYearAndWeek() {
        WeeklyDataTargetRow target = mapper.toTargetRow(row()
                .year("2025")
                .week("10")
                .build());

        assertEquals((short) 2025, target.year());
        assertEquals((short) 10, target.week());
    }

    @Test
    void decimalNullMapsToZero() {
        WeeklyDataTargetRow target = mapper.toTargetRow(row()
                .year("2025")
                .week("10")
                .salesRub(null)
                .revenue("")
                .gp("-")
                .build());

        assertEquals(BigDecimal.ZERO, target.salesRub());
        assertEquals(BigDecimal.ZERO, target.revenue());
        assertEquals(BigDecimal.ZERO, target.gp());
    }

    @Test
    void decimalValuesAreParsed() {
        WeeklyDataTargetRow target = mapper.toTargetRow(row()
                .year("2025")
                .week("10")
                .salesRub("1 234,56")
                .revenue("100.50")
                .build());

        assertEquals(new BigDecimal("1234.56"), target.salesRub());
        assertEquals(new BigDecimal("100.50"), target.revenue());
    }

    @Test
    void textFieldsAreTrimmed() {
        WeeklyDataTargetRow target = mapper.toTargetRow(row()
                .year("2025")
                .week("10")
                .storeRus("  Москва  ")
                .build());

        assertEquals("Москва", target.storeRus());
    }

    @Test
    void nbspTextIsCleaned() {
        WeeklyDataTargetRow target = mapper.toTargetRow(row()
                .year("2025")
                .week("10")
                .storeRus("\u00A0Москва\u00A0")
                .build());

        assertEquals("Москва", target.storeRus());
    }

    @Test
    void missingYearThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> mapper.toTargetRow(row()
                .year(null)
                .week("10")
                .build()));
    }

    @Test
    void invalidDecimalThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> mapper.toTargetRow(row()
                .year("2025")
                .week("10")
                .salesRub("abc")
                .build()));
    }

    private RowBuilder row() {
        return new RowBuilder();
    }

    private static class RowBuilder {
        private String year;
        private String week;
        private String storeRus;
        private String salesRub;
        private String revenue;
        private String gp;

        RowBuilder year(String year) {
            this.year = year;
            return this;
        }

        RowBuilder week(String week) {
            this.week = week;
            return this;
        }

        RowBuilder storeRus(String storeRus) {
            this.storeRus = storeRus;
            return this;
        }

        RowBuilder salesRub(String salesRub) {
            this.salesRub = salesRub;
            return this;
        }

        RowBuilder revenue(String revenue) {
            this.revenue = revenue;
            return this;
        }

        RowBuilder gp(String gp) {
            this.gp = gp;
            return this;
        }

        WeeklyDataRawRow build() {
            return new WeeklyDataRawRow(
                    1,
                    2,
                    3L,
                    null,
                    null,
                    null,
                    null,
                    year,
                    week,
                    null,
                    null,
                    storeRus,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    salesRub,
                    revenue,
                    gp,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
