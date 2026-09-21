# Air Quality Monitor — Development Plan

## 1. Purpose

This document defines the complete implementation roadmap for Version 1 of
Air Quality Monitor.

It translates the product requirements and architectural decisions into an
incremental development sequence that can be executed phase by phase.

The following documents remain the sources of truth for their respective
concerns:

- `docs/product-spec.md` — product requirements and observable product behavior
- `docs/architecture.md` — architecture, module boundaries, responsibilities,
  and dependency direction
- `.github/copilot-instructions.md` — coding and collaboration constraints for Copilot
- `docs/development-plan.md` — implementation order, phase scope, and completion criteria

This document does not redefine product requirements or architectural rules.
It defines what should be built, in what order, and what indicates that a
phase is complete.

---

## 2. Development Principles

### 2.1 Incremental Vertical Slices

Build the application through small, working vertical slices rather than
building all layers independently before validating the first user flow.

Each phase should leave the application in a buildable and testable state.

### 2.2 Real Boundaries First

The first meaningful product slice must use the real Open-Meteo API boundary.

Fakes and test doubles are allowed for automated tests, but the application
should not be designed around fake infrastructure and postpone the real data
boundary until later.

### 2.3 Minimal Abstraction

Introduce abstractions when they provide a concrete benefit in dependency
inversion, testability, reuse, or separation of responsibility.

Do not create abstractions, wrappers, modules, or frameworks merely because
they are common in other applications.

### 2.4 Preserve Module and Layer Boundaries

All implementation must respect the architecture defined in
`docs/architecture.md`.

In particular:

- Feature modules must not depend on other feature modules.
- Feature modules must not depend directly on `:core:data`.
- Feature modules consume domain contracts from `:core:domain`.
- `:core:data` implements domain repository interfaces.
- `:app` acts as the application composition root.
- Infrastructure details must not leak into Domain or Presentation.

### 2.5 Do Not Solve Future Problems Prematurely

Only implement what is required by the current phase unless a later
requirement creates a genuine dependency on an earlier decision.

Do not pre-build speculative caches, generic UI systems, extra modules,
unused abstractions, or integrations that are not required by the current
product scope.

### 2.6 Inspect the Real External Contract Before Modeling It

Before creating DTOs or persistence structures around an external service,
inspect the actual response shape and fields needed by the product.

Do not design the domain or database around assumptions about an API.

### 2.7 Behavioral Testing

Prioritize tests that verify meaningful behavior and architectural boundaries.
Avoid testing implementation details when the same behavior can be validated
through a stable public contract.

### 2.8 Build Frequently

Run the relevant Gradle build and tests after meaningful changes rather than
waiting until the end of a phase.

### 2.9 Design Before UI Implementation

Before implementing a significant new screen or user flow, establish its
visual and interaction direction first.

Use the product specification as the source of truth for user-visible
requirements. The UI design should translate those requirements into a clear,
consumer-friendly interface without inventing new product behavior.

Use Material 3 principles where appropriate and iterate through Compose
Preview and the emulator to validate visual hierarchy, spacing, states,
and interaction behavior.

---

# 3. Phase 0 — Project and Multi-Module Setup

**Status:** Completed

## Goal

Establish the intended multi-module Gradle structure and verify that the
application still builds and launches before implementing product behavior.

## Modules

- `:app`
- `:core:domain`
- `:core:data`
- `:feature:dashboard`
- `:feature:search`
- `:feature:favorites`

## Intended Dependency Direction

```text
Feature modules → :core:domain
:core:data     → :core:domain
:app           → feature modules
:app           → :core:data
```

The important architectural relationship remains:

```text
Feature → Domain ← Data
```

`:app` is the composition root and is responsible for assembling the
application, including application-level dependency injection configuration.

## Tasks

