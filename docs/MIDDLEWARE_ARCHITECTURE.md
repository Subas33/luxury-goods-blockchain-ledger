# Middleware (Spring Boot) Architecture

## Overview

The Middleware layer is a Spring Boot 3.3.5 REST API service that acts as the orchestration point between the frontend and blockchain. It coordinates blockchain operations, enforces business rules, manages off-chain data in MongoDB, and provides a developer-friendly REST interface.

## What This Component Does

The middleware provides a REST API with these core responsibilities:

1. **Asset Lifecycle Management**
   - Register new assets with metadata
   - Transfer ownership between custodians
   - Query current asset state and history

2. **Inspection Report Workflow**
   - Hash inspection payloads at rest (SHA-256)
   - Store full inspection documents in MongoDB
   - Anchor inspection hashes to blockchain
   - Track synchronization status between MongoDB and Fabric

3. **Data Synchronization**
   - Maintain MongoDB projection of on-chain asset state
   - Filter MongoDB data against Fabric existence (prevent stale entries)
   - Mark inspection reports with `ledgerSynced` flag for recovery

4. **Error Handling**
   - Map blockchain exceptions to HTTP status codes
   - Provide detailed error responses to clients
   - Prevent duplicate asset registrations

## Why It Is Involved

Hyperledger Fabric's Chaincode interface is low-level (byte[] serializable only). The middleware abstracts this complexity by:

- **Type Safety:** Strong typing with DTOs and response objects
- **Validation:** Enforce request constraints before hitting blockchain (saves gas-equivalent latency)
- **Bridging:** Connect stateless HTTP clients to stateful blockchain
- **Transformation:** Convert between HTTP/JSON and Fabric's gRPC/byte protocols
- **Privacy:** Keep large inspection documents off-chain while leveraging blockchain's auditability

## Role in the System

```
Frontend (Angular)
      ↓ HTTP REST
Spring Boot Middleware (8080)
      ├─→ MongoDB (asset projection, inspection documents)
      └─→ Fabric Gateway SDK
            ↓
         Chaincode Contract
            ↓
         Fabric Ledger
```

The middleware is the **single point of coordination** between frontend, database, and blockchain.

## Integration

### Upstream (Clients)
- **Angular Frontend** - HTTP REST calls to `/assets` endpoints
- **Browser** - CORS-enabled, calls from `http://localhost:4200`
- **External APIs** - Any HTTP client (curl, Postman, mobile apps)

### Downstream (Backend)
- **MongoDB** - Spring Data repositories for asset and inspection storage
- **Fabric Gateway SDK** - Java SDK for blockchain interaction
- **Fabric Network** - Endorsers, orderer, peers communicate via Gateway

## Implementation Details

### Technology Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.3.5
- **Web:** Spring MVC (REST endpoints via `@RestController`)
- **Data:** Spring Data MongoDB (ORM for MongoDB)
- **Blockchain:** Hyperledger Fabric Java SDK 1.10.1
- **Serialization:** Jackson 2.15+
- **Build Tool:** Maven 3.9+
- **Container Runtime:** Docker (runs on localhost:8080)

### Core Components

#### 1. **AssetController.java**
REST endpoint definitions for asset CRUD and inspection operations.

**Endpoints:**

| Method | Path | Operation |
|--------|------|-----------|
| GET | `/assets` | List all assets with summary information |
| POST | `/assets` | Register a new asset |
| GET | `/assets/{id}` | Get current asset state |
| POST | `/assets/{id}/transfer` | Transfer ownership to new custodian |
| POST | `/assets/{id}/inspection` | Add inspection report with hash |
| GET | `/assets/{id}/history` | Get immutable asset history from blockchain |
| GET | `/assets/{id}/inspections` | Get full off-chain inspection reports |

**Design Patterns:**
- **REST Resource Pattern** - Standard HTTP verbs (GET=read, POST=write)
- **Resource Identifier Pattern** - Assets uniquely identified by `{id}`
- **HATEOAS-lite** - Response includes complete resource state (no hypermedia for simplicity)

#### 2. **AssetService.java**
Business logic orchestration and database operations.

**Key Methods:**
- `registerAsset()` - Validates, calls blockchain, updates MongoDB projection
- `transferOwnership()` - Orchestrates ownership change on both blockchain and off-chain
- `addInspectionReport()` - Hash generation, dual-storage (MongoDB + blockchain), sync tracking
- `listAssets()` - Query MongoDB projection, filter by blockchain existence
- `getAsset()` - Single asset fetch with existence check
- `getInspectionReports()` - Off-chain document retrieval from MongoDB
- `getAssetHistory()` - Proxy to FabricLedgerService

