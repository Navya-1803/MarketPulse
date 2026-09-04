# MarketPulse — Project Architecture, Functional Specification & Personal Validation Manual

> **Core Philosophy:** Track → Remember → Compare → Explain → Prioritize  
> *Instead of dumping an overwhelming price table, MarketPulse tells the user what meaningfully changed since their last check, ranked by an attention score, with clear explanations of why it matters.*

---

## Table of Contents
1. [Project Overview & Core Narrative](#1-project-overview--core-narrative)
2. [Complete Technology Stack](#2-complete-technology-stack)
3. [System Architecture & Design Patterns](#3-system-architecture--design-patterns)
4. [Functional Specification & Modules](#4-functional-specification--modules)
   - 4.1 [Authentication & User Management](#41-authentication--user-management)
   - 4.2 [Watchlist Management & Data Ownership](#42-watchlist-management--data-ownership)
   - 4.3 [Market Catalog, Search & Infinite Scroll](#43-market-catalog-search--infinite-scroll)
   - 4.4 [Stock Details & Historical Charting](#44-stock-details--historical-charting)
   - 4.5 [Smart Change Detection & Explainability Engine](#45-smart-change-detection--explainability-engine)
   - 4.6 [Attention Scoring Algorithm](#46-attention-scoring-algorithm)
   - 4.7 [Checkpoint Lifecycle ("Mark as Checked")](#47-checkpoint-lifecycle-mark-as-checked)
   - 4.8 [User Settings & Configurable Thresholds](#48-user-settings--configurable-thresholds)
   - 4.9 [In-App Notifications Feed](#49-in-app-notifications-feed)
   - 4.10 [Market Data Provider Abstraction & Caching](#410-market-data-provider-abstraction--caching)
   - 4.11 [Database Migration & Zero Data-Loss Resilience](#411-database-migration--zero-data-loss-resilience)
5. [Database Schema & Data Model](#5-database-schema--data-model)
6. [Complete REST API Reference](#6-complete-rest-api-reference)
7. [Step-by-Step Self-Validation Guide](#7-step-by-step-self-validation-guide)
   - 7.1 [Prerequisites & Environment Setup](#71-prerequisites--environment-setup)
   - 7.2 [Starting Backend & Frontend](#72-starting-backend--frontend)
   - 7.3 [Interactive UI Validation Checklist (12 Tests)](#73-interactive-ui-validation-checklist-12-tests)
   - 7.4 [Command-Line API Validation Script (cURL / PowerShell / Python)](#74-command-line-api-validation-script-curl--powershell--python)
   - 7.5 [Automated Test Suite Verification](#75-automated-test-suite-verification)
8. [Troubleshooting & Edge Cases](#8-troubleshooting--edge-cases)

---

## 1. Project Overview & Core Narrative

### The Core Problem Solved
Traditional stock watchlist applications (e.g., standard brokerage apps) display raw price tables:
```
AAPL  $230.20  +0.8%
TSLA  $340.10  -2.1%
NVDA  $178.50  +1.4%
```
This forces the user to manually scan numbers, recall what prices were earlier, and deduce what actually demands attention.

### MarketPulse Intelligence Layer
MarketPulse treats a stock watchlist as a **state-comparison problem**:
1. **Track**: User defines personal watchlists of interest.
2. **Remember**: The application records an immutable snapshot of quotes whenever a user checks their dashboard.
3. **Compare**: On the next visit, MarketPulse evaluates real-time market data **relative to the user's last meaningful checkpoint**, not just arbitrary 24h market open.
4. **Explain**: Rather than presenting raw deltas, the engine synthesizes transparent, human-readable explanations (e.g., *"NVDA decreased 6.67% since your last check"*, *"Move exceeds your 3% threshold"*, *"Volume is 2.4× recent average"*, *"Near today's day high"*).
5. **Prioritize**: A deterministic attention score (0–100) mathematically ranks critical movements, placing the most urgent changes at the very top of the user's feed.

---

## 2. Complete Technology Stack

### Backend
| Component | Technology | Version / Details | Purpose |
|:---|:---|:---|:---|
| **Language** | Java | OpenJDK 21 / 24 | Strong typing, high throughput, modern switch expressions & records |
| **Framework** | Spring Boot | 3.5.5 | Modular Monolith application framework |
| **Security** | Spring Security | 6.x | Stateless filter chain, route protection, method security |
| **JWT** | JJWT (`io.jsonwebtoken`) | 0.12.6 | HMAC-SHA256 stateless Bearer token issuance & verification |
| **Password Hashing** | BCrypt | Standard (Strength 10) | Salted one-way cryptographic hashing for passwords |
| **Persistence / ORM** | Spring Data JPA / Hibernate | 6.x | Relational mapping, repository interfaces, ACID transactions |
| **Database (Dev/Local)**| H2 Database | 2.3.232 | In-memory or file-backed database (`./data/marketpulse.mv.db`) with PostgreSQL mode |
| **Database (Prod)** | PostgreSQL | 15+ | Production ACID relational database |
| **Validation** | Jakarta Bean Validation | Hibernate Validator | `@Valid`, `@NotBlank`, `@Size`, `@Email` DTO input assertions |
| **Data Serialization** | Jackson | FasterXML | Java time module, JSON DTO serialization |
| **Boilerplate Reduction**| Lombok | 1.18.38 | `@Getter`, `@Setter`, `@NoArgsConstructor`, `@Builder` |
| **Build Tool** | Apache Maven | Maven Wrapper (`mvnw` / `mvnw.cmd`) | Dependency management and build lifecycle |
| **Unit & Integration Tests** | JUnit 5, Mockito, MockMvc | Spring Boot Starter Test | Full MVC test slice, provider mocks, security testing |

### Frontend
| Component | Technology | Version / Details | Purpose |
|:---|:---|:---|:---|
| **Language** | TypeScript | ~5.9.2 | Type safety across API responses and component props |
| **Library** | React | 19.1.1 | Modern React hooks, state management, concurrent rendering |
| **Routing** | React Router Dom | 7.8.2 | Single-page client routing, protected routes, URL parameters |
| **HTTP Client** | Axios | 1.11.0 | REST API client with request/response Bearer token interceptors |
| **Styling** | Bootstrap 5 + Vanilla CSS Tokens | 5.3.8 + Custom CSS | Responsive layout, dark theme tokens, glassmorphism cards |
| **Icons** | SVG / Bootstrap Icons | Vector Icons | Visual badges, status indicators, trends |
| **Bundler & Dev Server**| Vite | 7.1.3 | Instant HMR dev server (runs on port 5173 / 5174) |
| **Testing** | Vitest + React Testing Library | Vitest 3.2.4, JSDOM 26 | Component unit and integration testing |

### Infrastructure & Deployment
- **Containerization**: `docker-compose.yml` for PostgreSQL.
- **Cross-Origin (CORS)**: Configurable allowed origins (`http://localhost:5173`, `http://localhost:5174`).
- **External Market Data**: Dual provider architecture:
  - `MockMarketDataProvider` (Zero-dependency local simulation with realistic fluctuations).
  - `FinnhubMarketDataProvider` (Finnhub REST API integration with API key).

---

## 3. System Architecture & Design Patterns

### High-Level Architecture Diagram
```
┌─────────────────────────────────────────────────────────────┐
│                      React 19 Frontend                      │
│     (TypeScript, Vite, Custom Dark Theme, Axios Interceptor) │
└──────────────────────────────┬──────────────────────────────┘
                               │ REST APIs (Bearer JWT Token)
┌──────────────────────────────▼──────────────────────────────┐
│                Spring Boot Modular Monolith                 │
│                                                             │
│  ┌────────────────────┐            ┌─────────────────────┐  │
│  │    Auth Module     │            │  Watchlist Module   │  │
│  │ (Register/Login/JWT│            │ (CRUD & User Guard) │  │
│  └─────────┬──────────┘            └──────────┬──────────┘  │
│            │                                  │             │
│  ┌─────────▼──────────────────────────────────▼──────────┐  │
│  │                 Smart Change Engine                   │  │
│  │  - Baseline State Comparison                          │  │
│  │  - Attention Score (0 - 100)                          │  │
│  │  - Explainable Reason Generator                       │  │
│  │  - User-Configured Threshold Multipliers              │  │
│  └──────────────────────────┬────────────────────────────┘  │
│                             │                               │
│  ┌──────────────────────────▼────────────────────────────┐  │
│  │                   Market Data Module                  │  │
│  │  - MarketDataProvider (Pluggable Abstraction)         │  │
│  │  - In-Memory Cache (30s TTL, ConcurrentHashMap)       │  │
│  │  - Graceful Degradation (UNAVAILABLE flag on failure) │  │
│  │  - Stale Quote Detection (>15 min timestamp)          │  │
│  │  - StockCatalogService (60+ stocks, pagination)       │  │
│  └─────────────┬───────────────────────────────┬─────────┘  │
│                │                               │            │
└────────────────┼───────────────────────────────┼────────────┘
                 │                               │
┌────────────────▼────────────────┐    ┌─────────▼────────────┐
│      Relational Database        │    │ Real External / Mock │
│ H2 File (Dev) / Postgres (Prod) │    │ Finnhub / Mock API   │
└─────────────────────────────────┘    └──────────────────────┘
```

### Key Architectural Patterns
1. **Modular Monolith**: Eliminates distributed systems network overhead and complexity while retaining strict package boundaries:
   - `com.marketpulse.auth`
   - `com.marketpulse.user`
   - `com.marketpulse.watchlist`
   - `com.marketpulse.market`
   - `com.marketpulse.change`
   - `com.marketpulse.dashboard`
   - `com.marketpulse.common`
2. **Provider Pattern (`MarketDataProvider`)**: Decouples business logic from specific market vendor APIs. Switching from mock data to Finnhub requires zero changes to the change detection or watchlist services.
3. **In-Memory TTL Caching**: A thread-safe `ConcurrentHashMap` caches stock quotes for 30 seconds to respect API rate limits and deliver sub-millisecond responses.
4. **Graceful Degradation**: If an external quote fails, only that single stock is flagged as `UNAVAILABLE`. The rest of the dashboard renders seamlessly without throwing a 500 error.
5. **Strict Data Ownership**: Watchlists and checkpoints belong to a specific `userId`. A user cannot view, edit, or delete another user's watchlist (`403 Forbidden` vs `404 Not Found`).

---

## 4. Functional Specification & Modules

### 4.1 Authentication & User Management
- **Registration**: `POST /api/auth/register`
  - Validates full name, email format, minimum password length (6 characters), and password confirmation match.
  - Generates BCrypt password hash.
  - Automatically initializes default sensitivity threshold (`thresholdPercent = 3.0`).
  - Returns `201 Created` with JWT Bearer token and user profile.
- **Login**: `POST /api/auth/login`
  - Validates credentials against BCrypt hash.
  - Returns `200 OK` with JWT token.
- **Security Filter Chain**:
  - Validates `Authorization: Bearer <token>` on all `/api/**` requests (except public endpoints `/api/auth/**`, `/api/health`, `/h2-console/**`).
  - Injects `AuthenticatedUser` into `@AuthenticationPrincipal`.

### 4.2 Watchlist Management & Data Ownership
- **Watchlists**: A user can create multiple watchlists (e.g., "Tech Giants", "Semiconductors", "Dividend Safe").
- **Stock Association**: Users can add any valid stock ticker symbol to their watchlist.
- **Duplicate Prevention**: Rejects duplicate watchlist names per user (`409 Conflict`) and duplicate tickers in the same watchlist (`409 Conflict`).
- **Authorization Guard**: If User A accesses User B's watchlist ID, the API strictly returns `403 Forbidden`. If the watchlist ID does not exist at all, it returns `404 Not Found`.

### 4.3 Market Catalog, Search & Infinite Scroll
- **Stock Universe**: Curated catalog of ~60 diverse stocks across Tech, Healthcare, Finance, Consumer, Energy, and Indices (AAPL, MSFT, GOOGL, NVDA, TSLA, AMZN, META, JPM, V, WMT, etc.).
- **Paginated Listing**: `GET /api/stocks?page=0&size=25`
  - Returns structured `PageResponse<MarketQuoteDto>` containing `content`, `page`, `size`, `totalElements`, `totalPages`, `hasMore`.
- **Search**: `GET /api/stocks/search?query=nv`
  - Fuzzy searches symbol, display ticker, or company name.
- **Filters**:
  - `ALL`: Default catalog view.
  - `GAINERS`: Stocks with positive `changePercent`.
  - `LOSERS`: Stocks with negative `changePercent`.
  - `HIGH_VOLUME`: Stocks with volume $\ge 10,000,000$.
- **Sorting**: Sort by `symbol`, `name`, `price`, `changePercent`, or `volume` in `asc` or `desc` order.
- **Direct Add to Watchlist**: Users on the Stocks page can add any stock directly into any of their existing watchlists via an interactive modal.

### 4.4 Stock Details & Historical Charting
- **Real-Time Stock Details**: `GET /api/stocks/{symbol}`
  - Price, Change Amount, Change %, Day High, Day Low, Open, Previous Close, Volume, Market Status (`ACTIVE`, `STALE`, `UNAVAILABLE`), Captured Timestamp.
- **Historical Candlestick / Line Data**: `GET /api/stocks/{symbol}/history?range=1D`
  - Supports ranges: `1D`, `1W`, `1M`, `1Y`.
  - Returns timestamps, OHLCV (Open, High, Low, Close, Volume) data points.
  - Rendered with an interactive price chart with tooltip inspection.

### 4.5 Smart Change Detection & Explainability Engine
- **Baseline Comparison**: Compares the stock's current price against the user's **last checkpoint snapshot**. If no checkpoint exists (first-time user), it gracefully infers today's `open` or `previousClose` as the baseline.
- **Severity Classification**:
  - `CRITICAL`: Absolute move $\ge 2.5 \times \text{User Threshold}$ (e.g., $\ge 7.5\%$ at 3% threshold).
  - `SIGNIFICANT`: Absolute move $\ge \text{User Threshold}$ (e.g., $\ge 3.0\%$).
  - `NOTABLE`: Absolute move $\ge 0.66 \times \text{User Threshold}$ (e.g., $\ge 2.0\%$).
  - `NORMAL`: Below notable threshold.
- **Explainable Reasons**:
  - Clear English explanations with exact percentages and prices.
  - Highlights user-configured threshold crossings.
  - Detects volume anomalies ($\ge 2.0\times$ baseline volume).
  - Flags proximity to day extremes (within $1.0\%$ of Day High or Day Low).

### 4.6 Attention Scoring Algorithm
Prioritizes movements on a **0 to 100** scale:
$$\text{Attention Score} = \min(100, S_{\text{price}} + S_{\text{volume}} + S_{\text{extreme}} + S_{\text{time}})$$

1. **Price Movement Points ($S_{\text{price}}$)**:
   - $\min(\text{abs}(\text{changePercent}) \times 10, 70)$ points.
   - A $7\%$ swing awards the maximum 70 price points.
2. **Volume Anomaly Points ($S_{\text{volume}}$)**:
   - $+20$ points if volume is $\ge 2.0\times$ the baseline volume.
3. **Near Extreme Points ($S_{\text{extreme}}$)**:
   - $+15$ points if the current price is within $1\%$ of the Day High or Day Low.
4. **Time Elapsed Points ($S_{\text{time}}$)**:
   - $+1$ point per hour since last check (up to $+12$ points maximum).

### 4.7 Checkpoint Lifecycle ("Mark as Checked")
1. When user opens `/dashboard`, `GET /api/dashboard` calculates changes against the stored baseline.
2. The user inspects the ranked changes, attention scores, and explanation pills.
3. When the user clicks **"Mark as checked"**:
   - Sends `POST /api/checkpoints`.
   - The backend records a new `Checkpoint` record with `checkedAt = Instant.now()`.
   - Stores new `MarketSnapshot` baselines for all symbols currently across the user's watchlists.
   - Marks all previous unacknowledged `ChangeEvent` records as acknowledged.
4. Subsequent dashboard refreshes now compare against this new baseline (meaningful changes reset to 0 until prices move again).

### 4.8 User Settings & Configurable Thresholds
- **User Threshold**: Each user can configure their personal sensitivity threshold (default is `3.0%`).
- **Endpoint**:
  - `GET /api/settings`: Returns `{ "thresholdPercent": 3.0 }`.
  - `PUT /api/settings`: Updates threshold (e.g., `{ "thresholdPercent": 5.0 }`).
- **Instant Engine Adaptation**: The change detection engine and attention explanations immediately adopt the user's custom threshold.

### 4.9 In-App Notifications Feed
- Unacknowledged critical/significant change events are saved in `change_events`.
- **Endpoints**:
  - `GET /api/notifications`: Returns unacknowledged notifications sorted by attention score.
  - `POST /api/notifications/{id}/acknowledge`: Clears a single notification.
  - `POST /api/notifications/acknowledge-all`: Clears all notifications.
- The UI displays an interactive notification drawer with unread counts.

### 4.10 Market Data Provider Abstraction & Caching
- **Interface**: `MarketDataProvider` with methods:
  - `MarketQuoteDto getQuote(String symbol)`
  - `List<StockMetadata> getStockUniverse()`
  - `Optional<StockHistoryResponse> getStockHistory(String symbol, String range)`
- **Implementations**:
  - `MockMarketDataProvider`: Generates realistic, seeded, fluctuating market prices with real company names and sectors.
  - `FinnhubMarketDataProvider`: Connects to `https://finnhub.io/api/v1` using an API key.
- **Cache**: 30-second TTL prevents repeated API calls when multiple users view the same stock.
- **Stale Data Detection**: If a quote's timestamp is $> 15$ minutes old, `status` is tagged `STALE`.

### 4.11 Database Migration & Zero Data-Loss Resilience
- MarketPulse includes `DatabaseMigrationConfig.java` (`DatabaseSchemaMigrator`) running before Hibernate's entity manager initializes.
- Safely verifies if the `threshold_percent` column exists on the `users` table.
- If missing, executes:
  `ALTER TABLE users ADD COLUMN threshold_percent DOUBLE PRECISION DEFAULT 3.0`
  `UPDATE users SET threshold_percent = 3.0 WHERE threshold_percent IS NULL`
- Uses `LOCK_TIMEOUT=10000` to prevent H2 file lock deadlocks on rapid reloads.

---

## 5. Database Schema & Data Model

### Entity Relationship Diagram (ERD)

```
┌─────────────────────────┐           ┌─────────────────────────┐
│          users          │           │       watchlists        │
├─────────────────────────┤           ├─────────────────────────┤
│ id (PK)                 │1         *│ id (PK)                 │
│ email (UNIQUE)          ├───────────┤ user_id (FK)            │
│ password_hash           │           │ name                    │
│ full_name               │           │ created_at              │
│ threshold_percent (3.0) │           │ updated_at              │
│ created_at              │           └────────────┬────────────┘
│ updated_at              │                        │ 1
└───────────┬─────────────┘                        │
            │ 1                                    │ *
            │                         ┌────────────▼────────────┐
            │ *                       │    watchlist_stocks     │
┌───────────▼─────────────┐           ├─────────────────────────┤
│       checkpoints       │           │ id (PK)                 │
├─────────────────────────┤           │ watchlist_id (FK)       │
│ id (PK)                 │           │ symbol                  │
│ user_id (FK)            │           │ added_at                │
│ checked_at              │           └─────────────────────────┘
└───────────┬─────────────┘
            │ 1
            │ *
┌───────────▼─────────────┐           ┌─────────────────────────┐
│    market_snapshots     │           │      change_events      │
├─────────────────────────┤           ├─────────────────────────┤
│ id (PK)                 │           │ id (PK)                 │
│ checkpoint_id (FK)      │           │ user_id (FK)            │
│ symbol                  │           │ symbol                  │
│ price                   │           │ change_percent          │
│ volume                  │           │ severity                │
│ captured_at             │           │ attention_score         │
└─────────────────────────┘           │ acknowledged (BOOLEAN)  │
                                      │ created_at              │
                                      └─────────────────────────┘
```

---

## 6. Complete REST API Reference

All protected endpoints require HTTP Header:
`Authorization: Bearer <jwt_token>`

| Category | Method | Endpoint | Request Body | Description | Expected Status |
|:---|:---|:---|:---|:---|:---|
| **Health** | `GET` | `/api/health` | None | System liveness probe | `200 OK` |
| **Auth** | `POST` | `/api/auth/register` | `RegisterRequest` | Register new user | `201 Created` |
| **Auth** | `POST` | `/api/auth/login` | `LoginRequest` | Authenticate user | `200 OK` |
| **Auth** | `POST` | `/api/auth/logout` | None | Client logout hook | `200 OK` |
| **Settings** | `GET` | `/api/settings` | None | Get user alert threshold | `200 OK` |
| **Settings** | `PUT` | `/api/settings` | `{ "thresholdPercent": 5.0 }` | Update user alert threshold | `200 OK` |
| **Watchlists** | `GET` | `/api/watchlists` | None | List user's watchlists | `200 OK` |
| **Watchlists** | `POST` | `/api/watchlists` | `{ "name": "Tech" }` | Create a new watchlist | `201 Created` |
| **Watchlists** | `GET` | `/api/watchlists/{id}` | None | Get watchlist + live quotes | `200 OK` (or `403`/`404`) |
| **Watchlists** | `PUT` | `/api/watchlists/{id}` | `{ "name": "Big Tech" }` | Rename watchlist | `200 OK` (or `403`/`404`) |
| **Watchlists** | `DELETE`| `/api/watchlists/{id}` | None | Delete watchlist | `204 No Content` |
| **Watchlists** | `POST` | `/api/watchlists/{id}/stocks`| `{ "symbol": "NVDA" }` | Add stock to watchlist | `200 OK` (or `409`) |
| **Watchlists** | `DELETE`| `/api/watchlists/{id}/stocks/{symbol}` | None | Remove stock from watchlist | `204 No Content` |
| **Stocks** | `GET` | `/api/stocks` | Query params (`page`, `size`, `filter`, `sortBy`) | Paginated stock catalog | `200 OK` |
| **Stocks** | `GET` | `/api/stocks/search` | Query param (`query`) | Search stock universe | `200 OK` |
| **Stocks** | `GET` | `/api/stocks/{symbol}`| None | Get quote for symbol | `200 OK` |
| **Stocks** | `GET` | `/api/stocks/{symbol}/history` | Query param (`range=1D`) | Historical chart data | `200 OK` |
| **Dashboard**| `GET` | `/api/dashboard` | None | Aggregated dashboard view | `200 OK` |
| **Changes** | `GET` | `/api/changes` | None | Feed ranked by attention | `200 OK` |
| **Checkpoint**| `POST`| `/api/checkpoints` | None | "Mark as checked" action | `200 OK` |
| **Notifications**| `GET` | `/api/notifications` | None | List active notifications | `200 OK` |
| **Notifications**| `POST`| `/api/notifications/{id}/acknowledge` | None | Dismiss single notification | `204 No Content` |
| **Notifications**| `POST`| `/api/notifications/acknowledge-all` | None | Dismiss all notifications | `204 No Content` |

---

## 7. Step-by-Step Self-Validation Guide

Follow this guide to validate the entire application independently.

### 7.1 Prerequisites & Environment Setup
Verify your development environment:
```powershell
# 1. Verify Java 21+
java -version

# 2. Verify Node.js 18+ and npm
node -v
npm -v
```

### 7.2 Starting Backend & Frontend

#### Terminal 1: Start Backend (Port 8080)
```powershell
cd d:\Groww\MarkerPulse\backend
$env:JAVA_HOME = "C:\Users\Asus\.jdks\ms-21.0.7"   # Or your installed JDK 21+ path
.\mvnw.cmd spring-boot:run
```
*Expected log output:*
- `Checking database schema compatibility...`
- `Started MarketpulseBackendApplication in X seconds`
- Application running at: `http://localhost:8080`

#### Terminal 2: Start Frontend (Port 5173 or 5174)
```powershell
cd d:\Groww\MarkerPulse\frontend
npm run dev
```
*Expected log output:*
- `VITE v7.1.3  ready in ... ms`
- `Local: http://localhost:5173/` (or `5174`)

---

### 7.3 Interactive UI Validation Checklist (12 Tests)

Open your browser at `http://localhost:5173` (or `http://localhost:5174`):

- [ ] **Test 1: Public Health & Route Guard**
  - Direct visit `http://localhost:8080/api/health` in browser $\rightarrow$ should return `{"status":"UP"}`.
  - Direct visit `http://localhost:5173/dashboard` while logged out $\rightarrow$ automatically redirects to `/login`.

- [ ] **Test 2: User Registration**
  - Click **"Register"** or navigate to `/register`.
  - Enter Name (`Validation User`), Email (`test@marketpulse.io`), Password (`TestPass123!`), Confirm Password (`TestPass123!`).
  - Click **"Sign Up"** $\rightarrow$ successfully redirects directly to the `/dashboard`.
  - Verify token is saved in browser `localStorage` (`marketpulse_token`).

- [ ] **Test 3: Initial Empty State & Greeting**
  - Dashboard displays a personalized greeting: *"Hello, Validation User"*.
  - Displays *"No Watchlists Yet"* or empty state with a prompt to create your first watchlist.

- [ ] **Test 4: Watchlist Creation**
  - Navigate to **"Watchlists"** in the top navigation.
  - Enter Watchlist Name: `Tech Leaders` and click **"Create Watchlist"**.
  - Verify `Tech Leaders` appears in the watchlist sidebar / list.
  - Attempt to create another watchlist with the same name `Tech Leaders` $\rightarrow$ should display a warning that name already exists.

- [ ] **Test 5: Adding Stocks to Watchlist**
  - With `Tech Leaders` selected, type `NVDA` in the Add Stock input and click **"Add"**.
  - Type `AAPL` and add it.
  - Type `TSLA` and add it.
  - Verify the table renders prices, percentage moves, day high/low, and `ACTIVE` market status pills.
  - Attempt to add `NVDA` again $\rightarrow$ should display an error preventing duplicate stocks.

- [ ] **Test 6: Explore Market / Stocks Catalog Page**
  - Click **"Stocks"** (or **"Market"**) in the navbar (`/stocks`).
  - Verify the catalog displays stocks with infinite scroll / pagination (~60 stocks).
  - Test Search: Type `micro` $\rightarrow$ shows `MSFT` (Microsoft Corporation).
  - Test Filter: Click **"Gainers"** $\rightarrow$ only shows stocks with positive green deltas.
  - Test Filter: Click **"Losers"** $\rightarrow$ only shows stocks with negative red deltas.
  - Test Filter: Click **"High Volume"** $\rightarrow$ shows high volume stocks.
  - Click the **"+ Watchlist"** button on any stock (e.g., `AMZN`) $\rightarrow$ select `Tech Leaders` in the dropdown $\rightarrow$ stock is immediately added.

- [ ] **Test 7: Stock Details Page & Interactive Charts**
  - On the Stocks page or Watchlist page, click on `NVDA`.
  - URL navigates to `/stocks/NVDA`.
  - Verify all metrics appear: Price, Previous Close, Day Open, Day High, Day Low, Volume.
  - Toggle chart ranges: Click `1D`, `1W`, `1M`, `1Y` $\rightarrow$ verify candlestick/line chart updates with historical data points.

- [ ] **Test 8: "What Changed Since Last Check" & Attention Feed**
  - Navigate back to the **Dashboard** (`/dashboard`).
  - Notice the **"What Changed Since Last Check"** section.
  - Stocks with significant movements display Attention Score badges (e.g., `Score: 85`), severity pills (`CRITICAL`, `SIGNIFICANT`, `NOTABLE`), and natural language explanation bullet points explaining why it matters.

- [ ] **Test 9: Checkpoint Lifecycle ("Mark as Checked")**
  - On the Dashboard, note the count of meaningful changes.
  - Click the primary button **"Mark as checked"**.
  - A toast notification confirms checkpoint saved.
  - The "Last checked" timestamp updates to *"Just now"*.
  - Meaningful changes counter resets to 0 (all current prices are now the baseline).

- [ ] **Test 10: Configurable User Threshold**
  - Navigate to **Profile / Settings** (`/profile`).
  - View the default threshold: `3.0%`.
  - Change the threshold to `6.0%` and click **"Save"**.
  - Navigate back to Dashboard $\rightarrow$ notice that movements between 3% and 6% are no longer flagged as `SIGNIFICANT`, proving dynamic threshold reactivity.

- [ ] **Test 11: In-App Notifications**
  - Click the Notification Bell icon in the navbar.
  - View unacknowledged market events with attention scores.
  - Click **"Dismiss"** on a notification or **"Mark All as Read"** $\rightarrow$ notifications clear and badge updates.

- [ ] **Test 12: Multi-User Security & Cross-Account Isolation**
  - Click **"Logout"** in the user menu.
  - Register a second user: `attacker@marketpulse.io`.
  - Attempt to fetch User 1's watchlist ID using the API $\rightarrow$ backend strictly responds with `403 Forbidden`. User 2 cannot access User 1's data.

---

### 7.4 Command-Line API Validation Script (cURL / PowerShell / Python)

You can run this automated Python script to validate all backend endpoints in 3 seconds:

```python
# Save as validate_marketpulse.py and run: python validate_marketpulse.py
import urllib.request, json, sys

BASE = "http://localhost:8080"

def log(msg, ok=True):
    print(f"[{'PASS' if ok else 'FAIL'}] {msg}")

def req(path, method="GET", body=None, token=None):
    url = f"{BASE}{path}"
    data = json.dumps(body).encode('utf-8') if body else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    r = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(r) as res:
            text = res.read().decode('utf-8')
            return res.status, json.loads(text) if text else {}
    except urllib.error.HTTPError as e:
        text = e.read().decode('utf-8')
        try:
            return e.code, json.loads(text)
        except:
            return e.code, text

print("--- Starting MarketPulse Automated Backend Validation ---")

# 1. Health
status, data = req("/api/health")
assert status == 200 and data.get("status") == "UP"
log("1. Health check is UP")

# 2. Register
import time
email = f"val_{int(time.time())}@marketpulse.io"
status, data = req("/api/auth/register", "POST", {
    "fullName": "Auto Validator",
    "email": email,
    "password": "Password123!",
    "confirmPassword": "Password123!"
})
assert status == 201, f"Failed registration: {status} {data}"
token = data["token"]
log("2. Registration successful with JWT token")

# 3. Settings Default
status, data = req("/api/settings", token=token)
assert status == 200 and data.get("thresholdPercent") == 3.0
log("3. User settings returned default threshold 3.0%")

# 4. Settings Update
status, data = req("/api/settings", "PUT", {"thresholdPercent": 4.5}, token=token)
assert status == 200 and data.get("thresholdPercent") == 4.5
log("4. User settings updated to 4.5%")

# 5. Create Watchlist
status, data = req("/api/watchlists", "POST", {"name": "Core Tech"}, token=token)
assert status == 201
wl_id = data["id"]
log(f"5. Created watchlist 'Core Tech' (ID: {wl_id})")

# 6. Add Stocks
for sym in ["NVDA", "AAPL", "MSFT"]:
    status, _ = req(f"/api/watchlists/{wl_id}/stocks", "POST", {"symbol": sym}, token=token)
    assert status == 200
log("6. Added NVDA, AAPL, MSFT to watchlist")

# 7. Stock Catalog & Pagination
status, data = req("/api/stocks?page=0&size=10", token=token)
assert status == 200 and len(data.get("content", [])) == 10
log("7. Stocks catalog returns paginated quotes (10 items)")

# 8. Stock Search
status, data = req("/api/stocks/search?query=nvda", token=token)
assert status == 200 and any(s["symbol"] == "NVDA" for s in data.get("content", []))
log("8. Stock search finds NVDA")

# 9. Stock History Chart Data
status, data = req("/api/stocks/NVDA/history?range=1D", token=token)
assert status == 200 and "points" in data
log("9. Stock history returns 1D candlestick data points")

# 10. Dashboard & Change Detection
status, data = req("/api/dashboard", token=token)
assert status == 200 and "changeSummary" in data
log("10. Dashboard returns ranked attention feed and change summary")

# 11. Checkpoint ("Mark as checked")
status, data = req("/api/checkpoints", "POST", token=token)
assert status == 200 and "checkedAt" in data
log("11. Checkpoint advanced successfully ('Mark as checked')")

print("\nALL BACKEND VALIDATIONS PASSED PERFECTLY!")
```

---

### 7.5 Automated Test Suite Verification

#### Run Backend Test Suite (JUnit 5 + MockMvc)
```powershell
cd d:\Groww\MarkerPulse\backend
$env:JAVA_HOME = "C:\Users\Asus\.jdks\ms-21.0.7"
.\mvnw.cmd clean test
```
*Expected Output:*
`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`  
`BUILD SUCCESS`

#### Run Frontend Test Suite (Vitest + JSDOM)
```powershell
cd d:\Groww\MarkerPulse\frontend
npm test
npm run build
```
*Expected Output:*
`9 passed (100%)`  
`vite build completed successfully with 0 TypeScript errors.`

---

## 8. Troubleshooting & Edge Cases

| Issue | Root Cause | Solution |
|:---|:---|:---|
| **`Database may be already in use: Locked by another process`** | Another Java process or IDE instance is holding the H2 lock file (`./data/marketpulse.mv.db`). | Run `cmd /c "taskkill /F /IM java.exe"` to terminate stale processes. H2 config has `LOCK_TIMEOUT=10000` to prevent deadlocks. |
| **`Port 8080 or 5173 already in use`** | A previously started server is still active in the background. | Kill the existing process: `netstat -ano \| findstr :8080` followed by `taskkill /PID <PID> /F`. |
| **`Column "threshold_percent" not found`** | Attempting to start the application with an older H2 database file. | Already resolved automatically by `DatabaseMigrationConfig.java` which adds the column on startup with default `3.0` without resetting data. |
| **`HTTP 401 Unauthorized` on API call** | Missing or expired JWT token in `Authorization: Bearer <token>` header. | Re-authenticate via `/api/auth/login` to obtain a fresh token. |
| **`HTTP 403 Forbidden` on Watchlist** | Authenticated user is attempting to read or modify a watchlist belonging to another user. | Expected security behavior. Ensure requests target watchlists created by the currently logged-in user. |
| **Finnhub Rate Limits (429 Too Many Requests)** | Finnhub free tier is limited to 60 calls/minute. | The in-memory 30s TTL cache prevents duplicate calls. Alternatively, set `MARKET_DATA_PROVIDER=mock` in `application.yml` for unlimited zero-dependency development. |

---

*Document compiled for personal validation & technical reference of MarketPulse.*
