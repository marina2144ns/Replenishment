package ru.stockmann.replenishment.services.storeturnover.process;

import java.sql.*;

public class StoreTurnoverDeletionRepository {
    public int deleteByLoadSessionId(Connection c,long id){
        try(PreparedStatement ps=c.prepareStatement("DELETE FROM dbo.StoreTurnover WHERE LoadSessionId = ?")){ps.setLong(1,id);return ps.executeUpdate();}
        catch(SQLException e){throw new RuntimeException("Failed to delete StoreTurnover rows by load session",e);}
    }
}
