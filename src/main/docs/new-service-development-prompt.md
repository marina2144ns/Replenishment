# Master prompt для Codex: разработка нового DWH-сервиса загрузки данных в Replenishment

Нужно реализовать новый сервис загрузки данных в проекте `Replenishment`.

Сервис должен быть построен **строго по существующей унифицированной DWH-архитектуре проекта**, используемой в современных сервисах:

* `WeeklyData`
* `CDData`
* `CDEcom`
* `SalesByChannel`

Не проектируй новую архитектуру с нуля.

Перед реализацией обязательно изучи перечисленные reference services и используй их реальные patterns, общие компоненты, naming conventions, transaction boundaries, session/error infrastructure и структуру тестов.

---

# 0. Входные данные: единственный canonical format

Пользователь передаёт только название сервиса, при необходимости source format и business delete criteria, а также таблицу business columns в следующем формате:

```text
SERVICE_NAME:

<SERVICE_NAME>

SOURCE_FORMAT:

<XLSX | другое явно заданное значение>

BUSINESS_DELETE_CRITERIA:
- Year + Week
- Name + Day
```

Секция `SOURCE_FORMAT` опциональна. Если она отсутствует, использовать `XLSX` без дополнительного вопроса пользователю.

Секция `BUSINESS_DELETE_CRITERIA` опциональна. Если она отсутствует:

* не спрашивать пользователя о business delete criteria;
* не придумывать criteria и speculative business delete endpoints;
* полностью реализовать все остальные capabilities сервиса;
* session delete реализовать только тогда, когда это обязательный canonical pattern текущей reference architecture;
* в финальном отчёте указать `Business delete criteria: not specified`.

Единственный допустимый формат таблицы колонок:

| # | Field | Source header | SQL type | Role | Required | Missing behavior | Format / constraints | Notes |
|---|---|---|---|---|---|---|---|---|
| 1 | ... | ... | ... | ... | ... | ... | ... | ... |

Значение колонок таблицы:

* `#` — физический порядок source column, всегда заданный пользователем в one-based numbering. Допустима только строгая последовательность `1..N`: начинать с `1`, без gaps и duplicates; строки таблицы должны идти в порядке `#`. Если внутренний Excel/parser API использует zero-based indexes, Codex преобразует их самостоятельно: input `#1 → index 0`, `#2 → index 1` и т. д. Пользователь никогда не передаёт zero-based indexes;
* `Field` — canonical logical technical/business name для вывода Java/DB naming;
* `Source header` — exact literal header в source file;
* `SQL type` — canonical typed STAGE/TARGET type, например `INT`, `BIGINT`, `SMALLINT`, `DECIMAL(18,2)`, `NVARCHAR(255)`, `DATE`, `DATETIME2`, `BIT`;
* `Role` — один из фиксированных business roles: `DIMENSION`, `METRIC`, `IDENTIFIER`, `ATTRIBUTE`;
* `Required` — только `YES` или `NO`;
* `Missing behavior` — только `ERROR`, `ZERO` или `NULL`;
* `Format / constraints` — опциональные явно заданные дополнительные ограничения в deterministic syntax `key=value`. Рекомендуемые формы: `maxLength=255`, `range=1..12`, `dateFormat=yyyy-MM-dd`, `allowed=A|B|C`, `scale=2`. Несколько constraints разделяются `;`, например `range=0..100; scale=2`;
* `Notes` — свободная поясняющая business information. Она не может silently переопределять structured contract fields.

Если `Format / constraints` пуст, специальных business constraints нет. Применять только SQL type contract, `Required`, `Missing behavior` и standard project parsing/technical validation. Запрещено выводить дополнительные ограничения из `Field`, `Source header`, `Role` или SQL type. Например:

```text
Field = Month
SQL type = INT
Format / constraints = empty
```

не означает автоматически `range=1..12`.

Canonical example:

```text
SERVICE_NAME:
ExampleService

SOURCE_FORMAT:
XLSX

BUSINESS_DELETE_CRITERIA:
- Year + Month
```

| # | Field | Source header | SQL type | Role | Required | Missing behavior | Format / constraints | Notes |
|---|---|---|---|---|---|---|---|---|
| 1 | year | Year | NVARCHAR(50) | DIMENSION | YES | ERROR | | |
| 2 | month | Month | NVARCHAR(50) | DIMENSION | YES | ERROR | | |
| 3 | sku | SKU | BIGINT | IDENTIFIER | NO | NULL | | |
| 4 | sales | Sales | DECIMAL(18,2) | METRIC | NO | ZERO | | |
| 5 | brand | Brand | NVARCHAR(255) | ATTRIBUTE | NO | NULL | | |

Этот пример только иллюстрирует формат и semantics. Он не является источником hardcoded field names, delete criteria или business rules для других сервисов.

Technical metadata пользователь в таблицу не добавляет. Codex самостоятельно добавляет `Id`, `LoadSessionId`, `ExcelRowNum`, `RawRowId` или их фактические equivalents согласно текущей архитектуре.

Не требовать от пользователя Java type, отдельный `Nullable` flag, RAW SQL type, package layout, repositories, permissions или другие technical implementation details.

## 0.1. Input table — source of truth

Явно заданное значение имеет приоритет над naming guesses, heuristics, историческими patterns и предположениями по названию поля. Например:

```text
Field = Year
SQL type = NVARCHAR(50)
Role = DIMENSION
Required = YES
Missing behavior = ERROR
```

означает required textual `Year`; запрещено автоматически превращать его в `INT`.

`Field` является canonical logical field name. Адаптировать его к Java/SQL conventions проекта можно, но произвольно переименовывать business field нельзя.

`# + Source header` образуют exact source schema:

```text
#             → exact physical position
Source header → exact literal header
```

## 0.2. Role и Missing behavior

Business role не выводится из SQL type или имени:

```text
numeric type != business role
```

* `DIMENSION` — business dimension, period, category axis и подобные значения;
* `METRIC` — measure/value;
* `IDENTIFIER` — business identifier/code;
* `ATTRIBUTE` — descriptive property.

Technical fields классифицируются отдельно и не являются business roles входной таблицы.

`Missing behavior` является authoritative и имеет ровно три значения:

```text
ERROR: missing/blank/supported null-equivalent → validation error
ZERO:  missing/blank/supported null-equivalent → typed numeric zero
NULL:  missing/blank/supported null-equivalent → typed null
```

Не придумывать четвёртую semantics. Единственные допустимые combinations:

| Required | Missing behavior | Meaning |
|---|---|---|
| YES | ERROR | поле обязательно; отсутствие создаёт validation error |
| NO | ZERO | поле optional at source; отсутствие становится canonical numeric zero |
| NO | NULL | поле optional; отсутствие становится typed null |

Все остальные combinations (`YES + ZERO`, `YES + NULL`, `NO + ERROR`) являются `Input contract conflict`. Codex не должен reinterpret их самостоятельно. Clarification по такой строке допустим, потому что explicit contract противоречив; независимо реализуемые части сервиса при этом не блокировать.

`NO + ZERO` допустим только для numeric SQL type, поддерживаемого current project parser, например `SMALLINT`, `INT`, `BIGINT`, `DECIMAL(...)` или другого фактически поддерживаемого numeric type. `ZERO` с явно nonnumeric `NVARCHAR`, `DATE`, `DATETIME2`, `BIT` и подобным типом является `Input contract conflict`; не придумывать zero semantics для таких domains без explicit расширения input contract. Business role сам по себе не запрещает numeric `ZERO`: unusual `ATTRIBUTE + DECIMAL(...) + NO + ZERO` технически допустим, если остальные structured fields согласованы.

## 0.3. Проверка input contract и derived contract

До создания файлов проверить:

* `SERVICE_NAME` присутствует;
* все обязательные columns canonical table присутствуют;
* `#` образует strict one-based sequence `1..N`, без gaps/duplicates, и порядок строк соответствует `#`;
* `Field` unique;
* `Source header` задан для каждой позиции и однозначно соответствует ей;
* `SQL type` поддерживается current project architecture/parser;
* `Role` входит в `DIMENSION/METRIC/IDENTIFIER/ATTRIBUTE`;
* `Required` входит в `YES/NO`;
* `Missing behavior` входит в `ERROR/ZERO/NULL`;
* combination `Required/Missing behavior` входит ровно в три допустимых варианта;
* `ZERO` используется только с supported numeric SQL type;
* `Format / constraints` пуст либо syntactically understandable в deterministic `key=value; key=value` form;
* `Notes` не противоречит `SQL type`, `Role`, `Required`, `Missing behavior` или `Format / constraints`;
* весь input contract внутренне логически согласован.

Различать:

* реальное противоречие business contract;
* необычный, но технически безопасно реализуемый contract.

Если `Notes` прямо противоречит structured field, считать это `Input contract conflict`, а не выбирать трактовку самостоятельно. Notes не имеет приоритета над structured contract и structured contract не используется для silent игнорирования явного противоречия в Notes.

