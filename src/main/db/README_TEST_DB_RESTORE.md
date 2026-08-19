# Восстановление тестовой базы ReplenishmentDWH из backup рабочего сервера

Эта инструкция описывает фактически проверенный порядок создания backup рабочей базы `ReplenishmentDWH` и восстановления его на тестовом SQL Server.

> Внимание: восстановление на тестовом сервере выполняется с `REPLACE` и полностью заменяет существующую тестовую базу `ReplenishmentDWH` содержимым backup. Перед выполнением внимательно проверьте, что подключение открыто именно к тестовому SQL Server.

## 1. Создать backup на рабочем сервере

Выполнить на рабочем SQL Server:

```sql
BACKUP DATABASE [ReplenishmentDWH]
TO DISK = N'D:\BAK\ReplenishmentDWH_2026-08-19.bak'
WITH
    COPY_ONLY,
    COMPRESSION,
    CHECKSUM,
    STATS = 10;
```

Используется:

- `COPY_ONLY` — разовый backup, не вмешивающийся в штатную backup-цепочку;
- `COMPRESSION` — уменьшение размера backup-файла;
- `CHECKSUM` — дополнительная проверка страниц при создании backup;
- `STATS = 10` — вывод прогресса выполнения.

## 2. Проверить backup на рабочем сервере

После завершения backup выполнить:

```sql
RESTORE VERIFYONLY
FROM DISK = N'D:\BAK\ReplenishmentDWH_2026-08-19.bak'
WITH CHECKSUM;
```

Продолжать перенос только после успешного завершения проверки.

## 3. Вручную перенести backup-файл на тестовый сервер

Файл backup необходимо вручную скопировать с рабочего сервера на тестовый сервер.

Проверенный сценарий:

```text
Рабочий сервер:
D:\BAK\ReplenishmentDWH_2026-08-19.bak

        ↓ ручное копирование

Тестовый сервер:
D:\Backup\fromdb01003\ReplenishmentDWH_2026-08-19.bak
```

Способ копирования зависит от доступной инфраструктуры: файловая система, RDP, сетевая папка и т. п.

Перед восстановлением убедиться, что SQL Server на тестовом сервере имеет доступ на чтение к скопированному `.bak`-файлу.

## 4. Проверить состав backup на тестовом сервере

На тестовом SQL Server выполнить:

```sql
RESTORE FILELISTONLY
FROM DISK = N'D:\Backup\fromdb01003\ReplenishmentDWH_2026-08-19.bak';
```

Для фактически использованного backup логические имена файлов:

```text
ReplenishmentDWH
ReplenishmentDWH_log
```

Именно эти logical names используются далее в `WITH MOVE`.

## 5. Перевести тестовую базу в SINGLE_USER

Восстановление выполнять из базы `master`.

```sql
USE master;
GO

ALTER DATABASE [ReplenishmentDWH]
SET SINGLE_USER
WITH ROLLBACK IMMEDIATE;
GO
```

`WITH ROLLBACK IMMEDIATE` принудительно закрывает активные соединения к тестовой базе. Это необходимо, потому что `RESTORE DATABASE` требует монопольного доступа.

Если этот шаг не выполнить, возможна ошибка:

```text
Сообщение 3101
Не удалось получить монопольный доступ, так как база данных используется.

Сообщение 3013
RESTORE DATABASE прервано с ошибкой.
```

## 6. Восстановить backup поверх тестовой базы

Физические файлы тестовой базы размещаются в `D:\Base`.

Выполнить:

```sql
RESTORE DATABASE [ReplenishmentDWH]
FROM DISK = N'D:\Backup\fromdb01003\ReplenishmentDWH_2026-08-19.bak'
WITH
    MOVE N'ReplenishmentDWH'
        TO N'D:\Base\ReplenishmentDWH.mdf',

    MOVE N'ReplenishmentDWH_log'
        TO N'D:\Base\ReplenishmentDWH.ldf',
    REPLACE,
    RECOVERY,
    STATS = 10;
```

Значение параметров:

- `MOVE` — размещает data/log files по путям тестового сервера;
- `REPLACE` — разрешает заменить существующую тестовую `ReplenishmentDWH`;
- `RECOVERY` — завершает restore и открывает базу для работы;
- `STATS = 10` — показывает прогресс.

## 7. Вернуть базу в MULTI_USER

После успешного восстановления выполнить:

```sql
ALTER DATABASE [ReplenishmentDWH]
SET MULTI_USER;
GO
```

## 8. Проверить состояние восстановленной базы

```sql
SELECT
    name,
    state_desc,
    recovery_model_desc,
    compatibility_level
FROM sys.databases
WHERE name = N'ReplenishmentDWH';
```

Ожидаемое основное состояние:

```text
state_desc = ONLINE
```

## 9. Проверить целостность базы

