# DWH Excel Loads

WeeklyData, CDData, and CDEcom use the shared `AbstractDWHExcelLoader` runtime for Excel upload, raw row staging, common load sessions, common load errors, and status tracking.

Runtime state is stored in:

- `dbo.DWH_Excel_Load_Session`
- `dbo.DWH_Excel_Load_Error`

CDEcom runtime processing is implemented in Java by `CDEcomProcessor`; it no longer calls `dbo.usp_CDEcom_ProcessLoadSession` at runtime.

The procedure `dbo.usp_CDEcom_ProcessLoadSession` is retained temporarily as rollback/reference material until production verification is complete.

Legacy CDEcom tables `dbo.CD_ecom_load_session` and `dbo.CD_ecom_load_error` are kept for production verification and can be removed in a later cleanup migration after the common DWH flow is verified.

CDEcom-specific processing rules:

- `skuStyleColor` is cleaned as numeric text, parsed as `BigDecimal`, rounded to 0 digits with `RoundingMode.HALF_UP`, then range-checked as `BIGINT`.
- `planRub`, `stockStoresPcs`, and `stockStoresDdp` are direct `BIGINT` fields; fractional values are invalid and are not rounded.
- CDEcom decimal fields use `BigDecimal`, scale 2, `RoundingMode.HALF_UP`, and DECIMAL(18,2) range checking after rounding.
- Scientific notation is accepted by numeric parsing where `BigDecimal` accepts it; `NaN` and infinity values are rejected.