Только реальное противоречие отражать в финальном блоке `Input contract conflict`. Не останавливаться из-за unusual, но допустимого contract.

Перед реализацией самостоятельно построить derived mapping для самопроверки:

| Source | Field | Role | Required | Missing behavior | Source kind | RAW type | Java type | STAGE type | TARGET type | Validation |
|---|---|---|---|---|---|---|---|---|---|---|

Workflow:

```text
read input
→ inspect reference services
→ derive complete implementation contract
→ validate contract
→ implement immediately
```

Не останавливать работу для подтверждения derived table пользователем.

## 0.4. Autonomy и граница inference

Codex обязан самостоятельно выводить все технические решения, которые достоверно следуют из input contract, canonical architecture, reference services и repository conventions. Не задавать вопросы о:

* package/class/file names и repository layout;
* endpoint base path и Spring registration;
* JDBC/JPA choice, batch/chunk settings и transaction pattern;
* technical metadata, traceability, indexes и test naming;
* DDL/migration naming, RAW representation и permissions;
* shared session/error/status infrastructure.

Codex не имеет права угадывать business key, uniqueness, deduplication, business delete criteria, replacement scope, special ranges, locale-specific semantics, duplicate policy, equivalence textual values или aggregation rules.

Unspecified optional business rule must not block implementation:

```text
business delete unknown → no business delete
uniqueness unknown      → no UNIQUE
deduplication unknown   → no deduplication
replacement unknown     → canonical session-scoped publish
special range unknown   → type/required/precision checks only
locale parsing unknown  → standard project parser
```

После реализации перечислить такие пункты в `Unresolved business rules`.

Codex должен стремиться завершить deployment-ready сервис за один проход. Clarification допустим только если explicit input contract внутренне противоречив и без ответа невозможно безопасно реализовать основной load path. Даже тогда сначала реализовать независимые части, если это возможно. Отсутствие optional capability не является причиной остановки.

---

# 1. Главный архитектурный принцип

Новый сервис должен использовать следующий flow:

```text
Controller
    ↓
BulkLoader / Orchestrator
    ↓
DWH Load Session
    ↓
Streaming source file reading
    ↓
RAW table
    ↓
Java Processor
    ↓
Java Validator + Mapper
    ↓
STAGE table / DWH Error table
    ↓
Transactional TARGET publish
    ↓
STAGE cleanup
    ↓
Session SUCCESS
```

Архитектурно это означает:

```text
Controller
→ typed BulkLoader
→ shared DWH Excel infrastructure
→ raw
→ Java processor
→ validator
→ typed stage
→ target repository
→ session/status
```

Основная бизнес-логика преобразования и валидации должна находиться **в Java**.

Не создавать stored procedure для processing/validation.

Не переносить бизнес-логику в SQL без отдельного прямого требования.

---

# 2. Сначала изучить существующую архитектуру

До внесения изменений изучи как минимум:

* `WeeklyDataController`
* `WeeklyDataBulkLoader`
* `WeeklyDataExcelLoadDefinition`
* `WeeklyDataProcessor`
* `WeeklyDataValidator`
* `WeeklyDataRowMapper`
* `WeeklyDataRawRepository`
* `WeeklyDataStageRepository`
* `WeeklyDataTargetRepository`
* `WeeklyDataErrorRepository`
* deletion classes;

аналогичные классы:

* `CDData`
* `CDEcom`
* `SalesByChannel`;

а также общую инфраструктуру:

* `AbstractDWHExcelLoader`
* `DWHExcelAsyncLoadService`
* `DWHExcelStatusService`
* `DWHExcelStatusController`
* session repository/model;
* error repository/model;
* Excel streaming infrastructure;
* `AsyncConfig`;
* существующие DDL;
* `Users.sql`;
* существующие schema contract tests.

Определи текущий canonical pattern проекта и следуй ему.

Если между reference services остались случайные технические различия, не копируй их автоматически.

Приоритет имеет **унифицированное поведение**, описанное в этом задании.

---

# 3. Не менять общую архитектуру проекта

При реализации нового сервиса не нужно без необходимости менять:

* `AbstractDWHExcelLoader`;
* общий status API;
* общую session infrastructure;
* общую error infrastructure;
* async executor;
* другие существующие сервисы;
* глобальную API architecture.

Если обнаружится, что для нового сервиса действительно требуется изменение shared infrastructure, сначала явно опиши:

1. почему существующего механизма недостаточно;
2. какие сервисы изменение затронет;
3. есть ли backward compatibility risk.

Не делай такой рефакторинг молча в рамках реализации нового сервиса.

---

# 4. API

## 4.1 Bulk load endpoint

Создай controller по pattern существующих DWH-сервисов.

Ожидаемый endpoint должен соответствовать naming conventions проекта, например:

```text
POST /<service>/v1.0/bulk
```

Используй общий request/response contract проекта, если он применим:

```text
DWHExcelLoadRequest
DWHExcelLoadResult
```

Не создавай новый собственный формат request/result без технической необходимости.

Controller должен быть тонким.

В controller допустимы:

* HTTP-level validation;
* вызов loader/orchestrator;
* формирование стандартного HTTP response.

В controller не должно быть:

* чтения Excel/CSV строк;
* конвертации каждой строки;
* бизнес-валидации;
* JDBC;
* JPA row-by-row save;
* SQL processing;
* управления batch;
* ручного управления транзакциями.

---

# 5. Асинхронная загрузка

Используй существующий shared async mechanism.

Не создавай отдельный executor для нового сервиса.

Загрузка должна использовать общий lifecycle session:

```text
QUEUED
→ RUNNING
→ SUCCESS
```

или:

```text
QUEUED
→ RUNNING
→ ERROR
```

Используй существующие значения status и существующий механизм их обновления.

Не вводи новые статусы без отдельной необходимости.

---

# 6. Load Session

Каждая загрузка должна иметь собственный:

```text
LoadSessionId
```

и использовать общую:

```text
DWH_Excel_Load_Session
```

Не создавать отдельную таблицу сессий для нового сервиса.

Session должна позволять определить как минимум:

* тип/сервис загрузки;
* исходный файл согласно существующей модели;
* время запуска;
* status;
* сообщение об ошибке;
* существующие counters, если они поддерживаются текущей infrastructure.

Не дублируй session metadata в собственной таблице сервиса без необходимости.

---

# 7. Чтение исходного файла

Если `SOURCE_FORMAT` не задан, считать его равным `XLSX` и использовать существующий streaming mechanism проекта без clarification question.

Не использовать:

```java
new XSSFWorkbook(...)
```

для загрузки всего workbook в память, если shared loader уже использует streaming SAX approach.

Ориентироваться на существующий `AbstractDWHExcelLoader`.

Файл должен обрабатываться потоково.

Если явно задан другой source format, не пытаться пропустить его через XLSX loader. Сначала найти canonical shared mechanism проекта для этого формата. Если его нет, минимально расширить architecture, предварительно зафиксировав техническую необходимость и влияние на shared infrastructure; не изобретать business semantics формата.

---

# 8. Header validation

Для нового сервиса header validation является обязательной частью schema contract.

Header row определяется как **первая фактически встреченная строка первого worksheet**.

Нельзя полагаться на:

```text
physical Excel row number == 0
```

Если первая реально существующая строка имеет physical row number `1`, `2` и т. п., она всё равно является header.

Canonical flow:

```text
session RUNNING
→ open first worksheet
→ read first actual row
→ treat it as header
→ collect complete physical header
→ strict schema validation
→ only after SUCCESS process subsequent actual rows as business data
→ RAW batch insert
```

Shared loader должен концептуально иметь отдельное состояние обработки header, например:

```text
headerProcessed = false
```

и соблюдать принцип:

```text
if header not processed:
    collect complete physical header
    validate schema
    mark header processed
    do not process this row as data
else:
    process row as business data
```

Конкретное имя переменной не является частью contract.

Header schema validation должна одновременно проверять:

```text
exact column count
exact literal header names
exact column order
```

Schema должна отклоняться при:

* missing column;
* blank header;
* renamed header;
* swapped columns;
* extra column in the middle;
* extra column справа;
* отличии регистра;
* leading whitespace;
* trailing whitespace;
* изменении внутренних пробелов.

Без отдельного business requirement запрещена автоматическая нормализация header:

```text
trim
case-insensitive comparison
fuzzy matching
aliases
automatic column reordering
```

Например, при ожидаемом `Year` фактические `year` и `" Year "` являются schema errors.

До завершения schema validation нельзя ограничивать сбор header значением:

```text
expectedColumnCount
```

Shared loader должен видеть **все физически присутствующие Excel columns**, включая `columnIndex >= expectedColumnCount`. Если ожидается `29` колонок, а физический header содержит `30`, результатом должна быть schema validation error, а не silent ignore.