1. Create the required Gradle modules.
2. Keep `:app` as the Android application entry point.
3. Configure module dependencies according to `docs/architecture.md`.
4. Establish package structure within each module.
5. Configure Hilt for the multi-module application.
6. Ensure the concrete data implementations can be bound to domain repository
   interfaces through the application dependency graph.
7. Preserve the existing Android project configuration unless a change is
   required for modularization.
8. Avoid unrelated cleanup or refactoring.
9. Build the project.
10. Launch the application and verify the app shell still works.

## Definition of Done

- All required modules exist.
- Feature modules do not depend on one another.
- Feature modules do not depend directly on `:core:data`.
- `:core:data` depends on `:core:domain`.
- `:app` can assemble the feature and data modules.
- Hilt configuration compiles.
- The application builds successfully.
- The application launches successfully.

## Tests / Verification

- Gradle sync succeeds.
- Debug build succeeds.
- Existing tests, if any, still pass.
- Module dependency graph matches the architecture.

---

# 4. Phase 1 — UI/UX Design and Visual Exploration

**Status:** Not Started

## Goal

Establish an appealing, coherent Version 1 visual direction for Air Quality
Monitor before substantial UI implementation begins.

This phase is a collaborative design exercise between the product
requirements, the developer, and Copilot. The goal is to decide what the
screens should look like and how the important user states should be presented
without introducing a separate design-tool dependency.

## Design Direction

The initial visual direction should be:

- Clean and modern
- Calm and trustworthy
- Strong visual hierarchy around current air quality
- Easy to scan on a phone
- Accessible and readable
- Polished enough to demonstrate production-oriented UI design

Use Material 3 principles where appropriate while allowing a distinctive
product identity through the chosen color system, typography, spacing, shapes,
and component treatment.

## Primary Screens and Flows

Design at minimum:

- Dashboard
- Location Search
- Favorites
- Historical Air Quality view

Define the main navigation and selection flows required to move between these
screens without introducing unnecessary navigation complexity.

## Required UI States

Design the important product states, not only the successful content state:

- Loading
- Current data loaded
- Cached data
- Stale cached data
- Network/API error
- No cached data
- Location permission request
- Permission denied
- Location unavailable
- Search results
- Empty search results
- Favorites empty state
- Historical data unavailable
- Missing individual pollutant values

Each state should have a clear hierarchy and understandable user action where
an action is applicable.

## Design System

Establish a small, coherent UI language covering at least:

- Color roles
- Typography
- Spacing
- Shapes/corner treatment
- Elevation where useful
- Icons
- Buttons
- Search/input fields
- Cards
- Chips/badges
- List items
- AQI status presentation
- Pollutant presentation
- Loading, empty, and error components

Create reusable Compose components only where there is a meaningful shared UI
concept or concrete reuse case.

Do not create a large generic component library for hypothetical future use.

## Collaborative UI Design with Copilot

Use Copilot as a design collaborator before substantial Compose implementation.

For each significant screen or flow:

1. Read the relevant product requirements.
2. Propose the screen structure and visual hierarchy.
3. Identify the important interaction and state variations.
4. Identify meaningful reusable Compose components.
5. Review the proposal before implementing substantial UI code.
6. Implement the agreed direction in Compose.
7. Use Compose Preview and the emulator to evaluate the result visually.
8. Iterate based on concrete visual feedback.

Copilot may propose alternative visual directions, but it must not invent new
product functionality to justify those designs.

## Design-to-Code Rules

When implementing a UI screen or component:

1. Read the relevant product requirement.
2. Read the relevant architecture and Copilot constraints.
3. Use the reviewed UI direction from this phase as the implementation reference.
4. Preserve the intended hierarchy, spacing, typography, visual states, and
   interaction intent.
5. Translate the design into idiomatic Jetpack Compose.
6. Do not copy design-tool implementation details into Android code.
7. Do not introduce business logic into Composables to support visual behavior.
8. Do not change product behavior merely to simplify the visual implementation.
9. Surface any conflict between the product requirements and the proposed UI
   before implementing the conflicting behavior.

