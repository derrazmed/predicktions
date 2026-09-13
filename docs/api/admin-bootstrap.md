# Initial admin bootstrap

Predicktions intentionally has no public endpoint for granting `ADMIN`. A user
must first be created through the normal registration flow, then an operator
can run the controlled bootstrap operation against that existing account.

## How it works

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

## Development procedure

1. Start the application normally.
2. Register the intended account through `POST /api/auth/register`.
3. Stop the application.
4. Start it once with `ADMIN_BOOTSTRAP_EMAIL` set to the registered email.
5. Remove the variable and restart normally.

The account keeps its existing password hash and can log in using the password
set during registration.

## Production procedure

1. Create the intended account through the normal registration flow over the
   protected deployment path.
2. Verify the email identifies the intended account.
3. Run one controlled application startup with `ADMIN_BOOTSTRAP_EMAIL` set to
   that email, using the production environment and database credentials.
4. Remove the variable and restart the application normally.

Do not put a password in application configuration or source code. The
existing registration and BCrypt password hashing flow remains responsible for
the account password.

## Verification

Log in as the promoted user, use the returned JWT to call an endpoint under
`/api/admin/**`, and confirm the request is authorized. A normal `USER` must
receive `403 Forbidden`, and an unauthenticated request must receive
`401 Unauthorized`.

There is intentionally no `make-me-admin`, promotion, or role-change API
endpoint. Role assignment is a backend-controlled database operation only.
