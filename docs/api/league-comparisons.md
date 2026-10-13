# League Prediction Comparison API

## Overview

The League Prediction Comparison API lets an authenticated league member reveal and compare the score predictions of the other members of the same league — but only for a **completed gameweek**.

A gameweek is revealed only after its matches and predictions are fully final. Before that point, other members' predictions stay hidden and the API does not leak whether or when they will become visible.

The feature is organized around two read-only endpoints under an existing league.

## Gameweek Definition

There is no separate gameweek entity. A gameweek is derived from match kickoff times:

- `gameweek` — the ISO-8601 week number of `kickoff_at` in UTC.
- `season` — the calendar year of `kickoff_at` in UTC.

This is the same grouping used by the leaderboard. Because matches are global fixtures, a gameweek is shared by every league; league membership only controls who may view the predictions.

## Visibility Rules

A gameweek is **completed** when all of the following hold:

1. The gameweek contains at least one match.
2. Every match is terminal:
   - `FINISHED` with both `homeScore` and `awayScore` present, or
   - `CANCELLED`.
3. Every stored prediction on the non-cancelled matches has been scored consistently with the scoring rules (see below).

If any match is `SCHEDULED` or `LIVE`, or a `FINISHED` match has no result, the gameweek is not complete. This also covers postponed matches, which remain `SCHEDULED`.

### Scoring Consistency

Predictions are stored with a `points` value. A gameweek is only revealed once those stored values match the current scoring rules:

| Outcome | Points |
|---|---:|
| Exact score | 3 |
| Correct outcome (win/draw/loss) | 1 |
| Incorrect outcome | 0 |
| Cancelled match | 0 (no result) |

Result synchronization automatically recalculates predictions for finished matches, so a gameweek unlocks as soon as the final results are persisted and scoring completes. A manual recalculation also satisfies the rule.

If scoring is incomplete, the gameweek stays hidden rather than exposing partially scored predictions.

## Authentication

All endpoints require a valid JWT access token.

```http
Authorization: Bearer <access-token>
```

The authenticated user is taken from the security context. The client never sends the viewer's own `userId`.

## Authorization

- The viewer must be a **member** of the league. Non-members receive `403 Forbidden`.
- The league must exist. Unknown leagues receive `404 Not Found`.
- The optional `memberId` filter must reference a member of the **same** league. A member id from another league receives `404 Not Found` (the API does not reveal whether that user exists elsewhere).

## List Completed Gameweeks

### Endpoint

```http
GET /api/leagues/{leagueId}/comparisons/gameweeks
```

Authentication and league membership are required.

Only completed gameweeks are returned, newest first. In-progress or not-yet-scored gameweeks are intentionally absent.

### Successful Response

```http
200 OK
```

```json
[
  {
    "season": 2026,
    "gameweek": 12,
    "label": "Gameweek 12",
    "matchCount": 18,
    "firstKickoffAt": "2026-03-16T17:30:00Z",
    "lastKickoffAt": "2026-03-18T20:00:00Z"
  }
]
```

If no gameweek is complete yet, the API returns:

```json
[]
```

## Retrieve a Gameweek Comparison

### Endpoint

```http
GET /api/leagues/{leagueId}/comparisons/gameweeks/{season}/{gameweek}
```

Optional query parameter:

| Parameter | Type | Required | Description |
|---|---|---:|---|
| `memberId` | UUID | No | Limit the response to a single league member |

Authentication and league membership are required, and the gameweek must be completed.

### Successful Response

```http
200 OK
```

```json
{
  "leagueId": "9b1d5c3e-1234-4567-8901-123456789abc",
  "leagueName": "Office League",
  "season": 2026,
  "gameweek": 12,
  "label": "Gameweek 12",
  "firstKickoffAt": "2026-03-16T17:30:00Z",
  "lastKickoffAt": "2026-03-18T20:00:00Z",
  "members": [
    { "userId": "3f82c7c2-1234-4567-8901-123456789abc", "username": "alice" },
    { "userId": "7e1c2c7e-1234-4567-8901-123456789abc", "username": "bob" }
  ],
  "matches": [
    {
      "matchId": "c0a8012e-1234-4567-8901-123456789abc",
      "homeTeam": "Home FC",
      "awayTeam": "Away FC",
      "kickoffAt": "2026-03-16T17:30:00Z",
      "homeScore": 2,
      "awayScore": 1,
      "status": "FINISHED",
      "predictions": [
        {
          "userId": "3f82c7c2-1234-4567-8901-123456789abc",
          "username": "alice",
          "predictedHomeScore": 2,
          "predictedAwayScore": 1,
          "points": 3,
          "outcome": "EXACT_SCORE"
        },
        {
          "userId": "7e1c2c7e-1234-4567-8901-123456789abc",
          "username": "bob",
          "predictedHomeScore": 0,
          "predictedAwayScore": 2,
          "points": 0,
          "outcome": "INCORRECT"
        }
      ]
    }
  ]
}
```

