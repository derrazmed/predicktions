# Initial admin bootstrap

Predicktions intentionally has no public endpoint for granting `ADMIN`. There
are two backend-controlled startup mechanisms; both are disabled unless
configured and neither ever uses default credentials.

1. **Create the initial administrator** when none exists yet (recommended for
   fresh deployments).
2. **Promote an already-registered user** to `ADMIN`.

Both run once at startup and are idempotent.

## 1. Creating the initial administrator automatically

Set all three environment variables for one startup:

```bash
SPRING_PROFILES_ACTIVE=dev \
ADMIN_INITIAL_NAME=admin \
ADMIN_INITIAL_EMAIL=admin@example.com \
ADMIN_INITIAL_PASSWORD='a-strong-password' \
  ./mvnw spring-boot:run
```

They are read as `admin.initial.name`, `admin.initial.email` and
`admin.initial.password`. Behaviour:

- The account is created with the `ADMIN` role, `enabled = true`, and a BCrypt
  hash of the password. The raw password is never stored or logged.
- The email is trimmed and lower-cased before storage, and the existing-user
  check is case-insensitive, so casing cannot bypass it.
- If any of the three values is blank, nothing is created and a warning is
  logged.
- If the database already contains an `ADMIN`, nothing is created.
- If the configured email or username already belongs to another account, the
  account is **not** promoted or overwritten; a warning is logged and
  initialization stops.
- Creation is idempotent: restarting does not create a duplicate. If two
  instances start simultaneously, the `users` table's unique constraints on
  `username`/`email` resolve the race and the losing instance logs and stops.
- After the account exists, the three variables can be removed; the account is
  left untouched. They are intentionally not reset on every startup.

On the deployed backend (e.g. Render) set the same three variables in the
service's environment, redeploy once, then remove them. Never set them on the
frontend, and never use a `VITE_` prefix — `VITE_*` values are embedded in the
public browser bundle.

## 2. Promoting an existing account

A user must first be created through the normal registration flow, then an
operator can run the controlled bootstrap operation against that existing
account.

Set `ADMIN_BOOTSTRAP_EMAIL` in the environment for one application startup:

```bash
SPRING_PROFILES_ACTIVE=dev ADMIN_BOOTSTRAP_EMAIL=admin@example.com \
  ./mvnw spring-boot:run
```

The application reads `admin.bootstrap.email` and promotes the existing user
with that exact email to `ADMIN` in the database. If the property is not set,
no bootstrap operation runs. If no matching user exists, startup fails instead
of creating an account or choosing a different user.

The operation is idempotent. Running it again leaves the same user as `ADMIN`
and does not create duplicate users. An existing `ADMIN` is never demoted.

## Verification

Log in as the administrator, use the returned JWT to call an endpoint under
`/api/admin/**`, and confirm the request is authorized. A normal `USER` must
receive `403 Forbidden`, and an unauthenticated request must receive
`401 Unauthorized`.

There is intentionally no `make-me-admin`, promotion, or role-change API
endpoint for anonymous callers. Role assignment is a backend-controlled
database operation only.
