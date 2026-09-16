# Architecture

## 1. Architectural Goal

The application should use a simple, testable layered architecture
that separates presentation, domain logic, and data access.

The architecture should remain proportional to the size of the product
and should avoid abstractions that do not provide a concrete benefit.

---

## 2. Layers

The application is organized into three primary layers:

### Presentation

Responsibilities:

- Compose UI
- ViewModels
- UI state
- User interaction handling

The presentation layer should not contain business logic or direct
network/database access.

---

### Domain

Responsibilities:

- Domain models
- Repository interfaces
- Business rules
- Use cases where they provide meaningful separation or reuse

The domain layer should not depend directly on Retrofit, Room, or
Android framework APIs where practical.

---

### Data

Responsibilities:

- Repository implementations
- Remote data sources
- Local data sources
- API DTOs
- Room entities
- Mapping between external/local representations and domain models

---

## 3. Data Flow

The expected data flow is:

Presentation
→ Domain
→ Repository
→ Remote/Local data sources

For example:

ViewModel
→ Use Case
→ Repository
→ Retrofit / Room
→ Domain Model
→ UI State
→ Composable

---

## 4. Networking

The application uses a REST-based air-quality API.

The networking stack is:

- Retrofit
- OkHttp
- Kotlin Coroutines

Network DTOs must remain separate from domain models.

The UI and domain layers must not depend directly on Retrofit DTOs.

---

## 5. Local Persistence

Room is used for:

- Cached air-quality data
- Favorite locations
- Other local state required by the product

The repository is responsible for coordinating remote and local
data sources.

---

## 6. Offline Strategy

When network access is available:

1. Fetch current data from the remote API.
2. Persist relevant data locally.
3. Expose the resulting domain model to the application.

When network access is unavailable:

1. Attempt to read the most recent cached data.
2. Expose cached data when available.
3. Clearly indicate stale/cached data to the presentation layer.

If no cached data exists, expose an appropriate error/empty state.

---

## 7. Dependency Injection

Hilt is used for dependency injection.

Dependencies should be provided through constructor injection whenever
practical.

Avoid service locators and unnecessary global state.

---

## 8. UI State

UI state should be explicit and observable.

ViewModels expose state using Kotlin Flow/StateFlow.

Composables render state and emit user actions.

Business logic should remain outside Composables.

---

## 9. Location

Location access is an Android platform concern.

Android framework APIs should be isolated from the domain layer where
practical.

Location permission handling belongs to the platform/presentation
boundary, while location-related application logic should remain
testable independently of Android framework details.

---

## 10. Model Mapping

External representations must not leak into the domain or presentation
layers.

The expected flow is:

Remote DTO
→ Mapper
→ Domain Model

and:

Room Entity
→ Mapper
→ Domain Model

The application should avoid coupling domain models to API-specific
or database-specific representations.

---

## 11. Testing

Testing should focus on behavior and architectural boundaries.

### Unit Tests

- Business rules
- Repository behavior
- ViewModel state transformations
- Data mapping

### UI Tests

Important user-facing flows should be covered with Compose UI tests.

Tests should not require the real network when testing application
logic.

---

## 12. Architectural Principles

### Separation of Concerns

Each layer should have a clear responsibility.

### Dependency Inversion

Higher-level application logic should depend on abstractions rather
than concrete infrastructure where appropriate.

### Testability

Business behavior should be independently testable.

### Simplicity

Avoid introducing abstractions solely because they are considered
"clean architecture."

### External Boundary Isolation

External APIs, databases, and Android framework APIs should not leak
unnecessarily into the core application logic.