package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSession;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSessionRepository;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DWHDeletionSessionRepositoryTest {

    @Test
    void createsPeriodSessionWithTypedParametersAndRunningStatus() {
        RecordingJdbc jdbc = new RecordingJdbc(7001L, 1);
        DWHDeletionSessionRepository repository =
                new DWHDeletionSessionRepository(jdbc.dataSource());

        long id = repository.create(DWHDeletionSession.byPeriod(
                DWHExcelLoadType.WEEKLY_DATA, 2026, 31
        ));

        assertEquals(7001L, id);
        assertTrue(jdbc.sql.contains("INSERT INTO dbo.DWH_Excel_Load_Session"));
        assertEquals(List.of(
                "setString:1:WEEKLY_DATA",
                "setString:2:Weekly data",
                "setString:3:DELETE",
                "setString:4:BY_PERIOD",
                "setInt:5:2026",
                "setInt:6:31",
                "setNull:7:" + Types.INTEGER,
                "setNull:8:" + Types.BIGINT,
                "setNull:9:" + Types.NVARCHAR,
                "setNull:10:" + Types.NVARCHAR,
                "setNull:11:" + Types.NVARCHAR,
                "setNull:12:" + Types.NVARCHAR,
                "setNull:13:" + Types.NVARCHAR,
                "setString:14:RUNNING"
        ), jdbc.calls);
    }

    @Test
    void createsYearMonthSessionWithoutUsingDeleteWeek() {
        RecordingJdbc jdbc = new RecordingJdbc(7006L, 1);
        DWHDeletionSessionRepository repository =
                new DWHDeletionSessionRepository(jdbc.dataSource());

        repository.create(DWHDeletionSession.byYearAndMonth(
                DWHExcelLoadType.SALES_BY_CHANNEL, 2026, 7
        ));

        assertEquals("setInt:5:2026", jdbc.calls.get(4));
        assertEquals("setNull:6:" + Types.INTEGER, jdbc.calls.get(5));
        assertEquals("setInt:7:7", jdbc.calls.get(6));
        assertEquals("setString:14:RUNNING", jdbc.calls.get(13));
    }

    @Test
    void createsCriteriaSessionWithCriterionAndEveryParameterValue() {
        RecordingJdbc jdbc = new RecordingJdbc(7005L, 1);
        DWHDeletionSessionRepository repository =
                new DWHDeletionSessionRepository(jdbc.dataSource());

        repository.create(DWHDeletionSession.byCriteria(
                DWHExcelLoadType.CD_DATA, "NAZVANIE_DEN",
                "nazvanie", "Main", "den", "15"
        ));

        assertEquals("setString:4:BY_CRITERIA", jdbc.calls.get(3));
        assertEquals("setString:9:NAZVANIE_DEN", jdbc.calls.get(8));
        assertEquals("setString:10:nazvanie", jdbc.calls.get(9));
        assertEquals("setString:11:Main", jdbc.calls.get(10));
        assertEquals("setString:12:den", jdbc.calls.get(11));
        assertEquals("setString:13:15", jdbc.calls.get(12));
        assertEquals("setString:14:RUNNING", jdbc.calls.get(13));
    }

    @Test
    void createsLoadSessionDeleteWithDistinctSourceIdAndNullPeriod() {
        RecordingJdbc jdbc = new RecordingJdbc(7002L, 1);
        DWHDeletionSessionRepository repository =
                new DWHDeletionSessionRepository(jdbc.dataSource());

        long id = repository.create(DWHDeletionSession.byLoadSession(
                DWHExcelLoadType.CD_DATA, 10521L
        ));

        assertEquals(7002L, id);
        assertEquals("setString:4:BY_LOAD_SESSION", jdbc.calls.get(3));
        assertEquals("setNull:5:" + Types.INTEGER, jdbc.calls.get(4));
        assertEquals("setNull:6:" + Types.INTEGER, jdbc.calls.get(5));
        assertEquals("setNull:7:" + Types.INTEGER, jdbc.calls.get(6));
        assertEquals("setLong:8:10521", jdbc.calls.get(7));
    }

    @Test
    void completesSuccessWithExactDeletedRowsAndFinishesErrorSeparately() {
        RecordingJdbc success = new RecordingJdbc(0L, 1);
        DWHDeletionSessionRepository repository =
                new DWHDeletionSessionRepository(success.dataSource());

        repository.completeSuccess(success.connection(), 7003L, 0L);

        assertTrue(success.sql.contains("DeletedRows = ?"));
        assertEquals(List.of(
                "setString:1:SUCCESS",
                "setLong:2:0",
                "setLong:3:7003"
        ), success.calls);

        RecordingJdbc error = new RecordingJdbc(0L, 1);
        repository = new DWHDeletionSessionRepository(error.dataSource());
        repository.completeError(7004L, "delete failed");

        assertTrue(error.sql.contains("FinishedAt = SYSDATETIME()"));
        assertEquals(List.of(
                "setString:1:ERROR",
                "setString:2:delete failed",
                "setLong:3:7004"
        ), error.calls);
    }

    private static final class RecordingJdbc {
        private final long generatedId;
        private final int updateCount;
        private final List<String> calls = new ArrayList<>();
        private String sql;

        private RecordingJdbc(long generatedId, int updateCount) {
            this.generatedId = generatedId;
            this.updateCount = updateCount;
        }

        private DataSource dataSource() {
            return (DataSource) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{DataSource.class},
                    (proxy, method, args) ->
                            "getConnection".equals(method.getName())
                                    ? connection()
                                    : defaultValue(method.getReturnType())
            );
        }

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if ("prepareStatement".equals(method.getName())) {
                            sql = (String) args[0];
                            calls.clear();
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
                        if (method.getName().startsWith("set")) {
                            calls.add(method.getName() + ":" + args[0] + ":" + args[1]);
                            return null;
                        }
                        if ("executeQuery".equals(method.getName())) {
                            return resultSet();
                        }
                        if ("executeUpdate".equals(method.getName())) {
                            return updateCount;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private ResultSet resultSet() {
            class State {
                boolean read;
            }
            State state = new State();
            return (ResultSet) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> {
                        if ("next".equals(method.getName())) {
                            if (state.read) return false;
                            state.read = true;
                            return true;
                        }
                        if ("getLong".equals(method.getName())) {
                            return generatedId;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            return null;
        }
    }
}
