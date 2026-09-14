# CD Data plan_rub decimal migration — September 2026

This migration changes `dbo.CD_data.plan_rub` and
`dbo.CD_data_stage.plan_rub` from `INT NOT NULL` to
`DECIMAL(18,2) NOT NULL`.

Existing integer values are preserved exactly because the complete SQL Server
`INT` range fits in `DECIMAL(18,2)`. The migration does not update application
data.

## Production run order

1. Stop CD Data loads and ensure that no application process writes to
   `dbo.CD_data` or `dbo.CD_data_stage`.
2. Take and verify a current database backup.
3. Run `00_precheck.sql`. Do not continue if it reports an unexpected column
   type or nullable contract.
4. Run `01_production_migration.sql`.
5. Run `02_verify.sql` and confirm that both columns are
   `DECIMAL(18,2) NOT NULL`.
6. Deploy the new application version. Do not deploy it before the database
   migration is complete.
7. Perform a test CD Data load, including a fractional `Plan, rub` value.

The scripts accept both the original `INT NOT NULL` state and an already
migrated `DECIMAL(18,2) NOT NULL` state. Any other type or nullability blocks
the migration.