Для header допустимо отдельное representation, сохраняющее все физически присутствующие колонки. Для обычных data rows можно использовать фиксированное representation длиной `expectedColumnCount`, если это соответствует shared loader architecture. Не требуется без необходимости увеличивать memory footprint всех data rows.

Если первый worksheet не содержит ни одной фактически прочитанной строки, header отсутствует и загрузка должна завершиться `ERROR`. Пустой first sheet не является корректным пустым dataset.

Успешная strict schema validation является обязательным invariant перед positional RAW mapping:

```text
strict header/schema validation
→ positional RAW mapping
```

Positional mapping безопасен только после доказанного совпадения position, header name и column count с ожидаемым schema contract. Row validator не заменяет header validation. Без этой проверки перестановка двух совместимых по типу колонок может вызвать silent logical data corruption: RAW mapping присвоит значения business fields по позиции, а последующий Java validator уже не знает исходного header.

До успешного завершения schema validation:

```text
RAW business rows = 0
```

При header/schema validation error:

* business data rows не вставляются в RAW;
* RAW transaction корректно откатывается согласно shared loader lifecycle;
* processor не запускается;
* target processing не происходит;
* session завершается `ERROR` и не остаётся в `RUNNING` или `QUEUED`;
* ошибка должна быть диагностируемой через существующий file/schema error lifecycle.

Не создавать отдельную header error table. Header/schema validation проверяет структуру файла и не должна моделироваться как invalid business row только ради записи в row-level error table. Row/business validation отдельно проверяет значения конкретных business rows.

Header validation должна использовать единственную shared implementation общей DWH infrastructure. Новый сервис не должен добавлять собственный `validateHeaderRow()` или equivalent comparison в своём `BulkLoader`, если shared loader уже предоставляет этот механизм.

Expected headers должны задаваться через `<SERVICE>ExcelLoadDefinition` и существующий shared contract:

```text
one shared header validation implementation
+
service-specific schema definitions
```

Не создавать несколько независимых service-specific validation implementations и не дублировать список headers в loader, controller или отдельном validator.

Имена должны соответствовать переданной мной таблице source columns.

---

# 9. RAW layer

Для нового сервиса создать отдельную raw table:

```text
<SERVICE>_raw
```

Используй фактический naming style проекта.

## Назначение RAW

RAW должен хранить данные максимально близко к исходному представлению файла.

RAW не является target business table.

На этапе записи raw не нужно выполнять полную business conversion.

Основные преобразования выполняются позже Java processor.

Обязательность business field не означает, что соответствующая RAW column должна быть `NOT NULL`. RAW является source-oriented layer и должен сохранять исходный input, включая отсутствующее или некорректное значение, до Java validation:

```text
RAW: source-oriented, nullable allowed
→ Validator
→ STAGE: typed business contract
→ TARGET: typed business contract
```

Поэтому `RAW nullable` полностью совместим с `STAGE NOT NULL` и `TARGET NOT NULL` для required field.

---

# 10. RAW metadata

RAW должен поддерживать traceability.

Следуй pattern современных DWH-сервисов.

Как минимум должны существовать технические идентификаторы, эквивалентные:

```text
Id
LoadSessionId
ExcelRowNum
```

Используй `BIGINT` для технических row/session identifiers в соответствии с текущей DWH architecture.

`ExcelRowNum` должен позволять точно определить строку исходного файла.

RAW row ID должен быть пригоден для keyset pagination.

---

# 11. RAW business columns

Для входных business values используй representation, соответствующее существующим RAW tables.

Если reference architecture хранит исходные значения как строки (`NVARCHAR(...)`) до Java conversion, следуй этому pattern.

Не выполняй SQL `TRY_CONVERT` как основной механизм валидации.

Не превращай некорректное значение silently в `NULL`.

Некорректное значение должно быть обнаружено validator'ом и зарегистрировано как validation error.

Размер raw string columns выбирай с учётом:

* входного schema contract;
* существующего project pattern;
* отсутствия ненужного truncation.

Не обрезай значение silently.

---

# 12. RAW loading

RAW insert должен выполняться batch-порциями.

Не делать:

```text
1 source row = 1 DB transaction
```

Используй существующий shared JDBC batching mechanism.

Сохраняй:

```text
LoadSessionId
ExcelRowNum
```

для каждой строки.

---

# 13. Processor

Создай отдельный Java processor:

```text
<SERVICE>Processor
```

Его ответственность:

1. подготовить processing текущей session;
2. читать RAW ограниченными chunks;
3. валидировать строки;
4. преобразовывать valid rows в typed stage rows;
5. сохранять validation errors;
6. записывать valid rows batch-порциями в stage;
7. после полной успешной validation выполнить publish;
8. корректно завершить session.

Processor не должен загружать весь RAW dataset одной session в Java memory.

---

# 14. RAW pagination

Использовать keyset pagination по pattern reference services.

Предпочтительный принцип:

```sql
WHERE LoadSessionId = ?
  AND Id > ?
ORDER BY Id
```

с ограниченным:

```sql
TOP (?)
```

Не использовать OFFSET pagination для больших raw datasets, если проект уже использует keyset approach.

Размер chunk/batch брать из существующего configuration pattern.

Не hardcode arbitrary значения, если аналогичные настройки уже вынесены в configuration.

---

# 15. Initial STAGE cleanup

Перед началом обработки session необходимо удалить возможные leftovers stage той же `LoadSessionId`.

То есть processor должен начинать processing по существующему pattern:

```text
DELETE stage
WHERE LoadSessionId = currentLoadSessionId
```

Это необходимо для безопасного повторного запуска обработки одной session.

Не удалять stage других sessions.

---

# 16. Validator

Создай отдельный:

```text
<SERVICE>Validator
```

Business validation должна выполняться в Java.

Validator должен работать исходя из переданного schema contract.

Если входной contract задаёт `Required = YES + Missing behavior = ERROR`, обязательность должна быть согласована по всей typed pipeline:

```text
required source field
→ validator rejects missing value
→ typed STAGE NOT NULL
→ TARGET NOT NULL
```

Нельзя оставлять typed DB column nullable для явно обязательного business field без отдельно доказанного и объяснённого исключения.

Проверять, где применимо:

* required / nullable;
* тип;
* число;
* integer range;
* decimal precision/scale;
* date format;
* length;
* допустимость пустой строки;
* специальные business constraints, только если они явно заданы.

Не придумывать business constraints по названию колонки.

Например, из названия `Year` можно определить ожидаемый тип только если это следует из входной таблицы или existing contract.

Нельзя самостоятельно придумывать диапазон вроде `2000..2100`, если такого требования нет.

---

# 17. NULL и blank semantics

Явно различать:

```text
NULL
blank string
invalid value
zero
```

согласно authoritative `Missing behavior` входной таблицы:

```text
ERROR → missing/blank/null marker вызывает validation error
ZERO  → missing/blank/null marker становится typed numeric zero
NULL  → missing/blank/null marker становится typed null
```

Ни один из этих вариантов нельзя выбирать по названию или типу поля. Invalid nonblank value никогда не становится zero или null silently.

До реализации mapping для каждой column явно определить:

```text
source blank          → ?
source null marker    → ?
invalid nonblank      → ?
valid numeric zero    → ?
```

При `Required = YES + Missing behavior = ERROR` значения `null`, empty, whitespace и supported special-null marker должны приводить к validation error. Для numeric field не заменять required-missing значением `0`, `0.0` или `BigDecimal.ZERO`.

Для required string field значение после стандартной text normalization должно быть non-null и nonblank. Значения, которые shared cleaner превращает в `null`, включая поддерживаемые whitespace-only Unicode representations, должны отклоняться. Не подставлять `""`, `"UNKNOWN"` или `"N/A"` вместо отсутствующего required text.

При `Missing behavior = ZERO` canonical flow:

```text
source blank/missing/null marker
→ parser identifies missing
→ typed zero
→ valid row
→ STAGE NOT NULL
→ TARGET NOT NULL
```

Invalid nonblank numeric (`abc`, `1x`, `12abc`) должен создавать parsing/validation error, а не zero.

При `Missing behavior = NULL` Java representation должна поддерживать null (`Integer`, `Long`, `Short`, `BigDecimal`, а не primitive для nullable numeric), а STAGE/TARGET должны быть nullable, если нет отдельного explicit непротиворечивого правила.

---

# 18. Mapper / Value parser

Если преобразование raw → typed row достаточно сложное, использовать отдельные classes по существующим patterns:

```text
<SERVICE>RowMapper
<SERVICE>ValueParser
```

Не превращать validator в огромный класс, совмещающий:

* parsing;
* validation;
* SQL;
* persistence.

Разделяй ответственности аналогично reference services.

---

# 19. Ошибки валидации

Использовать общую:

```text
DWH_Excel_Load_Error
```

Не создавать отдельную error table для сервиса, если общая infrastructure подходит.

Ошибка должна быть traceable до source row.

