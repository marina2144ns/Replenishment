# Отчёт: DECIMAL(18,2) для трёх полей CD_ecom

Дата аудита и реализации: 2026-11-11.

## Результат

Для `planRub`, `stockStoresPcs` и `stockStoresDdp` весь активный путь данных
переведён с целочисленного представления на точный `DECIMAL(18,2)` / `BigDecimal`:
Excel → raw → validation → stage → target. Значения с более чем двумя значащими
дробными знаками не округляются, а регистрируются как ошибка валидации.

| Поле | Java до | Java после | SQL stage/target до | SQL stage/target после |
|---|---|---|---|---|
| `planRub` | `Long` | `BigDecimal` | `BIGINT NOT NULL` | `DECIMAL(18,2) NOT NULL` |
| `stockStoresPcs` | `Long` | `BigDecimal` | `BIGINT NOT NULL` | `DECIMAL(18,2) NOT NULL` |
| `stockStoresDdp` | `Long` | `BigDecimal` | `BIGINT NOT NULL` | `DECIMAL(18,2) NOT NULL` |

## Аудит цепочки

- Excel: порядок колонок 28–30 сохранён. Они объявлены как `DECIMAL`. Для CDEcom
  числовое значение извлекается независимо от display-формата Excel, поэтому
  скрытое форматирование `123.456` как `123.46` не обходит строгую валидацию.
- Raw: `CD_ecom_raw` остаётся `NVARCHAR(100)`; JDBC продолжает использовать
  `setString`, дробная часть и запятая сохраняются. `CDEcomRawRow` остаётся строковым.
- Validation: для трёх полей используется строгий `BigDecimal`. Допускаются пустые
  значения по прежнему правилу `ZERO`, точные значения масштаба 0–2, отрицательные
  значения, научная запись и запятая. Переполнение, некорректный текст и фактический
  масштаб больше 2 создают стандартную validation error; округления нет.
- Stage: `CDEcomStageRow` использует `BigDecimal`; JDBC использует
  `setBigDecimal` и `Types.DECIMAL` для `NULL`.
- Target: `CDEcomTargetRow` и JPA `CDEcom` используют `BigDecimal`; публикация
  stage→target остаётся прямым `INSERT ... SELECT` без `CAST`, `ROUND` или целого типа.
- Processor/reload/errors: алгоритм транзакций, повторной обработки, очистки stage,
  публикации и регистрации ошибок не менялся и покрыт регрессией.
- API/deletion: controller, удаление по фильтрам и по сессии не используют эти
  метрики; код не менялся, endpoint-тесты пройдены.
- SQL procedure: `usp_CDEcom_ProcessLoadSession.sql` документирован как исторический,
  Java остаётся единственным runtime-путём. Тем не менее процедура исправлена:
  три значения нормализуются как decimal, строго проверяются без округления и
  записываются как `DECIMAL(18,2)`.
- DDL/старые установочные скрипты: основной DDL, zero-metrics migration и
  агрегированная migration 2026-08 больше не закрепляют `BIGINT` для этих колонок.

## Изменённые файлы

Основной код:

- `src/main/java/ru/stockmann/replenishment/services/dwhexcelload/core/AbstractDWHExcelLoader.java`
- `src/main/java/ru/stockmann/replenishment/services/CDEcomBulkLoader.java`
- `src/main/java/ru/stockmann/replenishment/services/dwhexcelload/definitions/CDEcomExcelLoadDefinition.java`
- `src/main/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomValueParser.java`
- `src/main/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomValidator.java`
- `src/main/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomStageRow.java`
- `src/main/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomStageRepository.java`
- `src/main/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomTargetRow.java`
- `src/main/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomRowMapper.java`
- `src/main/java/ru/stockmann/replenishment/models/CDEcom.java`

SQL и документация:

- `src/main/db/tables/CDecom_ddl.sql`
- `src/main/db/tables/cdecom_zero_metrics_migration.sql`
- `src/main/db/procedures/usp_CDEcom_ProcessLoadSession.sql`
- `src/main/db/migration/2026-08-production/01_production_migration.sql`
- `src/main/db/migration/2026-11-cdecom-decimal-fields/00_precheck.sql`
- `src/main/db/migration/2026-11-cdecom-decimal-fields/01_production_migration.sql`
- `src/main/db/migration/2026-11-cdecom-decimal-fields/02_verify.sql`
- `src/main/db/migration/2026-11-cdecom-decimal-fields/README.md`
- `src/main/docs/dwh-excel-loads.md`
- `src/main/docs/cdecom-decimal-fields-report.md`

Тесты:

