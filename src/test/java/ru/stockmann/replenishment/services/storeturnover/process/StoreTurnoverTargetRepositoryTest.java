package ru.stockmann.replenishment.services.storeturnover.process;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StoreTurnoverTargetRepositoryTest {
    @Test void publishIsStrictlyCurrentSessionScopedAndSupportsEmptyStage(){
        List<String> sql=new ArrayList<>();List<Long> params=new ArrayList<>();int[] calls={0};
        Connection connection=(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("prepareStatement")){sql.add((String)a[0]);int call=calls[0]++;return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(pp,mm,aa)->{
                if(mm.getName().equals("setLong")){params.add((Long)aa[1]);return null;}if(mm.getName().equals("executeUpdate"))return call==0?3:0;return defaultValue(mm.getReturnType());});}
            return defaultValue(m.getReturnType());});
        assertEquals(0,new StoreTurnoverTargetRepository().publishFromStage(connection,42L));
        assertEquals(List.of(42L,42L),params);
        String delete=normalize(sql.get(0));String insert=normalize(sql.get(1));
        assertEquals("DELETE FROM DBO.STORETURNOVER WHERE LOADSESSIONID = ?",delete);
        assertTrue(insert.contains("FROM DBO.STORETURNOVER_STAGE WHERE LOADSESSIONID = ?"));
        assertFalse(delete.contains("PERIOD"));assertFalse(delete.contains("SKU"));assertFalse(delete.contains("STORERUS"));
    }
    private static String normalize(String s){return s.replaceAll("\\s+"," ").trim().toUpperCase();}
    private static Object defaultValue(Class<?> t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;return null;}
}
