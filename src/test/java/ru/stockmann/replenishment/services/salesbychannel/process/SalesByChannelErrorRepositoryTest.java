package ru.stockmann.replenishment.services.salesbychannel.process;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesByChannelErrorRepositoryTest {

    @Test
    void bindsCommonErrorContractIncludingSessionRawAndExcelRow() {
        Jdbc jdbc = new Jdbc(new int[]{1});
        SalesByChannelValidationError error = new SalesByChannelValidationError(
                10L, 88L, 17L, "VALIDATION", "year",
                "REQUIRED_FIELD_EMPTY", "Required value is empty", "message"
        );

        new SalesByChannelErrorRepository(null)
                .insertBatch(jdbc.connection(), 10L, List.of(error));

        assertTrue(jdbc.sql.contains("INSERT INTO dbo.DWH_Excel_Load_Error"));
        assertEquals(List.of(
                "setLong:1:10", "setString:2:SALES_BY_CHANNEL", "setString:3:VALIDATION",
                "setLong:4:17", "setLong:5:88", "setString:6:year",
                "setString:7:REQUIRED_FIELD_EMPTY", "setString:8:Required value is empty",
                "setString:9:message"
        ), jdbc.calls);
    }

    @Test
    void bindsNullExcelRowAsBigintAndRejectsFailedBatch() {
        Jdbc nullable = new Jdbc(new int[]{1});
        new SalesByChannelErrorRepository(null).insertBatch(nullable.connection(), 10L, List.of(
                new SalesByChannelValidationError(
                        10L, 0L, null, "PROCESSING", null, "CODE", "reason", "message"
                )
        ));
        assertEquals("setNull:4:" + Types.BIGINT, nullable.calls.get(3));

        assertThrows(IllegalStateException.class, () -> new SalesByChannelErrorRepository(null)
                .insertBatch(new Jdbc(new int[]{Statement.EXECUTE_FAILED}).connection(), 10L, List.of(
                        new SalesByChannelValidationError(
                                10L, 1L, 2L, "VALIDATION", "year", "CODE", "reason", "message"
                        )
                )));
    }

    private static final class Jdbc {
        final int[] counts;
        final List<String> calls = new ArrayList<>();
        String sql;
        Jdbc(int[] counts) { this.counts = counts; }
        Connection connection() {
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql = (String) args[0];
                            return statement();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }
        PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                        if (method.getName().startsWith("set")) {
                            calls.add(method.getName() + ":" + args[0] + ":" + args[1]);
                            return null;
                        }
                        if ("executeBatch".equals(method.getName())) {
                            return Arrays.copyOf(counts, counts.length);
                        }
                        return defaultValue(method.getReturnType());
                    });
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        return 0;
    }
}