## Definition of Done

- The primary Version 1 screens have an agreed visual direction.
- Main navigation and user flows are understood.
- Important success, loading, empty, error, offline, stale, and permission
  states are considered.
- Typography, spacing, colors, shapes, and major component treatment have an
  initial coherent direction.
- The design is specific enough to guide Compose implementation.
- The first screen can be implemented without requiring another broad design
  review.

## Tests / Verification

- Review the screen structure against the product specification.
- Review state coverage against the documented product states.
- Validate visual hierarchy and readability in Compose Preview or the emulator
  once the first UI implementation exists.

---

# 5. Phase 2 — Current Air Quality Vertical Slice

**Status:** Not Started

## Goal

Implement the smallest useful end-to-end product flow for displaying current
air quality for a selected location using the real Open-Meteo API.

The target flow is:

```text
Selected Location
      ↓
Dashboard Intent
      ↓
DashboardViewModel
      ↓
AirQualityRepository
      ↓
AirQualityRepositoryImpl
      ↓
Retrofit / Open-Meteo
      ↓
DTO
      ↓
Mapper
      ↓
Domain Model
      ↓
UI State
      ↓
Compose UI
```

The first slice should focus on remote retrieval and presentation.
Do not build the complete offline system yet unless a concrete architectural
dependency makes cache support necessary earlier.

## Modules

- `:core:domain`
- `:core:data`
- `:feature:dashboard`
- `:app`

## Domain Tasks

1. Define `Location` with only the fields needed by the current flow.
2. Define the minimum pollutant representation required by the product.
3. Define `AirQualitySnapshot`.
4. Define the current-air-quality repository contract.
5. Add domain business rules only where they are genuinely required.
6. Keep cache freshness logic out of the Domain layer.

## Data Tasks

7. Inspect the actual Open-Meteo current-air-quality response.
8. Identify the exact API fields required by the product.
9. Define the Retrofit API contract.
10. Define the required DTOs.
11. Implement DTO → Domain mapping.
12. Implement `AirQualityRepositoryImpl`.
13. Configure Hilt bindings.
14. Translate external failures into app-level outcomes without leaking
    Retrofit-specific exceptions upward.

## Presentation Tasks

15. Define `DashboardUiState`.
16. Define dashboard intents/events.
17. Implement `DashboardViewModel`.
18. Implement the initial dashboard screen.
19. Render the current:
    - AQI
    - category
    - available pollutant values
    - timestamp
20. Handle loading and basic error states.
21. Keep business logic out of Composables.

## Testing Tasks

22. Add DTO-to-domain mapper tests.
23. Add focused repository tests where meaningful.
24. Add ViewModel tests for successful loading and failure behavior.
25. Avoid live-network dependencies in automated logic tests.

## Definition of Done

A selected location can retrieve real current air-quality data through the
real Open-Meteo boundary and render it in the dashboard.

The UI does not know about Retrofit DTOs, HTTP response objects, or Room
entities.

The first real product flow works end-to-end.

---

# 6. Phase 3 — Local Cache and Offline Behavior

**Status:** Not Started

## Goal

Add local caching and implement the product-defined fresh/stale behavior
while keeping remote/local coordination and freshness decisions in the Data
layer.

## Modules

- `:core:domain`
- `:core:data`
- `:feature:dashboard`

## Data Modeling Rules

Keep these concepts separate:

- **Freshness** — whether cached data is fresh or stale
- **Data origin** — whether displayed data came from remote or cache
- **Error** — whether an operation failed or data is unavailable

Do not collapse all three into a single generic status.

A valid result may, for example, be cached and still fresh.

## Tasks

1. Inspect the current domain and API models from Phase 2.
2. Define the minimum Room representation required for current air-quality
   caching.
3. Add the Room database and DAO.
4. Persist successful remote responses.
5. Retrieve the latest cached data for a location.
6. Implement freshness evaluation in `:core:data`.
7. Apply the product-defined 30-minute freshness policy.
8. Support these repository scenarios:
    - remote success
    - remote failure + usable cache
    - remote failure + no cache
    - offline + usable cache
    - offline + no cache
