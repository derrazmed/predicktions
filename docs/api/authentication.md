# Authentication API

This document describes the authentication endpoints provided by the Predicktions API.

## Authentication Overview

Predicktions uses JWT (JSON Web Token) authentication.

Registration and login are public endpoints. Protected endpoints require a valid JWT supplied in the `Authorization` HTTP header using the Bearer authentication scheme.

### Authentication flow

1. Register an account using `POST /api/auth/register`.
2. Log in using `POST /api/auth/login`.
3. The API returns an access token.
4. Include the token in the `Authorization` header when accessing protected endpoints.

Example:

```http
Authorization: Bearer <access-token>
```

---

## Register

Creates a new user account.

### Endpoint

```http
POST /api/auth/register
```

### Authentication

No authentication required.

### Request

Content-Type:

```http
application/json
```

Example:

```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123"
}
```

### Successful response

```http
HTTP/1.1 201 Created
```

Example:

```json
{
  "id": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
  "username": "testuser",
  "email": "test@example.com"
}
```

The password and password hash are never returned by the API.

### Validation

The registration request must contain valid values for:

- `username`
- `email`
- `password`

Invalid requests return:

```http
HTTP/1.1 400 Bad Request
```

Duplicate usernames or email addresses are rejected.

---

## Login

Authenticates an existing user and returns a JWT access token.

### Endpoint

```http
POST /api/auth/login
```

### Authentication

No authentication required.

### Request

Content-Type:

```http
application/json
```

Example:

```json
{
  "username": "testuser",
  "password": "password123"
}
```

### Successful response

```http
HTTP/1.1 200 OK
```

Example:

```json
{
  "accessToken": "<JWT>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

`expiresIn` is expressed in seconds.

### Invalid credentials

Invalid login credentials are rejected with:

```http
HTTP/1.1 401 Unauthorized
```

The API does not expose whether the username exists when authentication fails.

---

## Current User

Returns the user associated with the authenticated JWT.

### Endpoint

```http
GET /api/users/me
```

### Authentication

Authentication required.

A valid JWT must be provided using the Bearer authentication scheme.

### Request

Example:

```http
GET /api/users/me
Authorization: Bearer <access-token>
```

### Successful response

```http
HTTP/1.1 200 OK
```

Example:

```json
{
  "id": "76046ebc-93f1-41eb-81d1-e53a7164ad52",
  "username": "testuser",
  "email": "test@example.com"
}
```

The password and password hash are never returned.

### Missing or invalid token

Requests without a valid JWT are rejected with:

```http
HTTP/1.1 401 Unauthorized
```

---

## Bearer Token Usage

After successfully logging in, use the returned `accessToken` when calling protected endpoints.

The token must be included in the `Authorization` header:

```http
Authorization: Bearer <access-token>
```

For example:

```bash
curl http://localhost:8080/api/users/me   -H "Authorization: Bearer <access-token>"
```

The API is stateless. Authentication is performed using the JWT rather than a server-side HTTP session.

---

## JWT Configuration

JWT configuration is provided through environment variables.

### Required variables

| Variable | Description | Example |
|---|---|---|
| `JWT_SECRET` | Secret key used to sign and verify JWTs | `change-me` |
| `JWT_EXPIRATION` | JWT lifetime in milliseconds | `3600000` |

Example:

```env
JWT_SECRET=change-me
JWT_EXPIRATION=3600000
```

### Security considerations

Do not commit a real JWT secret to source control.

For local development, configure the values through the project's environment configuration.

For production, provide the JWT secret through the deployment environment or a dedicated secret-management system.

---

## Endpoint Summary

| Method | Endpoint | Authentication |
|---|---|---|
| `POST` | `/api/auth/register` | Public |
| `POST` | `/api/auth/login` | Public |
| `GET` | `/api/users/me` | Bearer JWT required |

Other `/api/**` endpoints are protected by default unless explicitly configured as public.
