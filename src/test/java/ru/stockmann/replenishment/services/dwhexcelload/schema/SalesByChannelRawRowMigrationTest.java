package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.normalizeSql;
import static ru.stockmann.replenishment.services.dwhexcelload.schema.DWHSchemaTestSupport.read;

class SalesByChannelRawRowMigrationTest {

    @Test
    void migrationSafelyAddsNullableRawRowIdAndMissingPeriodIndex() throws Exception {
        String sql = normalizeSql(read(
                "src/main/db/tables/SalesByChannel_raw_row_id_migration.sql"
        ));

        assertTrue(sql.contains(
                "col_length(n'dbo.salesbychannel', n'rawrowid') is null"
        ));
        assertTrue(sql.contains(
                "alter table dbo.salesbychannel add rawrowid bigint null"
        ));
        assertTrue(sql.contains(
                "col_length(n'dbo.salesbychannel_stage', n'rawrowid') is null"
        ));
        assertTrue(sql.contains(
                "alter table dbo.salesbychannel_stage add rawrowid bigint null"
        ));
        assertTrue(sql.contains("if not exists"));
        assertTrue(sql.contains(
                "create nonclustered index ix_salesbychannel_year_month "
                        + "on dbo.salesbychannel( year , month )"
        ));
        assertFalse(sql.contains("foreign key (rawrowid)"));
        assertFalse(sql.contains("index ix_salesbychannel_rawrowid"));
    }
}
