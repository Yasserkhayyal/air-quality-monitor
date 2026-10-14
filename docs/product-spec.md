# Air Quality Monitor

## 1. Product Overview

Air Quality Monitor is a consumer Android application that helps users
understand the current and historical air quality for their current
location or a location they select.

The product presents air-quality information in a simple and
understandable way without requiring the user to have technical
knowledge about environmental measurements.

---

## 2. Target User

The target user is a general smartphone user who wants to quickly
understand the air quality around them or in another location.

The application should prioritize clarity and ease of understanding
over technical complexity.

---

## 3. Primary User Problems

The application should help users answer:

1. What is the current air quality where I am?
2. Is the current air quality good, moderate, or unhealthy?
3. Which pollutants are contributing to the current air quality?
4. How has the air quality changed over time?
5. What is the air quality in another city?
6. Which locations do I want to monitor regularly?

---

## 4. Core Features

### 4.1 Current Location

The application can determine the user's current location and retrieve
air-quality information for that location.

Location permission must only be requested when necessary.

Location permission is optional for using the application.

If the user denies location permission, the application must remain
fully usable through location/city search.

The user may explicitly choose to use their current location later.
The most recently active location, whether selected through search or
current-location retrieval, is restored on later launches without requesting
location permission. A restored current-location selection may be outdated;
the user can explicitly refresh it by choosing to use the current location.

---

### 4.2 Current Air Quality

The dashboard displays:

- Current AQI
- Air-quality category
- Major pollutant measurements
- Data timestamp

The presentation should prioritize understandable information.

The application uses the AQI metric and category definitions provided
by the selected air-quality data source.

The AQI representation and category interpretation must remain
consistent throughout the application.

---

### 4.3 Pollutant Details

The application displays the following supported pollutants:

- PM2.5
- PM10
- NO2
- O3
- CO

If a supported pollutant is unavailable in a particular API response,
the UI should handle the missing value gracefully rather than displaying
an incorrect value.

---

### 4.4 Location Search

Users can search for another location or city.

The application uses a geocoding service to translate a user-entered
place name into a geographic location.

Search results should provide enough information for the user to
distinguish between locations with similar names.

After selecting a location, the application retrieves its air-quality
data using the selected location's coordinates.

---

### 4.5 Favorites

Users can save locations as favorites.

Users can:

- View favorite locations
- Open a favorite location
- Remove a favorite location
- Add the currently active Dashboard location to favorites

Favorites do not require authentication and are stored locally.

Favorite identity is the exact latitude/longitude pair. Adding a location
whose coordinates are already saved replaces its address/details without
creating a duplicate or changing its position. New favorites appear after
existing favorites, oldest first; removing and adding again places the
location at the end. Opening a favorite makes it the active location restored
on the next launch.

---

### 4.6 Historical Data

The application should display the last 24 hours of hourly
air-quality measurements when the data source provides sufficient
historical data.

The historical view should display the available supported metrics
for each measurement.

Historical measurements may include:

- AQI
- PM2.5
- PM10
- NO2
- O3
- CO

The exact fields displayed depend on data availability.

The application should gracefully handle metrics that are unavailable
for a specific measurement.

Historical measurements should be cached locally. A historical cache is fresh
for up to one hour after retrieval. When refresh fails or the response has no
usable hourly measurements, show cached history when available and identify it
as potentially outdated when it exceeds the one-hour freshness threshold.
When neither usable remote history nor cached history exists, show an
understandable unavailable/error state.

---

### 4.7 Offline Support

The application should cache recently retrieved current and historical
air-quality data.

Current measurements are fresh for up to 30 minutes after retrieval.
Historical hourly measurements are fresh for up to one hour after retrieval.

When network connectivity is unavailable:

- Cached data should be displayed when available.
- Current data older than 30 minutes and historical data older than one hour
  must be clearly marked as stale.
- The UI should clearly communicate when the displayed data may be outdated.
- If no cached data exists, an understandable offline state must be shown.

---

## 5. Error and Empty States

The application should explicitly handle:

- Location permission denied
- Location unavailable
- No network connection
- API/server failure
- Invalid API response
- Empty search results
- No cached data available
- Stale cached data
- Missing individual pollutant values

Errors shown to users should be understandable and actionable.

---

## 6. Non-Goals for Version 1

Version 1 does not include:

- User accounts
- Social features
- Chat
- Push notifications
- Complex air-quality forecasting
- Medical diagnosis or medical recommendations
- Wearable integrations
- Advanced analytics
- Multiple backend services

---

## 7. Product Principles

### Clarity

Users should understand the current air-quality condition without
needing specialist knowledge.

### Reliability

The application should behave predictably under network,
location, and API failures.

### Transparency

Cached or stale data must be clearly identified.

### Simplicity

The initial product should avoid unnecessary features and complexity.

### Privacy

Location access should be requested only when needed and should not
require an account for the core experience.

---

## 8. Data Sources

### 8.1 Air Quality Data

The initial version uses the Open-Meteo Air Quality API as the external
air-quality data source.

The application retrieves current and hourly air-quality information
using geographic coordinates.

### 8.2 Location Search

The application uses a geocoding service to convert user-entered
place names into geographic coordinates.

The application uses a geocoding service to convert user-entered
place names into geographic coordinates.