**Design Patterns:**
- **Service Layer Pattern** - Encapsulates business logic, dependencies injected via constructor
- **Repository Pattern** - Abstracts data access via Spring Data
- **Data Transfer Object (DTO)** - Request/response objects separate from domain models
- **Mapper Pattern** - Helper methods (`toAssetSummary()`, `toInspectionReportResponse()`) transform internal models

**Key Business Logic:**

```java
// Example: addInspectionReport() flow
1. Generate SHA-256 hash of inspection payload
2. Create MongoDB document with inspection data
3. Save to MongoDB (marked as ledgerSynced=false initially)
4. Call FabricLedgerService.addInspectionReport(hash)
5. Update MongoDB document: ledgerSynced=true, ledgerSyncedAt=now()
6. Return inspection response to client
```

**Data Synchronization Strategy:**
- MongoDB acts as a read-optimized projection of on-chain state
- On asset registration/transfer, upsert MongoDB with latest state
- On listing assets, filter MongoDB results against blockchain existence
- Prevents stale entries if blockchain network is reset but MongoDB persists

#### 3. **FabricLedgerService.java**
Low-level Fabric Gateway SDK wrapper.

**Key Methods:**
- `registerAsset()` - Invokes chaincode `registerAsset` transaction
- `transferOwnership()` - Invokes chaincode `transferOwnership` transaction
- `addInspectionReport()` - Invokes chaincode `addInspectionReport` transaction
- `getAssetHistory()` - Invokes chaincode `getAssetHistory` evaluation
- `assetExists()` - Checks asset existence on ledger
- `mapFabricException()` - Translates Fabric errors to application exceptions

**Transaction Flow:**

```
submit("registerAsset", assetId, type, brand, owner)
    ↓
contract.submitTransaction()  // Submits to endorser nodes
    ↓
Returns byte[] payload from chaincode
    ↓
objectMapper.readValue()     // Deserialize to AssetResponse
```

**Error Mapping:**
- Fabric `ASSET_ALREADY_EXISTS` → `DuplicateAssetException`
- Fabric `ASSET_NOT_FOUND` → `ResourceNotFoundException`
- Other Fabric exceptions → `FabricClientException`

**Design Patterns:**
- **Adapter Pattern** - Wraps low-level Fabric SDK into application-friendly methods
- **Exception Translation Pattern** - Converts Fabric exceptions to application-specific ones

#### 4. **InspectionHashService.java**
Cryptographic hashing for inspection payloads.

**Key Methods:**
- `generateHash(AddInspectionReportRequest)` - Serializes request to JSON, applies SHA-256

**Design Pattern:**
- **Strategy Pattern** - Hashing algorithm encapsulated; can swap SHA-256 for SHA-512, etc.

**Important:** Hash must be deterministic for the same payload (MongoDB query by hash).

#### 5. **FabricGatewayConfig.java**
Spring `@Configuration` class that sets up Fabric Gateway beans.

**Beans Created:**
- `ManagedChannel` - gRPC connection to Fabric peer endpoint
- `Identity` - User identity credentials (X.509 certificate)
- `Signer` - Private key signer for transaction endorsement
- `Gateway` - Main gateway object, manages connection lifecycle
- `Contract` - Chaincode contract proxy for transactions

**Security Considerations:**
- Private key and certificate loaded from filesystem paths
- In production, should use HashiCorp Vault or AWS Secrets Manager
- Current setup uses local file paths (development/demo only)

**Design Pattern:**
- **Builder Pattern** - `Gateway.newInstance()` uses fluent API
- **Factory Pattern** - Config class produces Gateway and Contract beans

#### 6. **Global Exception Handler**
Centralized exception handling via Spring `@RestControllerAdvice`.

**Handles:**
- `DuplicateAssetException` → HTTP 409 (Conflict)
- `ResourceNotFoundException` → HTTP 404 (Not Found)
- `FabricClientException` → HTTP 502 (Bad Gateway)
- `MethodArgumentNotValidException` → HTTP 400 (Bad Request)
- Generic `Exception` → HTTP 500 (Internal Server Error)

**Design Pattern:**
- **Aspect-Oriented Programming (AOP)** - Exception handling decoupled from business logic

### Data Models

#### MongoDB Models

**AssetDocument**
```java
@Document("assets")
public class AssetDocument {
    @Id
    private ObjectId id;                    // MongoDB internal ID
    private String assetId;                 // Business key (unique index)
    private String type;                    // Asset type
    private String brand;                   // Brand/manufacturer
    private String owner;                   // Current custodian
    private String latestInspectionHash;   // Link to latest inspection
    private Instant createdAt;             // When asset was first registered
    private Instant updatedAt;             // When asset was last modified
}
```

