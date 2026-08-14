package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.cddata.process.CDDataDeletionRepository;
import ru.stockmann.replenishment.services.cdecom.process.CDEcomDeletionRepository;
import ru.stockmann.replenishment.services.salesbychannel.process.SalesByChannelDeletionRepository;
import ru.stockmann.replenishment.services.weeklydata.process.WeeklyDataDeletionRepository;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetDataDeletionRepositoryTest {

    @Test
    void weeklyDataDeletesOnlyMatchingTargetPeriodWithBoundSmallints() {
        JdbcRecording jdbc = new JdbcRecording(3);

        int deleted = new WeeklyDataDeletionRepository()
                .deleteByPeriod(jdbc.connection(), (short) 2026, (short) 31);

        assertEquals(3, deleted);
        assertPeriodSql(jdbc.sql, "dbo.weekly_data", "[year]", "[week]");
        assertEquals((short) 2026, jdbc.parameters.get(1));
        assertEquals((short) 31, jdbc.parameters.get(2));
    }

    @Test
    void cdDataDeletesOnlyMatchingTargetPeriodWithBoundIntegers() {
        JdbcRecording jdbc = new JdbcRecording(4);

        int deleted = new CDDataDeletionRepository().deleteByPeriod(jdbc.connection(), 2026, 2);

        assertEquals(4, deleted);
        assertPeriodSql(jdbc.sql, "dbo.cd_data", "god", "sezon");
        assertEquals(2026, jdbc.parameters.get(1));
        assertEquals(2, jdbc.parameters.get(2));
    }

    @Test
    void cdecomDeletesOnlyMatchingTargetPeriodWithBoundIntegers() {
        JdbcRecording jdbc = new JdbcRecording(5);

        int deleted = new CDEcomDeletionRepository().deleteByPeriod(jdbc.connection(), 2026, 2);

        assertEquals(5, deleted);
        assertPeriodSql(jdbc.sql, "dbo.cd_ecom", "[year]", "season");
        assertEquals(2026, jdbc.parameters.get(1));
        assertEquals(2, jdbc.parameters.get(2));
    }

    @Test
    void loadSessionDeletesAreScopedToEachTargetAndUseBoundBigint() {
        JdbcRecording weekly = new JdbcRecording(1);
        JdbcRecording cdData = new JdbcRecording(2);
        JdbcRecording cdecom = new JdbcRecording(0);

        assertEquals(1, new WeeklyDataDeletionRepository().deleteByLoadSessionId(weekly.connection(), 41L));
        assertEquals(2, new CDDataDeletionRepository().deleteByLoadSessionId(cdData.connection(), 42L));
        assertEquals(0, new CDEcomDeletionRepository().deleteByLoadSessionId(cdecom.connection(), 43L));

        assertLoadSessionSql(weekly.sql, "dbo.weekly_data");
        assertLoadSessionSql(cdData.sql, "dbo.cd_data");
        assertLoadSessionSql(cdecom.sql, "dbo.cd_ecom");
        assertEquals(41L, weekly.parameters.get(1));
        assertEquals(42L, cdData.parameters.get(1));
        assertEquals(43L, cdecom.parameters.get(1));
    }

    @Test
    void newCompositeDeletesUseBothBoundValuesAndOnlyTargetTables() {
        JdbcRecording cdData = new JdbcRecording(2);
        assertEquals(2, new CDDataDeletionRepository()
                .deleteByNazvanieAndDen(cdData.connection(), "Main", 15));
        assertPeriodSql(cdData.sql, "dbo.cd_data", "nazvanie", "den");
        assertEquals("Main", cdData.parameters.get(1));
        assertEquals(15, cdData.parameters.get(2));

        JdbcRecording cdecom = new JdbcRecording(3);
        assertEquals(3, new CDEcomDeletionRepository()
                .deleteByNazvanieAndDen(cdecom.connection(), "Online", 16));
        assertPeriodSql(cdecom.sql, "dbo.cd_ecom", "name", "[day]");
        assertEquals("Online", cdecom.parameters.get(1));
        assertEquals(16, cdecom.parameters.get(2));

        JdbcRecording sales = new JdbcRecording(4);
        assertEquals(4, new SalesByChannelDeletionRepository()
                .deleteByYearAndMonth(sales.connection(), "2026", "08"));
        assertPeriodSql(sales.sql, "dbo.salesbychannel", "[year]", "[month]");
        assertEquals("2026", sales.parameters.get(1));
        assertEquals("08", sales.parameters.get(2));
    }

    private static void assertPeriodSql(String sql, String table, String year, String week) {
        String normalized = normalize(sql);
        assertTrue(normalized.startsWith("delete from " + table));
        assertTrue(normalized.contains("where " + year + " = ?"));
        assertTrue(normalized.contains("and " + week + " = ?"));
        assertTargetOnly(normalized);
        assertEquals(2, normalized.chars().filter(ch -> ch == '?').count());
    }

    private static void assertLoadSessionSql(String sql, String table) {
        String normalized = normalize(sql);
        assertTrue(normalized.startsWith("delete from " + table));
        assertTrue(normalized.contains("where loadsessionid = ?"));
        assertTargetOnly(normalized);
        assertEquals(1, normalized.chars().filter(ch -> ch == '?').count());
    }

    private static void assertTargetOnly(String sql) {
        assertFalse(sql.contains("_raw"));
        assertFalse(sql.contains("_stage"));
        assertFalse(sql.contains("dwh_excel_load_session"));
        assertFalse(sql.contains("dwh_excel_load_error"));
        assertFalse(sql.contains("truncate"));
    }

    private static String normalize(String sql) {
        return sql.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private static final class JdbcRecording {
        private final int affectedRows;
        private final Map<Integer, Object> parameters = new HashMap<>();
        private String sql;

        private JdbcRecording(int affectedRows) {
            this.affectedRows = affectedRows;
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql = (String) args[0];
                            return statement();
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("set") && args != null && args.length >= 2) {
                            parameters.put((Integer) args[0], args[1]);
                            return null;
                        }
                        if ("executeUpdate".equals(method.getName())) {
                            return affectedRows;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == short.class) return (short) 0;
            return null;
        }
    }
}
