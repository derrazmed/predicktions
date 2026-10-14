# League Prediction Comparison API

## Overview

The League Prediction Comparison API lets an authenticated league member reveal
and compare the score predictions of the other members of the same league.

Predictions are revealed **match by match**. A match becomes visible as soon as
its final result has been persisted and the predictions for that match have been
scored consistently with the scoring rules. A gameweek does not need to be
finished: already eligible matches are visible while the rest of the round is
still scheduled, live, or awaiting scoring.

Before a match is eligible, other members' predictions for it stay hidden and the
API does not leak whether or when they will become visible.

The feature is organized around two read-only endpoints under an existing league.

## Gameweek Definition

A gameweek is the sports provider's **matchday**, scoped to the competition and
season it belongs to. It is *not* derived from the kickoff date.

Matches persist the provider identity in three columns:

- `competition_code` — the competition the match belongs to (for example `CL`).
- `season` — the provider's season identifier (the season's start year).
- `matchday` — the provider's round/matchday within that competition and season.

The comparison groups matches by `(competition_code, season, matchday)`:

- `competition` — the competition code.
- `season` — the provider season.
- `gameweek` — the matchday.

This matters because a matchday is only meaningful inside its competition and
season: matchday 1 of the configured Champions League season kicks off in
September, whose ISO week is 37. Deriving the gameweek from the kickoff date
produced the unrelated label "Gameweek 37" instead of "Gameweek 1".

A match with no provider identity (for example rows synchronised before the
identity columns existed) cannot be placed in a gameweek and is therefore never
shown until it is re-synchronised. Matchdays are never guessed or fabricated.

## Match Eligibility

A match is **eligible** when both of the following hold:

1. Its final result has been persisted: `status` is `FINISHED`, and both
   `homeScore` and `awayScore` are present. A cancelled match has no final result
   and is never eligible.
2. Every stored prediction for the match has been scored consistently with the
   scoring rules (see below). A match with no predictions satisfies this
   condition vacuously.

Only eligible matches are returned, and only eligible matches contribute to a
gameweek's `eligibleMatchCount`. This is enforced in the backend query/service
layer; the frontend never filters incomplete data into a visible state.

### Scoring Consistency

Predictions are stored with a `points` value. A match is only revealed once
those stored values match the current scoring rules:

| Outcome | Points |
|---|---:|
| Exact score | 3 |
| Correct outcome (win/draw/loss) | 1 |
| Incorrect outcome | 0 |

Result synchronization automatically recalculates predictions for finished
matches, so a match unlocks as soon as its final result is persisted and scoring
completes. A manual recalculation also satisfies the rule.

If scoring is incomplete, the match stays hidden rather than exposing partially
scored predictions.

## Authentication

All endpoints require a valid JWT access token.

```http
Authorization: Bearer <access-token>
```

The authenticated user is taken from the security context. The client never sends
the viewer's own `userId`.

## Authorization

- The viewer must be a **member** of the league. Non-members receive `403 Forbidden`.
- The league must exist. Unknown leagues receive `404 Not Found`.
- The optional `memberId` filter must reference a member of the **same** league. A member id from another league receives `404 Not Found` (the API does not reveal whether that user exists elsewhere).

## List Gameweeks

### Endpoint

```http
GET /api/leagues/{leagueId}/comparisons/gameweeks
```

Authentication and league membership are required.

Every gameweek that contains at least one match with a provider identity is
returned, newest season and matchday first. Each summary reports how many of its
matches are currently eligible, so a partially completed round is visible as
soon as one of its matches is ready.

### Successful Response

```json
[
  {
    "competition": "CL",
    "season": 2026,
    "gameweek": 1,
    "label": "Gameweek 1",
    "matchCount": 18,
    "eligibleMatchCount": 6,
    "firstKickoffAt": "2026-09-08T16:45:00Z",
    "lastKickoffAt": "2026-09-10T19:00:00Z"
  }
]
```

If no matches have a provider identity yet, the API returns:

```json
[]
```