```sql
USE [ReplenishmentDWH];
GO

DBCC CHECKDB WITH NO_INFOMSGS;
```

Команда должна завершиться без сообщений об ошибках целостности.

## 10. Проверить логины и пользователей после восстановления на другом SQL Server

Backup базы переносит вместе с базой database users и их database-level permissions, но server logins принадлежат экземпляру SQL Server и в backup базы не входят.

Поэтому после восстановления рабочей базы на тестовом сервере необходимо отдельно проверить соответствие server logins и database users.

Сначала посмотреть пользователей базы и связанные с ними server logins:

```sql
USE [ReplenishmentDWH];
GO

SELECT
    dp.name AS DatabaseUser,
    dp.sid AS DatabaseUserSid,
    sp.name AS ServerLogin,
    sp.sid AS ServerLoginSid
FROM sys.database_principals dp
LEFT JOIN sys.server_principals sp
    ON dp.sid = sp.sid
WHERE dp.type IN ('S', 'U', 'G')
  AND dp.name NOT IN ('dbo', 'guest', 'INFORMATION_SCHEMA', 'sys')
ORDER BY dp.name;
```

Для проекта в первую очередь проверить:

```text
Repl_Service
ReplenishmentREAD
repl
```

### Если server login отсутствует

Создать отсутствующий login на тестовом SQL Server с помощью локального административного `Users.sql`.

Этот файл находится в `.gitignore`, содержит реальные `CREATE LOGIN ...` с паролями и не должен попадать в Git. Например, для пользователя `repl` в нём хранится фактический:

```sql
CREATE LOGIN [repl] ...
```

Локальный `Users.sql` в этом сценарии используется именно для создания server-level login. Он не должен автоматически удалять восстановленных database users и не предназначен для повторной выдачи всех database-level прав после каждого restore.

### Если database user существует, но не связан с нужным login

Не удалять пользователя базы только ради перепривязки: при удалении database user можно потерять выданные ему database-level permissions.

Перепривязать существующего пользователя к login:

```sql
USE [ReplenishmentDWH];
GO

ALTER USER [Repl_Service] WITH LOGIN = [Repl_Service];
ALTER USER [ReplenishmentREAD] WITH LOGIN = [ReplenishmentREAD];
ALTER USER [repl] WITH LOGIN = [repl];
GO
```

Выполнять только для реально существующих на тестовом server logins.

После этого повторить запрос соответствия `database user ↔ server login` и убедиться, что пользователи больше не orphaned.

### Важно про права

После обычного restore database-level права из backup уже находятся в восстановленной базе. Поэтому в рамках этого сценария **не требуется автоматически пересоздавать все GRANT-ы после каждого restore**, если пользователи базы сохранены и корректно перепривязаны к server logins.

Если пользователь базы был удалён и создан заново, его прежние object/schema permissions необходимо выдавать повторно отдельным permissions/deployment script.

## Полный порядок действий

1. На рабочем сервере создать `COPY_ONLY` backup.
2. На рабочем сервере выполнить `RESTORE VERIFYONLY`.
3. Вручную перенести `.bak` с рабочего сервера на тестовый.
4. На тестовом сервере выполнить `RESTORE FILELISTONLY` и проверить logical names.
5. Переключиться на `master`.
6. Перевести тестовую `ReplenishmentDWH` в `SINGLE_USER WITH ROLLBACK IMMEDIATE`.
7. Выполнить `RESTORE DATABASE ... WITH MOVE, REPLACE, RECOVERY`.
8. Вернуть базу в `MULTI_USER`.
9. Проверить, что база `ONLINE`.
10. Выполнить `DBCC CHECKDB WITH NO_INFOMSGS`.
11. Проверить наличие project server logins на тестовом SQL Server.
12. При необходимости создать отсутствующие logins локальным ignored `Users.sql`.
13. Проверить соответствие database users и server logins.
14. При несовпадении SID перепривязать существующих database users через `ALTER USER ... WITH LOGIN` вместо их удаления.

## Важные замечания

- Команды `SINGLE_USER`, `RESTORE ... REPLACE` и `MULTI_USER` предназначены только для тестового сервера в рамках этого сценария.
- Перед запуском destructive-части обязательно проверить имя SQL Server instance.
- Путь к `.bak` на рабочем и тестовом серверах различается.
- Дата в имени backup-файла должна соответствовать фактически созданному файлу; при следующем восстановлении заменить `ReplenishmentDWH_2026-08-19.bak` на актуальное имя.
- `RESTORE FILELISTONLY` следует выполнять перед restore нового backup, а logical names в `MOVE` использовать из фактического результата.
- Backup базы переносит database users и database-level permissions, но не переносит server logins SQL Server instance.
- Локальный `Users.sql` с реальными `CREATE LOGIN` находится в `.gitignore` и не должен коммититься.
- Не удалять восстановленных database users без необходимости: это может удалить связанные с ними database-level permissions.
