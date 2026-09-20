# Air Quality Monitor - Copilot Instructions

## 1. Project Context

This repository contains the Air Quality Monitor Android application.

Product requirements are defined in:

`docs/product-spec.md`

Architecture and technical design are defined in:

`docs/architecture.md`

Treat these documents as the source of truth.

Do not invent major product requirements, business rules, user flows,
or architectural decisions that are not defined in those documents.

When a requirement is ambiguous or missing, identify the ambiguity
before implementation rather than silently inventing a solution.

---

## 2. Technology Stack

The project uses:

- Kotlin
- Jetpack Compose
- Kotlin Coroutines
- Flow / StateFlow
- Hilt
- Retrofit
- OkHttp
- Room

Do not introduce additional libraries unless there is a concrete
technical benefit.

When proposing a new dependency, explain why it is necessary and what
problem it solves.

---

## 3. Architecture

Follow the layered architecture defined in `docs/architecture.md`:

- Presentation
- Domain
- Data

Respect the dependency direction between these layers.

Do not bypass architectural boundaries for convenience.

Prefer the simplest implementation that satisfies the documented
requirements.

Avoid premature abstraction and architecture that is disproportionate
to the size of the application.

---

## 4. Presentation / MVI

The presentation layer must follow the MVI-style unidirectional data
flow defined in `docs/architecture.md`.

Use:

- Immutable UI state
- Explicit UI intents/events
- ViewModels as state holders and intent processors
- StateFlow for exposing persistent UI state
- Separate handling for one-time side effects when required

The expected flow is:

User Action
→ Intent/Event
→ ViewModel
→ Domain logic / Use Case
→ UI State
→ Composable

Composables should:

- Render the provided state.
- Emit explicit intents/events.
- Avoid direct access to repositories, DAOs, APIs, or other
  infrastructure.
- Avoid business logic.

Do not mix competing state-management approaches within the same
feature without a concrete reason.

MVI should be implemented using standard Kotlin/Android mechanisms
unless a third-party MVI framework provides a concrete, documented
benefit.

---

## 5. Compose Componentization and Reuse

Prefer small, focused Composables with clear responsibilities.

Extract reusable Composables when:

- they represent a meaningful shared UI concept;
- the same UI structure or behavior is used in multiple places;
- reuse improves consistency, maintainability, or testability.

Do not create generic UI abstractions solely for the sake of reuse.

Keep feature-specific UI scoped to its feature when there is no clear
benefit to sharing it.

Preserve state hoisting and keep business logic outside Composables.

---

## 6. Domain

The domain layer represents application concepts and business rules.

Prefer domain models that represent application needs rather than
external API or database structures.

Repository interfaces belong at the domain boundary when they represent
application-level data access.

Use cases should be introduced when they provide meaningful separation,
reuse, or testability.

Do not automatically create an interface and implementation pair for
every use case or class.

The domain layer must not depend directly on Retrofit, OkHttp, Room, or
Android framework APIs where practical.

---

## 7. Data

The data layer owns:

- Repository implementations
- Remote data sources
- Local data sources
- Retrofit APIs
- Network DTOs
- Room entities
- DAOs
- Mappers
- Cache coordination

Keep infrastructure-specific representations inside the data layer.

Do not expose Retrofit DTOs or Room entities directly to the domain or
presentation layers.

DTO-to-domain and entity-to-domain mapping belongs in the data layer.

---

## 8. Networking

The application uses REST APIs through Retrofit and OkHttp.

Open-Meteo is the primary air-quality data source.

A geocoding service is used for location search.

Treat external API contracts as external boundaries.

Do not allow API-specific field names, response types, or Retrofit
annotations to leak into domain models unnecessarily.

Handle network failures explicitly.

Do not silently swallow exceptions.

---

## 9. Persistence and Offline Behavior

Room is used for local persistence and caching.

Follow the product's documented cache freshness policy.

Do not invent or silently change cache semantics.

Repository implementations are responsible for coordinating remote and
local data sources.

Cached and stale data must remain distinguishable from fresh data.

Persistence-specific models must remain separate from domain models.

---

## 10. Location

Location is an Android platform concern.

Keep Android location APIs isolated from the domain layer where
practical.

Location permission handling belongs at the appropriate
presentation/platform boundary.

The application must remain usable through location search when
location permission is denied.

Avoid unnecessary repeated permission requests.

---

## 11. Testing

Test behavior and architectural boundaries rather than implementation
details.

Add or update tests when changing meaningful behavior.

Prioritize tests for:

- Business rules
- Data mapping
- Repository behavior
- Cache freshness behavior
- ViewModel state transformations
- Important Compose user flows

Do not depend on the real external network for application-logic tests.

When fixing a bug, add a regression test when practical.

---

## 12. Code Quality

Prefer idiomatic Kotlin.

Prefer immutable data.

Keep classes focused and reasonably small.

Avoid duplicated business logic.

Avoid unnecessary global state and service locators.

Do not introduce abstractions without a concrete benefit.

Do not perform unrelated cleanup while implementing a task.

Follow existing project conventions unless there is a concrete reason to
change them.

---

## 13. Working Process

Before making non-trivial changes:

1. Inspect the existing implementation.
2. Read the relevant sections of `docs/product-spec.md`.
3. Read the relevant sections of `docs/architecture.md`.
4. Identify the files that actually need to change.
5. Explain the intended approach before making large or architectural
   changes.

For non-trivial tasks, prefer:

Plan
→ Review
→ Implement
→ Build/Test
→ Review

Do not implement future features that are outside the current task.

After making changes:

1. Run the relevant Gradle build and tests.
2. Fix failures caused by the changes.
3. Review the resulting diff.
4. Summarize changed files.
5. Summarize important architectural decisions or assumptions.

---

## 14. Handling Ambiguity

When the product specification and architecture are incomplete or
ambiguous:

- Do not invent major product behavior.
- Identify the ambiguity explicitly.
- Distinguish product decisions from implementation decisions.
- Explain relevant trade-offs.
- Ask for clarification before making a significant assumption.

If an implementation detail can safely be chosen without affecting
product behavior, choose the simplest reasonable option and state the
assumption.

---

## 15. Change Scope

Keep changes surgical.

Modify only the files necessary to accomplish the requested task.

Do not:

- refactor unrelated code;
- introduce speculative abstractions;
- add unrelated dependencies;
- change documented architecture without justification;
- modify product requirements as a side effect of implementation.

When a requested change conflicts with the documented product or
architecture, identify the conflict before implementing it.