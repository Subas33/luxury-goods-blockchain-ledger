# Luxury Goods Blockchain Prototype

**Blockchain backed Ecommerce application for registering luxury goods on Hyperledger Fabric, with off-chain inspection management via MongoDB, a Java Spring Boot REST API, and a modern Angular storefront.**

A blockchain-based asset provenance tracking, combining immutable on-chain state with privacy-preserving off-chain document storage.

![alt text](docs/images/UI.png)
---

## 📋 Table of Contents

- [Quick Start](#quick-start)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Component Documentation](#component-documentation)
- [Project Structure](#project-structure)
- [Setup & Deployment](#setup--deployment)
- [REST API Reference](#rest-api-reference)
- [Demo Workflows](#demo-workflows)
- [Design Patterns & Concepts](#design-patterns--concepts)
- [Limitations & Future Work](#limitations--future-work)

---

## Quick Start

### Prerequisites
- **Git** 2.20+
- **Docker & Docker Compose** 20.10+
- **Java 17** or newer
- **Maven 3.8.x** or newer
- **Node.js & npm** 16+
- Internet access for Maven/Fabric dependencies

### 60-Second Setup


## 1. Clone and enter directory
```bash
git clone <repo-url>
cd LuxuryGoodsBlockchain
```
## 2. Start dependencies (MongoDB)
```bash
docker compose up -d
```

## 3. Start Fabric network (downloads samples on first run)
```bash
./scripts/start-fabric.sh
```
## 4. Deploy chaincode
```bash
./scripts/deploy-chaincode.sh
```
## 5. Start middleware (Spring Boot)
```bash
./scripts/run-middleware.sh    # Runs on http://localhost:8080
```
## 6. Start frontend (Angular)
```bash
./scripts/run-frontend.sh      # Runs on http://localhost:4200
```

## 7. Open browser to http://localhost:4200 and click "Load Showcase Inventory"


For detailed setup instructions, see [Setup & Deployment](#setup--deployment).

---

## System Architecture

### Component Overview

```
┌─────────────────────────────┐
│  Angular Frontend (4200)    │  
│  - Asset Catalog View       │
│  - Registration Form         │
│  - Ownership Transfers       │
│  - Inspection Management     │
└────────────┬────────────────┘
             │ HTTP REST
             ↓
┌─────────────────────────────┐
│  Spring Boot Middleware     │  (8080)
│  - REST API Endpoints       │
│  - Business Logic           │
│  - Hash Generation (SHA256) │
│  - Fabric Gateway Client    │
└────────┬────────────┬───────┘
         │            │
         ↓            ↓
    ┌─────────┐  ┌──────────────────┐
    │ MongoDB │  │ Hyperledger      │
    │ - Assets│  │ Fabric Network   │
    │ - Insp  │  │ - Ledger         │
    └─────────┘  │ - Peers          │
                 │ - Orderer        │
                 │ - Chaincode      │
                 └──────────────────┘
```

### Data Flow

**Typical Transaction:**
```
User submits form
    ↓
Angular validates locally
    ↓
HTTP POST to middleware API
    ↓
Middleware validates & processes
    ↓
│ MongoDB  ← Upserted (projection)
│ Fabric   ← Transaction submitted
↓
Consensus achieved (3+ peer endorsement, orderer commit)
    ↓
Ledger updated on all peers
    ↓
Response sent back to frontend
    ↓
UI updates with confirmation
```

### Architectural Flow Diagram

```
Browser                  Frontend              Middleware          MongoDB
  │                        │                       │                  │
  ├─ Load Catalog ────────→│                       │                  │
  │                        ├─ GET /assets ───────→│                  │
  │                        │                       ├─ Query ─────────→│
  │                        │                {filtered} ←───────────┤
  │                        ←─ AssetSummary[] ────────┤
  │  ←─ Catalog ──────────┤                       │                  │
  │                        │                       │                  │
  ├─ Register Asset ──────→│                       │                  │
  │  {assetId, type...}    ├─ POST /assets ─────→│                  │
  │                        │                       ├─ Submit ────→ Fabric
  │                        │                    (Consensus)          │
  │                        │                       ↓                  │
  │                        │                    ← Response            │
  │                        │                       ├─ Upsert ────────→│
  │                        │                       ← Done             │
  │                        ← Success ──────┤
  │  ←─ Asset Confirmed ──┤                       │                  │
```

---

## Technology Stack

### Backend
| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17+ |
| Web Framework | Spring Boot | 3.3.5 |
| Data Access | Spring Data MongoDB | 4.0+ |
| Blockchain | Fabric Java SDK | 1.10.1 |
| Build Tool | Maven | 3.9+ |
| Container | Docker | 20.10+ |
| HTTP Server | Embedded Tomcat | 10.1+ |

### Frontend  
| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Angular | 19+ |
| Language | TypeScript | 5.5+ |
| Styling | SCSS | 1.6+ |
| HTTP Client | RxJS | 7.8+ |
| Package Manager | npm | 10+ |
| Build Tool | ng CLI/Webpack | 19+ |

### Blockchain & Data
| Component | Technology | Version |
|-----------|-----------|---------|
| Blockchain | Hyperledger Fabric | 2.5.15+ |
| Consensus | PBFT (Fabric Built-in) | - |
| Database (Off-chain) | MongoDB | 7.0+ |
| Serialization (Java) | Genson | 1.6+ |
| Serialization (API) | Jackson | 2.15+ |

---

## Component Documentation

Each layer of the application has comprehensive documentation:

### 📘 **[Chaincode Architecture](./docs/CHAINCODE_ARCHITECTURE.md)**
- **What:** Java smart contract for Hyperledger Fabric
- **Why:** Maintains immutable asset state and event history
- **How:** Validates transactions, manages state transitions
- **Patterns:** Event Sourcing, State Machine, Command Pattern
- **Performance:** ~2-4s latency per transaction, immutable audit trail

**Key Concepts:**
- Embedded event history for self-describing state
- SHA-256 hash-based inspection linking
- Fabric history queries for complete timeline

### 📗 **[Middleware Architecture](./docs/MIDDLEWARE_ARCHITECTURE.md)**
- **What:** Spring Boot REST API orchestration layer
- **Why:** Bridges HTTP clients with blockchain, manages dual-write consistency
- **How:** Validates requests, orchestrates blockchain/database calls
- **Patterns:** Service Layer, Repository, Adapter, DTO, Exception Translation
- **Performance:** ~100 concurrent requests, ~10K MongoDB ops/sec

**Key Services:**
- `AssetService` - Business logic orchestration
- `FabricLedgerService` - Blockchain interaction
- `InspectionHashService` - Cryptographic hashing
- `GlobalExceptionHandler` - Centralized error handling

### 📙 **[Frontend Architecture](./docs/FRONTEND_ARCHITECTURE.md)**
- **What:** Angular 19+ standalone web application
- **Why:** User-friendly interface for blockchain operations
- **How:** Reactive UI with Signals, RxJS Observables, forms
- **Patterns:** Reactive Programming, Service Layer, Master-Detail
- **Performance:** <500ms list load, instant updates via computed properties

**Key Components:**
- Home Page - Asset catalog with search/filters
- Asset Detail Page - Ownership, history, inspections
- Asset Card - Reusable presentation component

### 🏗️ **[Architecture Diagrams](./docs/ARCHITECTURE_DIAGRAMS.md)**
- System overview with all components
- Sequence diagrams for key workflows
- State transition machines
- Class diagrams with design patterns
- Error handling flows

---

## Project Structure

```
LuxuryGoodsBlockchain/
├── docs/                                    # Component documentation
│   ├── CHAINCODE_ARCHITECTURE.md           # Smart contract design
│   ├── MIDDLEWARE_ARCHITECTURE.md          # Spring Boot design
│   ├── FRONTEND_ARCHITECTURE.md            # Angular design
│   └── ARCHITECTURE_DIAGRAMS.md            # Mermaid diagrams
│
├── chaincode/                              # Java Chaincode (Fabric)
│   ├── pom.xml
│   └── src/main/java/com/luxurygoods/blockchain/chaincode/
│       ├── AssetContract.java              # Main contract
│       ├── AssetState.java                 # Data model
│       ├── AssetEvent.java                 # Event definitions
│       └── AssetHistoryRecord.java         # History tracking
│
├── middleware/                             # Spring Boot API
│   ├── pom.xml
│   └── src/main/java/com/luxurygoods/blockchain/middleware/
│       ├── controller/                     # REST endpoints
│       │   └── AssetController.java
│       ├── service/                        # Business logic
│       │   ├── AssetService.java
│       │   ├── FabricLedgerService.java
│       │   └── InspectionHashService.java
│       ├── config/                         # Configuration
│       │   ├── FabricGatewayConfig.java
│       │   ├── FabricGatewayProperties.java
│       │   └── WebConfig.java
│       ├── repository/                     # Data access
│       │   ├── AssetRepository.java
│       │   └── InspectionReportRepository.java
│       ├── dto/                            # Request/Response DTOs
│       │   ├── request/
│       │   └── response/
│       ├── model/                          # MongoDB documents
│       │   ├── AssetDocument.java
│       │   └── InspectionReportDocument.java
│       ├── exception/                      # Custom exceptions
│       │   ├── DuplicateAssetException.java
│       │   ├── FabricClientException.java
│       │   └── GlobalExceptionHandler.java
│       └── LuxuryGoodsBlockchainApplication.java
│
├── frontend/                               # Angular Application
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig.json
│   └── src/app/
│       ├── app.ts                          # Root component
│       ├── app.config.ts                   # Configuration
│       ├── app.routes.ts                   # Route definitions
│       ├── core/
│       │   ├── models/
│       │   │   └── luxury-goods.models.ts  # TypeScript interfaces
│       │   ├── services/
│       │   │   └── luxury-goods-api.service.ts
│       │   ├── data/
│       │   │   └── showcase-assets.ts      # Demo data
│       │   └── utils/
│       │       └── luxury-asset.presenter.ts
│       ├── features/
│       │   ├── home/
│       │   │   └── home-page.*             # Catalog view
│       │   └── asset-detail/
│       │       └── asset-detail-page.*     # Asset details
│       └── shared/
│           └── components/
│               └── asset-card/
│                   └── asset-card.*        # Reusable card
│
├── scripts/                                # Deployment & setup
│   ├── start-fabric.sh                     # Start Fabric network
│   ├── deploy-chaincode.sh                 # Deploy chaincode
│   ├── run-middleware.sh                   # Start Spring Boot
│   ├── run-frontend.sh                     # Start Angular
│   └── stop-fabric.sh                      # Stop Fabric
│
├── pom.xml                                 # Root Maven aggregator
├── mvnw & mvnw.cmd                         # Maven wrappers
├── docker-compose.yml                      # MongoDB + utilities
├── .gitignore                              # Git exclusions
└── README.md                               # This file
```

---

## Setup & Deployment

### Full Installation Guide

#### 1. **Prerequisites Installation**

```bash
# macOS
brew install git docker node openjdk@17

# Ubuntu/Debian
sudo apt-get install git docker.io nodejs openjdk-17-jdk

# Windows (use Chocolatey or WSL)
choco install git docker nodejs openjdk17
```

**Verify Installation:**
```bash
java -version      # Should show Java 17+
docker --version   # Should show 20.10+
npm --version      # Should show 16+
git --version      # Should show 2.20+
mvn --version      # Should show 3.8.x+
```

#### 2. **Clone Repository**

```bash
git clone <repo-url> LuxuryGoodsBlockchain
cd LuxuryGoodsBlockchain
```

#### 3. **Start MongoDB**

```bash
# Using Docker Compose
docker compose up -d

# Verify MongoDB is running
docker logs -f <mongodb-container-id>
```

#### 4. **Start Hyperledger Fabric Test Network**

```bash
# First run: Downloads fabric-samples (~1GB), takes 5-10 minutes
./scripts/start-fabric.sh

# Subsequent runs: Much faster (reuses downloaded files)
# Override versions if needed:
FABRIC_VERSION=2.5.15 CA_VERSION=1.5.17 ./scripts/start-fabric.sh

# Verify Fabric is running
docker ps | grep fabric
```

**Expected Output:**
- 6+ Fabric containers (peers, orderer, CA, etc.)
- No failed startup logs
- Port 7051 responsive (peer endpoint)

#### 5. **Deploy Chaincode**

```bash
./scripts/deploy-chaincode.sh

# Optional: Override chaincode parameters
CHAINCODE_NAME=luxuryasset CHAINCODE_VERSION=1.0 ./scripts/deploy-chaincode.sh
```

**Verification:**
```bash
# Check if chaincode pod is running
docker ps | grep luxuryasset

# Logs should show "Successfully committed chaincode"
```

#### 6. **Build & Run Middleware**

```bash
# Build all Java modules
./mvnw clean package -DskipTests

# Start middleware
./scripts/run-middleware.sh

# Should see: "Started LuxuryGoodsBlockchainApplication"
# Middleware runs on http://localhost:8080
```

**Verify:**
```bash
curl http://localhost:8080/assets
# Should return: []  (empty list, no assets yet)
```

#### 7. **Install & Run Frontend**

```bash
cd frontend

# First run: Install dependencies
npm install --legacy-peer-deps --cache .npm-cache

# Start dev server
npm start
# OR use the helper script
../scripts/run-frontend.sh

# Should see: "Application bundle generation complete"
# Frontend runs on http://localhost:4200
```

**Verify:**
Open browser to `http://localhost:4200` - should see the asset catalog page.

### Environment Variables

**Middleware Configuration** (`middleware/src/main/resources/application.yml`):
```yaml
FABRIC_PEER_ENDPOINT=localhost:7051         # Fabric peer endpoint
FABRIC_CHANNEL=mychannel                    # Fabric channel name
FABRIC_CHAINCODE_NAME=luxuryasset           # Deployed chaincode name
FABRIC_MSP_ID=Org1MSP                       # Organization MSP
FABRIC_CERT_DIR=/path/to/certs/signcerts
FABRIC_KEY_DIR=/path/to/certs/keystore
FABRIC_TLS_CERT=/path/to/tlscacerts.pem
```

**Frontend Configuration** (`frontend/src/app/core/services/..`):
```typescript
const API_BASE_URL = 'http://localhost:8080';  // Middleware endpoint
const CORS_ORIGIN = '4200';                    // Dev frontend port
```

### Docker Compose Services

```yaml
# docker-compose.yml
services:
  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
    volumes:
      - mongodb_data:/data/db
```

---

## REST API Reference

### Base URL
```
http://localhost:8080/assets
```

### Endpoints

#### **1. List All Assets**
```http
GET /assets
Content-Type: application/json
```

**Response (200 OK):**
```json
[
  {
    "assetId": "ROLEX-001",
    "type": "watch",
    "brand": "Rolex Daytona",
    "owner": "Maison Aurelia",
    "createdAt": "2026-04-04T15:42:31Z",
    "updatedAt": "2026-04-05T10:00:00Z",
    "latestInspectionHash": "e3b0c44298fc...",
    "inspectionCount": 2,
    "latestInspectionStatus": "AUTHENTICATED",
    "latestInspectionDate": "2026-04-05T08:30:00Z"
  }
]
```

---

#### **2. Get Single Asset**
```http
GET /assets/{assetId}
```

**Response (200 OK):**
```json
{
  "assetId": "ROLEX-001",
  "type": "watch",
  "brand": "Rolex Daytona",
  "owner": "Collector A",
  "createdAt": "2026-04-04T15:42:31Z",
  "updatedAt": "2026-04-05T10:00:00Z",
  "latestInspectionHash": "e3b0c44298fc...",
  "inspectionCount": 2,
  "latestInspectionStatus": "AUTHENTICATED",
  "latestInspectionDate": "2026-04-05T08:30:00Z"
}
```

---

#### **3. Register New Asset**
```http
POST /assets
Content-Type: application/json

{
  "assetId": "OMEGA-001",
  "type": "watch",
  "brand": "Omega Seamaster",
  "owner": "Collector B"
}
```

**Response (200 OK):**
```json
{
  "assetId": "OMEGA-001",
  "type": "watch",
  "brand": "Omega Seamaster",
  "owner": "Collector B",
  "createdAt": "2026-04-05T14:30:00Z",
  "updatedAt": "2026-04-05T14:30:00Z",
  "eventHistory": [
    {
      "eventType": "REGISTERED",
      "timestamp": "2026-04-05T14:30:00Z",
      "previousOwner": null,
      "newOwner": "Collector B",
      "reportHash": null
    }
  ]
}
```

**Errors:**
- `400 Bad Request` - Invalid field format
- `409 Conflict` - Asset already exists

---

#### **4. Transfer Ownership**
```http
POST /assets/{assetId}/transfer
Content-Type: application/json

{
  "newOwner": "Collector C"
}
```

**Response (200 OK):**
```json
{
  "assetId": "OMEGA-001",
  "type": "watch",
  "brand": "Omega Seamaster",
  "owner": "Collector C",
  "updatedAt": "2026-04-05T15:00:00Z",
  "eventHistory": [
    { "eventType": "REGISTERED", ... },
    {
      "eventType": "OWNERSHIP_TRANSFERRED",
      "timestamp": "2026-04-05T15:00:00Z",
      "previousOwner": "Collector B",
      "newOwner": "Collector C",
      "reportHash": null
    }
  ]
}
```

**Errors:**
- `404 Not Found` - Asset doesn't exist
- `502 Bad Gateway` - Blockchain error

---

#### **5. Add Inspection Report**
```http
POST /assets/{assetId}/inspection
Content-Type: application/json

{
  "inspector": "ANTIQUORUM AUCTIONEERS",
  "status": "AUTHENTICATED",
  "location": "Geneva Headquarters",
  "notes": "Serial number verified, case excellent condition",
  "inspectedAt": "2026-04-05T14:00:00Z",
  "metadata": {
    "serialMatch": true,
    "condition": "excellent",
    "movementType": "Automatic"
  }
}
```

**Response (200 OK):**
```json
{
  "id": "507f1f77bcf86cd799439011",
  "assetId": "OMEGA-001",
  "reportHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "inspector": "ANTIQUORUM AUCTIONEERS",
  "status": "AUTHENTICATED",
  "location": "Geneva Headquarters",
  "inspectedAt": "2026-04-05T14:00:00Z",
  "storedAt": "2026-04-05T14:05:00Z",
  "ledgerSynced": true,
  "ledgerSyncedAt": "2026-04-05T14:05:01Z"
}
```

**Note:** Hash is stored on-chain; full document lives in MongoDB.

---

#### **6. Get Asset History**
```http
GET /assets/{assetId}/history
```

**Response (200 OK):**
```json
{
  "assetId": "OMEGA-001",
  "history": [
    {
      "txId": "62e362b0b7d5cd0cd8d46d926ee06a73a37bcd4078ffa1f7cc7b53ea8e85dd2e",
      "timestamp": "2026-04-05T14:30:00Z",
      "deleted": false,
      "asset": {
        "assetId": "OMEGA-001",
        "owner": "Collector B",
        "eventHistory": [
          { "eventType": "REGISTERED", ... }
        ]
      }
    },
    {
      "txId": "abc123def456...",
      "timestamp": "2026-04-05T15:00:00Z",
      "deleted": false,
      "asset": {
        "assetId": "OMEGA-001",
        "owner": "Collector C",
        "eventHistory": [
          { "eventType": "REGISTERED", ... },
          { "eventType": "OWNERSHIP_TRANSFERRED", ... }
        ]
      }
    }
  ]
}
```

---

#### **7. Get Inspection Reports**
```http
GET /assets/{assetId}/inspections
```

**Response (200 OK):**
```json
[
  {
    "id": "507f1f77bcf86cd799439011",
    "assetId": "OMEGA-001",
    "reportHash": "e3b0c44298fc...",
    "inspector": "ANTIQUORUM AUCTIONEERS",
    "status": "AUTHENTICATED",
    "location": "Geneva Headquarters",
    "notes": "Serial number verified, case excellent condition",
    "inspectedAt": "2026-04-05T14:00:00Z",
    "metadata": { "serialMatch": true, "condition": "excellent" },
    "storedAt": "2026-04-05T14:05:00Z",
    "ledgerSynced": true,
    "ledgerSyncedAt": "2026-04-05T14:05:01Z"
  }
]
```

---

## Demo Workflows

### Workflow 1: Quick Start Demo (5 minutes)

1. **Launch Frontend**
   ```bash
   # Browser: http://localhost:4200
   ```

2. **Click "Load Showcase Inventory"**
   - Three pre-configured luxury items are registered
   - Each includes an inspection report
   - Takes 10-15 seconds to seed

3. **Browse the Catalog**
   - Search by brand, type, owner
   - View summary statistics (pieces, custodians, inspections)

4. **Inspect an Asset**
   - Click any asset card
   - View ownership timeline
   - View blockchain transaction history
   - View inspection reports with details

5. **Transfer Ownership**
   - Select new owner
   - Click "Transfer Ownership"
   - Confirm action
   - See updated owner in blockchain history

6. **Add Inspection**
   - Fill inspection form
   - Inspect status, location, notes
   - Submit
   - See report instantly reflected in details

### Workflow 2: API-Only Demo (curl)

```bash
# Create an asset
curl -X POST http://localhost:8080/assets \
  -H "Content-Type: application/json" \
  -d '{
    "assetId": "DEMO-001",
    "type": "handbag",
    "brand": "Hermès",
    "owner": "Fashion House A"
  }'

# Transfer it
curl -X POST http://localhost:8080/assets/DEMO-001/transfer \
  -H "Content-Type: application/json" \
  -d '{"newOwner": "Collector D"}'

# Add inspection
curl -X POST http://localhost:8080/assets/DEMO-001/inspection \
  -H "Content-Type: application/json" \
  -d '{
    "inspector": "Luxury Goods Verifier",
    "status": "AUTHENTIC",
    "location": "New York Office",
    "notes": "Craftmanship verified, excellent condition",
    "inspectedAt": "2026-04-05T16:00:00Z",
    "metadata": {"material": "leather", "year": "2024"}
  }'

# Query history
curl http://localhost:8080/assets/DEMO-001/history | jq .

# Query inspections
curl http://localhost:8080/assets/DEMO-001/inspections | jq .
```

---

## Design Patterns & Concepts

### Architectural Design Patterns

| Pattern | Implementation | Benefit |
|---------|----------------|---------|
| **Service Layer** | AssetService, FabricLedgerService | Business logic isolation & testability |
| **Repository** | Spring Data MongoDB | Data access abstraction |
| **DTO (Data Transfer Object)** | Request/response records | API contract decoupling from domain |
| **Adapter** | FabricLedgerService wraps SDK | Simplifies low-level SDK complexity |
| **Factory** | FabricGatewayConfig | Complex object construction |
| **Builder** | Gateway.newInstance() fluent API | Readable configuration |
| **Mapper** | toAssetSummary(), etc. | Type-safe transformations |
| **Exception Translation** | FabricLedgerService error mapping | Domain-specific error handling |
| **Global Exception Handler** | @RestControllerAdvice | Centralized exception processing |
| **Event Sourcing** | AssetState.eventHistory[] | Complete audit trail, temporal queries |

### System Design Concepts

#### **1. Eventual Consistency**
- Blockchain consensus is asynchronous (2-4 seconds)
- MongoDB is updated immediately
- Between consensus and DB update, systems may be temporarily inconsistent
- Solved via `ledgerSynced` flag for inspections

#### **2. Dual-Write Pattern**
- Write to MongoDB (off-chain)
- Write to Blockchain (on-chain)
- If blockchain fails, MongoDB marks it with `ledgerSynced=false`
- No automatic retry (enhancement opportunity)

#### **3. CQRS (Command Query Responsibility Segregation)**
- **Writes** → Blockchain (immutable, durable)
- **Reads** → MongoDB (fast, indexed)
- Blockchain validates; MongoDB optimizes

#### **4. Event Sourcing**
- Every state change is recorded as an event in `eventHistory`
- Current state is derived from events
- Enables temporal queries ("what was owner at time X?")
- Perfect for audit & compliance

#### **5. Byzantine Fault Tolerance**
- Fabric requires >50% peer endorsement
- Malicious peers cannot unilaterally commit bad state
- Cryptographic signatures ensure integrity

#### **6. Reactive Programming** (Frontend)
- Angular Signals for state management
- Computed properties auto-derive from signals
- Template bindings auto-update
- No Redux or complex state libraries needed

#### **7. Stateless REST API**
- Each HTTP request is independent
- No session state on server
- Enables horizontal scaling (multiple middleware instances)

---

## Limitations

### Current Limitations

| Limitation | Reason | Future Work |
|-----------|--------|-------------|
| Single organization | Simplified demo setup | Multi-org with cross-chain consensus |
| No authentication | Demo purposes | OAuth 2.0, JWT tokens, role-based access |
| No transactional atomicity | Independent systems | Distributed transaction coordinator |
| No automatic retry | Scope constraints | Background message queue + replay |
| No private data collections | Simplified design | Fabric collectionconfiguration |
| File-based secret management | Development simplicity | HashiCorp Vault, AWS Secrets Manager |
| No payment workflow | Asset provenance focus | Stripe/PayPal integration |
| Single MongoDB instance | Demo deployment | Replica sets, sharding for production |

### Enhancement Opportunities

1. **Multi-Party Consensus**
   - Add multiple organizations to Fabric network
   - Implement cross-org approval workflows

2. **Automatic Retry & Recovery**
   - Kafka-based message queue for transactions
   - Dead-letter queue for failed operations
   - Time-travel queries to reconcile state

3. **Advanced Inspection Workflows**
   - Multi-stage inspection (authentication → grading → condition)
   - Confidence scoring for automations
   - Report digitally signed by inspectors

4. **Mobile & PWA Support**
   - Responsive design for inspection field teams
   - Offline support with service workers
   - QR code scanning for asset lookup

5. **Advanced Analytics**
   - Ownership network graphs
   - Inspection trend analysis
   - Fraud detection via machine learning

6. **Integration Capabilities**
   - Webhooks for external systems
   - GraphQL endpoint alongside REST
   - Event streaming (Kafka/RabbitMQ)

---

## Notes & References

### Configuration Files
- **Middleware:** `middleware/src/main/resources/application.yml`
- **Frontend API Client:** `frontend/src/app/core/services/luxury-goods-api.service.ts`
- **Fabric Credentials:** `.fabric/organizations/peerOrganizations/org1.example.com/`

### Environment Overrides
```bash
# Chaincode deployment
CHAINCODE_NAME=luxuryv2 CHAINCODE_VERSION=2.0 ./scripts/deploy-chaincode.sh

# Fabric network
FABRIC_VERSION=2.5.15 CA_VERSION=1.5.17 ./scripts/start-fabric.sh

# Frontend dev server
npm start -- --port 4300 --host 0.0.0.0
```

### Debugging

**View Middleware Logs:**
```bash
# If running directly:
tail -f /var/log/spring-boot.log

# If in Docker:
docker logs -f <middleware-container>
```

**View Fabric Logs:**
```bash
# Peer logs
docker logs <peer-container> | grep -i error

# Chaincode logs
docker logs <chaincode-container>
```

**MongoDB Queries:**
```bash
# Connect to MongoDB
mongo mongodb://localhost:27017

# View assets
db.assets.find().pretty()

# View inspections
db.inspection_reports.find().pretty()

# Count documents
db.assets.countDocuments()
```

---

## Documentation

For in-depth technical details, see the comprehensive documentation:
- [Chaincode Architecture](./docs/CHAINCODE_ARCHITECTURE.md)
- [Middleware Architecture](./docs/MIDDLEWARE_ARCHITECTURE.md)
- [Frontend Architecture](./docs/FRONTEND_ARCHITECTURE.md)
- [Architecture Diagrams](./docs/ARCHITECTURE_DIAGRAMS.md)

---

## References & Resources

- **Hyperledger Fabric:** https://hyperledger-fabric.readthedocs.io/
- **Spring Boot:** https://spring.io/projects/spring-boot
- **Angular:** https://angular.io/
- **MongoDB:** https://docs.mongodb.com/
- **RxJS:** https://rxjs.dev/
- **Genson JSON:** https://github.com/owlike/genson

---