Использовать существующие поля, включая эквиваленты:

```text
LoadSessionId
ExcelRowNum
RawId
```

и существующие error metadata.

Сообщение должно позволять понять:

* какая строка;
* какая колонка;
* какое значение;
* какое правило нарушено.

Не использовать только generic:

```text
Invalid data
```

если можно вернуть конкретную причину.

---

# 20. Поведение при validation errors

Если хотя бы одна строка не прошла validation, target publish не должен частично изменять production data.

Следовать существующей all-or-nothing policy reference DWH services.

Validation errors сохраняются в общей error infrastructure.

Session должна завершаться согласно существующему error contract.

Не публиковать только «хорошие строки», если текущая архитектура сервиса предполагает запрет publish при наличии ошибок.

---

# 21. STAGE layer

Создать typed stage table:

```text
<SERVICE>_stage
```

Stage содержит уже преобразованные и validated значения.

Business columns в stage должны иметь целевые SQL types согласно переданному schema contract.

Не хранить все stage business values как `NVARCHAR`, если target имеет типизированную структуру.

Для required fields typed stage columns должны быть `NOT NULL`. Для nullable fields Java stage representation должна уметь представить `NULL`.

---

# 22. STAGE metadata

Stage должен сохранять traceability.

Следовать reference DWH tables и использовать технические поля, эквивалентные:

```text
LoadSessionId
ExcelRowNum
RawRowId
```

`RawRowId` должен ссылаться на конкретную RAW row, из которой получена stage row.

Использовать типы технических идентификаторов, совместимые с RAW/session tables.

---

# 23. STAGE persistence

Stage rows сохранять batch-порциями.

Не использовать JPA `save()` для каждой строки.

Использовать JDBC repository pattern современных DWH processors.

Chunk processing должен иметь понятные transaction boundaries по pattern существующих сервисов.

---

# 24. TARGET layer

Создать итоговую typed table:

```text
<SERVICE>
```

или имя согласно naming conventions/schema contract.

Target business columns должны строго соответствовать переданной таблице:

* SQL type;
* length;
* precision;
* scale;
* nullability.

STAGE и TARGET business columns должны совпадать по SQL type, length, precision, scale и nullability, если отдельно не задано доказанное архитектурное исключение. Не допускать случайного `STAGE NULL / TARGET NOT NULL` или обратного расхождения.

Не изменять типы по собственному усмотрению.

Если входной contract содержит потенциально опасный тип, сообщить об этом отдельно, но не silently менять его.

---

# 25. TARGET metadata

Target должен сохранять traceability согласно современному pattern проекта.

Как минимум использовать эквиваленты:

```text
LoadSessionId
RawRowId
```

если они присутствуют во всех актуальных reference target tables и применимы к новой реализации.

Это должно позволять определить:

```text
target row
→ load session
→ raw row
→ Excel row
```

---

# 26. КРИТИЧЕСКИ ВАЖНО: publish semantics

Обычная загрузка **НЕ должна автоматически удалять предыдущие данные того же business period/business key**.

Не реализовывать:

```text
DELETE WHERE Year = ?
DELETE WHERE Year = ? AND Month = ?
DELETE WHERE Year = ? AND Week = ?
DELETE WHERE Season = ?
DELETE WHERE business columns match stage
```

как часть обычного publish, если это отдельно явно не требуется.

Нельзя самостоятельно выводить business scope из названий колонок.

Publish должен работать **по текущей LoadSessionId**, как унифицированные DWH-сервисы проекта.

---

# 27. TARGET publish transaction

Canonical flow:

```text
BEGIN TRANSACTION

DELETE FROM target
WHERE LoadSessionId = currentLoadSessionId

INSERT INTO target (...)
SELECT ...
FROM stage
WHERE LoadSessionId = currentLoadSessionId

verify publishedRows == expectedRows

DELETE FROM stage
WHERE LoadSessionId = currentLoadSessionId

verify cleanedStageRows == expectedRows

COMMIT
```

При любой ошибке:

```text
ROLLBACK
```

---

# 28. Зачем DELETE target по текущей session

Это обеспечивает idempotent processing одной и той же загрузочной session.

Если processing текущей session выполняется повторно, target rows этой session заменяются.

При этом target rows:

```text
других LoadSessionId
```

не должны автоматически удаляться.

Даже если другая session содержит те же business values или тот же период.

---

# 29. Проверка количества опубликованных строк

Перед commit обязательно проверить:

```text
publishedRows == expectedValidRows
```

Если количество не совпадает:

```text
ROLLBACK
```

Session не должна завершиться SUCCESS.

Не считать publish успешным только потому, что SQL statement не бросил exception.

---

# 30. STAGE cleanup после успешного publish

После успешной target insertion, но **до commit**, необходимо:

```sql
DELETE FROM <SERVICE>_stage
WHERE LoadSessionId = ?
```

Затем проверить:

```text
cleanedStageRows == expectedRows
```

Если количество не совпадает:

```text
ROLLBACK
```

---

# 31. Transaction boundary publish + cleanup

Следующие операции должны находиться **на одном connection в одной DB transaction**:

```text
target delete current session
target insert current session
publish count verification
stage cleanup current session
stage cleanup count verification
```

Commit выполняется только после всех проверок.

---

# 32. Поведение rollback

Если ошибка возникает:

* при target DELETE;
* при target INSERT;
* при publish count verification;
* при stage DELETE;
* при cleanup count verification;
* до commit;

необходимо rollback всей publish transaction.

После rollback не должно оставаться частично опубликованного target состояния.

Stage не должен быть потерян из-за failed publish transaction.

---

# 33. Что делать с RAW после SUCCESS

Не придумывай новую cleanup policy RAW.

Следуй существующему lifecycle reference DWH services.

Если современные reference services сохраняют RAW после обработки для traceability — сохраняй.

Не удаляй RAW только ради экономии места без отдельного требования.

---

# 34. Delete API

Разделять:

```text
обычная загрузка
```

и:

```text
явное удаление данных
```

Business deletion не должна быть скрытой частью publish.

---

# 35. Delete по LoadSessionId

Если это соответствует текущему общему pattern DWH services, реализовать явное удаление итоговых данных текущего сервиса по:

```text
LoadSessionId
```

через отдельный DELETE endpoint и deletion service.

Удаление должно касаться **target data**, а не уничтожать audit history загрузки.

Не удалять автоматически:

* RAW;
* DWH load session;
* DWH errors;

только потому, что удаляется target.

---

# 36. Business delete criteria

Business deletion создаётся только для criteria из опциональной секции `BUSINESS_DELETE_CRITERIA`, например:

```text
Year + Week
Year + Month
Name + Day
God + Sezon
```

реализовать соответствующий typed delete endpoint по pattern существующих services.

Для каждого явно заданного delete criterion или business-key field дополнительно проверить required/nullability contract. Использование в delete criteria само по себе **не делает поле required**: это определяется входным schema/business contract.

Если delete field явно required, должны быть согласованы:

```text
Validator required
STAGE NOT NULL
TARGET NOT NULL
```

Если business contract допускает `NULL`, отдельно проверить и описать, достижимы ли такие target rows через business delete API. Не придумывать изменение required status или SQL NULL semantics самостоятельно.

Если business delete criteria **не переданы**, НЕ ПРИДУМЫВАТЬ их самостоятельно.

Нельзя считать, что наличие колонок:

```text
Year
Month
Week
Season
Date
Name
```

автоматически означает необходимость соответствующего DELETE API.

В таком случае:

* реализовать только архитектурно достоверную часть;
* отдельно написать, что business delete criteria не заданы;
* не создавать speculative endpoint.

Для каждого composite criterion создать отдельный canonical endpoint/service/repository и audit tests. Не создавать generic ambiguous endpoint.

Domain параметров delete API должен точно совпадать с field domain. Textual field принимает `String`; запрещён lossy integer conversion. Значения вроде `"7"` и `"07"` не считаются эквивалентными без explicit rule.

Deletion audit должен сохранять criteria losslessly. Если shared metadata недостаточно, минимально расширить shared audit schema отдельными прозрачно названными typed/textual columns, не переиспользуя numeric metadata с ложной семантикой и не ломая existing services.

Если criterion содержит nullable field, не менять requiredness самостоятельно. Проверить достижимость rows с `NULL` через API и указать concern в финальном отчёте, если такие rows нельзя адресовать; это не блокирует реализацию остальных capabilities.

---

# 37. Delete session logging

Явная операция удаления должна использовать общий session/audit mechanism проекта.

Удаление должно регистрироваться как отдельная operation/session согласно существующему deletion pattern.

Сохранять:

* тип операции;
* критерии удаления, если infrastructure их поддерживает;
* результат;
* количество удалённых строк;
* SUCCESS/ERROR.

Не считать delete частью предыдущей load session.

---

# 38. Delete transaction

Target DELETE и успешное завершение deletion session должны следовать существующему transactional pattern проекта.

