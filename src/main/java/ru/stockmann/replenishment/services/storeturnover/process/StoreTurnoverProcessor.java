package ru.stockmann.replenishment.services.storeturnover.process;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StoreTurnoverProcessor {
    private final DataSource dataSource; private final StoreTurnoverLoadSessionRepository sessions;
    private final StoreTurnoverRawRepository raw; private final StoreTurnoverStageRepository stage;
    private final StoreTurnoverErrorRepository errors; private final StoreTurnoverTargetRepository target;
    private final StoreTurnoverValidator validator;
    public StoreTurnoverProcessor(DataSource ds,StoreTurnoverLoadSessionRepository sessions,StoreTurnoverRawRepository raw,
                                  StoreTurnoverStageRepository stage,StoreTurnoverErrorRepository errors,
                                  StoreTurnoverTargetRepository target,StoreTurnoverValidator validator){
        this.dataSource=ds;this.sessions=sessions;this.raw=raw;this.stage=stage;this.errors=errors;this.target=target;this.validator=validator;
    }
    public StoreTurnoverProcessResult process(long id){
        long total=0,staged=0,errorCount=0;boolean validated=false;
        try{
            if(!sessions.existsById(id))return result(id,false,0,0,0,0,"Load session not found or has unexpected LoadTypeCode");
            validated=true;cleanup(id);long last=StoreTurnoverRawRepository.INITIAL_LAST_RAW_ID;
            while(true){List<StoreTurnoverRawRow> chunk=raw.findChunk(id,last);if(chunk.isEmpty())break;
                List<StoreTurnoverStageRow> valid=new ArrayList<>();List<StoreTurnoverValidationError> invalid=new ArrayList<>();
                for(StoreTurnoverRawRow row:chunk){StoreTurnoverRowValidationResult r=validator.validateAndMap(row);if(r.valid())valid.add(r.stageRow());else invalid.addAll(r.errors());}
                write(id,valid,invalid);total+=chunk.size();staged+=valid.size();errorCount+=invalid.size();last=chunk.get(chunk.size()-1).id();
            }
            if(errorCount>0)return result(id,false,total,staged,0,errorCount,"Validation failed; target was not changed");
            if(staged!=total)throw new IllegalStateException("StoreTurnover processing counter mismatch");
            long loaded=publish(id,staged);return result(id,true,total,staged,loaded,0,"StoreTurnover processed and published successfully");
        }catch(RuntimeException e){if(validated)errors.insertBestEffort(new StoreTurnoverValidationError(id,0L,null,"PROCESSING",null,"UNEXPECTED_PROCESSING_ERROR",e.getMessage(),"Unexpected processing error: "+e.getMessage()));return result(id,false,total,staged,0,errorCount+1,e.getMessage());}
    }
    private void cleanup(long id){transaction(c->{stage.deleteByLoadSessionId(c,id);errors.deleteByLoadSessionId(c,id);});}
    private void write(long id,List<StoreTurnoverStageRow> valid,List<StoreTurnoverValidationError> invalid){transaction(c->{stage.insertBatch(c,id,valid);errors.insertBatch(c,id,invalid);});}
    private long publish(long id,long expected){final int[] published={0};transaction(c->{published[0]=target.publishFromStage(c,id);if(published[0]!=expected)throw new IllegalStateException("StoreTurnover publish row count mismatch");int cleaned=stage.deleteByLoadSessionId(c,id);if(cleaned!=expected)throw new IllegalStateException("StoreTurnover stage cleanup row count mismatch");});return published[0];}
    private void transaction(SqlWork work){try(Connection c=dataSource.getConnection()){boolean old=c.getAutoCommit();try{c.setAutoCommit(false);work.run(c);c.commit();}catch(RuntimeException|SQLException e){c.rollback();throw e instanceof RuntimeException r?r:new RuntimeException(e);}finally{c.setAutoCommit(old);}}catch(SQLException e){throw new RuntimeException(e);}}
    private StoreTurnoverProcessResult result(long id,boolean ok,long total,long staged,long loaded,long errors,String message){return new StoreTurnoverProcessResult(id,ok,total,staged,loaded,errors,message);}
    @FunctionalInterface private interface SqlWork{void run(Connection c)throws SQLException;}
}
