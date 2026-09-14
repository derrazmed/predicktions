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