**InspectionReportDocument**
```java
@Document("inspection_reports")
public class InspectionReportDocument {
    @Id
    private ObjectId id;
    private String assetId;         // Foreign key to asset
    private String reportHash;      // SHA-256 hash (unique index)
    private String inspector;       // Who performed the inspection
    private String status;          // PASSED, FAILED, PENDING, etc.
    private String location;        // Where inspection occurred
    private String notes;           // Inspection comments
    private Instant inspectedAt;   // When inspection was performed
    @org.springframework.data.mongodb.core.mapping.Field
    private Map<String, Object> metadata;  // Additional inspection data
    private Instant storedAt;      // When document was created
    private boolean ledgerSynced;   // Whether hash is on blockchain
    private Instant ledgerSyncedAt; // When sync occurred
}
```

**Indexes:**
- `assetId` - Unique for assets, non-unique for inspections
- `reportHash` - Unique for inspection reports
- `inspectedAt` - For sorting inspections by date
- `ledgerSynced` - For querying unsync'd reports

#### Request DTOs

**CreateAssetRequest**
```java
public record CreateAssetRequest(
    String assetId,
    String type,
    String brand,
    String owner
) {}
```

**TransferOwnershipRequest**
```java
public record TransferOwnershipRequest(
    String newOwner
) {}
```

**AddInspectionReportRequest**
```java
public record AddInspectionReportRequest(
    String inspector,
    String status,
    String location,
    String notes,
    OffsetDateTime inspectedAt,
    Map<String, Object> metadata
) {}
```

#### Response DTOs

**AssetResponse** - Chaincode response
```java
public record AssetResponse(
    String assetId,
    String type,
    String brand,
    String owner,
    String createdAt,
    String updatedAt,
    List<AssetEventResponse> eventHistory
) {}
```

**AssetSummaryResponse** - Aggregated view (MongoDB + chaincode)
```java
public record AssetSummaryResponse(
    String assetId,
    String type,
    String brand,
    String owner,
    String latestInspectionHash,
    Instant createdAt,
    Instant updatedAt,
    long inspectionCount,
    String latestInspectionStatus,
    Instant latestInspectionDate
) {}
```

**Design Pattern:**
- **DTO Pattern** - Decouples API contracts from internal models

## Configuration Management

### application.yml
Spring Boot configuration mapped to `FabricGatewayProperties`.

```yaml
app:
  fabric:
    peer:
      endpoint: localhost:7051
      hostAlias: peer0.org1.example.com
    channel: mychannel
    chaincodeName: luxuryasset
    mspId: Org1MSP
    certificateDirectory: /path/to/certs/signcerts
    privateKeyDirectory: /path/to/certs/keystore
    tlsCertificatePath: /path/to/peer-tlscacerts.pem
    timeouts:
      evaluate: 10s
      endorse: 30s
      submit: 30s
      commitStatus: 5m
```

### Environment Variables
Override properties for different deployments:
```bash
FABRIC_PEER_ENDPOINT=fabric-peer:7051
FABRIC_CHANNEL=productionChannel
FABRIC_CHAINCODE_NAME=luxuryv2
```

## REST API Examples

### Register Asset
```bash
POST /assets
Content-Type: application/json

{
  "assetId": "LV-601",
  "type": "watch",
  "brand": "Omega Seamaster",
  "owner": "Collector A"
}

Response: 200 OK
{
  "assetId": "LV-601",
  "type": "watch",
  "brand": "Omega Seamaster",
  "owner": "Collector A",
  "createdAt": "2026-04-04T15:42:31Z",
  "updatedAt": "2026-04-04T15:42:31Z",
  "eventHistory": [...]
}
```

### Add Inspection
```bash
POST /assets/LV-601/inspection
Content-Type: application/json

{
  "inspector": "LuxeVerify Lab A",
  "status": "PASSED",
  "location": "Geneva",
  "notes": "Serial number verified, no authenticity concerns",
  "inspectedAt": "2026-04-05T10:00:00Z",
  "metadata": {
    "serialMatch": true,
    "condition": "excellent"
  }
}

Response: 200 OK
{
  "id": "...",
  "assetId": "LV-601",
  "reportHash": "e3b0c4...",
  "inspector": "LuxeVerify Lab A",
  "status": "PASSED",
  "ledgerSynced": true,
  "ledgerSyncedAt": "2026-04-05T10:00:01Z"
}
```

