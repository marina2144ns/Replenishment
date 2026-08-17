package ru.stockmann.replenishment.services.storeturnover.process;

import javax.sql.DataSource;
import java.sql.*;

public class StoreTurnoverLoadSessionRepository {
    private final DataSource dataSource;
    public StoreTurnoverLoadSessionRepository(DataSource dataSource){this.dataSource=dataSource;}
    public boolean existsById(long id){
        try(Connection c=dataSource.getConnection();PreparedStatement ps=c.prepareStatement("SELECT 1 FROM dbo.DWH_Excel_Load_Session WHERE Id = ? AND LoadTypeCode = 'STORE_TURNOVER'")){
            ps.setLong(1,id);try(ResultSet rs=ps.executeQuery()){return rs.next();}
        }catch(SQLException e){throw new RuntimeException("Failed to check StoreTurnover session",e);}
    }
}
