# Air Quality Monitor - Copilot Instructions

## 1. Project Context

This repository contains the Air Quality Monitor Android application.

Product requirements are defined in `docs/product-spec.md`.
Architecture and module boundaries are defined in `docs/architecture.md`.
Implementation order is defined in `docs/development-plan.md`.

Treat these documents as source of truth for their respective concerns.
Do not invent major product requirements, business rules, or architectural
decisions that are not supported by them.

## 2. Technology Stack

The current architecture uses:

- Kotlin
- Jetpack Compose
- Kotlin Coroutines
- Flow / StateFlow
- Hilt
- Retrofit
- OkHttp
- Room
- Open-Meteo REST API
- Geocoding service for location search
- Android Geocoder API for the current location
- Android location APIs at the platform boundary

Do not add libraries without a concrete benefit. Explain any new dependency
before introducing it when the change is non-trivial.

Don't introduce deprecated code

Always check for required permissions before introducing any code that only functions
properly when these permissions are granted

## Dependency Management

All external library and Gradle plugin dependencies must be declared
and versioned through `gradle/libs.versions.toml`.

Module and project build files must reference dependencies through the
version catalog rather than hard-coding Maven coordinates or versions.

Do not introduce unused dependencies merely to prepare for future phases.

Instead, define the dependency in `gradle/libs.versions.toml` and use
the generated `libs.*` accessor from the build file.

Before adding a dependency:

- Check whether it already exists in the version catalog.
- Reuse an existing catalog entry when appropriate.
- Add a new catalog entry only when the dependency is genuinely required.
- Keep versions centralized and avoid duplicate version definitions.

Do not introduce unused dependencies merely to prepare for future phases.

## 3. Architecture

Follow the Presentation / Domain / Data boundaries defined in
`docs/architecture.md`.

Do not bypass those boundaries.
Prefer the simplest implementation that satisfies the architecture.
Avoid premature abstraction.

### Multi-Module Architecture

Follow the multi-module structure and dependency rules defined in
`docs/architecture.md` and `docs/development-plan.md`.

- `:app` is the application composition root.
- `:app` may depend on feature modules, `:core:data`, and `:core:design-system`
  for application assembly, dependency injection configuration, and shared
  theming.
- Feature modules depend on `:core:domain`.
- Feature modules may depend on `:core:design-system` for shared presentation
  theme and UI components.
- Feature modules must not depend directly on `:core:data`.
- Feature modules must not depend on other feature modules.
- `:core:data` depends on `:core:domain` and implements its repository
  interfaces.
- `:core:design-system` owns shared Material 3 theme, tokens, and genuinely
  reusable UI components; it must not depend on features or data modules.
- Do not use transitive Gradle dependencies to bypass intentional module
  boundaries.
- Do not introduce additional modules without a concrete architectural
  reason.

## 4. Presentation / MVI

Use MVI-style unidirectional data flow as defined by the architecture:

User Action → Intent → ViewModel → Domain → UI State → Composable

Follow these rules:

- UI state is immutable.
- Expose observable UI state through StateFlow where appropriate.
- Intents/events represent user or external input to the ViewModel.
- ViewModels process intents and update UI state.
- One-time effects are handled separately from persistent UI state.
- Composables render state and emit actions.
- Composables must not access repositories, DAOs, Retrofit APIs, or other
  infrastructure directly.
- Do not put business logic in Composables.
- Retry actions are intents, not one-time side effects.
- Android permission APIs must not be invoked directly from ViewModels.

Do not introduce a third-party MVI framework unless there is a concrete
justification that cannot be satisfied with standard Kotlin, coroutines,
Flow, and StateFlow.

## 5. Compose Componentization and Reuse

Prefer focused Composables organized around meaningful UI responsibility.

Extract a reusable Composable when:

- it represents a meaningful shared UI concept
- the same structure or behavior is used in multiple places
- reuse improves consistency, maintainability, or testability

