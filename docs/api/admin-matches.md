# Administrative match operations

Administrative match endpoints require an authenticated JWT with the `ADMIN`
role. Authorization is enforced by the backend `/api/admin/**` security rule.

## Correct a match result

```http
PATCH /api/admin/matches/{matchId}/result
Content-Type: application/json
```

```json
{
  "homeScore": 2,
  "awayScore": 1
}
```

Both scores are required non-negative integers. A malformed request, missing
value, or negative score returns `400 Bad Request`. An unknown match returns
`404 Not Found`; unauthenticated and non-admin requests return `401` and `403`
respectively.

The existing match is updated in a transaction and its predictions are
recalculated using the application's score rules (3 points for an exact score,
1 for the correct result, and 0 otherwise). This means leaderboard totals are
updated through the existing persisted prediction-point aggregation. The match
and prediction relationships are preserved. Submitting the same result is
idempotent.

Example response:

```json
{
  "id": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
  "externalId": "12345",
  "homeTeam": "Home",
  "awayTeam": "Away",
  "kickoffAt": "2026-09-14T12:30:00Z",
  "homeScore": 2,
  "awayScore": 1,
  "status": "FINISHED"
}
```

Each correction is logged with the authenticated administrator, match ID,
previous result, new result, and timestamp. The endpoint never accepts an
administrator identity from the request body.

## Recalculate prediction points

```http
POST /api/admin/matches/{matchId}/recalculate
```

This endpoint requires the `ADMIN` role and has no request body. It leaves the
match result and all match metadata unchanged, then evaluates every prediction
against the currently persisted result using the shared prediction-scoring
service. Only predictions whose persisted points differ are written.

Example response:

```json
{
  "matchId": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
  "homeScore": 2,
  "awayScore": 1,
  "predictionsProcessed": 42,
  "predictionsUpdated": 7,
  "predictionsUnchanged": 35,
  "recalculatedAt": "2026-09-15T10:30:00Z"
}
```

The operation is transactional and idempotent. A match with no predictions
returns `200 OK` with all three counts set to zero. Unknown matches return
`404 Not Found`; unauthenticated and non-admin requests return `401` and
`403`. Unlike `PATCH /result`, this endpoint never changes the match result.
