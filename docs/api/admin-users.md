# Admin users API

## List users

```http
GET /api/admin/users?page=0&size=20
```

Requires a valid JWT for a user with the `ADMIN` role. Unauthenticated
requests return `401 Unauthorized`; authenticated non-admin users return
`403 Forbidden`.

The endpoint is read-only and returns paginated, safe administrative user
information. Passwords, password hashes, tokens, and authentication secrets
are never included.

### Response

```json
{
  "content": [
    {
      "id": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
      "username": "testuser",
      "email": "test@example.com",
      "role": "USER",
      "createdAt": "2026-09-12T23:24:18.470425Z",
      "enabled": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

`page` is zero-based. `size` defaults to `20` and is limited to `100`.
Users are ordered by newest registration first.

## User details

```http
GET /api/admin/users/{userId}
```

Requires the `ADMIN` role. Missing authentication returns `401 Unauthorized`,
authenticated non-admin users receive `403 Forbidden`, and an unknown UUID
returns `404 Not Found`.

The response contains account information, prediction count, total points,
current league summaries, and the global leaderboard position. It uses the
same leaderboard ranking and tie behavior as the global leaderboard endpoint.

## Change user role

```http
PATCH /api/admin/users/{userId}/role
Content-Type: application/json
```

Example request:

```json
{
  "role": "ADMIN"
}
```

Only authenticated `ADMIN` users can perform this operation. Valid roles are
`USER` and `ADMIN`; role changes are idempotent. Unauthenticated requests
return `401 Unauthorized` and authenticated non-admin users return
`403 Forbidden`.

The final administrator cannot be demoted. Attempts to submit an unknown,
missing, or null role return `400 Bad Request`. Unknown user IDs return
`404 Not Found`. The response is the safe user representation and never
contains passwords, password hashes, tokens, or credentials.

## Enable or disable a user

```http
PATCH /api/admin/users/{userId}/status
Content-Type: application/json
```

Example:

```json
{
  "enabled": false
}
```

Only authenticated `ADMIN` users can perform this operation. Missing
authentication returns `401 Unauthorized`; authenticated non-admin users
receive `403 Forbidden`. The `enabled` property is required and must be a
boolean. Disabling an account does not delete its data, but prevents password
login and invalidates its existing JWT access immediately. Re-enabling restores
normal authentication.

The last enabled administrator cannot be disabled. Attempts to disable that
account return `400 Bad Request`; unknown user IDs return `404 Not Found`.
The response uses the safe admin user representation and never contains
passwords, tokens, or credentials.

## Adjust points for a user

```http
POST /api/admin/users/{userId}/points
Content-Type: application/json
```

Example request:

```json
{
  "points": 10,
  "reason": "Bonus for administrative correction"
}
```

Negative adjustments use the same endpoint:

```json
{
  "points": -5,
  "reason": "Manual correction"
}
```

This operation requires the `ADMIN` role. The points value must be a signed,
non-zero integer between `-1000000` and `1000000`; positive values add points
and negative values remove points. The reason is required (maximum 500
characters). The authenticated
administrator is recorded automatically; an administrator identity cannot be
provided by the client.

Each adjustment is permanently stored as an immutable audit event containing
the player, signed points value, reason, administrator, and timestamp.
Adjustments are added to the player's prediction points for global and league
leaderboard ranking, and are reflected in user details. Do not modify
prediction scores to remove points. Unknown users return `404 Not Found`,
validation errors return `400 Bad Request`, unauthenticated requests return
`401 Unauthorized`, and authenticated non-admin users return `403 Forbidden`.

The response confirms the award without exposing passwords, tokens, or other
credentials:

```json
{
  "userId": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
  "username": "testuser",
  "points": 10,
  "reason": "Correction for incorrectly scored prediction",
  "awardedBy": "1bd7a7d3-4ea8-4c10-9bf6-17a7d5d2d2e1",
  "createdAt": "2026-09-14T12:30:00Z"
}
```

Every successful adjustment creates a permanent audit record. The authenticated
administrator is stored as `adminId`; clients cannot provide or impersonate
that identity.

## Point adjustment history

```http
GET /api/admin/users/{userId}/points/adjustments?page=0&size=20
```

This endpoint requires the `ADMIN` role and returns the player's signed manual
point adjustments, newest first. Pagination is zero-based, defaults to page
`0` and size `20`, and is limited to `100` records per page.

```json
{
  "content": [
    {
      "id": "6db0f0c0-1f30-4d31-9d35-7a1a6761d1a0",
      "userId": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
      "adminId": "1bd7a7d3-4ea8-4c10-9bf6-17a7d5d2d2e1",
      "points": -5,
      "reason": "Manual correction",
      "createdAt": "2026-09-14T12:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

An existing player with no adjustments returns an empty `content` array.
Unknown players return `404 Not Found`; unauthenticated requests return
`401 Unauthorized`; authenticated non-admin users return `403 Forbidden`.

## All point adjustments

```http
GET /api/admin/points/adjustments?page=0&size=20
```

Administrators can inspect all manual adjustments across all players. Optional
filters are `userId`, `adminId`, `from`, and `to`:

```http
GET /api/admin/points/adjustments?userId=<uuid>&adminId=<uuid>&from=2026-09-01T00:00:00Z&to=2026-09-15T23:59:59Z&page=0&size=50
```

`from` and `to` are inclusive ISO-8601 timestamps. Results are ordered by
newest `createdAt`, then descending adjustment ID, and use the same paginated
response shape as the user-specific history endpoint. Malformed UUIDs,
timestamps, reversed date ranges, and page sizes above `100` return
`400 Bad Request`. No matching records return `200 OK` with an empty
`content` array. This endpoint is ADMIN-only and is read-only; retrieving
history never changes points or creates audit records.