Не допускать ситуации:

```text
target удалён
→ deletion session осталась RUNNING из-за отдельного commit
```

если reference deletion services уже решают это в одной transaction.

Используй существующий pattern.

---

# 39. Delete count

Результат удаления должен содержать фактическое количество удалённых target rows согласно существующей deletion architecture.

Не возвращать просто:

```text
OK
```

если reference services используют структурированный result/session mechanism.

---

# 40. DDL: отдельные таблицы

Добавить DDL для:

```text
<SERVICE>_raw
<SERVICE>_stage
<SERVICE>
```

Использовать схему:

```text
dbo
```

если это стандарт проекта.

Не создавать отдельные session/error tables.

---

# 41. DDL должен быть create/install DDL

Основной DDL нового сервиса не должен начинаться с destructive:

```sql
DROP TABLE ...
```

Не удалять существующие business tables в deployment script.

Не копировать старые destructive patterns вроде legacy `ABCdata_ddl.sql`.

DDL должен быть пригоден для fresh install и соответствовать текущему DDL approach проекта.

---

# 42. Foreign keys

Следовать существующим DWH schema patterns для FK к:

```text
DWH_Excel_Load_Session
RAW row
```

там, где эти FK реально используются reference tables.

Не создавать cascade delete, который может уничтожить audit/history data, если такого pattern нет в современных сервисах.

---

# 43. SQL types

Строго использовать переданный schema contract.

Особенно внимательно:

```text
BIGINT
INT
SMALLINT
DECIMAL(p,s)
NVARCHAR(n)
DATE/DATETIME
BIT
```

Не выбирать `INT` автоматически для денежных, количественных или технических identifiers, если диапазон может быть недостаточен.

Но также не менять явно заданный тип без согласования.

Если обнаружен риск переполнения — сообщить отдельно.

---

# 44. Nullability

Source contract, parser, validator, Java typed row и DDL должны быть согласованы:

```text
source
→ parser
→ validator
→ typed STAGE
→ TARGET
```

Если `Required = YES + Missing behavior = ERROR`:

* stage и target columns должны быть `NOT NULL`;
* validator должен запрещать отсутствие значения;
* Java representation должна корректно отражать обязательность.

Если `Missing behavior = ZERO`, successful typed value non-null, поэтому STAGE/TARGET должны быть `NOT NULL`; SQL `DEFAULT 0` из этого не следует.

Если `Missing behavior = NULL`, STAGE/TARGET должны быть nullable и нельзя использовать Java primitive, если необходимо корректно представить DB NULL.

Например, учитывать разницу между:

```java
int
Integer
```

Комбинация `DB nullable + Java primitive` является потенциальным mismatch, потому что primitive не представляет business `NULL`. Комбинация `DB NOT NULL + validator allows null` является потенциальным runtime failure. Проверить согласованность Java representation, Validator, STAGE DDL и TARGET DDL для каждой business column.

---

# 45. Raw vs typed types

Важно не смешивать layers.

RAW:

```text
source-oriented representation
```

STAGE:

```text
validated typed representation
```

TARGET:

```text
validated typed business representation
```

Не пытаться сделать RAW идентичным target, если это уничтожит возможность корректно зарегистрировать parsing errors.

Required/nullability contract начинается на переходе `RAW → Validator → STAGE`: RAW остаётся source-oriented и может быть nullable, даже когда соответствующие typed columns обязательны.

Пользователь не задаёт RAW type. Codex выводит его из current canonical RAW representation reference services. Если современные loaders используют `NVARCHAR`, следовать этому pattern и сохранять возможность представить source absence/invalid input до validation.

Java type также выводится автоматически после проверки фактических mappings reference services. Canonical mapping ожидается в духе:

```text
INT          → Integer
BIGINT       → Long
SMALLINT     → Short
DECIMAL(p,s) → BigDecimal
NVARCHAR(n)  → String
DATE         → LocalDate
DATETIME2    → соответствующий modern project date/time type
BIT          → Boolean
```

Не использовать primitive, когда `Missing behavior = NULL`. SQL type определяет технический value kind, но не business role и не missing semantics.

---

# 46. Индексы

Создать только индексы, необходимые runtime flow.

Codex самостоятельно создаёт runtime-required indexes по фактическому repository flow. Для canonical architecture обязательны или должны иметь доказанное equivalent:

RAW:

```text
LoadSessionId + Id
```

или эквивалентный индекс для keyset processing.

STAGE:

```text
LoadSessionId
```

для:

* publish;
* cleanup.

TARGET:

```text
LoadSessionId
```

для:

* idempotent publish;
* explicit session delete;
* traceability.

---

# 47. Business indexes

Business indexes создавать только если они оправданы:

* delete criteria;
* documented production query;
* service contract;
* существующим request.

Не создавать индексы на все business columns «на всякий случай».

Если business delete criteria явно заданы, индекс по этим критериям рассмотреть обязательно.

Не создавать indexes «на всякий случай» и не спрашивать пользователя об index naming/layout: вывести их из SQL predicates и current project convention.

---

# 48. Index naming

Следовать naming convention существующего проекта:

```text
IX_<table>_<columns>
```

или фактическому project pattern.

Не вводить новый naming style.

---

# 49. Не создавать stored procedure

Для нового DWH service не создавать:

```text
usp_<SERVICE>_ProcessLoadSession
```

если задача не требует этого явно.

Validation, conversion и processing должны быть Java-based.

Не создавать historical SQL implementation «про запас».

---

# 50. Не использовать SQL как silent validator

Не использовать:

```sql
TRY_CONVERT(...)
```

с превращением invalid input в NULL без регистрации ошибки.

SQL должен отвечать главным образом за:

* persistence;
* selection;
* target publication;
* deletion.

Business parsing/validation — Java.

---

# 51. Repository architecture

Разделить repositories по responsibility согласно reference services.

Ожидаемая структура примерно:

```text
<SERVICE>RawRepository
<SERVICE>StageRepository
<SERVICE>TargetRepository
<SERVICE>ErrorRepository
```

и при наличии deletion:

```text
<SERVICE>DeletionRepository
```

Не создавать один giant repository со всей SQL-логикой сервиса.

---

# 52. Configuration

Использовать существующий configuration approach.

Настройки вроде:

* raw batch size;
* processing chunk size;
* stage batch size;

не размножать hardcoded constants, если в reference architecture они configurable.

---

# 53. Error handling

Разделять:

## File-level error

Например:

* файл не существует;
* файл нельзя открыть;
* неверная структура;
* неверный header;
* пустой first worksheet.

Header/schema error должен использовать существующий file/schema error lifecycle, завершать session в `ERROR` и предотвращать RAW business insert и запуск processor. Не записывать его как обычную invalid business row в row-level error table.

## Row validation error

Например:

* required value отсутствует;
* invalid decimal;
* invalid date;
* value too long.

## Processing/system error

Например:

* DB failure;
* transaction failure;
* unexpected processing exception.

Все должны приводить session в корректное конечное состояние согласно существующей architecture.

Не оставлять session `RUNNING` после fatal exception.

---

# 54. Idempotency

Повторный processing одной `LoadSessionId` должен быть безопасным.

Для этого использовать:

```text
initial stage cleanup
+
target delete current LoadSessionId
+
target insert current LoadSessionId
+
successful stage cleanup
```

Не обеспечивать idempotency посредством удаления других sessions.

---

# 55. Concurrency

Архитектура нового сервиса не должна использовать global shared STG без `LoadSessionId`.

Нельзя повторять legacy pattern:

```text
TRUNCATE shared_stage
→ load
→ process
```

который создаёт race condition между параллельными загрузками.

Все intermediate rows должны быть изолированы по:

```text
LoadSessionId
```

---

# 56. Не использовать partial commits внутри бизнес-загрузки без необходимости

Не повторять legacy ABCData pattern с промежуточными commits глобальной stage.

Batch != commit.

Batch insert используется для производительности.

Transaction boundary определяется архитектурой, а не каждым batch автоматически.

Следовать transaction model reference DWH loaders/processors.

---

# 57. Не использовать row-by-row JPA persistence

Не повторять StoreTurnover legacy pattern:

```text
for each row:
    repository.save(row)
```

если сервис предназначен для bulk DWH loading.

Использовать JDBC batching по существующему pattern.

---

# 58. API response

Не возвращать plain text:

```text
OK
```

или:

```text
List.toString()
```

Использовать существующий structured API contract.

Новый сервис должен выглядеть для клиента так же, как остальные современные DWH services.

---

# 59. DB permissions

Обновить deployment permission contract:

```text
src/main/db/tables/Users.sql
```

по существующему pattern.

Давать runtime principal только реально необходимые permissions.

---

# 60. Least privilege

Определи фактические SQL operations каждого repository и выдай минимальные object-level rights.

Ориентировочно могут потребоваться:

RAW:

```text
SELECT
INSERT
```

STAGE:

```text
SELECT
INSERT
DELETE
```

TARGET:

```text
SELECT
INSERT
DELETE
```

Но не копируй этот список blindly.

Проверь реальный runtime SQL.

Если repository действительно выполняет дополнительную операцию, право должно соответствовать ей.

---

# 61. Не выдавать ненужные permissions

Без отдельной причины не добавлять:

```text
ALTER
CONTROL
ADMINISTER BULK OPERATIONS
SHOWPLAN
EXECUTE на processing procedures
UPDATE
```

если runtime их не использует.

Не давать permission stored procedure, которой нет или которая не вызывается.

---

# 62. DDL/permission contract

Tests должны подтверждать, что DDL и `Users.sql` содержат необходимые объекты и permissions.

Не полагаться только на ручную проверку SQL-файлов.

---

# 63. Migration existing DB

Различать:

```text
fresh-install DDL
```

и:

```text
upgrade существующей production DB
```

Если новый сервис создаёт абсолютно новые таблицы, основной DDL должен описывать fresh installation этих объектов сразу с правильным contract. Не создавать бессмысленную migration для только что создаваемых RAW/STAGE/TARGET tables.

Fresh-install tables сразу создавать с правильным `NULL / NOT NULL` contract.

Если реализация требует изменения уже существующей общей таблицы или общего DB object, не маскируй это внутри create DDL.

Отдельно перечисли, какой migration нужен для существующей БД.

Не добавляй destructive migration автоматически.

При изменении существующей column `NULL → NOT NULL` отдельная migration обязательна. До `ALTER COLUMN` проверить legacy rows в TARGET и STAGE: `NULL`, а для required text также blank/whitespace согласно business normalization. Не считать STAGE гарантированно пустой после failed/incomplete processing.

Если `NULL` является invalid business data (`ERROR` contract), migration должна fail fast с диагностируемой ошибкой. Без отдельного data-correction requirement запрещены:

```text
UPDATE NULL → 0
UPDATE NULL → ''
UPDATE → UNKNOWN
DELETE invalid rows
```

Не добавлять fake `DEFAULT` только ради перехода в `NOT NULL`. Migration должна быть repeat-safe по существующему SQL Server/project pattern, например через `sys.columns.is_nullable`.

Если explicit `Missing behavior = ZERO` относится к уже существующей nullable business/shared column, migration может осознанно выполнить:

```text
UPDATE NULL → 0
ALTER COLUMN → NOT NULL
```

поскольку zero является заданным business value. Проверять и TARGET, и STAGE; не затрагивать RAW. `ZERO` не означает автоматическое создание SQL `DEFAULT 0`: preferred contract — Java canonicalization + DB `NOT NULL`.

---

# 64. Naming

Использовать naming conventions проекта для:

* Java packages;
* controllers;
* services;
* definitions;
* processors;
* validators;
* repositories;
* DB tables;
* indexes;
* endpoint names;
* tests.

Не вводить произвольное новое именование.

Перед созданием файлов посмотри ближайший reference service.

Из `SERVICE_NAME` самостоятельно вывести package, Controller, BulkLoader, Processor, Validator/Mapper, repositories, ExcelLoadDefinition, DDL/migration/test names, endpoint base path, load type/service identifier и index names. При нескольких historical styles выбирать current canonical reference pattern, а не задавать вопросы пользователю.

---

# 65. Package structure

Использовать современную структуру packages, аналогичную reference services.

Примерно:

```text
services/<service>/
services/<service>/process/
services/dwhexcelload/definitions/
controllers/
```

Точную структуру брать из текущего проекта.

---

# 66. Definition

Создать load definition нового сервиса по pattern:

```text
<SERVICE>ExcelLoadDefinition
```

Definition должна описывать:

* service/load type;
* raw insert contract;
* source columns;
* schema contract для shared loader: expected column count, expected physical positions и exact expected header names;
* raw column mapping;
* processor connection.

`<SERVICE>ExcelLoadDefinition` является единственным service-specific источником schema information для shared header validation и последующего positional RAW mapping. Не дублировать тот же список headers в loader, controller или validator.

Если Java processing используется напрямую, не оставлять фиктивный stored procedure contract.

---

# 67. Не создавать parallel legacy path

После реализации не должно появляться двух runtime путей:

```text
Java processor
```

и одновременно:

```text
stored procedure processor
```

для одной загрузки.

Нужен один canonical runtime path.

---

# 68. Документация

Если в проекте существует документ со списком shared DWH services, обновить его новым сервисом.

Документация должна описывать реальный runtime.

Не писать, что используется stored procedure, если processing Java-based.

Не ссылаться на nonexistent migrations.

## Automatic integration discovery

После создания core files фактически исследовать repository и сравнить новый сервис с reference services. Найти все реальные integration points, включая при применимости:

* Spring discovery/beans;
* definitions, controllers и async load routing;
* load type/service registries, enums и maps;
* shared status/session/error infrastructure;
* DDL deployment mechanism и `Users.sql`;
* schema/permission/registration contract tests;
* документацию и другие фактически используемые registration points.

Этот список не является заменой repository search. Нельзя считать заранее известный checklist исчерпывающим.

После завершения не должно требоваться вручную дописывать Java registration, SQL objects, permissions, definition, wiring, tests или documentation для deployment/use.

Новый сервис должен быть deployment-ready:

```text
fresh database
+ application deployment
+ source file matching input contract
→ runnable complete service flow
```

Для абсолютно нового сервиса fresh-install DDL достаточно для новых business tables; migration создавать только при изменении existing shared/already-deployed objects.

---

# 69. ТЕСТЫ — общий принцип

Новый сервис считается завершённым только вместе с тестами.

Не ограничиваться одним happy-path test.

Использовать testing style современных DWH services.

---

# 70. Header tests

Обязательно проверить strict schema contract и lifecycle:

1. valid header с exact count, literal names и order принимается;
2. renamed header отклоняется;
3. swapped columns отклоняются;
4. missing column отклоняется;
5. extra column in the middle отклоняется;
6. extra trailing column (`expected N`, `actual N + 1`) отклоняется; это обязательный regression case, подтверждающий видимость complete physical header;
7. blank header отклоняется;
8. case mismatch отклоняется;
9. leading или trailing whitespace mismatch отклоняется;
10. изменение внутренних пробелов отклоняется;
11. первая фактически прочитанная строка с physical row index, не равным `0`, всё равно считается header и не попадает в RAW как business data;
12. empty first sheet завершает загрузку `ERROR`;
13. при любой header/schema error в RAW вставлено `0` business rows и processor не запущен;
14. при invalid header session проходит существующий lifecycle `QUEUED → RUNNING → ERROR` (или его точный эквивалент проекта), фиксирует ошибку и не остаётся незавершённой.

Tests не должны закреплять `trim`, case-insensitive comparison, aliases, fuzzy matching или automatic column reordering, если такое business requirement отдельно не задано.

---

# 71. Definition tests

Проверить:

* корректное число source columns;
* raw insert mapping;
* expected table;
* load type/service name;
* processor wiring;
* отсутствие runtime stored procedure processing, если применимо.
* exact `#`, `Source header`, value kind, `Required` и `Missing behavior` каждой input column;
* отсутствие дополнительных speculative business columns;
* technical metadata добавлено implementation layer, а не ошибочно включено в source header schema.

---

# 72. Validator tests

Tests для каждой business column детерминируются input contract:

```text
ERROR → missing/blank/supported null marker вызывает error
ZERO  → missing/blank/supported null marker даёт typed zero; invalid даёт error
NULL  → missing/blank/supported null marker остаётся typed null
```

Дополнительно проверить relevant cases:

* valid value;
* null;
* blank;
* invalid type;
* boundary values;
* too long;
* precision overflow;
* scale;
* required field missing.

Проверять только правила, реально следующие из schema contract.

Для каждого required numeric field обязательно проверить:

```text
null → error
blank → error
whitespace → error
supported null marker → error
invalid number → error
valid value → accepted
```

Для каждого required string field обязательно проверить:

```text
null → error
"" → error
whitespace-only → error
supported normalized Unicode whitespace → error
valid nonblank → accepted
```

Если существует хотя бы одно required business field, fully empty business row не может быть valid. Количество ошибок должно следовать существующему validator style: collect-all либо fail-fast.

Role-specific regression:

* `METRIC + ZERO` — typed zero и DB `NOT NULL`;
* `IDENTIFIER + NULL` — missing остаётся null, а не zero;
* `DIMENSION + ERROR` — missing отклоняется;
* `ATTRIBUTE` — строго следует `Required/Missing behavior`, без дополнительных guesses.

---

# 73. Mapper/parser tests

Проверить raw → typed conversion:

* string;
* integers;
* decimals;
* dates;
* nullable;
* zero;
* negative values, если допустимы/недопустимы согласно contract;
* whitespace handling;
* locale-specific formats только если они являются частью входного формата.

