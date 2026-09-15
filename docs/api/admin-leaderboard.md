# Administrative leaderboards

These read-only endpoints require an authenticated JWT with the `ADMIN` role.
They use the same `LeaderboardService` ranking, ordering, tie handling, and
point aggregation as the normal application endpoints.

## Global leaderboard

```http
GET /api/admin/leaderboard
GET /api/admin/leaderboard?gameweek=3
GET /api/admin/leaderboard?season=2026
GET /api/admin/leaderboard?season=2026&gameweek=3
```

`gameweek` is the UTC ISO week (`1` through `53`) of a match kickoff, matching
the existing administrative match/prediction convention. `season` is the UTC
kickoff calendar year because the current match model has no separate season
column. Filters are applied to prediction points in the requested scope.

The response is the existing leaderboard array:

```json
[
  {
    "rank": 1,
    "userId": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
    "username": "testuser",
    "totalPoints": 127
  }
]
```

Valid filters with no matching predictions return `200 OK` and an empty array.
Invalid gameweeks return `400 Bad Request`. Invalid numeric filter values also
follow the standard `400` request handling.

## League leaderboard

```http
GET /api/admin/leagues/{leagueId}/leaderboard
```

This returns the existing league leaderboard response for the requested league
and uses the same ranking implementation as the normal member-only endpoint.
An unknown league returns `404 Not Found`; invalid UUIDs return `400 Bad
Request`. Unauthenticated and non-admin requests return `401` and `403`.

## Recalculate the global leaderboard

```http
POST /api/admin/leaderboard/recalculate
```

This administrative recovery endpoint requires the `ADMIN` role and has no
request body. The application calculates leaderboard standings dynamically from
persisted prediction points (plus existing point adjustments); it does not
persist leaderboard totals or positions. The operation therefore re-runs the
existing global leaderboard query and ranking logic without incrementing points
or creating derived leaderboard state.

Example response:

```json
{
  "recalculatedAt": "2026-09-15T10:30:00Z",
  "usersProcessed": 25,
  "entriesUpdated": 0,
  "entriesUnchanged": 25,
  "message": "Leaderboard recalculated from prediction data."
}
```

The operation is idempotent. Use
`POST /api/admin/matches/{matchId}/recalculate` to repair persisted prediction
points after a match-result issue; this endpoint only rebuilds the dynamic
leaderboard view from the current source data.
