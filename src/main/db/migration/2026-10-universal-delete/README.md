# Universal delete session criteria migration

Run in order against the target SQL Server database:

1. `00_precheck.sql`
2. `01_production_migration.sql`
3. `02_verify.sql`

The migration is additive and idempotent. It adds nullable
`dbo.DWH_Excel_Load_Session.DeleteCriteria NVARCHAR(MAX)` and does not remove or
modify any legacy deletion metadata columns.
