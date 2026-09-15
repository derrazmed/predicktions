# Administrative league operations

Administrative league endpoints require an authenticated JWT with the `ADMIN`
role. The backend `/api/admin/**` security rule enforces this requirement.

## Retrieve league details

```http
GET /api/admin/leagues/{leagueId}
```

The response contains the league metadata, safe owner/member details, the
existing league leaderboard (including its tie-ranking behavior), and
statistics derived from persisted predictions. Members are ordered by join
time. This endpoint is read-only and does not modify league, membership,
prediction, or leaderboard data.

Example response:

```json
{
  "id": "d73d3337-2d35-4f18-8d43-ad7619d28d9d",
  "name": "mo3a9in",
  "createdAt": "2026-09-12T23:24:18.470425Z",
  "joinCode": "VBS3S3KA",
  "memberCount": 1,
  "owner": {
    "id": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
    "username": "testuser"
  },
  "members": [
    {
      "userId": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
      "username": "testuser",
      "joinedAt": "2026-09-12T23:24:18.470425Z",
      "role": "USER"
    }
  ],
  "leaderboard": [],
  "statistics": {
    "totalPredictions": 0,
    "predictionsWithPoints": 0,
    "totalPointsAwarded": 0,
    "averagePointsPerPrediction": 0.0,
    "exactScorePredictions": 0,
    "participatingMembers": 0
  }
}
```

Unknown leagues return `404 Not Found`; invalid UUIDs return `400 Bad Request`.
Unauthenticated and non-admin requests return `401` and `403`.

## Delete a league

```http
DELETE /api/admin/leagues/{leagueId}
```

This destructive operation requires the `ADMIN` role and returns `204 No
Content` when the league exists. The league's membership rows are deleted
before the league row. The owner and all members remain intact, including
their accounts, predictions, points, and memberships in other leagues.
Matches, global leaderboard data, point-adjustment history, and unrelated
leagues are also preserved. A repeated deletion or unknown league returns
`404 Not Found`; unauthenticated and non-admin requests return `401` and
`403`.
