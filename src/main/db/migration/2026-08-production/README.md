# Production migration — August 2026

Temporary migration pack for upgrading the existing production `ReplenishmentDWH` database to the schema currently expected by the application.

This directory is intentionally temporary. After the migration has been successfully applied to production and verified, remove this directory together with the superseded one-off migration files under `src/main/db/tables`.

## Scope

The migration brings the existing production database to the current contracts for:

1. `Weekly_data` / `Weekly_data_stage` zero-metric contract.
2. `CD_data` / `CD_data_stage` required delete keys and zero-metric contract.
3. `CD_ecom` / `CD_ecom_stage` required delete keys and zero-metric contract.
4. `DWH_Excel_Load_Session` deletion metadata.
5. `DWH_Excel_Load_Error` lookup index.
6. Required CDData/CDEcom delete-key indexes.
7. `StoreTurnover` v2 coexistence migration, including `StoreTurnover_raw`, `StoreTurnover_stage`, `LoadSessionId`, `RawRowId`, foreign keys and indexes.

`SalesByChannel` is not altered by this pack because the restored production snapshot already matches the current target contract for `year`, `month`, zero metrics and indexes.

The migration deliberately does **not** convert legacy `StoreTurnover.LoadDateTime DATETIME NULL DEFAULT GETDATE()` to the fresh-install `DATETIME2(0) NOT NULL DEFAULT SYSDATETIME()` definition. The v2 migration preserves compatibility with the legacy v1 target table while adding the new v2 pipeline around it.

## Files

- `00_precheck.sql` — read-only checks. Run first.
- `01_production_migration.sql` — production schema/data migration.
- `02_verify.sql` — read-only post-migration verification.

## Production run order

### 1. Stop application writes

Stop or disable the Replenishment application/Tomcat deployment so no loader writes to the affected tables while the migration is running.

### 2. Take a production backup

Take and verify a fresh backup before any schema changes.

### 3. Run `00_precheck.sql`

The important result sets are the CDData and CDEcom required-key violations.

The restored production-like database used for rehearsal contained one legacy invalid `CD_ecom` target row originating from an empty Excel row. Do **not** assume the live production row has the same identity. If precheck returns any rows:

1. inspect the target row;
2. inspect its `RawRowId` in the corresponding RAW table;
3. inspect its `LoadSessionId` in `DWH_Excel_Load_Session`;
4. remove only rows confirmed to be invalid legacy data;
5. rerun `00_precheck.sql` until both required-key violation result sets are empty.

Do not continue while invalid required-key rows remain.

### 4. Run `01_production_migration.sql`

Run the complete file in SSMS against `ReplenishmentDWH`.

The script is guarded with metadata checks and is designed to tolerate already-applied parts of the migration. Required-key conversions for CDData and CDEcom are transactional. Zero-metric conversions replace legacy `NULL` values with `0` before changing the columns to `NOT NULL`.

If any error is raised, stop. Do not continue with deployment until the cause is understood and the migration has been rerun successfully.

### 5. Run `02_verify.sql`

Verification must show:

- Weekly zero metrics are `NOT NULL` in target and stage.
- CDData `nazvanie/god/sezon/den` are `NOT NULL` in target and stage.
- CDData zero metrics are `NOT NULL` in target and stage.
- CDEcom `name/year/season/day` are `NOT NULL` in target and stage.
- CDEcom zero metrics are `NOT NULL` in target and stage.
- current deletion metadata columns exist in `DWH_Excel_Load_Session`.
- `IX_DWH_Excel_Load_Error_LoadSessionId` exists.
- `IX_CD_data_nazvanie_den`, `IX_CD_data_god_sezon`, `IX_CD_ecom_name_day`, `IX_CD_ecom_year_season` exist and are enabled.
- `StoreTurnover_raw` and `StoreTurnover_stage` exist with the v2 columns.
- `StoreTurnover.LoadSessionId` and `StoreTurnover.RawRowId` exist.
- StoreTurnover v2 indexes and foreign keys exist and are enabled/trusted where applicable.

### 6. Apply security scripts

After schema migration, apply the current project user/login/grant contract (`Users.sql` locally, based on `Users.example.sql`). Do not rely on users or grants restored from a backup.

### 7. Deploy application and smoke-test loaders

Deploy the application only after schema verification succeeds. Test the canonical services and StoreTurnover v2 against the migrated database.

### 8. Cleanup after successful production migration

After production has been verified and is stable:

- remove this temporary migration directory;
- remove superseded one-off `*_migration.sql` files from `src/main/db/tables`;
- keep the canonical `*_ddl.sql` files as the fresh-install database contract.
