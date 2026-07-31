# ShareHub backend

Java 17, Spring Boot, Spring Security, MyBatis-Plus, Flyway, Redis and Spring Session.

## Local PostgreSQL

Set the variables from the project `.env`, then run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:SPRING_PROFILES_ACTIVE='postgresql'
mvn -s C:\Users\Admin\.m2\settings.xml spring-boot:run
```

## Local MySQL

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:SPRING_PROFILES_ACTIVE='mysql'
mvn -s C:\Users\Admin\.m2\settings.xml spring-boot:run
```

Flyway selects `db/migration/postgresql` or `db/migration/mysql` automatically.

## Business API permissions

- `GET /api/services`, `/api/memberships`, `/api/orders`: authenticated users; members only receive their own data.
- `GET /api/memberships/{id}/access`: only the owning member or an administrator; expired and cancelled memberships cannot reveal sensitive access data to members.
- `/api/admin/services/**`, `/api/admin/memberships/**`, `/api/admin/orders`: administrator only.
- All state-changing endpoints require the `X-CSRF-Token` returned by the login or session endpoint.

Service credentials, proxy panel URLs and subscription URLs are encrypted before storage. List APIs never return these plaintext values.
