# SnapURL

SnapURL is a full-stack URL shortening platform. It combines a Spring Boot backend, a React dashboard, Redis-backed performance features, and RabbitMQ-driven asynchronous analytics to handle both product polish and backend scalability concerns.

## What It Does

SnapURL lets users create short links, manage them from a dashboard, and track how those links perform over time. The project includes custom aliases, analytics, secure authentication flows, server-side querying, and operational concerns such as caching, throttling, and asynchronous event processing.

## Core Capabilities

### Link Management

- Create short links from long URLs
- Support custom aliases with validation and collision protection
- Reuse an existing short link when the same authenticated user shortens the same URL again
- Delete owned links safely with related analytics cleanup

### Analytics

- Track clicks per short link
- View dashboard totals and per-link trends
- Cache analytics reads in Redis
- Process click events asynchronously so redirects stay lightweight

### Search and Querying

- Search links by short code and original URL
- Filter by date range, click counts, and status
- Sort by created date, clicks, and last activity
- Use server-driven pagination instead of client-side filtering

### Authentication and Security

- JWT access tokens with refresh token rotation
- forgot-password flow using one-time reset codes
- supported-provider email validation
- Redis-backed login throttling
- temporary account lockout after repeated failed logins
- rate limiting for public and authenticated shorten endpoints

## Architecture

![Architecture](Snap_Url_Architecture.png)

### Redirect Flow

1. A short URL is requested
2. The backend checks Redis for the destination
3. On cache miss, MySQL is queried and Redis is updated
4. The redirect returns quickly
5. A click event is dispatched asynchronously for analytics processing

### Analytics Flow

1. Redirect activity creates a click event
2. RabbitMQ carries the event off the request path
3. A consumer updates click history and counters
4. Related Redis analytics cache entries are evicted

## Backend Highlights

### Spring Boot API

The backend focuses on system behavior, not just endpoint count. It includes:

- entity persistence with JPA
- targeted update queries for click counters to reduce contention
- transactional deletion and auth-sensitive operations
- repository-driven data access and DTO-based responses
- global API error handling with consistent response shapes

### Redis

Redis is used for multiple production-style concerns:

- redirect lookup caching
- analytics caching
- login/API rate limiting

This makes the project stronger both technically and from a resume perspective because Redis is applied to real bottlenecks, not added as a token dependency.

### RabbitMQ

RabbitMQ is used to decouple analytics writes from the redirect path. Instead of blocking the redirect response on click persistence, SnapURL publishes an event and processes analytics asynchronously in the background.

That design improves the architecture story significantly:

- fast reads
- asynchronous writes
- clearer separation between user-facing latency and analytics work

## Security Notes

SnapURL includes a number of security-focused decisions:

- provider-restricted email validation
- refresh token rotation on renewal
- refresh token revocation on password reset
- one-time password reset codes with expiry
- account lockout after repeated failed logins
- Redis-backed throttling with rate-limit headers
- environment-variable based secret configuration

## Frontend

The frontend is a dark, minimal SaaS-style interface built around:

- a compact link-creation flow
- a polished dashboard
- real-time search and filtering controls
- custom confirmation and recovery flows
- responsive layout and refined dark-mode typography

The UI is intentionally paired with backend features rather than acting as a thin mock layer. Most dashboard interactions are driven by real API querying, not local-only state tricks.

## Technical Highlights

- **Backend:** Java, Spring Boot, Spring Security, Spring Data JPA
- **Messaging:** RabbitMQ
- **Caching / Throttling:** Redis
- **Database:** MySQL
- **Frontend:** React, Vite, Tailwind CSS
- **Auth:** JWT access tokens, refresh tokens, reset codes
- **Testing:** JUnit, Mockito
