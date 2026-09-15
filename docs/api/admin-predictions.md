# Admin predictions API

## Retrieve predictions

```http
GET /api/admin/predictions?page=0&size=20
```

This endpoint requires a JWT for an `ADMIN` user. Unauthenticated requests
return `401 Unauthorized`; authenticated non-admin users return `403 Forbidden`.

Pagination is zero-based. `size` defaults to `20` and is limited to `100`.
Results are ordered by prediction creation time descending, with prediction ID
as a deterministic tie-breaker. An empty or out-of-range page returns an empty
`content` array with pagination metadata.

### Filters

The endpoint supports any combination of:

- `userId` - user UUID
- `matchId` - match UUID
- `leagueId` - league UUID; includes predictions made by members of that league
- `gameweek` - ISO week number (`1` through `53`) of the match kickoff in UTC
- `fromDate` and `toDate` - inclusive UTC dates (`YYYY-MM-DD`) based on when the
  prediction was created

Example:

```http
GET /api/admin/predictions?gameweek=3&leagueId=d73d3337-2d35-4f18-8d43-ad7619d28d9d&page=0&size=50
```

Invalid UUIDs, dates, page values, sizes, gameweeks, or a date range where
`fromDate` follows `toDate` return `400 Bad Request`.

### Response

```json
{
  "content": [
    {
      "id": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
      "userId": "f4e0c20b-7f4a-4c56-a2d5-1d5b7d6b9a0e",
      "username": "testuser",
      "matchId": "d73d3337-2d35-4f18-8d43-ad7619d28d9d",
      "gameweek": 3,
      "predictedHomeScore": 2,
      "predictedAwayScore": 1,
      "points": 3,
      "createdAt": "2026-09-13T10:30:00Z",
      "updatedAt": "2026-09-13T10:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

The response is a dedicated administrative DTO and does not include passwords,
password hashes, tokens, credentials, or other authentication data.

## Retrieve predictions for a match

```http
GET /api/admin/matches/{matchId}/predictions?page=0&size=20
```

This endpoint uses the same administrative response and pagination format as
the filtered endpoint above, but scopes the result to one match. It requires
an `ADMIN` JWT. A missing match returns `404 Not Found`; a match with no
submitted predictions returns an empty `content` array. Pagination is
zero-based, defaults to `page=0` and `size=20`, and `size` is limited to `100`.

For example:

```http
GET /api/admin/matches/d73d3337-2d35-4f18-8d43-ad7619d28d9d/predictions?page=0&size=50
```
