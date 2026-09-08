# Predicktions

Predicktions is a sports prediction platform where users predict the outcomes of upcoming matches, earn points based on their predictions, and compete on a leaderboard.

The platform is designed around a simple prediction workflow:

1. Users authenticate.
2. Available matches are displayed.
3. Users submit predictions before kickoff.
4. Predictions are locked once a match starts.
5. Match results are retrieved from a sports data provider.
6. Users receive points based on the accuracy of their predictions.
7. Users compete on a global leaderboard.

---

## MVP Scope

The initial MVP focuses on the core prediction experience.

### Included

- User authentication.
- Displaying available matches.
- Predicting match outcomes.
- Viewing the user's predictions.
- Automatically calculating prediction points after matches are completed.
- Global leaderboard.

### Prediction Rules

For the initial MVP:

- A prediction can be one of:
    - `HOME`
    - `DRAW`
    - `AWAY`
- Predictions cannot be modified after kickoff.
- A correct outcome awards **3 points**.
- An incorrect outcome awards **0 points**.

The scoring system may evolve in future versions.

---

## Technology Stack

### Backend

- Java 21 LTS
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA / Hibernate
- Bean Validation
- PostgreSQL
- Flyway
- Maven

### Frontend

The frontend will be introduced in a later development phase.

Planned technologies:

- React
- TypeScript
- Vite
- React Router
- TanStack Query
- Tailwind CSS

### Development & Deployment

- Git
- GitHub
- Docker
- Docker Compose

### Testing

- JUnit
- Spring Boot Test
- Spring Security Test
- Testcontainers

Testcontainers will be introduced when database integration testing becomes necessary.

---

## Prerequisites

Before running the project locally, make sure the following are installed:

- Java 21 or a compatible JDK
- Git
- Docker
- Docker Compose

Verify the installations:

```bash
java -version
git --version
docker --version
docker compose version
```

