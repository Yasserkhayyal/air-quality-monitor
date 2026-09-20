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

The presentation layer uses Jetpack Compose and follows an MVI-style
unidirectional data flow between Composables and ViewModels.

Each feature should define:

- UI State: the complete immutable state required to render the screen.
- User Intent/Event: an explicit representation of actions initiated by
  the user or UI.
- ViewModel: receives intents/events, executes the required application
  logic, and produces new UI state.
- Side Effects: one-time effects such as navigation, transient messages,
  or permission requests should be modeled separately from persistent
  UI state when necessary.

The expected interaction flow is:

User Action
→ UI Intent/Event
→ ViewModel
→ Domain logic / Use Case
→ new UI State
→ Composable renders the state

Composables must not directly invoke domain or data-layer operations.

UI state should be immutable and exposed from the ViewModel through
StateFlow.

The architecture should avoid multiple independent mutable state holders
for the same screen state.

MVI should be implemented as an architectural pattern, not through a
third-party MVI framework unless a concrete requirement justifies one.

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

## 3. Module Structure

The application uses a small multi-module Gradle structure to separate
user-facing features from shared domain contracts and data infrastructure.

### Modules

- `:app`
    - Application entry point
    - MainActivity
    - Application-level dependency injection composition
    - Navigation orchestration
    - Assembles the feature and core modules

- `:feature:dashboard`
    - Dashboard screen and presentation logic
    - Dashboard ViewModel
    - Dashboard UI state and intents
    - Dashboard-specific Compose components

- `:feature:search`
    - Search screen and presentation logic
    - Search ViewModel
    - Search UI state and intents
    - Search-specific Compose components

- `:feature:favorites`
    - Favorites screen and presentation logic
    - Favorites ViewModel
    - Favorites UI state and intents
    - Favorites-specific Compose components

- `:core:domain`
    - Domain models
    - Repository interfaces
    - Domain-level business rules
    - Use cases where meaningful

- `:core:data`
    - Repository implementations
    - Remote and local data sources
    - Retrofit APIs and DTOs
    - Room entities and DAOs
    - Data-layer mappers
    - Cache and freshness coordination

### Module Dependency Rules

Feature modules depend on `:core:domain` and must not depend on
`:core:data` or on other feature modules.

`:core:data` depends on `:core:domain` and provides the concrete
implementations of domain repository interfaces.

`:app` is the composition root. It depends directly on the feature
modules and on `:core:data` to assemble the application and configure
dependency injection.

`:app` should not contain business logic or bypass domain abstractions
simply because it has access to the data module.

## 4. Domain Concepts

The primary domain concepts are:

### AirQuality

Represents the application's understanding of air quality at a
specific location and time.

It may contain:

- AQI
- air-quality category
- pollutant measurements
- timestamp
- location

### Location

Represents a geographic location selected or detected by the user.

It contains the information required to identify and query that
location, including coordinates and a human-readable name.

### FavoriteLocation

Represents a location saved by the user for quick future access.

The domain layer must not depend on API DTOs, Room entities, or Android
framework classes.

## 5. Data Flow

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

## 6. Repository Boundaries

The domain layer defines repository interfaces for application-level
data access.

Expected repository responsibilities include:

### AirQualityRepository

Responsible for:

- retrieving current air quality for a location
- retrieving historical hourly air-quality data
- providing locally cached air-quality data when necessary

### LocationSearchRepository

Responsible for:

- searching for locations from user-provided text

### FavoritesRepository

Responsible for:

- saving favorite locations
- retrieving favorite locations
- removing favorite locations

---

## 7. Networking

The application uses REST-based external APIs.

The networking stack consists of:

- Retrofit
- OkHttp
- Kotlin Coroutines

The primary external data source is the Open-Meteo Air Quality API.

A geocoding API/service is used to resolve user-entered place names
into geographic coordinates.

Network DTOs must remain separate from domain models.

The UI and domain layers must not depend directly on Retrofit DTOs or
HTTP-specific representations.

---

## 8. Local Persistence

Room is used for:

- cached air-quality measurements
- favorite locations
- local state required by the product

Historical hourly measurements are stored with their observation
timestamp so that time-range queries can retrieve the last 24 hours.

The local schema should support querying measurements by location and
time range efficiently.

Room entities remain separate from domain models.

---

## 9. Offline Strategy

The repository coordinates remote and local data sources.

### Freshness

Cached air-quality data is considered fresh for up to 30 minutes after
retrieval.

### Online

When network access is available:

1. Request current data from the remote API.
2. Persist relevant data locally.
3. Return the resulting domain model.

### Offline

When network access is unavailable:

1. Attempt to read cached data from Room.
2. If cached data exists, return it together with freshness metadata.
3. If the cached data is older than 30 minutes, the application must
   expose it as stale.
4. If no cached data exists, expose an appropriate offline/error state.

---

## 10. Dependency Injection

Hilt is used for dependency injection.

Dependencies should be provided through constructor injection whenever
practical.

Avoid service locators and unnecessary global state.

---

## 11. UI State

UI state should be explicit and observable.

ViewModels expose state using Kotlin Flow/StateFlow.

Composables render state and emit user actions.

Business logic should remain outside Composables.

---

## 12. Location

Location access is an Android platform concern.

The application should isolate Android location APIs behind an
application-level abstraction.

The domain layer must not depend directly on Android location framework
classes.

The presentation layer handles permission-related user interaction,
while the data/platform layer provides the actual device location.

If permission is denied, the application remains usable through the
location-search flow.

The geocoding implementation is an external data-source concern and
must not be exposed directly to the presentation layer.

---

## 13. Model Mapping

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

## 14. Testing

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

## 15. Architectural Principles

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

## Architectural Decision Scope

Product requirements define observable product behavior.

Architecture defines system boundaries, responsibilities, and dependency direction.

Concrete implementation details such as exact domain model fields,
repository method signatures, API provider selection, UI composition,
and internal caching mechanisms are decided during implementation
and must remain consistent with the documented product and architecture.