9. Expose freshness and data-origin semantics without exposing persistence
   details to the UI.
10. Update the dashboard UI to identify cached/stale data clearly.
11. Keep cache decisions out of ViewModels and Composables.

## Definition of Done

The repository behaves predictably for online, offline, remote-failure, and
cache-fallback scenarios.

Fresh cached data is distinguishable from stale cached data.

The dashboard clearly communicates cached/stale information to the user.

## Testing Tasks

Add focused tests for:

- freshness calculation
- remote success
- remote failure with usable cache
- remote failure without cache
- offline with cache
- offline without cache
- cache persistence and retrieval

---

# 7. Phase 4 — Location Search

**Status:** Not Started

## Goal

Allow users to search for another city or location and make the selected
location the active dashboard location without requiring device-location
permission.

## Modules

- `:core:domain`
- `:core:data`
- `:feature:search`
- `:feature:dashboard`
- `:app`

## Tasks

1. Confirm the selected geocoding service and inspect its actual response
   contract.
2. Define the location-search domain contract using app-owned models.
3. Implement the geocoding Retrofit API.
4. Define only the DTO fields required by the product.
5. Implement DTO → Domain mapping.
6. Implement `LocationSearchRepository`.
7. Configure Hilt binding.
8. Define search intents/events.
9. Define `SearchUiState`.
10. Implement `SearchViewModel`.
11. Implement `SearchScreen`.
12. Handle:
    - loading
    - successful results
    - empty results
    - search failure
13. Allow the user to select a search result.
14. Make the selected `Location` available to the dashboard flow.
15. Do not create a direct `:feature:search` → `:feature:dashboard`
    dependency.
16. Coordinate cross-feature navigation through the application composition
    layer.
17. Preserve the ability to use search when location permission is denied.

## Definition of Done

The user can:

```text
Search
  ↓
See location results
  ↓
Select a location
  ↓
Open the dashboard for that location
  ↓
Retrieve current air quality
```

Location permission is not required for the search flow.

## Testing Tasks

Test:

- successful search
- empty results
- search failure
- DTO mapping
- result selection
- selected-location dashboard loading

---

# 8. Phase 5 — Favorites

**Status:** Not Started

## Goal

Allow users to save, inspect, select, and remove favorite locations locally.

Favorites require no authentication.

## Modules

- `:core:domain`
- `:core:data`
- `:feature:favorites`
- `:app`

## Tasks

1. Define `FavoriteLocation` if it is not already established by the
   existing domain model.
2. Define `FavoriteLocationRepository`.
3. Add the required Room entity.
4. Add the required DAO.
5. Implement favorite persistence operations.
6. Expose favorites as an observable local collection where appropriate.
7. Configure Hilt binding.
8. Define favorites intents/events.
9. Define `FavoritesUiState`.
10. Implement `FavoritesViewModel`.
11. Implement `FavoritesScreen`.
12. Add favorite/remove interactions.
13. Allow selection of a favorite location.
14. Route the selected location through application-level navigation rather
    than feature-to-feature module dependencies.
15. Integrate favorite state into relevant dashboard interactions where
    required by the product.

## Definition of Done

The user can:

```text
Add location to favorites
      ↓
View favorites
      ↓
Select favorite
      ↓
Open dashboard for that location
      ↓
Remove favorite
```

Favorites persist across application restarts.

## Testing Tasks

Test:

- adding a favorite
- removing a favorite
- observing favorites
- persistence across recreation/restart where practical
- selecting a favorite
- ViewModel state transitions

---

# 9. Phase 6 — Historical Air Quality

**Status:** Not Started

## Goal

Display the last 24 hours of hourly air-quality measurements when the data
source provides sufficient historical data.

The product requirement should not be prematurely coupled to a specific
visualization such as a chart.

