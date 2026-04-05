# Luxury Goods Chaincode Architecture

## Overview

The Chaincode layer is the immutable smart contract logic that runs on the Hyperledger Fabric network. It implements the core business logic for managing luxury asset lifecycle events on the blockchain, ensuring transaction permanence and auditability.

## What This Component Does

The chaincode provides four primary transaction functions that govern asset state transitions:

1. **registerAsset** - Mints a new asset on the ledger with initial metadata
2. **transferOwnership** - Records custodian changes with full historical tracking
3. **addInspectionReport** - Anchors inspection verification hashes to the asset record
4. **getAssetHistory** - Retrieves the complete immutable transaction history for an asset

## Why It Is Involved

Hyperledger Fabric requires chaincode to define the contract interface between applications and the blockchain. The chaincode serves as a trusted arbiter that:

- **Validates** all asset operations against business rules
- **Enforces** single-source-of-truth for ownership and event history
- **Provides** cryptographic proof of asset events through Fabric's consensus
- **Prevents** backdating or rewriting of historical events

## Role in the System

```
Frontend/API Layer 
      ↓
   Middleware (Java Spring Boot)
      ↓
   Fabric Gateway SDK
      ↓
   Chaincode (Java Contract)
      ↓
   Fabric Ledger & Peer Network
```

The chaincode is the authoritative keeper of asset state. All operational changes must flow through it, creating an immutable audit trail.

## Integration

**Upstream Integration:**
- **Fabric Gateway SDK** (Middleware) - Invokes chaincode transactions via gRPC
- External applications call chaincode methods indirectly through the middleware layer

**Downstream Integration:**
- **Fabric Ledger** - All state changes are persisted by Fabric's consensus mechanism
- **Fabric History Database** - Key modification history is automatically maintained by Fabric

## Implementation Details

### Technology Stack

- **Language:** Java 17
- **Framework:** Hyperledger Fabric Chaincode Library (org.hyperledger.fabric:fabric-chaincode-java)
- **Serialization:** Genson JSON library for state marshalling
- **Build Tool:** Maven 3.9+
- **Target Fabric Version:** 2.5.15+

### Core Components

#### 1. **AssetContract.java**
The primary contract class implementing `ContractInterface`.

**Key Methods:**
- `registerAsset(ctx, assetId, type, brand, owner)` - Submitting transaction
- `transferOwnership(ctx, assetId, newOwner)` - Submitting transaction
- `addInspectionReport(ctx, assetId, reportHash)` - Submitting transaction
- `getAssetHistory(ctx, assetId)` - Evaluating transaction (read-only)
- `assetExists(ctx, assetId)` - Evaluating transaction (private utility)

**Design Patterns:**
- **Command Pattern** - Each transaction method encapsulates a complete operation
- **Repository Pattern** - The ledger acts as the persistent store via `ctx.getStub()`
- **Error Handling Pattern** - Standard `ChaincodeException` for business logic violations

#### 2. **AssetState.java**
Immutable data class representing the current state of an asset on the ledger.

**Properties:**
- `assetId` - Unique business identifier (composite key for lookup)
- `type` - Asset category (e.g., "watch", "handbag", "jewelry")
- `brand` - Manufacturer/house name
- `owner` - Current custodian identifier
- `createdAt` - ISO 8601 timestamp of first registration
- `updatedAt` - ISO 8601 timestamp of most recent change
- `eventHistory` - Ordered list of `AssetEvent` records for audit trail

**Design Pattern:**
- **Value Object Pattern** - Immutable, fully self-contained, strongly typed
- **Timestamp Versioning** - Supports temporal queries and before/after state reconstruction

#### 3. **AssetEvent.java**
Immutable record of a state-changing operation with associated metadata.

**Event Types:**
```
REGISTERED:
  - Triggered: Asset first minted
  - Payload: timestamp, initialOwner
  - reportHash: null (no inspection at registration)

OWNERSHIP_TRANSFERRED:
  - Triggered: Custodian change
  - Payload: timestamp, previousOwner, newOwner
  - reportHash: null (ownership events don't reference inspections)

INSPECTION_RECORDED:
  - Triggered: New inspection verification added
  - Payload: timestamp, SHA-256 hash of inspection payload
  - reportHash: cryptographic link to off-chain inspection in MongoDB
```

**Design Pattern:**
- **Event Sourcing Pattern** - Complete event history enables event replay and audit compliance

#### 4. **AssetHistoryRecord.java**
Wrapper for Fabric's native key modification history with asset snapshots.