Do not extract components solely to maximize reuse or to make files smaller.
Do not create generic abstractions without a concrete reuse case.

Feature-specific UI should remain within the owning feature module when there
is no clear cross-feature reuse case.

Shared UI components must remain independent of business logic and data access.

## 6. UI Design and Collaboration

Before implementing a significant new screen or user flow, use
`docs/product-spec.md` and the relevant phase in `docs/development-plan.md`
to establish the intended UI behavior and visual direction.

For substantial UI work:

1. Propose the screen structure and visual hierarchy before writing substantial
   Compose code.
2. Identify important UI states and interactions.
3. Identify meaningful reusable components.
4. Review the proposed direction against the product requirements.
5. Implement the agreed design using idiomatic Jetpack Compose.
6. Validate the result with Compose Preview and the emulator.
7. Iterate based on concrete visual feedback.

Prefer Material 3 principles and a clear, accessible visual hierarchy.

Do not invent product functionality merely to make a UI more interesting.
Do not create a large generic design system for hypothetical future use.
Do not make a substantially different visual design when an agreed UI
direction already exists.

The UI design process is a development-time collaboration and must not add
a design-tool runtime dependency to the Android application.

If the proposed UI conflicts with the product specification or architecture,
identify the conflict before implementation rather than silently changing
the product behavior.

## 7. Domain

The Domain layer owns:

- domain models
- repository interfaces
- meaningful business rules
- use cases where they provide meaningful separation

Keep Domain independent from Retrofit, OkHttp, Room, and Android framework
types where architectural isolation requires it.

Do not create interface/implementation pairs for every class or use case
without a concrete reason.

Do not move cache-freshness implementation details into Domain.

## 8. Data

The Data layer owns:

- repository implementations
- remote and local data sources
- Retrofit APIs
- DTOs
- Room entities and DAOs
- Data-layer mappers
- remote/local coordination
- cache/freshness behavior
- infrastructure error handling and translation

Keep infrastructure-specific representations inside Data.

DTO/entity mapping belongs in Data.

Do not expose DTOs, Room entities, Retrofit response types, HTTP exceptions,
or other infrastructure types to Domain or Presentation.

## 9. Networking

Use REST APIs with Retrofit and OkHttp as defined by the architecture.

Open-Meteo is the initial air-quality data source.
A geocoding service provides location search.

Inspect the actual external response before defining DTOs around assumptions.

Handle remote failures explicitly and translate infrastructure-specific
failures into appropriate application/domain outcomes.

Do not silently swallow networking exceptions.

## 10. Persistence and Offline Behavior

Room is the local persistence mechanism defined by the architecture.

Follow the product-defined freshness policy:

- a cached measurement is fresh for up to 30 minutes
- older cached data is stale
- cached/stale data must be distinguishable from fresh remote data

Freshness decisions belong in the Data layer.

Keep these concepts separate:

- freshness
- data origin
- error/unavailability

Do not flatten them into a single generic status unless there is a concrete
reason.

Do not over-design historical persistence before the actual API shape and
product usage justify it.

Do not expose internal data-origin information such as "Cached" as a
user-facing status unless the product requirements provide a concrete
user benefit.

Prefer user-facing language that communicates freshness or that data may
be outdated.

## 11. Location

Keep Android location APIs at the platform boundary.

The ViewModel may express that permission is required through a one-time
side effect, but Android permission APIs must be invoked by the appropriate
Composable/Activity/platform boundary.

Location search must remain usable when device-location permission is denied.

Do not repeatedly request permission without a product-driven reason.

Geocoding implementation details remain isolated inside Data.

## 12. Testing

Use the following testing tools and frameworks consistently:

- **Unit tests:** JUnit 4 with **MockK** for mocking and verification.
- **Android/UI tests:** **AndroidJUnit4** with **ComposeTestRule** for
  instrumented Android and Compose UI testing.
