package ru.stockmann.replenishment.services.dwhexcelload.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DWHRequestedByTest {

    @Test
    void acceptsAbsentAndOneHundredCharacterValues() {
        assertNull(DWHRequestedBy.normalizeAndValidate(null));
        assertNull(DWHRequestedBy.normalizeAndValidate("   "));
        String value = "x".repeat(100);
        assertEquals(value, DWHRequestedBy.normalizeAndValidate(value));
    }

    @Test
    void rejectsValuesLongerThanOneHundredCharacters() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> DWHRequestedBy.normalizeAndValidate("x".repeat(101)));
        assertEquals("requestedBy must not be longer than 100 characters", error.getMessage());
    }
}