The frontend preselects the most recent gameweek whose `eligibleMatchCount` is
greater than zero, falling back to the newest gameweek when none is ready.

## Retrieve a Gameweek Comparison

### Endpoint

```http
GET /api/leagues/{leagueId}/comparisons/gameweeks/{competition}/{season}/{gameweek}
```

Optional query parameter:

| Parameter | Type | Required | Description |
|---|---|---:|---|
| `memberId` | UUID | No | Limit the response to a single league member |

Authentication and league membership are required. The requested
`(competition, season, gameweek)` identifies the round. Only eligible matches
are returned; if the gameweek exists but no match is eligible yet, the response
is `200 OK` with an empty `matches` array.

### Successful Response

```json
{
  "leagueId": "9b1d5c3e-1234-4567-8901-123456789abc",
  "leagueName": "Office League",
  "competition": "CL",
  "season": 2026,
  "gameweek": 1,
  "label": "Gameweek 1",
  "firstKickoffAt": "2026-09-08T16:45:00Z",
  "lastKickoffAt": "2026-09-10T19:00:00Z",
  "members": [
    { "userId": "3f82c7c2-1234-4567-8901-123456789abc", "username": "alice" },
    { "userId": "7e1c2c7e-1234-4567-8901-123456789abc", "username": "bob" }
  ],
  "matches": [
    {
      "matchId": "c0a8012e-1234-4567-8901-123456789abc",
      "homeTeam": "Home FC",
      "awayTeam": "Away FC",
      "kickoffAt": "2026-09-08T16:45:00Z",
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

Members without a prediction are still listed for each match, with
`predictedHomeScore`, `predictedAwayScore`, and `points` set to `null` and
`outcome` set to `NO_PREDICTION`. A missing prediction is therefore never
confused with a zero-point prediction.

### Filtering by member

```http
GET /api/leagues/{leagueId}/comparisons/gameweeks/CL/2026/1?memberId=7e1c2c7e-1234-4567-8901-123456789abc
```

When `memberId` is supplied, both `members` and each match's `predictions`
contain only that member.

## Privacy

The comparison responses expose only:

- league-member usernames;
- their predictions for eligible matches in the requested gameweek;
- the final results of those matches.

Predictions from users outside the league are never returned, and unrelated
members are never surfaced through counts, pagination, or metadata. Email
addresses, password hashes, and other private account data are never included.
The current user is always taken from the security context, never from the
request.

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
An existing gameweek with no eligible matches is **not** a 404: it returns
`200 OK` with an empty `matches` array.

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Gameweek not found",
  "path": "/api/leagues/9b1d5c3e-1234-4567-8901-123456789abc/comparisons/gameweeks/CL/2026/99"
}
```

## Example Requests

List gameweeks:

```bash
curl http://localhost:8080/api/leagues/9b1d5c3e-1234-4567-8901-123456789abc/comparisons/gameweeks \
  -H "Authorization: Bearer <access-token>"
```

Retrieve a gameweek comparison:

```bash
curl "http://localhost:8080/api/leagues/9b1d5c3e-1234-4567-8901-123456789abc/comparisons/gameweeks/CL/2026/1" \
  -H "Authorization: Bearer <access-token>"
```

## API Summary

| Method | Endpoint | Authentication | Description |
|---|---|---|---|
| `GET` | `/api/leagues/{leagueId}/comparisons/gameweeks` | Required + membership | List gameweeks with eligible-match counts |
| `GET` | `/api/leagues/{leagueId}/comparisons/gameweeks/{competition}/{season}/{gameweek}` | Required + membership | Compare member predictions for eligible matches |

## Notes and Limitations

- A gameweek is the provider matchday scoped to its competition and season; it
  is persisted on matches, never inferred from the kickoff date.
- Grouping is global (shared fixtures). League membership is enforced per request.
- Only matches with a persisted final result and completed scoring appear; the
  rest of the gameweek may still be in progress.
- The comparison is read-only; it does not allow editing or deleting predictions.
