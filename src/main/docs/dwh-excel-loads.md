# DWH Excel Loads

WeeklyData, CDData, and CDEcom use the shared `AbstractDWHExcelLoader` runtime for Excel upload, raw row staging, common load sessions, common load errors, and status tracking.

Runtime state is stored in:

- `dbo.DWH_Excel_Load_Session`
- `dbo.DWH_Excel_Load_Error`

Runtime processing is implemented in Java:

- WeeklyData: `WeeklyDataProcessor`
- CDData: `CDDataProcessor`
- CDEcom: `CDEcomProcessor`

WeeklyData `Year` and `Week` are required by the Java validator. The raw table
keeps incoming values as nullable text so missing or malformed values can be
reported as validation errors, while target columns `dbo.Weekly_data.Year` and
`dbo.Weekly_data.Week` are `SMALLINT NOT NULL`. Existing databases must pass a
NULL precheck before applying the NOT NULL migration.

Their load definitions do not expose active runtime stored procedure names and throw `UnsupportedOperationException` from `processProcedureName()` to prevent accidental fallback to the default procedure hook.

The procedures `dbo.usp_WeeklyData_ProcessLoadSession`, `dbo.usp_CDData_ProcessLoadSession`, and `dbo.usp_CDEcom_ProcessLoadSession` are retained as historical SQL reference files. Java processing is the only intended runtime path for these three loaders. WeeklyData and CDData legacy procedures may be dropped manually after production verification of the Java flow.

CDEcom was not deployed to production before Java processing, so the current CDEcom DB install does not create `dbo.CD_ecom_load_session`, `dbo.CD_ecom_load_error`, or `dbo.usp_CDEcom_ProcessLoadSession`. The historical `usp_CDEcom_ProcessLoadSession.sql` file remains in the repository as reference only and is not part of the runtime path.

CDEcom-specific processing rules:

- `skuStyleColor` is cleaned as numeric text, parsed as `BigDecimal`, rounded to 0 digits with `RoundingMode.HALF_UP`, then range-checked as `BIGINT`.
- `planRub`, `stockStoresPcs`, and `stockStoresDdp` are direct `BIGINT` fields; fractional values are invalid and are not rounded.
- CDEcom decimal fields use `BigDecimal`, scale 2, `RoundingMode.HALF_UP`, and DECIMAL(18,2) range checking after rounding.
- Scientific notation is accepted by numeric parsing where `BigDecimal` accepts it; `NaN` and infinity values are rejected.
