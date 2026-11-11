# requestedBy unification report

Date: 2026-11-11

## Result

Session-based loading and universal deletion use one contract: optional
`requestedBy` is read from the query string first, falls back to the existing
JSON field, and becomes `NULL` when the selected value is absent or blank.
`DWHRequestedBy` remains the single normalization and 100-character validation
mechanism. SQL Server schema and business transactions were not changed.

## Endpoint audit

| Endpoint | Operation | Query `requestedBy` | JSON compatibility | Result |
|---|---|---:|---:|---|
| `POST /cddata/v1.0/bulk` | load | optional | yes | unified |
| `POST /cdecom/v1.0/bulk` | load | optional | yes | unified |
| `POST /weeklydata/v1.0/bulk` | load | optional | yes | unified |
| `POST /salesbychannel/v1.0/bulk` | load | optional | yes | unified |
| `POST /storeturnover/v2.0/bulk` | load | optional | yes | unified |
| `POST /data/v1.0/delete` | universal delete | optional | yes | unified; URL wins |
| `DELETE /cddata/v1.0/god-sezon` | legacy delete | optional | n/a | contract preserved |
| `DELETE /cddata/v1.0/session` | legacy delete | optional | n/a | contract preserved |
| `DELETE /cddata/v1.0/nazvanie-den` | legacy delete | optional | n/a | contract preserved |
| `DELETE /cdecom/v1.0/year-season` | legacy delete | optional | n/a | contract preserved |
| `DELETE /cdecom/v1.0/session` | legacy delete | optional | n/a | contract preserved |
| `DELETE /cdecom/v1.0/nazvanie-den` | legacy delete | optional | n/a | contract preserved |
| `DELETE /weeklydata/v1.0/year-week` | legacy delete | optional | n/a | contract preserved |
| `DELETE /weeklydata/v1.0/session` | legacy delete | optional | n/a | contract preserved |
| `DELETE /salesbychannel/v1.0/year-month` | legacy delete | optional | n/a | contract preserved |
| `DELETE /salesbychannel/v1.0/session` | legacy delete | optional | n/a | contract preserved |
| `DELETE /storeturnover/v2.0/session` | legacy delete | optional | n/a | contract preserved |
| `POST /abcdata/v1.0/bulk` | legacy load | not supported | no | intentionally unchanged |
| `POST /storeturnover/v1.0/` | legacy load | not supported | no | intentionally unchanged |

Metadata and status GET endpoints were checked and do not initiate operations,
so `requestedBy` is not applicable.

## Resolution and validation

`DWHRequestedBy.resolve(queryValue, jsonValue)` selects a non-null query input
before JSON and then delegates to `normalizeAndValidate`. Consequently an
explicit empty query value overrides a non-empty JSON value and resolves to
`NULL`. Length 100 is accepted; length 101 is rejected. Load and deletion
session creation retain their existing defensive validation before JDBC write.

Moving validation out of the JSON DTO setter is required for correct
precedence: an unselected invalid JSON value must not reject a request whose URL
value is valid. The selected JSON-only value is still validated by the resolver.

## Compatibility and persistence

- Existing JSON-only clients continue to work.
- Existing calls without `requestedBy` continue to create sessions with SQL
  `NULL` in `DWH_Excel_Load_Session.RequestedBy`.
- Legacy DELETE paths, parameters, response types, and service transactions are
  unchanged.
- Universal deletion dry-run still performs only a count and creates no
  session.
- No database DDL or migration was added.

## Changed files

- Controllers: `CDDataController`, `CDEcomController`, `WeeklyDataController`,
  `SalesByChannelController`, `StoreTurnoverV2Controller`, `DWHDataController`.
- Shared contract: `DWHRequestedBy`, `DWHExcelLoadRequest`.
- Tests: `CDEcomControllerTest`, `DWHDataControllerTest`,
  `SalesByChannelControllerTest`, `StoreTurnoverV2ControllerTest`,
  `DWHRequestedByTest`, `DWHUniversalDeletionServiceTest`, and the new
  `RequestedByEndpointContractTest`.
- Documentation: `dwh-excel-loads.md`,
  `Replenishment.postman_collection.json`, and this report.

## Tests

The automated contract covers URL-only, JSON-only, conflicting URL/JSON,
absent, empty, 100-character and 101-character values; persistence into load
and deletion sessions; dry-run behavior; annotation coverage for all modern
bulk endpoints; and explicit exclusion of ABCData and StoreTurnover v1.

Verification results:

- focused requestedBy and controller regression: 37 passed, 0 failed, 0 skipped;
- full Maven suite: 514 passed, 0 failed, 0 skipped;
- Postman collection: valid JSON;
- live SQL Server integration: not run; persistence is verified with the
  existing JDBC proxy/contract tests;
- branch: `master`.