- `src/test/java/ru/stockmann/replenishment/services/CDEcomBulkLoaderTest.java`
- `src/test/java/ru/stockmann/replenishment/services/cdecom/CDEcomSqlMigrationTest.java`
- `src/test/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomProcessorTest.java`
- `src/test/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomRepositoryTest.java`
- `src/test/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomRowMapperTest.java`
- `src/test/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomStageRepositoryTest.java`
- `src/test/java/ru/stockmann/replenishment/services/cdecom/process/CDEcomValidatorTest.java`
- `src/test/java/ru/stockmann/replenishment/services/dwhexcelload/definitions/CDEcomExcelLoadDefinitionTest.java`
- `src/test/java/ru/stockmann/replenishment/services/dwhexcelload/schema/CDEcomStageSchemaContractTest.java`

## Тестирование

| Сценарий | Результат |
|---|---|
| `123` | `123.00` |
| `123.45` / `123,45` | `123.45` |
| `0.01` | `0.01` |
| `-123.45` | `-123.45` |
| `9999999999999999.99` | принято |
| `10000000000000000.00` | `NUMERIC_OVERFLOW` |
| `123.456` | `DECIMAL_SCALE_EXCEEDED`, без округления |
| пустое / special-null | прежнее правило: `0.00` |
| некорректный текст | `INVALID_DECIMAL` |
| старое целое значение | принято и преобразовано в scale 2 |
| Excel numeric `123.456` с display format `0.00` | raw получает `123.456`; validation отклоняет |

- Профильный прогон: 91 тест, 0 failures, 0 errors, 0 skipped.
- Полный прогон `./mvnw test`: 506 тестов, 0 failures, 0 errors, 0 skipped.
- SQL Server в среде не обнаружен; реальная миграция и интеграционная проверка
  блокировок/плана выполнения не запускались, как и требовалось.

## Миграция

Каталог: `src/main/db/migration/2026-11-cdecom-decimal-fields/`.

- `00_precheck.sql` проверяет наличие и допустимое состояние шести колонок,
  convertibility, индексы, statistics, default/check/FK constraints,
  schema-bound dependencies, количество строк и контрольные суммы.
- `01_production_migration.sql` работает в одной транзакции с `XACT_ABORT ON`,
  принимает только `BIGINT NOT NULL` или уже целевой тип, изменяет ровно шесть
  колонок и перед commit повторно проверяет метаданные, row counts и суммы.
- `02_verify.sql` строго проверяет `decimal`, precision 18, scale 2,
  `is_nullable = 0` и выводит row counts/control totals.
- Повторный запуск после успешной миграции не выполняет `ALTER COLUMN` и не падает
  из-за зависимостей уже целевых decimal-колонок.

Порядок применения: остановить writers → проверить backup → выполнить и сохранить
результаты precheck → выполнить production migration в maintenance window → выполнить
verify и сравнить counts/totals → развернуть приложение → провести smoke load,
повторную загрузку и удаление. Полные инструкции, блокировки и recovery описаны в
README миграции.

## Матрица проверки

| Слой | Проверен | Исправлен | Тест |
|---|---|---|---|
| Excel | да | да | XLSX numeric/text, display rounding, order |
| Raw | да | тип не требовал изменения; сохранение исправлено | raw JDBC и repository mapping |
| Validation | да | да | границы, scale, overflow, comma, empty, invalid |
| Stage model | да | да | compile/validator/processor |
| Stage JDBC | да | да | `setBigDecimal`, `Types.DECIMAL` |
| Stage SQL | да | да | schema contract |
| Target model | да | да | record + JPA reflection |
| Target repository | да | не требовал SQL-конверсии | explicit INSERT SELECT contract |
| Target SQL | да | да | schema contract |
| SQL procedures | да | да, historical reference | SQL contract assertions |
| API / deletion | да | не требовалось | controller/deletion regression |
| Migration | да | добавлена | static migration contract |
| Documentation | да | да | repository audit |

## Риски и непроверенные сценарии

- Скрипты не выполнялись на SQL Server 2014: необходим rehearsal на восстановленной
  production-sized копии, включая проверку фактических dependencies и времени ALTER.
- `ALTER COLUMN` берёт schema-modification locks, может сканировать/переписывать
  большие таблицы и существенно увеличить transaction log; нужен maintenance window.
- Контрольные суммы миграции основаны на row count и трёх decimal sums; operator
  дополнительно сравнивает сохраненные precheck/verify result sets.
- Историческая процедура исправлена статически, но не выполнялась на SQL Server;
  активный Java runtime полностью покрыт автоматическими тестами.

Ветка: `master`.

Commit SHA публикации: последний commit ветки `master`; точный SHA сообщается в
handoff после создания commit, поскольку Git вычисляет его из содержимого этого файла.

Итог автоматических тестов: 506 пройдено, 0 пропущено.
