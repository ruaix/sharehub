# ShareHub security notes

## Architecture

- The browser UI never receives database credentials or encryption keys.
- Spring Security enforces authentication, RBAC and CSRF on the server.
- Spring Session stores sessions in Redis.
- Redis stores CAPTCHA digests, rate-limit counters and temporary account locks.
- PostgreSQL is the recommended production database; MySQL 8.4 is also supported.
- Flyway maintains separate, reviewed migrations for PostgreSQL and MySQL.

## Authentication controls

- Passwords use BCrypt with cost 12.
- Login CAPTCHA values are HMAC protected, expire after five minutes and are consumed once.
- Limits apply independently to source IP and normalized account.
- Five failures lock an account for fifteen minutes.
- Registration is closed by default and can only be enabled by an administrator.
- Approval, registration switches and security-relevant actions are written to the audit log.
- Cookies are HttpOnly and SameSite Strict. Set `COOKIE_SECURE=true` behind production HTTPS.

## Sensitive fields

- Shared passwords, panel URLs and subscription URLs use authenticated AES-256-GCM encryption.
- `SHAREHUB_MASTER_KEY` must be a Base64-encoded 32-byte key.
- Keep the master key outside the database and source repository.
- Losing the key makes encrypted fields unrecoverable; leaking it together with the database exposes those fields.

## Production requirements

- Run the Java process as an unprivileged user.
- Keep PostgreSQL, MySQL and Redis on a private Docker network; do not publish their ports.
- Replace every value in `.env`; never commit `.env`.
- Terminate HTTPS at Nginx or Caddy.
- Set a small JDBC pool on 1GB systems (`DB_POOL_SIZE=5` or lower).
- Back up the database with `pg_dump` or `mysqldump`, encrypt the resulting file, store it off-host and test recovery.
- Apply dependency and container-image security updates regularly.

## Docker startup

PostgreSQL:

```bash
docker compose -f docker-compose.yml -f docker-compose.postgresql.yml up -d --build
```

MySQL:

```bash
docker compose -f docker-compose.yml -f docker-compose.mysql.yml up -d --build
```