Для numeric fields отдельно доказать, что invalid nonblank (`abc`, `1x`, `12abc`) не превращается в zero/null. Precision, scale, date format, allowed values и ranges проверять только когда они следуют из SQL type или `Format / constraints`.

---

# 74. RAW repository tests

Проверить SQL contract:

* selection только текущей session;
* keyset `Id > lastId`;
* ordering;
* chunk limit;
* правильный mapping columns.

---

# 75. Pagination tests

Обязательно проверить несколько chunks.

Например:

```text
chunk 1
chunk 2
chunk 3
```

и убедиться, что:

* строка не теряется;
* строка не читается дважды;
* next cursor соответствует последнему RAW ID.

---

# 76. STAGE repository tests

Проверить:

* batch insert;
* все business columns;
* metadata;
* `LoadSessionId`;
* `ExcelRowNum`;
* `RawRowId`;
* `deleteByLoadSessionId`.

---

# 77. Error tests

Проверить, что invalid row:

* не попадает как valid stage row;
* создаёт structured DWH error;
* сохраняет правильный Excel row;
* сохраняет Raw ID;
* содержит диагностируемое описание.

---

# 78. Processor happy path

Проверить полный processor flow:

```text
initial stage cleanup
→ raw chunks
→ validation
→ stage batches
→ target publish
→ publish count verification
→ stage cleanup
→ cleanup count verification
→ commit
→ SUCCESS
```

---

# 79. Processor validation failure

Проверить:

* validation error зарегистрирован;
* target publish не происходит;
* session не становится SUCCESS;
* existing target data не изменяются.

Отдельно проверить required-field failure:

```text
validation error
→ invalid row не превращается в STAGE row
→ error сохраняется
→ TARGET publish не выполняется согласно all-or-nothing policy
```

---

# 80. Publish repository tests

КРИТИЧЕСКИ ВАЖНО проверить правильную session semantics.

## Scenario A

Target содержит:

```text
LoadSessionId = 100
```

Stage содержит:

```text
LoadSessionId = 100
```

После publish target rows session `100` заменены текущими rows session `100`.

---

# 81. Не удалять другую session того же business scope

## Scenario B

Target:

```text
LoadSessionId = 100
Year = 2026
Month = 7
```

Stage:

```text
LoadSessionId = 101
Year = 2026
Month = 7
```

если такие поля существуют.

После publish session `101`:

```text
session 100 должна остаться
session 101 должна появиться
```

Обычный publish не должен удалять session 100 по совпадению business columns.

Этот тест обязателен там, где target имеет business-period-like columns.

---

# 82. Other sessions unaffected

Проверить, что target rows других `LoadSessionId` не изменяются.

---

# 83. Publish count mismatch test

Если:

```text
publishedRows != expectedRows
```

должен произойти:

```text
ROLLBACK
```

Commit запрещён.

---

# 84. Stage cleanup test

После successful publish:

```text
deleteByLoadSessionId(currentSession)
```

обязательно вызывается.

Проверить:

```text
cleanedRows == expectedRows
```

---

# 85. Stage cleanup mismatch test

Если:

```text
cleanedRows != expectedRows
```

должен быть rollback всей publish transaction.

Target publish не должен остаться committed.

---

# 86. Stage cleanup exception test

Если stage DELETE бросает exception:

```text
ROLLBACK
```

Проверить отсутствие commit.

---

# 87. Publish exception test

Если target publish падает:

```text
ROLLBACK
```

Stage не должен быть потерян из-за failed publish transaction.

---

# 88. Initial stage cleanup/retry test

Проверить повторный processing той же session:

* stale stage удаляется в начале;
* stage формируется заново;
* успешный publish выполняется;
* stage очищается после успеха.

---

# 89. Transaction tests

Проверить, что:

```text
target publish
stage cleanup
```

используют одну transaction.

Не должно быть commit между:

```text
target insert
```

и:

```text
stage delete
```

---

# 90. Delete tests

Если реализуется delete API:

проверить:

* правильный target predicate;
* удаляется только target;
* другие rows не затрагиваются;
* deletion session создаётся;
* удалённое количество фиксируется;
* SUCCESS;
* rollback/error path.

---

# 91. Session delete tests

Для:

```text
DELETE .../session
```

проверить:

```text
WHERE LoadSessionId = ?
```

и отсутствие удаления других sessions.

---

# 92. Business delete tests

Если business criteria явно переданы, проверить:

* полный composite predicate;
* каждый обязательный parameter;
* отсутствие partial accidental delete;
* correct deleted row count;
* deletion audit metadata.

---

# 93. Controller tests

Проверить:

* valid request;
* invalid request;
* loader invocation;
* стандартный response;
* отсутствие business processing в controller.

---

# 94. DDL schema contract tests

Проверить:

* существуют raw/stage/target definitions;
* все columns присутствуют;
* SQL types;
* nullability;
* precision;
* lengths;
* technical metadata;
* indexes;
* FKs;
* отсутствие destructive DROP TABLE;
* отсутствие duplicate CREATE TABLE.

Для каждой STAGE/TARGET business column test должен проверять не только наличие, но и exact SQL type, length/precision/scale и `NULL / NOT NULL`. Required fields должны иметь явные assertions на `NOT NULL`, а STAGE/TARGET business contracts — проверку равенства типов и nullability.

Если добавляется nullability migration, contract tests должны подтверждать TARGET/STAGE coverage, точный набор columns и repeat-safe guard. Для `ERROR` contract требовать diagnostic fail-fast и отсутствие silent repair/delete/fake defaults/destructive `DROP`. Для explicit `ZERO` contract разрешать и проверять только осознанное `UPDATE NULL → 0`, затем `NOT NULL`; RAW, unrelated columns и SQL `DEFAULT` не затрагивать.

---

# 95. Permissions tests

Проверить `Users.sql`:

* новый target;
* raw;
* stage;
* необходимые rights;
* отсутствие ненужного `EXECUTE`;
* отсутствие unnecessary `ALTER`;
* отсутствие broad privileges только ради нового сервиса.

---

# 96. Regression tests

После targeted tests обязательно выполнить:

```bash
./mvnw test
```

Все существующие tests проекта должны продолжить проходить.

Нельзя считать задачу законченной, если новый сервис работает, но ломает existing services.

---

# 97. git diff validation

До завершения выполнить:

```bash
git diff --check
git status --short
git diff --stat
```

Не включать случайные изменения файлов.

Не форматировать весь проект.

Не менять unrelated code.

---

# 98. Не создавать commit автоматически

После реализации и тестов:

**commit не создавать**, если я отдельно этого не попросила.

Показать мне изменения для review.

---

# 99. Что Codex не имеет права угадывать

Если из входных данных нельзя достоверно определить:

* business delete criteria;
* uniqueness/business key;
* whether duplicate business rows are allowed;
* mandatory business constraints помимо explicit `Required/Missing behavior`;
* append vs business replacement semantics;
* специальные диапазоны значений;
* locale-specific parsing;
* специальную deduplication policy;

не придумывать их.

Отдельно перечислить как:

```text
Business rule not specified
```

Но техническую реализацию, не зависящую от этого правила, выполнить полностью.

---

# 100. Нельзя автоматически выводить business key

Наличие колонок:

```text
Year
Week
Month
Day
Name
SKU
Store
Season
```

не означает автоматически, что их комбинация является:

* PK;
* UNIQUE;
* delete criteria;
* publish scope;
* deduplication key.

Business key существует только если он явно задан требованиями или уже существует в достоверном contract.

---

# 101. Не добавлять automatic deduplication

Не использовать:

```text
DISTINCT
GROUP BY
ROW_NUMBER() ... keep first
MERGE
```

для удаления дублей, если такого business requirement нет.

Если две одинаковые business rows присутствуют в source и допустимость дублей не определена — не менять данные silently.

---

# 102. Не делать automatic business-period replacement

Особенно важно:

если source содержит:

```text
Year + Week
Year + Month
Year + Season
Name + Day
```

это **не означает**, что новая загрузка должна автоматически удалить старые данные этого периода.

Обычный publish — session-scoped.

Business delete — отдельная явная операция.

---

# 103. Производительность

При реализации проверить отсутствие очевидных anti-patterns:

* whole workbook in memory;
* row-by-row DB insert;
* row-by-row JPA transaction;
* OFFSET на огромной RAW;
* N+1 queries;
* unbounded result list;
* global stage;
* full table DELETE вместо session predicate.

---

# 104. SQL Server compatibility

Все SQL должно соответствовать версии SQL Server, используемой проектом/production.

Не использовать syntax/features более новой версии без проверки.

Ориентироваться на SQL patterns уже существующего проекта.

---

# 105. Final architecture verification

После реализации сравни новый сервис с:

* WeeklyData
* CDData
* CDEcom
* SalesByChannel

по следующим пунктам:

| Area                        | Verify                               |
| --------------------------- | ------------------------------------ |
| Input format                | canonical fixed table                |
| Input completeness          | every source column mapped exactly once |
| Business role               | explicit input value respected       |
| Missing behavior            | explicit `ERROR` / `ZERO` / `NULL`  |
| Numeric ZERO                | only explicit `ZERO`                 |
| Identifier null             | preserved for `NULL` contract        |
| Required field              | `ERROR` + typed `NOT NULL`           |
| Java type                   | derived from SQL type + nullability  |
| RAW type                    | derived, source-oriented             |
| Controller                  | thin                                 |
| Shared loader               | yes                                  |
| Session                     | shared                               |
| File reading                | streaming                            |
| Header source               | first actual row; physical row `0` не предполагается |
| Header column count         | strict                               |
| Header literal names/order  | strict                               |
| Extra/missing columns       | reject                               |
| Empty first sheet           | reject                               |
| RAW before schema SUCCESS   | no                                   |
| Header implementation       | shared; schema from definition       |
| RAW                         | session-scoped                       |
| RAW metadata                | traceable                            |
| Processing                  | Java                                 |
| Pagination                  | keyset/chunked                       |
| Validator                   | separate                             |
| Required contract           | Validator/STAGE/TARGET aligned       |
| RAW nullability             | source-oriented                      |
| STAGE/TARGET nullability    | schema contract aligned              |
| Missing vs zero             | authoritative input behavior         |
| Migration to NOT NULL       | fail-fast for `ERROR`; normalize only explicit `ZERO` |
| Errors                      | shared DWH error                     |
| STAGE                       | typed                                |
| Stage batching              | yes                                  |
| TARGET                      | typed                                |
| Target traceability         | yes                                  |
| Publish                     | current LoadSessionId only           |
| Publish transaction         | atomic                               |
| Publish count check         | yes                                  |
| Stage cleanup               | before publish retry + after SUCCESS |
| Cleanup count check         | yes                                  |
| Rollback                    | target + stage cleanup               |
| Delete                      | explicit, not hidden publish         |
| Delete session              | audited                              |
| DDL                         | non-destructive                      |
| Indexes                     | runtime-driven                       |
| Permissions                 | least privilege                      |
| Integration points          | discovered from repository and updated |
| Deployment readiness        | fresh deployment is runnable         |
| Manual post-work            | none                                 |
| Stored processing procedure | no                                   |
| Tests                       | comprehensive                        |

Если новый сервис отличается по любому пункту, отдельно объясни почему.

---

# 106. Финальный отчёт после реализации

После завершения дай отчёт в следующей структуре.

## 0. Input contract implemented

Явно показать:

```text
SERVICE_NAME: ...
SOURCE_FORMAT: ...
BUSINESS_DELETE_CRITERIA: ... | not specified
Column count: ...
```

Если обнаружен реальный конфликт explicit input, добавить отдельный блок `Input contract conflict`. Не использовать его для unusual, но технически допустимых contracts.

## 1. Реализованный flow

Покажи:

```text
Controller
→ Loader
→ RAW
→ Processor
→ Validator
→ STAGE/Error
→ TARGET
→ Stage cleanup
→ Session
```

## 2. Созданные/изменённые Java files

Для каждого кратко назначение.

## 3. DB objects

Перечислить:

* raw;
* stage;
* target;
* indexes;
* FKs;
* permissions.

## 4. Complete field contract

Показать все business fields, каждое ровно один раз:

| # | Source header | Field | Role | Required | Missing behavior | RAW | Java | STAGE | TARGET | Validation |
|---|---|---|---|---|---|---|---|---|---|---|

## 5. Transaction boundaries

Отдельно описать:

* raw load;
* processing chunks;
* target publish;
* stage cleanup;
* deletion.

## 6. Error behavior

Показать поведение:

* header error;
* row validation error;
* DB processing error;
* publish failure;
* cleanup failure.

## 7. Delete behavior

Указать:

* session deletion;
* business deletion, если задана;
* какие таблицы реально удаляются;
* deletion session/audit.

## 8. Tests

Перечислить:

* targeted tests;
* количество tests;
* full `./mvnw test`;
* failures/errors.

## 9. Git state

Показать:

```text
git diff --check
git diff --stat
git status --short
```

## 10. Unresolved business rules

Если были требования, которые нельзя определить из входного schema contract, перечислить их здесь.

Не угадывать ответы.

## 11. Final self-audit

Перед завершением ответить на каждый пункт и устранить технические пропуски:

```text
A. Every input column implemented?
B. Exact header order implemented?
C. Role respected?
D. Missing behavior respected?
E. Java type derived correctly?
F. RAW source-oriented?
G. STAGE/TARGET contract aligned?
H. Session isolation correct?
I. Publish atomic?
J. Stage cleanup atomic?
K. Deletion only if explicitly defined?
L. Audit metadata lossless?
M. Permissions complete?
N. Registration complete?
O. DDL deployment-ready?
P. Tests complete?
Q. Full suite green?
```

---

# 107. Definition of Done

Новый сервис считается готовым только если:

* canonical input format разобран без запроса technical details;
* каждая input column представлена ровно один раз, без speculative business columns;
* для каждого field явно соблюдены `Role` и `Missing behavior`;
* technical metadata добавлено автоматически согласно architecture;
* Java type автоматически выведен из SQL type/nullability после проверки reference mappings;
* RAW representation автоматически выведен и остаётся source-oriented;
* `ZERO` применяется только для explicit `Missing behavior = ZERO`;
* required fields не превращаются silently в zero;
* nullable identifiers сохраняют null и используют nullable-capable Java types;
* invalid numeric никогда не превращается в zero или null silently;
* используется shared DWH architecture;
* controller thin;
* файл читается потоково;
* первая фактически прочитанная строка first worksheet считается header независимо от physical row number;
* physical row number `0` не предполагается;
* header schema строго проверяет exact expected column count, literal names и order;
* missing, blank, renamed, swapped, extra middle и extra trailing headers отклоняются;
* case differences, leading/trailing whitespace и изменения внутренних пробелов отклоняются;
* `trim`, fuzzy matching, aliases, case-insensitive comparison и automatic reordering не применяются без явного требования;
* complete physical header, включая columns за `expectedColumnCount`, доступен schema validation;
* до успешной schema validation не вставляется ни одной RAW business row;
* empty first sheet отклоняется;
* после schema failure processor не запускается, а session завершается `ERROR`;
* используется shared header validation со schema из definition, без service-specific duplicate code;
* RAW сохраняет source traceability;
* RAW processing chunked/keyset;
* validation Java-based;
* required source fields отклоняются при отсутствии значения;
* required strings после normalization являются non-null и nonblank;
* missing required numerics не превращаются в zero без явного требования;
* optional nullable fields используют nullable-capable Java types;
* RAW остаётся source-oriented и не ужесточается автоматически из-за required business contract;
* STAGE и TARGET совпадают по business types и nullability;
* STAGE/TARGET отражают successful typed contract для `ERROR`, `ZERO` и `NULL`;
* required typed fields имеют `NOT NULL` в STAGE и TARGET;
* schema tests явно проверяют `NULL / NOT NULL`;
* required-field validator tests покрывают null, blank, whitespace и null-marker cases;
* fully empty row не проходит validation при наличии required fields;
* nullability migrations проверяют legacy TARGET и STAGE data до `NULL → NOT NULL`;
* `ERROR` migrations fail-fast и не исправляют/delete legacy data silently;
* explicit `ZERO` migrations изменяют только подтверждённые `NULL → 0`, без SQL default;
* migrations безопасны для повторного запуска;
* invalid rows записываются в shared error table;
* STAGE typed;
* target publish session-scoped;
* другой `LoadSessionId` не удаляется при обычном publish;
* publish atomic;
* published row count проверяется;
* stage очищается после successful publish в той же transaction;
* cleanup row count проверяется;
* rollback покрывает publish + cleanup;
* explicit delete отделён от load;
* business delete создан только при наличии `BUSINESS_DELETE_CRITERIA`;
* delete parameter/audit domain lossless и соответствует input fields;
* deletion sessions логируются;
* DDL non-destructive;
* indexes обоснованы runtime;
* runtime permissions минимальны;
* все integration points фактически обнаружены и обновлены;
* fresh deployment запускает полный service flow без manual post-work;
* processing stored procedure отсутствует;
* comprehensive tests добавлены;
* все existing project tests проходят;
* `git diff --check` чистый;
* unrelated changes отсутствуют;
* final self-audit пройден;
* commit не создан без отдельного запроса.

---

# 108. Основное правило при сомнении

Если нужно выбрать между:

```text
«придумать удобное новое решение»
```

и:

```text
«повторить проверенный современный pattern Replenishment»
```

выбирай существующий pattern Replenishment.

Если существующий pattern не может выполнить явно заданное новое требование — объясни это отдельно и внеси минимально необходимое изменение.

Не расширяй scope задачи без необходимости.
