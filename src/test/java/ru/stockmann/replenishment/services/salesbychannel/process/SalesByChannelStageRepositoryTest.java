package ru.stockmann.replenishment.services.salesbychannel.process;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesByChannelStageRepositoryTest {

    @Test
    void bindsAllThirtyOneParametersInDdlOrder() {
        Jdbc jdbc = new Jdbc(new int[]{1});
        new SalesByChannelStageRepository().insertBatch(jdbc.connection(), 10L, List.of(row()));

        assertTrue(jdbc.sql.contains("INSERT INTO dbo.SalesByChannel_stage"));
        assertTrue(jdbc.sql.contains("[year], [month]"));
        assertFalse(jdbc.sql.contains("SELECT *"));
        assertEquals(31, jdbc.calls.size());
        assertEquals("setLong:1:10", jdbc.calls.get(0));
        assertEquals("setLong:2:17", jdbc.calls.get(1));
        assertEquals("setString:7:2025", jdbc.calls.get(6));
        assertEquals("setInt:17:3", jdbc.calls.get(16));
        assertEquals("setBigDecimal:18:1.25", jdbc.calls.get(17));
        assertEquals("setString:31:product", jdbc.calls.get(30));
        assertEquals(1, jdbc.addBatch);
        assertEquals(1, jdbc.executeBatch);
    }

    @Test
    void validatesUpdateCountsStrictly() {
        assertThrows(IllegalStateException.class, () -> new SalesByChannelStageRepository()
                .insertBatch(new Jdbc(new int[0]).connection(), 10L, List.of(row())));
        assertThrows(IllegalStateException.class, () -> new SalesByChannelStageRepository()
                .insertBatch(new Jdbc(new int[]{Statement.EXECUTE_FAILED}).connection(),
                        10L, List.of(row())));
    }

    private SalesByChannelStageRow row() {
        BigDecimal decimal = new BigDecimal("1.25");
        return new SalesByChannelStageRow(
                10L, 17L, "sy", "s6", "ym", "ys", "2025", "April",
                "channel", "store", "type", "division", "department", "campaign",
                "seasonality", "brand", 3, decimal, decimal, decimal, decimal,
                "budget", "storeBpo", "channelBpo", "sub", "tm", "node", "section",
                "group", "phase", "product"
        );
    }

    private static final class Jdbc {
        final int[] counts;
        final List<String> calls = new ArrayList<>();
        String sql;
        int addBatch;
        int executeBatch;

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
                        if ("addBatch".equals(method.getName())) { addBatch++; return null; }
                        if ("executeBatch".equals(method.getName())) {
                            executeBatch++;
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
