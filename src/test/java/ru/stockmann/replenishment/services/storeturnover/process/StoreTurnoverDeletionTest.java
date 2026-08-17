package ru.stockmann.replenishment.services.storeturnover.process;

import org.junit.jupiter.api.Test;
import ru.stockmann.replenishment.services.dwhexcelload.core.*;
import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

class StoreTurnoverDeletionTest {
    @Test void loadSessionDeleteIsTransactionalAndAudited(){
        Tx tx=new Tx();Sessions sessions=new Sessions();StoreTurnoverDeletionRepository repository=new StoreTurnoverDeletionRepository(){@Override public int deleteByLoadSessionId(Connection c,long id){assertEquals(44,id);return 3;}};
        DWHDataDeleteResult result=new StoreTurnoverDeletionService(tx.ds(),repository,sessions).deleteByLoadSessionId(44);
        assertEquals(3,result.deletedRows());assertEquals(DWHExcelLoadType.STORE_TURNOVER,sessions.created.loadType());assertEquals(DWHDeletionOperationMode.BY_LOAD_SESSION,sessions.created.operationMode());assertEquals(44,sessions.created.sourceLoadSessionId());assertEquals(3,sessions.deleted);assertTrue(tx.commit);
    }
    @Test void repositoryPredicateCannotTouchLegacyNullOrOtherSessions(){
        StringBuilder sql=new StringBuilder();Connection c=(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->m.getName().equals("prepareStatement")?Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{java.sql.PreparedStatement.class},(pp,mm,aa)->{if(mm.getName().equals("setLong"))assertEquals(44L,aa[1]);if(mm.getName().equals("executeUpdate"))return 2;return null;}):null);
        Connection recording=(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{if(m.getName().equals("prepareStatement")){sql.append(a[0]);return c.prepareStatement((String)a[0]);}return null;});
        assertEquals(2,new StoreTurnoverDeletionRepository().deleteByLoadSessionId(recording,44));assertTrue(sql.toString().contains("WHERE LoadSessionId = ?"));assertFalse(sql.toString().contains("IS NULL"));
    }
    private static class Sessions extends DWHDeletionSessionRepository{DWHDeletionSession created;long deleted;Sessions(){super(null);}@Override public long create(DWHDeletionSession s){created=s;return 99;}@Override public void completeSuccess(Connection c,long id,long count){deleted=count;}@Override public void completeError(long id,String message){fail(message);}}
    private static class Tx{boolean commit;DataSource ds(){return (DataSource)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{DataSource.class},(p,m,a)->m.getName().equals("getConnection")?connection():null);}Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->switch(m.getName()){case "getAutoCommit"->true;case "commit"->{commit=true;yield null;}case "setAutoCommit","close","rollback"->null;default->null;});}}
}
