package ru.stockmann.replenishment.services.storeturnover.process;

import ru.stockmann.replenishment.services.dwhexcelload.core.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class StoreTurnoverDeletionService {
    private final DataSource dataSource; private final StoreTurnoverDeletionRepository repository; private final DWHDeletionSessionRepository sessions;
    public StoreTurnoverDeletionService(DataSource ds,StoreTurnoverDeletionRepository repository){this(ds,repository,new DWHDeletionSessionRepository(ds));}
    StoreTurnoverDeletionService(DataSource ds,StoreTurnoverDeletionRepository repository,DWHDeletionSessionRepository sessions){this.dataSource=ds;this.repository=repository;this.sessions=sessions;}
    public DWHDataDeleteResult deleteByLoadSessionId(long sourceSession){
        if(sourceSession<=0)throw new IllegalArgumentException("loadSessionId must be positive");
        long deletionSession=sessions.create(DWHDeletionSession.byLoadSession(DWHExcelLoadType.STORE_TURNOVER,sourceSession));
        try(Connection c=dataSource.getConnection()){
            boolean old=c.getAutoCommit();try{c.setAutoCommit(false);int count=repository.deleteByLoadSessionId(c,sourceSession);sessions.completeSuccess(c,deletionSession,count);c.commit();return new DWHDataDeleteResult(count);}
            catch(RuntimeException|SQLException e){c.rollback();RuntimeException failure=e instanceof RuntimeException r?r:new RuntimeException(e);completeError(deletionSession,failure);throw failure;}
            finally{c.setAutoCommit(old);}
        }catch(SQLException e){RuntimeException failure=new RuntimeException("Failed StoreTurnover deletion",e);completeError(deletionSession,failure);throw failure;}
    }
    private void completeError(long id,RuntimeException failure){try{sessions.completeError(id,failure.getMessage());}catch(RuntimeException logging){failure.addSuppressed(logging);}}
}
