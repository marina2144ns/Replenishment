package ru.stockmann.replenishment.services.storeturnover.process;

import java.sql.*;
import java.util.List;

public class StoreTurnoverStageRepository {
    public int deleteByLoadSessionId(Connection c, long id) {
        try (PreparedStatement ps=c.prepareStatement("DELETE FROM dbo.StoreTurnover_stage WHERE LoadSessionId = ?")) {
            ps.setLong(1,id); return ps.executeUpdate();
        } catch(SQLException e){throw new RuntimeException("Failed to delete StoreTurnover_stage",e);}
    }
    public void insertBatch(Connection c,long session,List<StoreTurnoverStageRow> rows){
        if(rows==null||rows.isEmpty())return;
        String sql="""
                INSERT INTO dbo.StoreTurnover_stage
                (LoadSessionId, ExcelRowNum, sku, period, storeRus, remainingSum, remainingDays,
                 salesQuantity, sales, asp, revenue, gp, discountTotal, RawRowId)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try(PreparedStatement ps=c.prepareStatement(sql)){
            for(StoreTurnoverStageRow r:rows){
                if(r.loadSessionId()!=session)throw new IllegalArgumentException("Stage row belongs to another session");
                ps.setLong(1,r.loadSessionId()); if(r.excelRowNum()==null)ps.setNull(2,Types.BIGINT);else ps.setLong(2,r.excelRowNum());
                ps.setString(3,r.sku()); ps.setDate(4,Date.valueOf(r.period())); ps.setString(5,r.storeRus());
                ps.setInt(6,r.remainingSum()); ps.setInt(7,r.remainingDays()); ps.setInt(8,r.salesQuantity());
                ps.setInt(9,r.sales()); ps.setInt(10,r.asp()); ps.setInt(11,r.revenue()); ps.setInt(12,r.gp());
                ps.setInt(13,r.discountTotal()); ps.setLong(14,r.rawRowId()); ps.addBatch();
            }
            int[] counts=ps.executeBatch(); if(counts.length!=rows.size())throw new IllegalStateException("Unexpected stage batch count");
            for(int count:counts)if(count==Statement.EXECUTE_FAILED)throw new IllegalStateException("Stage batch failed");
        }catch(SQLException e){throw new RuntimeException("Failed to insert StoreTurnover_stage",e);}
    }
}
