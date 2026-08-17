package ru.stockmann.replenishment.services.storeturnover.process;

import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StoreTurnoverProcessorTest {
    @Test void validRowsPublishAndInvalidRowsBlockAllPublication(){
        Tx tx=new Tx();FakeStage stage=new FakeStage();FakeTarget target=new FakeTarget();FakeErrors errors=new FakeErrors();
        StoreTurnoverProcessResult ok=processor(tx,new FakeRaw(List.of(List.of(row(1,"1")),List.of())),stage,errors,target).process(10);
        assertTrue(ok.success());assertEquals(1,ok.loadedRows());assertEquals(2,stage.cleanups);assertEquals(3,tx.commits);

        Tx invalidTx=new Tx();FakeTarget untouched=new FakeTarget();FakeErrors invalidErrors=new FakeErrors();
        StoreTurnoverProcessResult invalid=processor(invalidTx,new FakeRaw(List.of(List.of(row(2,"abc")),List.of())),new FakeStage(),invalidErrors,untouched).process(10);
        assertFalse(invalid.success());assertEquals(0,untouched.calls);assertEquals(1,invalidErrors.persisted.size());
    }

    @Test void emptyValidSessionPublishesZeroAndCommits(){
        Tx tx=new Tx();FakeStage stage=new FakeStage();FakeTarget target=new FakeTarget();target.count=0;
        StoreTurnoverProcessResult result=processor(tx,new FakeRaw(List.of(List.of())),stage,new FakeErrors(),target).process(10);
        assertTrue(result.success());assertEquals(0,result.totalRows());assertEquals(0,result.stagedRows());assertEquals(0,result.loadedRows());
        assertEquals(1,target.calls);assertEquals(2,stage.cleanups);assertEquals(2,tx.commits);assertEquals(0,tx.rollbacks);
    }

    @Test void publishOrCleanupMismatchRollsBackAtomicTransaction(){
        Tx tx=new Tx();FakeStage stage=new FakeStage();stage.publishCleanupOverride=0;FakeErrors errors=new FakeErrors();
        StoreTurnoverProcessResult result=processor(tx,new FakeRaw(List.of(List.of(row(1,"1")),List.of())),stage,errors,new FakeTarget()).process(10);
        assertFalse(result.success());assertEquals(1,tx.rollbacks);assertEquals(1,errors.bestEffort.size());
    }

    private StoreTurnoverProcessor processor(Tx tx,FakeRaw raw,FakeStage stage,FakeErrors errors,FakeTarget target){return new StoreTurnoverProcessor(tx.ds(),new FakeSessions(),raw,stage,errors,target,new StoreTurnoverValidator());}
    private StoreTurnoverRawRow row(long id,String metric){return new StoreTurnoverRawRow(id,10L,id+1,"sku","08.2026","Store",metric,"","","","","","","");}

    private static class FakeSessions extends StoreTurnoverLoadSessionRepository{FakeSessions(){super(null);}@Override public boolean existsById(long id){return true;}}
    private static class FakeRaw extends StoreTurnoverRawRepository{Deque<List<StoreTurnoverRawRow>> chunks;FakeRaw(List<List<StoreTurnoverRawRow>> c){super(null,10);chunks=new ArrayDeque<>(c);}@Override public List<StoreTurnoverRawRow> findChunk(long s,long l){return chunks.removeFirst();}}
    private static class FakeStage extends StoreTurnoverStageRepository{int cleanups;int publishCleanupOverride=-1;List<StoreTurnoverStageRow> rows=new ArrayList<>();@Override public int deleteByLoadSessionId(Connection c,long id){cleanups++;if(cleanups>1&&publishCleanupOverride>=0)return publishCleanupOverride;return rows.size();}@Override public void insertBatch(Connection c,long id,List<StoreTurnoverStageRow> r){rows.addAll(r);}}
    private static class FakeTarget extends StoreTurnoverTargetRepository{int calls;int count=1;@Override public int publishFromStage(Connection c,long id){calls++;return count;}}
    private static class FakeErrors extends StoreTurnoverErrorRepository{List<StoreTurnoverValidationError> persisted=new ArrayList<>(),bestEffort=new ArrayList<>();FakeErrors(){super(null);}@Override public void deleteByLoadSessionId(Connection c,long id){}@Override public void insertBatch(Connection c,long id,List<StoreTurnoverValidationError> e){persisted.addAll(e);}@Override public void insertBestEffort(StoreTurnoverValidationError e){bestEffort.add(e);}}
    private static class Tx{int commits,rollbacks;DataSource ds(){return (DataSource)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{DataSource.class},(p,m,a)->m.getName().equals("getConnection")?connection():null);}Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->switch(m.getName()){case "getAutoCommit"->true;case "commit"->{commits++;yield null;}case "rollback"->{rollbacks++;yield null;}case "setAutoCommit","close"->null;default->null;});}}
}
