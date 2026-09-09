# Predicktions

Predicktions is a sports prediction application where authenticated users submit score predictions for matches and track their results.

## Backend Stack

- Java
- Spring Boot
- Spring Security
- JWT authentication
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Maven
- Testcontainers

## Local Development

### Prerequisites

- Java
- Docker
- Git
- PostgreSQL, or the project's PostgreSQL Docker setup

### Environment Variables

Create a local `.env` file from `.env.example`.

```env
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://localhost:5432/predicktions
DB_USERNAME=predicktions
DB_PASSWORD=change-me
SERVER_PORT=8080
JWT_SECRET=change-me
JWT_EXPIRATION=3600000
```

Do not commit `.env` or real secrets.

### Start the Application

Start the PostgreSQL database, then:

```bash
set -a
source .env
set +a

./mvnw spring-boot:run
```

The API is available at:

```text
http://localhost:8080
```

Flyway applies database migrations automatically at application startup.

## Authentication

Protected API endpoints require a JWT access token.

Use:

```http
Authorization: Bearer <access-token>
```

The authenticated user's identity comes from the security context. Clients do not provide their own `userId` when working with predictions.

## Prediction API

Detailed documentation is available at:

```text
docs/api/predictions.md
```

Current prediction endpoints:

```text
POST /api/predictions
GET  /api/predictions
```

Predictions use an exact score format:

```json
{
  "matchId": "...",
  "predictedHomeScore": 2,
  "predictedAwayScore": 1
}
```

Scores must be non-negative integers.

A user can create only one prediction per match.

### Prediction Locking

Predictions can only be submitted before match kickoff:

```text
Before kickoff → allowed
At kickoff      → locked
After kickoff   → locked
```

Predictions cannot be created for finished or cancelled matches.

See `docs/api/predictions.md` for endpoint details, request/response examples, authentication, validation, duplicate behavior, locking rules, and error responses.

## Testing

Run the complete test suite:

```bash
./mvnw clean test
```

Integration tests use PostgreSQL Testcontainers.

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── com/ven/predicktions/
│   └── resources/
│       └── db/migration/
└── test/

docs/
└── api/
    └── predictions.md
```

## Development Workflow

The project uses Jira-based development, feature branches, pull requests, and conventional commits.

Example branch:

```text
feature/PRED-31-prediction-api-documentation
```

Example commit:

```text
docs(PRED-31): document prediction API
```
