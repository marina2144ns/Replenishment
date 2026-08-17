package ru.stockmann.replenishment.services.dwhexcelload.schema;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class StoreTurnoverSchemaContractTest {
    private static String read(String path)throws Exception{return Files.readString(Path.of(path)).replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);}
    @Test void ddlHasSourceOrientedRawAndTypedStageTarget()throws Exception{
        String ddl=read("src/main/db/tables/StoreTurnover_ddl.sql");
        String target=between(ddl,"create table dbo.storeturnover (","create table dbo.storeturnover_raw");
        String stage=between(ddl,"create table dbo.storeturnover_stage (","create index ix_storeturnover_loadsessionid");
        assertTrue(ddl.contains("create table dbo.storeturnover_raw"));assertTrue(ddl.contains("create table dbo.storeturnover_stage"));
        for(String nullable:java.util.List.of("sku nvarchar(255) null","period date null","storerus nvarchar(255) null","remainingsum int null","remainingdays int null","salesquantity int null","sales int null","asp int null","revenue int null","gp int null","discounttotal int null","loadsessionid bigint null","rawrowid bigint null"))assertTrue(target.contains(nullable),nullable);
        for(String required:java.util.List.of("sku nvarchar(255) not null","period date not null","storerus nvarchar(255) not null","remainingsum int not null","remainingdays int not null","salesquantity int not null","sales int not null","asp int not null","revenue int not null","gp int not null","discounttotal int not null","loadsessionid bigint not null","rawrowid bigint not null"))assertTrue(stage.contains(required),required);
        assertTrue(ddl.contains("ix_storeturnover_raw_loadsessionid_id"));assertFalse(ddl.contains("drop table"));assertFalse(ddl.contains("default 0"));
    }
    @Test void targetUpgradeIsIdempotentNonDestructiveAndLegacyCompatible()throws Exception{
        String migration=read("src/main/db/tables/storeturnover_v2_migration.sql");
        assertTrue(migration.contains("col_length(n'dbo.storeturnover', n'loadsessionid') is null"));
        assertTrue(migration.contains("add loadsessionid bigint null"));assertTrue(migration.contains("add rawrowid bigint null"));
        assertTrue(migration.contains("type_name(system_type_id)"));assertTrue(migration.contains("max_length"));assertTrue(migration.contains("is_nullable"));
        assertTrue(migration.contains("alter column sku nvarchar("));assertTrue(migration.contains("alter column storerus nvarchar("));
        assertTrue(migration.contains("@skumaxlength / 2 > 255"));assertTrue(migration.contains("@storerusmaxlength / 2 > 255"));
        assertTrue(migration.contains("@skutype = n'varchar' and @skumaxlength > 4000 then n'max'"));assertTrue(migration.contains("@storerustype = n'varchar' and @storerusmaxlength > 4000 then n'max'"));
        assertFalse(migration.contains("alter column period"));
        for(String metric:java.util.List.of("remainingsum","remainingdays","salesquantity","sales","asp","revenue","gp","discounttotal"))assertFalse(migration.contains("alter column "+metric),metric);
        assertFalse(migration.contains("alter column sku nvarchar(255) not null"));assertFalse(migration.contains("alter column storerus nvarchar(255) not null"));assertFalse(migration.contains("legacy target rows violate"));
        assertTrue(migration.contains("sys.indexes"));assertTrue(migration.contains("sys.foreign_keys"));
        for(String forbidden:java.util.List.of("update dbo.storeturnover","delete from","drop table","drop column","truncate"))assertFalse(migration.contains(forbidden),forbidden);
    }
    @Test void permissionsCoverV2AndRetainLegacyRights()throws Exception{
        String users=read("src/main/db/tables/Users.example.sql");
        assertTrue(users.contains("grant select, insert on object::dbo.storeturnover_raw"));
        assertTrue(users.contains("grant select, insert, delete on object::dbo.storeturnover_stage"));
        assertTrue(users.contains("loadstoreturnoverfromcsv"));assertTrue(users.contains("administer bulk operations"));
    }
    private static String between(String value,String start,String end){int from=value.indexOf(start);int to=value.indexOf(end,from+start.length());assertTrue(from>=0&&to>from);return value.substring(from,to);}
}
