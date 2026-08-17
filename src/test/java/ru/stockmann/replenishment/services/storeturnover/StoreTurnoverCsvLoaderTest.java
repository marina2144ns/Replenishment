package ru.stockmann.replenishment.services.storeturnover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.StoreTurnoverExcelLoadDefinition;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StoreTurnoverCsvLoaderTest {
    private static final String HEADER="Sku;Period;StoreRus;RemainingSum;RemainingDays;SalesQuantity;Sales;Asp;Revenue;Gp;DiscountTotal";
    @TempDir Path temp;

    @Test void strictHeaderRejectsEverySchemaMismatch(){
        TestLoader loader=new TestLoader(null);String[] valid=HEADER.split(";",-1);loader.header(valid);
        List<String[]> invalid=new ArrayList<>();
        invalid.add(Arrays.copyOf(valid,10));invalid.add(new String[]{"Sku","Period","StoreRus","RemainingSum","RemainingDays","SalesQuantity","Sales","Asp","Revenue","Gp","DiscountTotal","Extra"});
        String[] extraMiddle=new String[12];System.arraycopy(valid,0,extraMiddle,0,5);extraMiddle[5]="Extra";System.arraycopy(valid,5,extraMiddle,6,6);invalid.add(extraMiddle);
        for(int i=0;i<8;i++){String[] copy=valid.clone();if(i==0)copy[0]="sku";if(i==1)copy[0]=" Sku";if(i==2)copy[0]="Sku ";if(i==3)copy[0]="S ku";if(i==4)copy[0]="";if(i==5)copy[0]="Renamed";if(i==6){copy[0]="Period";copy[1]="Sku";}if(i==7){copy[5]="Extra";}invalid.add(copy);}
        for(String[] headers:invalid)assertThrows(IllegalArgumentException.class,()->loader.header(headers));
    }

    @Test void streamingCsvUsesSemicolonQuotesUtf8AndPhysicalRowNumbers()throws Exception{
        RecordingDb db=new RecordingDb();TestLoader loader=new TestLoader(db.dataSource());Path file=temp.resolve("source.csv");
        Files.writeString(file,"\uFEFF"+HEADER+"\r\nsku;08.2026;\"Магазин;Центр\";;;;;;;;\r\n",StandardCharsets.UTF_8);
        loader.read(file,77L);
        assertEquals(1,db.rows.size());assertEquals(77L,db.rows.get(0).get(1));assertEquals(2L,db.rows.get(0).get(2));
        assertEquals("Магазин;Центр",db.rows.get(0).get(5));assertNull(db.rows.get(0).get(6));assertEquals(1,db.commits);assertEquals(0,db.rollbacks);
    }

    @Test void headerOnlyIsValidButEmptyAndMalformedRowsRollback()throws Exception{
        RecordingDb headerDb=new RecordingDb();Path headerOnly=temp.resolve("header.csv");Files.writeString(headerOnly,HEADER+"\n");new TestLoader(headerDb.dataSource()).read(headerOnly,1L);assertEquals(1,headerDb.commits);assertTrue(headerDb.rows.isEmpty());
        for(String content:List.of("",HEADER+"\na;08.2026;x;1\n",HEADER+"\na;08.2026;x;1;2;3;4;5;6;7;8;9\n")){
            RecordingDb db=new RecordingDb();Path file=temp.resolve("bad"+Math.abs(content.hashCode())+".csv");Files.writeString(file,content);
            assertThrows(Exception.class,()->new TestLoader(db.dataSource()).read(file,2L));assertEquals(1,db.rollbacks);assertTrue(db.rows.isEmpty());
        }
    }

    private static final class TestLoader extends StoreTurnoverBulkLoader{
        TestLoader(DataSource ds){super(ds,new StoreTurnoverExcelLoadDefinition(),null);}void header(String[] h){validateHeaderRow(h);}void read(Path p,long id)throws Exception{readAndInsertExcel(p.toString(),id);}
    }
    private static final class RecordingDb{
        List<Map<Integer,Object>> rows=new ArrayList<>();int commits;int rollbacks;
        DataSource dataSource(){return (DataSource)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{DataSource.class},(p,m,a)->m.getName().equals("getConnection")?connection():defaultValue(m.getReturnType()));}
        Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},new java.lang.reflect.InvocationHandler(){boolean auto=true;public Object invoke(Object p,java.lang.reflect.Method m,Object[] a){return switch(m.getName()){case "prepareStatement"->statement();case "getAutoCommit"->auto;case "setAutoCommit"->{auto=(Boolean)a[0];yield null;}case "commit"->{commits++;yield null;}case "rollback"->{rollbacks++;rows.clear();yield null;}default->defaultValue(m.getReturnType());};}});}
        PreparedStatement statement(){return (PreparedStatement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},new java.lang.reflect.InvocationHandler(){Map<Integer,Object> current=new HashMap<>();List<Map<Integer,Object>> batch=new ArrayList<>();public Object invoke(Object p,java.lang.reflect.Method m,Object[] a){return switch(m.getName()){case "setLong","setString"->{current.put((Integer)a[0],a[1]);yield null;}case "addBatch"->{batch.add(new HashMap<>(current));yield null;}case "executeBatch"->{rows.addAll(batch);int[] result=new int[batch.size()];Arrays.fill(result,1);batch.clear();yield result;}default->defaultValue(m.getReturnType());};}});}
    }
    private static Object defaultValue(Class<?> t){if(!t.isPrimitive())return null;if(t==boolean.class)return false;if(t==int.class)return 0;if(t==long.class)return 0L;return null;}
}