## Modules

Primary implementation work occurs in:

- `:core:domain`
- `:core:data`

Add or use the appropriate existing feature presentation area as determined
by the implemented navigation and screen structure.

Do not create another Gradle feature module unless a concrete architectural
reason appears.

## Tasks

1. Inspect the actual historical Open-Meteo API response and supported range.
2. Confirm the hourly fields available from the data source.
3. Define the minimum historical domain representation.
4. Prefer `List<HistoricalAirQualityPoint>` unless additional metadata
   genuinely requires a wrapper object.
5. Implement the historical Retrofit API contract.
6. Define only the DTO fields required by the product.
7. Implement DTO → Domain mapping.
8. Add historical repository access.
9. Determine whether historical caching provides enough value to justify
   persistence in Version 1.
10. Do not introduce historical persistence solely because Room already exists.
11. Define the required Presentation state.
12. Implement the simplest clear UI for the hourly data.
13. Handle unavailable pollutant measurements gracefully.
14. Handle unavailable historical data clearly.
15. Keep API-specific response structures inside `:core:data`.

## Definition of Done

The application can retrieve and display supported hourly historical
measurements for the previous 24 hours when available.

The UI remains independent of API-specific response structures.

## Testing Tasks

Test:

- historical DTO mapping
- repository behavior
- unavailable metrics
- unavailable historical data
- ViewModel state
- important UI rendering behavior

---

# 10. Phase 7 — Location Permission, Error States, and UX Completion

**Status:** Not Started

## Goal

Complete the current-location experience and the major product-defined
loading, error, empty, permission, and retry behaviors.

## Modules

- `:app`
- relevant feature modules
- `:core:domain`
- `:core:data` where needed for platform/data coordination

## Permission Flow

The intended flow is:

```text
ViewModel
    ↓
RequestLocationPermission side effect
    ↓
Composable / Activity
    ↓
Android permission API
    ↓
Permission result
    ↓
ViewModel intent
    ↓
Updated UI state
```

The ViewModel must never directly invoke Android permission APIs.

## Tasks

1. Isolate Android location access at the platform boundary.
2. Implement the location permission request flow.
3. Expose a one-time permission-request side effect from the relevant
   presentation logic.
4. Have the Composable/Activity perform the actual Android permission call.
5. Return the result to the ViewModel through an intent.
6. Ensure search remains usable when permission is denied.
7. Handle unavailable device location.

## User-visible State Tasks

8. Review dashboard loading state.
9. Review network failure state.
10. Review API failure state.
11. Review invalid-response behavior.
12. Review stale/cached data indication.
13. Review missing-cache behavior.
14. Review search empty and error states.
15. Review favorites empty state.
16. Add retry intents where meaningful.
17. Keep retry actions as intents rather than side effects.
18. Ensure transient messages and navigation remain one-time effects.

## Navigation / Effects

Use one-time side effects only for operations such as:

- navigation
- permission requests
- transient messages
- other genuinely one-time UI effects

Do not store one-time events as persistent UI state.

## Definition of Done

The principal product flows remain usable under:

- permission denial
- unavailable location
- no network
- API failure
- stale cache
- missing cache
- empty search results

The user receives understandable feedback for these states.

---

# 11. Phase 8 — Final Testing, Architecture Review, and Cleanup

**Status:** Not Started

## Goal

Validate the complete Version 1 application and ensure the final codebase
still respects the product requirements and architectural boundaries.

## Testing Tasks

1. Run the complete unit test suite.
2. Run relevant Compose UI tests.
3. Verify repository/cache behavior.
4. Verify ViewModel state transitions.
5. Verify important navigation and user flows.
6. Verify offline and stale-data behavior.
7. Verify permission-denied fallback behavior.

## Architecture Review Tasks

