# DWH Excel Loads

WeeklyData, CDData, and CDEcom use the shared `AbstractDWHExcelLoader` runtime for Excel upload, raw row staging, common load sessions, common load errors, and status tracking.

Runtime state is stored in:

- `dbo.DWH_Excel_Load_Session`
- `dbo.DWH_Excel_Load_Error`

CDEcom stage 1 is migrated to the common framework for upload and status tracking, but its load-session processing is still performed by `dbo.usp_CDEcom_ProcessLoadSession`.

The next CDEcom migration stage is to move `dbo.usp_CDEcom_ProcessLoadSession` processing logic to Java, following the established WeeklyData and CDData processor pattern.

Legacy CDEcom tables `dbo.CD_ecom_load_session` and `dbo.CD_ecom_load_error` are kept for production verification and can be removed in a later cleanup migration after the common DWH flow is verified.