### Get Asset History
```bash
GET /assets/LV-601/history

Response: 200 OK
{
  "assetId": "LV-601",
  "history": [
    {
      "txId": "62e362...",
      "timestamp": "2026-04-04T15:42:31Z",
      "deleted": false,
      "asset": { /* full AssetState */ }
    },
    {
      "txId": "abc123...",
      "timestamp": "2026-04-05T10:00:00Z",
      "deleted": false,
      "asset": { /* full AssetState after inspection */ }
    }
  ]
}
```

## Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Service Layer** | AssetService, FabricLedgerService | Encapsulate business logic, enable reuse |
| **Repository** | AssetRepository, InspectionReportRepository | Abstract data access from business logic |
| **DTO** | Request/response records | Decouple API contracts from domain models |
| **Adapter** | FabricLedgerService wraps Fabric SDK | Simplify low-level SDK complexity |
| **Factory** | FabricGatewayConfig | Create complex objects (Gateway, Contract) |
| **Builder** | Gateway.newInstance() | Fluent API for constructing objects |
| **Mapper** | toAssetSummary(), toInspectionReport() | Transform between data models |
| **Exception Translation** | FabricLedgerService.mapFabricException() | Map domain exceptions to application-specific ones |
| **Aspect-Oriented** | GlobalExceptionHandler | Centralize cross-cutting concerns |

## System Design Concepts

### 1. **Eventual Consistency**
- Asset registration is asynchronous:
  1. Blockchain consensus (orderer commits block)
  2. Peers apply block to state database
  3. Middleware updates MongoDB projection
- Between steps, MongoDB and Fabric may be inconsistent
- Solved via `ledgerSynced` flag for inspections and timestamp comparisons

### 2. **Dual-Write Consistency**
- Inspection workflow writes to both MongoDB and blockchain
- Mitigated by:
  - Save to MongoDB first (marked `ledgerSynced=false`)
  - Submit to blockchain
  - Update MongoDB (set `ledgerSynced=true`)
  - If blockchain fails, MongoDB marks it; client can retry

### 3. **Read Optimization**
- MongoDB projection enables fast reads without blockchain latency
- `listAssets()` filters by blockchain existence (could become stale if blockchain resets)
- Single-asset queries hit both MongoDB (details) and blockchain (validation)

### 4. **Statelessness**
- Each HTTP request is independent
- No session state maintained server-side
- Enables horizontal scaling (multiple middleware instances)

### 5. **Command Segregation**
- Write operations (POST) → Blockchain + MongoDB
- Read operations (GET) → MongoDB (with blockchain validation) or Blockchain-only
- Clear separation enables future caching strategies

## Performance Characteristics

### Latency
| Operation | Typical Latency | Bottleneck |
|-----------|-----------------|-----------|
| Register Asset | 2-4 seconds | Blockchain consensus |
| Transfer Ownership | 2-4 seconds | Blockchain consensus |
| Add Inspection | 2-4 seconds | Blockchain consensus |
| List Assets | 200-500ms | MongoDB query |
| Get Asset | 300-800ms | MongoDB + blockchain |
| Get History | 1-2s | Blockchain history query |

### Scaling
- Single middleware instance can handle ~100 concurrent requests
- MongoDB can handle ~10,000 queries/sec
- Blockchain throughput: ~1000 TPS with 3-org network
- Recommendation: Use horizontal load balancing for production

## Testing Strategy

### Unit Tests
- Mock dependencies (FabricLedgerService, repositories)
- Test business logic in AssetService
- Verify DTO mappings

### Integration Tests
- Use `@DataMongoTest` for repository tests
- Use `@WebMvcTest` for controller tests with mock services
- Use testcontainers for real MongoDB instance

### End-to-End Tests
- Deploy middleware + Fabric test network
- Execute full registration → inspection → history workflow
- Validate REST responses and MongoDB state

## Deployment

### Build
```bash
cd middleware
../mvnw clean package
# Produces: target/luxury-goods-middleware-1.0.0.jar
```

### Run
```bash
java -jar target/luxury-goods-middleware-1.0.0.jar
# Runs on http://localhost:8080
```

### Docker
```bash
docker run -p 8080:8080 \
  -e FABRIC_PEER_ENDPOINT=fabric-peer:7051 \
  -v /path/to/fabric-creds:/fabric-creds \
  luxury-goods-middleware:1.0.0
```

## References

- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **Spring Data MongoDB:** https://spring.io/projects/spring-data-mongodb
- **Fabric Java SDK:** https://github.com/hyperledger/fabric-sdk-java
- **Jackson Documentation:** https://github.com/FasterXML/jackson