8. Verify module dependencies.
9. Verify feature modules do not depend on one another.
10. Verify feature modules do not directly depend on `:core:data`.
11. Verify `:core:data` implements domain repository contracts.
12. Verify infrastructure types do not leak into Domain or Presentation.
13. Verify Android platform concerns remain at the appropriate boundary.
14. Verify Hilt configuration and application-level composition.
15. Verify navigation does not create feature-to-feature dependencies.

## Code Review Tasks

16. Remove unused dependencies.
17. Remove unused classes and abstractions.
18. Remove temporary implementation code.
19. Review Compose componentization and reuse.
20. Keep reusable UI components only where there is meaningful reuse.
21. Remove generic abstractions created solely for theoretical reuse.
22. Review naming and package consistency.
23. Avoid unrelated refactoring.

## Final Build

24. Perform a clean build.
25. Run the final test suite.
26. Review the final Git diff.
27. Confirm the repository is ready for final documentation and delivery.

## Definition of Done

- Version 1 product requirements are implemented.
- Tests pass.
- The application builds successfully.
- The application launches successfully.
- Module boundaries are respected.
- No known architecture violations remain.
- No unnecessary abstractions or dependencies remain.
- The repository is ready for final delivery work.

---

# 12. Implementation Order Summary

```text
Phase 0 — UI/UX Design + Visual Exploration
        ↓
Phase 1 — Project + Multi-Module Setup
        ↓
Phase 2 — Current AQI Vertical Slice
        ↓
Phase 3 — Room + Cache + Offline/Stale Behavior
        ↓
Phase 4 — Location Search
        ↓
Phase 5 — Favorites
        ↓
Phase 6 — Historical Air Quality
        ↓
Phase 7 — Permission + Error + UX Completion
        ↓
Phase 8 — Testing + Architecture Review + Cleanup
```

Each phase should leave the project in a buildable state.

Do not start implementation work from a later phase until the current phase
has reached its Definition of Done, unless a genuine dependency requires a
small amount of preparation.

---

# 13. Decision Rules During Implementation

The development plan intentionally does not define every concrete technical
detail in advance.

When an implementation decision is required:

1. Prefer the simplest solution consistent with the existing architecture.
2. Inspect the real external API before defining DTO or persistence details.
3. Keep infrastructure-specific decisions inside `:core:data`.
4. Keep product/domain semantics in `:core:domain` where appropriate.
5. Keep screen-specific presentation behavior inside the owning feature module.
6. Do not create a new Gradle module without a concrete architectural reason.
7. Do not create a new abstraction without a concrete problem it solves.
8. Do not rely on transitive Gradle dependencies to bypass intentional module
   boundaries.
9. Do not introduce unrelated refactoring or dependencies.
10. If a decision materially changes product behavior or the architecture,
    stop and clarify it before implementation.
11. If a decision is a routine implementation detail, choose the simplest
    reasonable approach and record the assumption briefly if useful.

---

# 14. Phase Execution Rule for Copilot

The full development roadmap is documented here from the beginning, but
implementation should proceed one phase at a time.

When working on a phase, Copilot should:

1. Read the relevant source-of-truth documents.
2. Read this development plan.
3. Inspect the current repository state.
4. Implement only the current phase unless a prerequisite from a later phase
   is genuinely required.
5. Run the relevant build and tests.
6. Review the resulting diff.
7. Report what was changed, what was verified, and any decisions that were
   necessary during implementation.

For UI work, Copilot should first use the reviewed UI direction from Phase 0,
then validate the result through Compose Preview and the emulator.

Do not ask for another broad architecture review when the current issue is a
normal implementation detail covered by the existing architecture.

If an implementation issue reveals a genuine contradiction between the
product specification and architecture, stop and surface that contradiction
before proceeding.

---

# 15. Progress Tracking

Update the status of a phase only when its Definition of Done has been met.

Allowed status values:

- `Not Started`
- `In Progress`
- `Completed`
- `Blocked`

Do not mark a phase `Completed` merely because its code has been written;
relevant build and test verification must also be complete.