### Prediction outcomes

| Outcome | Meaning |
|---|---|
| `EXACT_SCORE` | Predicted score matched exactly (3 points) |
| `CORRECT_OUTCOME` | Correct win/draw/loss, wrong score (1 point) |
| `INCORRECT` | Wrong outcome (0 points) |
| `NO_PREDICTION` | The member did not predict this match |
| `NO_RESULT` | Match was cancelled; no result to score against |

Members without a prediction are still listed for each match, with `predictedHomeScore`, `predictedAwayScore`, and `points` set to `null` and `outcome` set to `NO_PREDICTION`.

### Filtering by member

```http
GET /api/leagues/{leagueId}/comparisons/gameweeks/2026/12?memberId=7e1c2c7e-1234-4567-8901-123456789abc
```

When `memberId` is supplied, both `members` and each match's `predictions` contain only that member.

## Privacy

The comparison responses expose only:

- league-member usernames;
- their predictions for the requested completed gameweek;
- match results for that gameweek.

Predictions from users outside the league are never returned, and unrelated members are never surfaced through counts, pagination, or metadata. Email addresses, password hashes, and other private account data are never included.

## Error Responses

### 400 Bad Request

Malformed path values, such as a non-numeric `season` or `gameweek`.

### 401 Unauthorized

Returned when the endpoint is accessed without valid authentication.

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Authentication is required",
  "path": "/api/leagues/9b1d5c3e-1234-4567-8901-123456789abc/comparisons/gameweeks"
}
```

### 403 Forbidden

Returned when the authenticated user is not a member of the league.

```json
{
  "status": 403,
  "error": "FORBIDDEN",
  "message": "You are not a member of this league",
  "path": "/api/leagues/9b1d5c3e-1234-4567-8901-123456789abc/comparisons/gameweeks"
}
```

### 404 Not Found

Returned when the league, the gameweek, or the requested member cannot be found.

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "League not found",
  "path": "/api/leagues/9b1d5c3e-1234-4567-8901-123456789abc/comparisons/gameweeks"
}
```

### 409 Conflict

Returned when the requested gameweek exists but is not yet completed.

```json
{
  "status": 409,
  "error": "GAMEWEEK_NOT_COMPLETED",
  "message": "Predictions for this gameweek are not available yet",
  "path": "/api/leagues/9b1d5c3e-1234-4567-8901-123456789abc/comparisons/gameweeks/2026/12"
}
```

## Example Requests

List completed gameweeks:

```bash
curl http://localhost:8080/api/leagues/9b1d5c3e-1234-4567-8901-123456789abc/comparisons/gameweeks \
  -H "Authorization: Bearer <access-token>"
```

Retrieve a gameweek comparison:

```bash
curl "http://localhost:8080/api/leagues/9b1d5c3e-1234-4567-8901-123456789abc/comparisons/gameweeks/2026/12" \
  -H "Authorization: Bearer <access-token>"
```

## API Summary

| Method | Endpoint | Authentication | Description |
|---|---|---|---|
| `GET` | `/api/leagues/{leagueId}/comparisons/gameweeks` | Required + membership | List completed gameweeks |
| `GET` | `/api/leagues/{leagueId}/comparisons/gameweeks/{season}/{gameweek}` | Required + membership | Compare member predictions for a completed gameweek |

## Notes and Limitations

- Gameweeks are derived from kickoff ISO week plus calendar year because the application does not persist an explicit matchday on matches.
- Grouping is global (shared fixtures). League membership is enforced per request.
- The comparison is read-only; it does not allow editing or deleting predictions.
