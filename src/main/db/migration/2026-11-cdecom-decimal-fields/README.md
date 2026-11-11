# CDEcom decimal fields migration — November 2026

This SQL Server 2014-compatible migration changes exactly six columns: `planRub`,
`stockStoresPcs`, and `stockStoresDdp` in `dbo.CD_ecom` and
`dbo.CD_ecom_stage`. The target contract is `DECIMAL(18,2) NOT NULL`.

The scripts do not delete, truncate, or recreate tables and do not update data.
They accept either the original `BIGINT NOT NULL` state or the already migrated
`DECIMAL(18,2) NOT NULL` state. Any other type, precision, scale, or nullability
blocks execution.

## Production order

1. Stop CDEcom uploads and all writers to both tables. Keep them stopped through
   verification.
2. Take a current full database backup and verify that it can be restored.
3. Run `00_precheck.sql` with an account that can read catalog views and both
   tables. Save both the metadata and row-count/control-total result sets.
4. Review the dependency result. Rows with `DependencyType = STATISTICS`,
   `IsAutoCreated = 1`, and `IsBlocking = 0` are diagnostic only and are removed
   automatically by the production script. Do not continue for any row with
   `IsBlocking = 1`, including user-created statistics, indexes, constraints, or
   schema-bound expressions. Script and resolve such dependencies in a separately
   reviewed change, then rerun the precheck.
5. Run `01_production_migration.sql` during an approved maintenance window.
6. Run `02_verify.sql`. Confirm six `decimal / 18 / 2 / is_nullable=0` rows,
   compare both row counts and three control totals with the saved precheck output,
   and retain the statistics diagnostic result. Auto-created statistics do not
   need to be present: SQL Server may recreate them later when queries require
   them.
7. Deploy the matching application and run one CDEcom test load containing integer,
   fractional, negative, comma-decimal, and empty metric values. Verify a repeated
   load and both deletion paths.

## Locks and duration

`ALTER COLUMN` takes schema-modification locks. The migration also acquires table
locks before capturing its in-transaction control totals, so reads and writes may
block for the duration. On large tables the conversion can scan and rewrite data,
grow the transaction log, and take materially longer than a metadata-only change.
Estimate duration and log space on a production-sized restored backup first. Do
not run concurrently with uploads, deletes, index maintenance, or reporting jobs.

## Safety and repeat execution

The migration runs in one transaction with `XACT_ABORT ON`. Before alteration it
checks table/column existence, accepted source states, conversion safety, and
dependencies. Before commit it verifies the final metadata plus row counts and
numeric control totals. A failure rolls the transaction back. Re-running after a
successful deployment performs the checks and skips all six `ALTER COLUMN`
statements because the columns already have the target type.

Immediately before `ALTER COLUMN`, while the transaction holds table locks, the
script discovers statistics through `sys.stats`, `sys.stats_columns`, and
`sys.columns`. It drops only rows where `sys.stats.auto_created = 1` and only when
they reference one of the six source `BIGINT` columns. Statistics are not selected
by `_WA_Sys_` naming. User-created statistics remain blocking, and the script does
not recreate automatic statistics manually. Dropped automatic statistics and all
column alterations roll back together if the transaction fails.

## Recovery

If the migration fails before commit, investigate the reported guard and confirm
the transaction rolled back; rerun `00_precheck.sql` before trying again. If a
problem is discovered after commit, stop writers and restore the verified backup
according to the database recovery procedure. Do not alter the columns back to
`BIGINT`: fractional values written after deployment cannot be represented without
data loss.
