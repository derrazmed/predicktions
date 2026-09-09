# Prediction API

## Overview

The Prediction API allows authenticated users to submit and retrieve score predictions for matches.

Predictions use an exact score format:

- `predictedHomeScore` — predicted home-team score.
- `predictedAwayScore` — predicted away-team score.

A user can have at most one prediction for a given match.

Predictions are locked at kickoff.

## Authentication

Prediction endpoints require a valid JWT access token.

Send the token with:

```http
Authorization: Bearer <access-token>
```

The authenticated user's identity is obtained from the security context. The client does not provide a `userId` when creating or retrieving predictions.

## Create a Prediction

### Endpoint

```http
POST /api/predictions
```

Authentication is required.

### Request

```json
{
  "matchId": "3f82c7c2-1234-4567-8901-123456789abc",
  "predictedHomeScore": 2,
  "predictedAwayScore": 1
}
```

### Request fields

| Field | Type | Required | Description |
|---|---|---:|---|
| `matchId` | UUID | Yes | ID of the match being predicted |
| `predictedHomeScore` | Integer | Yes | Predicted home-team score |
| `predictedAwayScore` | Integer | Yes | Predicted away-team score |

Both scores must be non-negative integers (`>= 0`). Negative scores are rejected with `400 Bad Request`.

## Successful Response

A successful creation returns `201 Created`.

```json
{
  "id": "7e1c2c7e-1234-4567-8901-123456789abc",
  "matchId": "3f82c7c2-1234-4567-8901-123456789abc",
  "predictedHomeScore": 2,
  "predictedAwayScore": 1,
  "points": 0,
  "createdAt": "2026-09-09T20:30:00Z",
  "updatedAt": "2026-09-09T20:30:00Z"
}
```

`points` is initially `0` and is used by the scoring system.

Password and password-hash information is never included in prediction responses.

## Duplicate Predictions

A user can only create one prediction per match.

A second prediction for the same user and match returns:

```http
409 Conflict
```

Example:

```json
{
  "status": 409,
  "error": "DUPLICATE_RESOURCE",
  "message": "Prediction already exists for this match",
  "path": "/api/predictions"
}
```

The database also enforces uniqueness for the `(user_id, match_id)` combination.

## Prediction Locking

The core rule is:

```text
Before kickoff → prediction allowed
At kickoff      → prediction locked
After kickoff   → prediction locked
```

Predictions submitted at or after kickoff are rejected.

A locked prediction returns:

```http
409 Conflict
```

Example:

```json
{
  "status": 409,
  "error": "PREDICTION_LOCKED",
  "message": "Predictions are locked because the match has already started.",
  "path": "/api/predictions"
}
```

Predictions cannot be created for finished or cancelled matches.

## Retrieve User Predictions

### Endpoint

```http
GET /api/predictions
```

Authentication is required.

The user's identity is taken from the JWT/security context. Only predictions belonging to the authenticated user are returned.

### Successful Response

```http
200 OK
```

Example:

```json
[
  {
    "id": "7e1c2c7e-1234-4567-8901-123456789abc",
    "matchId": "3f82c7c2-1234-4567-8901-123456789abc",
    "predictedHomeScore": 2,
    "predictedAwayScore": 1,
    "points": 3,
    "createdAt": "2026-09-09T20:30:00Z",
    "updatedAt": "2026-09-09T20:30:00Z"
  }
]
```

If the user has no predictions, the API returns:

```json
[]
```

## Error Responses

### 400 Bad Request

Used for invalid request data or invalid prediction state, such as negative scores or a cancelled match.

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "predictedHomeScore must be non-negative",
  "path": "/api/predictions"
}
```

### 401 Unauthorized

Returned when a protected prediction endpoint is accessed without valid authentication.

```json
{
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Authentication is required",
  "path": "/api/predictions"
}
```

### 404 Not Found

Returned when the requested match does not exist.

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Match not found",
  "path": "/api/predictions"
}
```

### 409 Conflict

Used for conflicts such as:

- duplicate prediction for the same user and match;
- prediction submitted at or after kickoff.

## Bearer Token Usage

Retrieve predictions:

```bash
curl -X GET http://localhost:8080/api/predictions   -H "Authorization: Bearer <access-token>"
```

Create a prediction:

```bash
curl -X POST http://localhost:8080/api/predictions   -H "Authorization: Bearer <access-token>"   -H "Content-Type: application/json"   -d '{
    "matchId": "3f82c7c2-1234-4567-8901-123456789abc",
    "predictedHomeScore": 2,
    "predictedAwayScore": 1
  }'
```

## API Summary

| Method | Endpoint | Authentication | Description |
|---|---|---|---|
| `POST` | `/api/predictions` | Required | Create a score prediction |
| `GET` | `/api/predictions` | Required | Retrieve the authenticated user's predictions |

The client supplies the match and predicted scores. The authenticated user is determined from the JWT.
