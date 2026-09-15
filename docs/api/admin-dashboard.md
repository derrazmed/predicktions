# Administrative dashboard

## Retrieve dashboard statistics

```http
GET /api/admin/dashboard
```

This endpoint requires an authenticated JWT with the `ADMIN` role. It is
read-only and calculates current values from the existing database tables.

```json
{
  "users": 152,
  "activeUsers": 121,
  "admins": 2,
  "leagues": 34,
  "predictions": 4821,
  "matches": 120,
  "predictionsToday": 87
}
```

Definitions:

- `users`: all registered users, including disabled accounts.
- `activeUsers`: users whose persisted `enabled` value is `true`.
- `admins`: users with the `ADMIN` role.
- `leagues`: persisted leagues.
- `predictions`: persisted predictions.
- `matches`: persisted matches.
- `predictionsToday`: predictions created during the current UTC day,
  inclusive of `00:00:00Z` and exclusive of the following UTC midnight.

Counts are retrieved using database-side count queries; no dashboard counters
or cache is persisted. Admin requests return `200 OK`, authenticated
non-admin requests return `403`, and unauthenticated requests return `401`.
