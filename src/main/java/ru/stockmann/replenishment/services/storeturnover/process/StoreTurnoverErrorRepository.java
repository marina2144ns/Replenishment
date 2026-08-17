package ru.stockmann.replenishment.services.storeturnover.process;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

public class StoreTurnoverErrorRepository {
    private final DataSource dataSource;
    public StoreTurnoverErrorRepository(DataSource dataSource){this.dataSource=dataSource;}
    public void deleteByLoadSessionId(Connection c,long id){
        try(PreparedStatement ps=c.prepareStatement("DELETE FROM dbo.DWH_Excel_Load_Error WHERE LoadSessionId = ? AND LoadTypeCode = 'STORE_TURNOVER'")){ps.setLong(1,id);ps.executeUpdate();}
        catch(SQLException e){throw new RuntimeException("Failed to delete StoreTurnover errors",e);}
    }
    public void insertBatch(Connection c,long session,List<StoreTurnoverValidationError> errors){
        if(errors==null||errors.isEmpty())return;
        String sql="""
                INSERT INTO dbo.DWH_Excel_Load_Error
                (LoadSessionId, LoadTypeCode, ErrorLayer, ExcelRowNum, RawId, FieldName, ErrorCode, ErrorReason, ErrorMessage)
                VALUES (?, 'STORE_TURNOVER', ?, ?, ?, ?, ?, ?, ?)
                """;
        try(PreparedStatement ps=c.prepareStatement(sql)){
            for(StoreTurnoverValidationError e:errors){
                if(e.loadSessionId()!=session)throw new IllegalArgumentException("Error belongs to another session");
                ps.setLong(1,e.loadSessionId());ps.setString(2,e.errorLayer());nullableLong(ps,3,e.excelRowNum());nullableLong(ps,4,e.rawId());
                ps.setString(5,e.fieldName());ps.setString(6,e.errorCode());ps.setString(7,e.errorReason());ps.setString(8,e.errorMessage());ps.addBatch();
            }
            int[] counts=ps.executeBatch();if(counts.length!=errors.size())throw new IllegalStateException("Unexpected error batch count");
        }catch(SQLException e){throw new RuntimeException("Failed to insert StoreTurnover errors",e);}
    }
    public void insertBestEffort(StoreTurnoverValidationError error){try(Connection c=dataSource.getConnection()){insertBatch(c,error.loadSessionId(),List.of(error));}catch(Exception ignored){}}
    private void nullableLong(PreparedStatement ps,int index,Long value)throws SQLException{if(value==null)ps.setNull(index,Types.BIGINT);else ps.setLong(index,value);}
}
