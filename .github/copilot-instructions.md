# Air Quality Monitor - Copilot Instructions

## 1. Product Context

This repository contains the Air Quality Monitor Android application.

The product requirements are defined in:

- `docs/product-spec.md`

Treat `docs/product-spec.md` as the source of truth for product
requirements and user-facing behavior.

Do not invent major product features, business rules, or user flows that
are not defined there.

If a requirement is ambiguous or missing, identify the ambiguity before
implementing it and ask for clarification when necessary.

---

## 2. Architecture Context

The application architecture is defined in:

- `docs/architecture.md`

Treat `docs/architecture.md` as the source of truth for architectural
boundaries and technical design decisions.

Preserve the separation between:

- Presentation
- Domain
- Data

Do not introduce architectural changes without a concrete reason.

Avoid unnecessary abstraction and keep the architecture proportional to
the size and complexity of the application.

---

## 3. Technology Stack

The project uses:

- Kotlin
- Jetpack Compose
- Kotlin Coroutines
- Flow / StateFlow
- Hilt
- Retrofit
- OkHttp
- Room

Do not introduce additional libraries unless they provide a clear
benefit and the reason for introducing them is explained.

---

## 4. Dependency Direction

Keep external infrastructure details isolated from core application
logic.

The domain layer should not depend directly on:

- Retrofit
- OkHttp
- Room
- Android framework APIs

where practical.

External representations should not leak into the domain or presentation
layers.

Use mapping between:

- Remote DTOs and domain models
- Room entities and domain models

when appropriate.

---

### Presentation / MVI

The presentation layer must follow the MVI-style unidirectional data
flow defined in `docs/architecture.md`.

Use:

- Immutable UI state
- Explicit UI intents/events
- ViewModel as the state holder and intent processor
- StateFlow for exposing UI state
- Separate handling for one-time side effects when required

Composables should:

- Render the provided UI state.
- Emit explicit user intents/events.
- Avoid direct calls to repositories, DAOs, APIs, or other data sources.
- Avoid owning business logic.

Do not mix multiple competing state-management approaches within the
same feature without a concrete reason.

---

## 6. Domain

The domain layer should contain application-level concepts and rules.

Prefer domain models that represent the application's needs rather than
the structure of external APIs or databases.

Repository interfaces belong to the domain boundary when they represent
application-level data access.

Introduce use cases when they provide meaningful separation, reuse, or
testability.

Do not create a use-case class for trivial one-line operations without a
real architectural benefit.

---

## 7. Data

The data layer owns:

- Retrofit API interfaces
- Network DTOs
- Room entities
- DAOs
- Repository implementations
- Data-source coordination
- Mapping between external/local models and domain models

Do not expose Retrofit DTOs or Room entities directly to the UI layer.

---

## 8. Networking

The application uses REST APIs.

The primary air-quality source is Open-Meteo.

A geocoding service is used for location search.

Handle network failures explicitly.

Do not assume that an API request always succeeds.

Do not silently swallow exceptions.

Preserve enough error information for the presentation layer to expose
an understandable user-facing state.

---

## 9. Local Persistence and Offline Behavior

Room is used for local persistence and caching.

The product defines a 30-minute freshness threshold for cached
air-quality data.

Preserve this product rule.

Do not invent a different cache freshness policy without explicitly
identifying the conflict.

The repository is responsible for coordinating remote and local data
sources.

Cached or stale data must be distinguishable from fresh data.

---

## 10. Location

Location is an Android platform concern.

Keep Android location APIs isolated from domain logic where practical.

Location permission is optional for the core application experience.

If permission is denied, the application must remain usable through
location search.

Do not introduce unnecessary repeated permission requests.

---

## 11. Testing

Test behavior rather than implementation details.

Add unit tests for meaningful:

- business rules
- mapping logic
- repository behavior
- ViewModel state transformations

Use Compose UI tests for important user-facing flows.

Avoid tests that depend on the real external network when testing
application logic.

When changing behavior, update or add tests as appropriate.

---

## 12. Code Quality

Prefer idiomatic Kotlin.

Prefer immutable data.

Keep classes small and focused.

Avoid duplicated business logic.

Avoid premature abstraction.

Avoid speculative features.

Prefer the simplest implementation that satisfies the documented
requirements.

Do not modify unrelated files while implementing a task.

---

## 13. Working Process

Before making changes:

1. Inspect the existing implementation.
2. Read the relevant product and architecture documentation.
3. Identify the files that actually need to change.
4. Preserve existing conventions unless there is a concrete reason to
   change them.

For non-trivial tasks, explain the intended approach before making
large changes.

After making changes:

1. Run the relevant Gradle build or tests.
2. Fix compilation and test failures caused by the changes.
3. Review the final diff.
4. Summarize the files changed and important architectural decisions.

Do not make unrelated cleanup changes unless explicitly requested.

---

## 14. Product and Architecture Authority

When a technical implementation conflicts with the product requirements,
identify the conflict rather than silently changing the product.

When a requested implementation conflicts with the documented
architecture, identify the conflict and explain the trade-off before
making a significant architectural change.

Do not invent missing product requirements to complete a task.