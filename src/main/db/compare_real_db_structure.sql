USE [ReplenishmentDWH];
GO

/* ============================================================
   Real database structure snapshot for comparison with repository DDL

   Purpose:
   - inspect the actual table/column structure in ReplenishmentDWH;
   - inspect defaults and column properties;
   - inspect index definitions and indexed columns;
   - run before/after restore, migration, deployment or schema review;
   - compare the real database with src/main/db DDL files.

   Read-only diagnostic script. It does not modify the database.
   ============================================================ */

/* ============================================================
   1. TABLES + COLUMNS + DEFAULTS + INDEX MEMBERSHIP
   ============================================================ */

SELECT
    s.name AS SchemaName,
    t.name AS TableName,
    t.object_id AS TableObjectId,
    t.create_date AS TableCreateDate,
    t.modify_date AS TableModifyDate,

    c.column_id AS ColumnId,
    c.name AS ColumnName,
    ty.name AS DataType,
    c.max_length AS MaxLengthBytes,
    c.precision AS [Precision],
    c.scale AS Scale,
    c.is_nullable AS IsNullable,
    c.is_identity AS IsIdentity,
    c.is_computed AS IsComputed,
    c.collation_name AS CollationName,

    dc.name AS DefaultConstraintName,
    dc.definition AS DefaultDefinition,

    i.index_id AS IndexId,
    i.name AS IndexName,
    i.type_desc AS IndexType,
    i.is_unique AS IsUnique,
    i.is_primary_key AS IsPrimaryKey,
    i.is_unique_constraint AS IsUniqueConstraint,
    i.is_disabled AS IsDisabled,
    i.has_filter AS HasFilter,
    i.filter_definition AS FilterDefinition,

    ic.key_ordinal AS IndexKeyOrdinal,
    ic.is_descending_key AS IsDescendingKey,
    ic.is_included_column AS IsIncludedColumn

FROM sys.tables t
JOIN sys.schemas s
    ON s.schema_id = t.schema_id
JOIN sys.columns c
    ON c.object_id = t.object_id
JOIN sys.types ty
    ON ty.user_type_id = c.user_type_id
LEFT JOIN sys.default_constraints dc
    ON dc.parent_object_id = c.object_id
   AND dc.parent_column_id = c.column_id
LEFT JOIN sys.index_columns ic
    ON ic.object_id = c.object_id
   AND ic.column_id = c.column_id
LEFT JOIN sys.indexes i
    ON i.object_id = ic.object_id
   AND i.index_id = ic.index_id
WHERE t.is_ms_shipped = 0
ORDER BY
    s.name,
    t.name,
    c.column_id,
    i.index_id,
    ic.key_ordinal;
GO

/* ============================================================
   2. INDEX DEFINITIONS IN COMPACT FORM

   FOR XML PATH is used instead of STRING_AGG ... WITHIN GROUP
   for compatibility with the SQL Server version/compatibility level
   used by ReplenishmentDWH.
   ============================================================ */

SELECT
    s.name AS SchemaName,
    t.name AS TableName,
    i.index_id AS IndexId,
    i.name AS IndexName,
    i.type_desc AS IndexType,
    i.is_unique AS IsUnique,
    i.is_primary_key AS IsPrimaryKey,
    i.is_unique_constraint AS IsUniqueConstraint,
    i.is_disabled AS IsDisabled,
    i.has_filter AS HasFilter,
    i.filter_definition AS FilterDefinition,

    STUFF((
        SELECT
            N', ' + QUOTENAME(c2.name) +
            CASE
                WHEN ic2.is_descending_key = 1 THEN N' DESC'
                ELSE N' ASC'
            END
        FROM sys.index_columns ic2
        JOIN sys.columns c2
            ON c2.object_id = ic2.object_id
           AND c2.column_id = ic2.column_id
        WHERE ic2.object_id = i.object_id
          AND ic2.index_id = i.index_id
          AND ic2.is_included_column = 0
        ORDER BY ic2.key_ordinal, ic2.index_column_id
        FOR XML PATH(''), TYPE
    ).value('.', 'nvarchar(max)'), 1, 2, N'') AS KeyColumns,

    STUFF((
        SELECT
            N', ' + QUOTENAME(c2.name)
        FROM sys.index_columns ic2
        JOIN sys.columns c2
            ON c2.object_id = ic2.object_id
           AND c2.column_id = ic2.column_id
        WHERE ic2.object_id = i.object_id
          AND ic2.index_id = i.index_id
          AND ic2.is_included_column = 1
        ORDER BY ic2.index_column_id
        FOR XML PATH(''), TYPE
    ).value('.', 'nvarchar(max)'), 1, 2, N'') AS IncludedColumns

FROM sys.indexes i
JOIN sys.tables t
    ON t.object_id = i.object_id
JOIN sys.schemas s
    ON s.schema_id = t.schema_id
WHERE t.is_ms_shipped = 0
  AND i.index_id > 0
ORDER BY
    s.name,
    t.name,
    i.index_id;
GO
