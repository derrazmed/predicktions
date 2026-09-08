# Code Quality & Formatting Guidelines

## Java Version

The project targets Java 21 LTS.

Code should remain compatible with the configured Java version.

## Formatting

Java code follows these formatting rules:

- 4 spaces for indentation.
- No tabs.
- Opening braces remain on the same line.
- One statement per line.
- Keep methods reasonably small and focused.
- Use blank lines to separate logical sections.
- Avoid unnecessary comments.
- Prefer clear, readable code over overly compact code.
- Keep lines reasonably short where practical.
- Use UTF-8 source encoding.

Example:

```java
public class UserService {

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
```

## Naming Conventions

### Packages

Use lowercase names:

```text
com.ven.predicktions
```

### Classes

Use `PascalCase`:

```text
UserService
GlobalExceptionHandler
PredictionController
```

### Interfaces

Use `PascalCase`:

```text
SportsProvider
UserService
```
Interfaces should describe a capability or contract.

### Methods

Use `camelCase`:

```text
findById()
calculatePoints()
createPrediction()
```

### Variables

Use `camelCase`:

```text
userId
predictedResult
kickoffAt
```

### Constants

Use `UPPER_SNAKE_CASE`:

```java
private static final int MAX_PREDICTIONS = 10;
```

### Boolean Values

Prefer descriptive names:

```text
isActive
hasPrediction
canPredict
```

### Imports

- Remove unused imports.
- Do not use wildcard imports.
- Keep imports organized consistently.
- Let the IDE manage imports automatically where possible.
- Do not introduce unnecessary dependencies.

Example:

```java
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
```