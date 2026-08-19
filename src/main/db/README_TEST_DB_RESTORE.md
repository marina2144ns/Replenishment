# Восстановление тестовой базы ReplenishmentDWH из backup рабочего сервера

Эта инструкция описывает фактически проверенный порядок создания backup рабочей базы `ReplenishmentDWH` и восстановления его на тестовом SQL Server.

Главный принцип после восстановления: **не использовать пользователей и права из backup как итоговую конфигурацию тестового сервера**. После restore проектные logins/users/permissions приводятся к чистому воспроизводимому состоянию проектными SQL-скриптами — так же, как при установке проекта на пустой SQL Server.

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

## 10. После restore пересоздать проектных пользователей и права

Restore переносит database users и database-level permissions из рабочей базы. Для тестового сервера это считается только промежуточным состоянием.

После восстановления необходимо привести security configuration к проектному эталону:

1. удалить project-specific database users, если они пришли из backup;
2. удалить project-specific server logins, если они уже существуют на тестовом SQL Server;
3. создать server logins заново;
4. создать database users для этих logins заново;
5. выдать права заново по актуальному deployment contract проекта.

Для проекта это относится к следующим principals:

```text
Repl_Service
ReplenishmentREAD
repl
```

Именно такой подход исключает зависимость от SID, старых GRANT/DENY и других настроек безопасности, случайно попавших в backup.

### Пароли и локальный Users.sql

Реальные пароли не хранятся в Git.

Локальный `Users.sql` находится в `.gitignore` и содержит реальные `CREATE LOGIN ...`, например:

```sql
CREATE LOGIN [repl] ...
```

Он используется как локальная административная часть установки server-level logins и не должен коммититься.

### Tracked deployment contract

Tracked-файл:

```text
src/main/db/tables/Users.example.sql
```

должен описывать воспроизводимую структуру project principals и актуальные права без реальных секретов.

Его назначение — быть эталоном, по которому security setup можно воспроизвести:

- после восстановления backup на тестовом сервере;
- при развертывании проекта на новом/пустом SQL Server;
- после изменения состава таблиц, процедур или требуемых runtime permissions.

После выполнения security setup итог не должен зависеть от того, какие project users/permissions находились в backup до restore.

## 11. Требование к установке на пустом SQL Server

DB-скрипты проекта должны позволять воспроизвести окружение с нуля.

Для чистой установки последовательность должна быть концептуально такой:

```text
пустой SQL Server
    ↓
создание ReplenishmentDWH
    ↓
создание таблиц / индексов / constraints / procedures
    ↓
создание server logins из локального ignored Users.sql
    ↓
создание database users
    ↓
выдача актуальных permissions
    ↓
готовая база для запуска приложения
```

Backup рабочего сервера не является обязательной частью deployment contract. Он используется только для переноса production-like данных на тестовый сервер.

Таким образом, восстановление тестовой базы и fresh installation должны приходить к одному и тому же итоговому состоянию схемы и security configuration.

## Полный порядок восстановления тестовой базы

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
11. Удалить project-specific users/logins, пришедшие из backup или уже существующие на test instance.
12. Создать project server logins заново с локальными реальными паролями.
13. Создать database users заново.
14. Выдать все актуальные project permissions заново.
15. После этого запускать приложение и функциональные тесты.

## Важные замечания

- Команды `SINGLE_USER`, `RESTORE ... REPLACE` и security reset предназначены только для тестового сервера в рамках этого сценария.
- Перед запуском destructive-части обязательно проверить имя SQL Server instance.
- Путь к `.bak` на рабочем и тестовом серверах различается.
- Дата в имени backup-файла должна соответствовать фактически созданному файлу; при следующем восстановлении заменить `ReplenishmentDWH_2026-08-19.bak` на актуальное имя.
- `RESTORE FILELISTONLY` следует выполнять перед restore нового backup, а logical names в `MOVE` использовать из фактического результата.
- Локальный `Users.sql` с реальными `CREATE LOGIN` находится в `.gitignore` и не должен коммититься.
- После restore не считать project users/permissions из backup итоговой конфигурацией: они пересоздаются по актуальному проектному contract.
- Цель DB deployment scripts — одинаково воспроизводимое состояние и после restore, и на новом пустом SQL Server.
