# Admin audit log

Successful sensitive administrator mutations append an immutable `admin_audit_logs`
record in the same transaction as the mutation. Records contain the authenticated
administrator ID, controlled action and target values, human-readable details, and
UTC `createdAt`. The administrator ID is taken from the authenticated JWT/security
context; it is never accepted from a request body.

`GET /api/admin/audit-logs` is restricted to `ADMIN` and uses database pagination
(`page=0`, `size=20`, maximum size 100), ordered newest first (`createdAt`, then
`id`). Optional filters are `adminId`, `action`, `targetType`, `targetId`, `from`,
and `to` (ISO-8601 instants). Audit rows cannot be edited or deleted through the API.
`LEADERBOARD` entries have no target ID.

Actions include role/status changes, points adjustments, match-result changes,
prediction recalculation, league deletion/member removal, and leaderboard
recalculation. Existing `PointAdjustment` history remains unchanged and
complements this administrative history.