**Properties:**
- `txId` - Unique transaction identifier from the orderer
- `timestamp` - Consensus timestamp (ISO 8601)
- `deleted` - Boolean flag indicating soft-delete state
- `asset` - Complete `AssetState` snapshot at this point in history

**Purpose:**
- Enables the `getAssetHistory()` evaluation to return full historical snapshots
- Allows clients to reconstruct complete asset state at any point in time
- Supports forensics and audit trail reconstruction

## Design Decisions

### 1. **Event-Sourced Asset State**
**Decision:** Embed `eventHistory` list inside the asset state itself.

**Rationale:**
- Self-describing state: Reading any asset state immediately reveals its full event trail
- No need for separate history query in common demo/UI scenarios
- Coupled with Fabric's key history for complete auditability
- Simplifies debugging and on-chain verification

**Trade-offs:**
- Asset state size grows linearly with event count (not a concern for typical jewelry/asset counts)
- Cannot directly prune old events without losing history access (intentional immutability)

### 2. **Hashing at Middleware, Storage at Ledger**
**Decision:** Inspection reports are hashed in the middleware; only the hash is stored on-chain.

**Rationale:**
- Full inspection documents (potentially large, private) live off-chain in MongoDB
- Ledger stores only deterministic SHA-256 hash (small, immutable reference)
- Enables privacy: inspection details not visible to all network participants
- Blockchain confirms hashes without storing sensitive inspection data

**Trade-offs:**
- Requires dual-write: hash to ledger, full document to MongoDB
- Hash alone doesn't prove document authenticity (but combined with ledger signature, it does)
- Off-chain document must be reconstructed from MongoDB queries, not derived from ledger

### 3. **Genson for JSON Serialization**
**Decision:** Use Genson instead of Jackson or Gson.

**Rationale:**
- Lightweight dependency, minimal memory footprint
- Built-in support for Fabric's serialization patterns
- Deterministic JSON output (important for hash consistency)
- Zero-config usage for POJOs

**Trade-offs:**
- Fewer features than Jackson
- Smaller ecosystem of tutorials/documentation

### 4. **Timestamp Format (ISO 8601 strings)**
**Decision:** Store all timestamps as ISO 8601 formatted strings in the state.

**Rationale:**
- Human-readable format (aids debugging and log inspection)
- Language-agnostic interchange format
- Sortable as strings (lexicographically)
- Preserves nanosecond precision via `Instant.toString()`

**Trade-offs:**
- Slightly larger storage footprint than Unix epoch integers
- Requires parsing on deserialization

## Business Logic Flow

### Asset Registration Flow
```
Client Request (via Middleware)
    ↓
AssetContract.registerAsset()
    ├─ Check assetExists() → throw if duplicate
    ├─ Create AssetEvent.registered()
    ├─ Create new AssetState with eventHistory=[REGISTERED]
    └─ Persist via ctx.getStub().putStringState()
    ↓
Fabric Consensus
    ├─ Endorse on 3+ peers
    ├─ Orderer commits to block
    └─ State accessible to all peers
```

### Ownership Transfer Flow
```
Client Request → Middleware
    ↓
AssetContract.transferOwnership()
    ├─ Retrieve existing AssetState
    ├─ Create AssetEvent.ownershipTransferred()
    ├─ Append to eventHistory[]
    ├─ Update owner property
    ├─ Update updatedAt timestamp
    └─ Persist via putStringState()
    ↓
Fabric History DB
    └─ Automatically records key modification
```

### Asset History Query
```
Client Request → Middleware
    ↓
AssetContract.getAssetHistory()
    ├─ Validate asset exists first
    ├─ Iterate ctx.getStub().getHistoryForKey()
    ├─ For each modification:
    │   ├─ Deserialize snapshot if not deleted
    │   └─ Create AssetHistoryRecord(txId, timestamp, deleted, snapshot)
    └─ Serialize list to JSON and return
    ↓
Middleware
    └─ Returns to client as AssetHistoryResponse[]
```

## Data Structures

### AssetState JSON Example
```json
{
  "assetId": "LV-601-OMEGA",
  "type": "watch",
  "brand": "Omega",
  "owner": "Alice",
  "createdAt": "2026-04-04T15:42:31.123456789Z",
  "updatedAt": "2026-04-05T10:15:00.987654321Z",
  "eventHistory": [
    {
      "eventType": "REGISTERED",
      "timestamp": "2026-04-04T15:42:31.123456789Z",
      "previousOwner": null,
      "newOwner": "Alice",
      "reportHash": null
    },
    {
      "eventType": "OWNERSHIP_TRANSFERRED",
      "timestamp": "2026-04-04T16:30:22.111111111Z",
      "previousOwner": "Alice",
      "newOwner": "Bob",
      "reportHash": null
    },
    {
      "eventType": "INSPECTION_RECORDED",
      "timestamp": "2026-04-05T09:45:00.222222222Z",
      "previousOwner": null,
      "newOwner": null,
      "reportHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
  ]
}
```

