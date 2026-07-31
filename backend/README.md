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