- **Compose screenshot tests:** Use **Compose Preview Screenshot Testing**
  for visual regression testing of meaningful Composable UI states, including
  layout and visual attributes.

Do not introduce alternative mocking or UI-testing frameworks unless there
is a concrete technical reason and the change is explicitly justified.

Test behavior and architectural boundaries rather than implementation details.

Distinguish behavioral UI testing from visual regression testing:

- Use **ComposeTestRule** for UI behavior, semantics, interaction, and
  important user flows.
- Use **Compose Preview Screenshot Testing** for visual presentation and
  regression coverage of meaningful UI states.
- Screenshot tests complement behavioral UI tests; they do not replace them.

When a meaningful user-visible UI state is introduced or changed, add or
update the corresponding screenshot regression test unless the state is not
suitable for deterministic screenshot testing.

When meaningful behavior changes, update or add regression tests.

Prioritize meaningful tests for:

- domain business rules
- mapping
- repository behavior
- cache freshness and fallback behavior
- ViewModel state transitions
- important Compose user flows
- meaningful Compose UI states
- permission-related presentation state and side effects
- visual presentation of important user-facing states

Do not rely on live external APIs for application logic tests.
Use fakes, fixtures, or controlled test doubles where appropriate.

## 13. Code Quality

Prefer idiomatic Kotlin and immutable data.

Keep classes focused and responsibilities clear.

Avoid unnecessary global state and service locators.

Do not duplicate business logic across layers.

Do not perform unrelated cleanup or refactoring while implementing a phase.

Preserve existing conventions unless there is a concrete reason to change them.

Always add JavaDoc/KDoc for every new method and for existing method and classes describing its function

## 14. Working Process

For non-trivial changes, follow:

Plan → Review → Implement → Build/Test → Review

Before modifying code:

1. Read the relevant source-of-truth documents.
2. Read the applicable phase in `docs/development-plan.md`.
3. Inspect the existing repository and relevant files.
4. Identify the smallest set of files required.
5. Explain the approach before making large or architectural changes.

After making changes:

1. Run the relevant Gradle build and tests.
2. Fix failures caused by the changes.
3. Review the Git diff.
4. Check module and layer boundaries.
5. Summarize changed files and important implementation assumptions.

For UI work, validate the implementation in Compose Preview and/or the
emulator and iterate on concrete visual feedback.

Do not implement later development phases prematurely.

## 15. Handling Ambiguity

Do not invent major product behavior.

When ambiguity is encountered:

- identify the ambiguity
- determine whether it is a product, architecture, or implementation issue
- use the simplest reasonable implementation when it is a routine detail
- ask for clarification before making a significant product or architectural
  assumption

Do not restart broad architecture reviews for routine implementation details
that are already covered by the existing documents.

## 16. Change Scope

Keep changes surgical and phase-scoped.

Do not:

- introduce unrelated refactoring
- add speculative abstractions
- add unrelated dependencies
- create new modules without a concrete reason
- change product requirements as a side effect of implementation
- bypass module boundaries as a shortcut
- alter the architecture without explicit justification

If a change reveals a genuine contradiction between the product specification,
architecture, or development plan, surface it before proceeding.

<!-- caveman-begin -->
Respond terse like smart caveman. All technical substance stay. Only fluff die.

Rules:
- Drop: articles (a/an/the), filler (just/really/basically), pleasantries, hedging
- Fragments OK. Short synonyms. Technical terms exact. Code unchanged.
- Pattern: `[thing] [action] [reason]. [next step]`.
- Not: "Sure! I'd be happy to help you with that."
- Yes: "Bug in auth middleware. Fix:"

Switch level: /caveman lite|full|ultra|wenyan-lite|wenyan-full|wenyan-ultra
Stop: "stop caveman" or "normal mode"

Auto-Clarity: drop caveman for security warnings, irreversible actions, user confused. Resume after.

Boundaries: code/commits/PRs written normal.
<!-- caveman-end -->