### Key History Entry JSON Example
```json
{
  "txId": "62e362b0b7d5cd0cd8d46d926ee06a73a37bcd4078ffa1f7cc7b53ea8e85dd2e",
  "timestamp": "2026-04-04T15:42:31Z",
  "deleted": false,
  "asset": { /* complete AssetState object */ }
}
```

## Error Handling

### Business Rule Violations
- **Duplicate Asset** → `ChaincodeException` with `ASSET_ALREADY_EXISTS` error code
- **Asset Not Found** → `ChaincodeException` with `ASSET_NOT_FOUND` error code
- Middleware maps these to HTTP 409 (Conflict) and 404 (Not Found)

### Validation Strategy
- Minimize on-chain validation (defer to middleware when possible for performance)
- Validate only critical business rules on-chain (e.g., asset existence, ownership)
- Assume middleware has already validated field formats and lengths

## System Design Concepts

### 1. **Immutability & Auditability**
- All state changes leave an indelible trail via Fabric history
- Event history inside asset state provides self-documenting audit trail
- Perfect for compliance scenarios (luxury goods provenance, regulatory reporting)

### 2. **Eventual Consistency**
- Chaincode submits to endorsers first, then orderer, then broadcast to peers
- Between submit and commit, state is not yet finalized
- Middleware handles this via `ledgerSynced` flag in MongoDB

### 3. **Byzantine Fault Tolerance**
- Fabric consensus requires endorsement from over 50% of peers
- Malicious peers cannot unilaterally commit incorrect state
- Each asset transaction is cryptographically signed by the submitter

### 4. **Event Sourcing**
- Asset state is reconstructed from event history rather than direct mutations
- Enables temporal queries ("what was the owner at timestamp X?")
- Supports full audit trail reconstruction for compliance

## Performance Characteristics

### Write Operations
- **registerAsset, transferOwnership, addInspectionReport**
  - Latency: 1-3 seconds (depends on network, endorser response times)
  - Throughput: ~1000 TPS with 3-org network (Fabric benchmark)
  - Bottleneck: Orderer consensus, not chaincode execution

### Read Operations
- **assetExists, getAssetHistory**
  - Latency: 100-500ms (peer query only, no consensus needed)
  - Throughput: 10,000+ QPS per peer
  - Bottleneck: Network I/O, not ledger lookup

### State Growth
- Typical asset: ~2-5 KB per state
- With 10 events: ~3-8 KB
- With 100 events: ~20-40 KB
- Acceptable for typical jewelry/collectibles catalogs

## Testing Strategy

**Unit Tests:**
- Mock `Context` and `ChaincodeStub` to test business logic in isolation
- Verify event creation and state transitions
- Validate error handling for duplicate assets, missing assets

**Integration Tests:**
- Deploy chaincode to Fabric test network
- Execute full registration → transfer → inspection flow
- Validate on-chain state and history queries

**Compliance Tests:**
- Verify immutability: confirm past events cannot be altered
- Verify history: confirm complete event trail is accessible
- Verify timestamps: confirm event ordering matches transaction commit order

## Deployment

### Build Process
```bash
cd chaincode
../mvnw clean package
# Produces: target/chaincode.jar
```

### Deployment Script
```bash
./scripts/deploy-chaincode.sh
# Defaults:
#   - Chaincode name: luxuryasset
#   - Version: 1.0
#   - Sequence: 1
#   - Channel: mychannel
```

### Runtime Configuration
- **JVM Minimum Heap:** 256MB (recommended for test network)
- **Environment:** Fabric provides peer, orderer, CA information via environment variables
- **Logging:** Fabric captures stdout/stderr; use `System.out.println()` for debugging

## References & Documentation

- **Hyperledger Fabric Docs:** https://hyperledger-fabric.readthedocs.io/
- **Fabric Java Chaincode Tutorial:** https://hyperledger-fabric.readthedocs.io/en/latest/write_first_app.html
- **Fabric Sample Chaincodes:** https://github.com/hyperledger/fabric-samples
- **Genson Library:** https://github.com/owlike/genson
