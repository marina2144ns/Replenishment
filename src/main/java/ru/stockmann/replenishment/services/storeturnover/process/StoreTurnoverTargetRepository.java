package ru.stockmann.replenishment.services.storeturnover.process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StoreTurnoverTargetRepository {
    public int publishFromStage(Connection c,long session){
        try(PreparedStatement ps=c.prepareStatement("DELETE FROM dbo.StoreTurnover WHERE LoadSessionId = ?")){ps.setLong(1,session);ps.executeUpdate();}
        catch(SQLException e){throw new RuntimeException("Failed to delete StoreTurnover target session",e);}
        String sql="""
                INSERT INTO dbo.StoreTurnover
                (sku, period, storeRus, remainingSum, remainingDays, salesQuantity, sales, asp,
                 revenue, gp, discountTotal, LoadSessionId, RawRowId)
                SELECT sku, period, storeRus, remainingSum, remainingDays, salesQuantity, sales, asp,
                       revenue, gp, discountTotal, LoadSessionId, RawRowId
                FROM dbo.StoreTurnover_stage WHERE LoadSessionId = ?
                """;
        try(PreparedStatement ps=c.prepareStatement(sql)){ps.setLong(1,session);return ps.executeUpdate();}
        catch(SQLException e){throw new RuntimeException("Failed to publish StoreTurnover target",e);}
    }
}
