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

If the user denies location permission, the application must remain
usable through city/location search.

---

### 4.2 Current Air Quality

The dashboard displays:

- Current AQI
- Air-quality category
- Major pollutant measurements
- Data timestamp

The presentation should prioritize understandable information.

---

### 4.3 Pollutant Details

The application should provide information for supported major
pollutants, such as:

- PM2.5
- PM10
- NO2
- O3
- CO

The exact supported pollutants depend on the selected air-quality
data source.

---

### 4.4 Location Search

Users can search for another location or city and view its current
air quality.

Search results should allow the user to select a location.

---

### 4.5 Favorites

Users can save locations as favorites.

Users can:

- View favorite locations
- Open a favorite location
- Remove a favorite location

Favorites do not require authentication and are stored locally.

---

### 4.6 Historical Data

The application should provide historical air-quality information
and trends when supported by the selected data source.

The exact historical range and granularity will depend on the API.

---

### 4.7 Offline Support

The application should cache recently retrieved air-quality data.

When network connectivity is unavailable:

- Recently cached data may be displayed.
- The application must clearly indicate that the displayed data is
  cached or stale.
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

## 8. Data Source

The initial version uses the Open-Meteo Air Quality API as its external
air-quality data source.

The application communicates with the API over HTTP.

The data source provides current and hourly air-quality information,
including AQI and pollutant measurements.

The exact API fields used by the application will be documented in the
architecture and data-layer design.