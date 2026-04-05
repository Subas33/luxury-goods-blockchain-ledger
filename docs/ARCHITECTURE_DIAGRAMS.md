# Architecture Diagrams

## System Architecture Overview

```mermaid
graph TB
    subgraph Client Layer
        Browser["🌐 Web Browser<br/>localhost:4200"]
    end
    
    subgraph Frontend Layer
        UI["Angular Frontend<br/>(Standalone Components)"]
        HomePage["Home Page<br/>(Catalog, Search)"]
        DetailPage["Detail Page<br/>(Asset Operations)"]
    end
    
    subgraph Middleware Layer
        API["Spring Boot API<br/>localhost:8080"]
        AssetSvc["AssetService<br/>(Orchestration)"]
        FabricSvc["FabricLedgerService<br/>(Blockchain)"]
        HashSvc["InspectionHashService<br/>(SHA-256)"]
    end
    
    subgraph Data Layer
        MongoDB["MongoDB<br/>(Asset Projection)"]
        InspectionDB["MongoDB<br/>(Inspection Reports)"]
    end
    
    subgraph Blockchain Layer
        FabricGW["Fabric Gateway SDK<br/>(Java Client)"]
        Peer["Fabric Peers<br/>(Endorsers)"]
        Orderer["Fabric Orderer<br/>(Consensus)"]
        Ledger["Ledger<br/>(Immutable State)"]
    end
    
    Browser -->|HTTP REST| UI
    UI --> HomePage
    UI --> DetailPage
    HomePage -->|API Calls| API
    DetailPage -->|API Calls| API
    
    API --> AssetSvc
    AssetSvc --> FabricSvc
    AssetSvc --> HashSvc
    AssetSvc --> MongoDB
    AssetSvc --> InspectionDB
    
    FabricSvc --> FabricGW
    FabricGW -->|gRPC| Peer
    Peer -->|Consensus| Orderer
    Orderer -->|Commit Block| Ledger
    Orderer -->|Broadcast| Peer
    
    style Browser fill:#e1f5ff
    style UI fill:#f3e5f5
    style API fill:#fff3e0
    style MongoDB fill:#e8f5e9
    style FabricGW fill:#ffe0b2
    style Ledger fill:#ffccbc
```

## Component Component Interaction Diagram

```mermaid
graph LR
    subgraph "User Interface"
        A["Asset Register Form"]
        B["Search & Filter"]
        C["Transfer Form"]
        D["Inspection Form"]
    end
    
    subgraph "Service Layer"
        E["AssetService"]
        F["Validation Service"]
    end
    
    subgraph "Data Access"
        G["AssetRepository"]
        H["InspectionRepository"]
        I["FabricLedgerService"]
    end
    
    subgraph "External Systems"
        J["MongoDB"]
        K["Fabric Network"]
    end
    
    A -->|registerAsset| E
    C -->|transferOwnership| E
    D -->|addInspection| E
    B -->|listAssets| E
    
    E --> F
    E --> G
    E --> H
    E --> I
    
    G -->|CRUD| J
    H -->|CRUD| J
    I -->|Submit/Evaluate| K
    
    style A fill:#bbdefb
    style E fill:#fff9c4
    style J fill:#c8e6c9
    style K fill:#ffe0b2
```

## Asset Registration Flow

```mermaid
sequenceDiagram
    Actor User
    participant Frontend as Angular UI
    participant Middleware as Spring Boot API
    participant Fabric as Fabric Network
    participant MongoDB as MongoDB
    
    User->>Frontend: Submit Registration Form
    Frontend->>Middleware: POST /assets (CreateAssetRequest)
    
    Middleware->>Middleware: Validate request
    Middleware->>Fabric: Submit registerAsset transaction
    
    par Blockchain Consensus
        Fabric->>Fabric: Endorse transaction (peers)
        Fabric->>Fabric: Order transaction (orderer)
        Fabric->>Fabric: Commit to ledger
    end
    
    Fabric-->>Middleware: Return AssetResponse (JSON)
    
    par Off-Chain Storage
        Middleware->>MongoDB: Upsert asset document
        MongoDB-->>Middleware: Success
    end
    
    Middleware-->>Frontend: 200 OK (AssetResponse)
    Frontend-->>User: Display success message
```

## Ownership Transfer Flow

```mermaid
sequenceDiagram
    Actor User
    participant Frontend as Angular UI
    participant Middleware as Spring Boot
    participant Fabric as Blockchain
    participant MongoDB as MongoDB
    
    User->>Frontend: Select new owner + click Transfer
    Frontend->>Middleware: POST /assets/{id}/transfer
    
    Middleware->>Fabric: Call transferOwnership(assetId, newOwner)
    Fabric->>Fabric: Validate asset exists
    Fabric->>Fabric: Create OWNERSHIP_TRANSFERRED event
    Fabric->>Fabric: Update owner + eventHistory
    Fabric->>Fabric: Commit & consensus
    
    Fabric-->>Middleware: Return updated AssetState
    
    Middleware->>MongoDB: Update asset document (owner)
    MongoDB-->>Middleware: Done
    
    Middleware-->>Frontend: 200 OK with updated asset
    Frontend->>Frontend: Refresh asset view
    Frontend-->>User: Show confirmation + new owner
```

## Inspection Report Workflow

```mermaid
sequenceDiagram
    Actor User
    participant Frontend as Angular UI
    participant Middleware as Spring Boot
    participant Hash as HashService
    participant MongoDB as MongoDB
    participant Fabric as Blockchain
    
    User->>Frontend: Fill inspection form + submit
    Frontend->>Middleware: POST /assets/{id}/inspection
    
    Middleware->>Hash: generateHash(inspectionPayload)
    Hash-->>Middleware: SHA-256 hash
    
    Middleware->>MongoDB: Create inspection document<br/>(ledgerSynced=false)
    MongoDB-->>Middleware: Document created
    
    Middleware->>Fabric: Submit addInspectionReport(hash)
    
    par Blockchain Processing
        Fabric->>Fabric: Validate asset exists
        Fabric->>Fabric: Add INSPECTION_RECORDED event
        Fabric->>Fabric: Append to eventHistory
        Fabric->>Fabric: Consensus & commit
    end
    
    Fabric-->>Middleware: Success
    
    Middleware->>MongoDB: Update inspection document<br/>(ledgerSynced=true)
    MongoDB-->>Middleware: Updated
    
    Middleware-->>Frontend: Return InspectionReportResponse
    Frontend-->>User: Show success + full inspection details
```

## Asset History Query Flow

```mermaid
sequenceDiagram
    Actor User
    participant Frontend as Angular UI
    participant Middleware as Spring Boot
    participant Fabric as Blockchain
    participant Ledger as Ledger DB
    
    User->>Frontend: Click "View History"
    Frontend->>Middleware: GET /assets/{id}/history
    
    Middleware->>Fabric: Call getAssetHistory(assetId)
    
    Fabric->>Ledger: Query key history for assetId
    Ledger-->>Fabric: [KeyModification]
    
    loop For each modification
        Fabric->>Fabric: Deserialize AssetState from JSON
        Fabric->>Fabric: Create AssetHistoryRecord
        Note over Fabric: {txId, timestamp, deleted, asset}
    end
    
    Fabric-->>Middleware: Return JSON array (AssetHistoryResponse)
    
    Middleware-->>Frontend: 200 OK with history array
    
    Frontend->>Frontend: Transform for display
    Frontend-->>User: Show timeline with:<br/>- Transaction IDs<br/>- Timestamps<br/>- Event descriptions<br/>- Asset snapshots
```

## Data Synchronization Between MongoDB and Fabric

```mermaid
stateDiagram-v2
    [*] --> Initial: Asset created
    
    Initial --> OnChain: Submit registerAsset to Fabric
    OnChain --> Consensus: Endorsement + Ordering
    Consensus --> Committed: Block committed to ledger
    Committed --> OffChainProjection: Upsert MongoDB\n(AssetDocument)
    OffChainProjection --> ConsistentState: Both systems consistent
    
    ConsistentState --> InspectionEvent: Add inspection report
    InspectionEvent --> HashGenerated: Generate SHA-256 hash
    HashGenerated --> MongoSave1: Save to MongoDB\n(ledgerSynced=false)
    MongoSave1 --> FabricSubmit: Submit hash to blockchain
    FabricSubmit --> FabricConsensus: Consensus
    FabricCommitted: Block committed
    FabricConsensus --> FabricCommitted
    FabricCommitted --> MongoSync: Update MongoDB\n(ledgerSynced=true)
    MongoSync --> SyncedState: Inspection synced
    
    SyncedState --> ListAssets: Query /assets
    ListAssets --> MongoQuery: Read from MongoDB
    MongoQuery --> FilterByFabric: Check asset exists on Fabric
    FilterByFabric --> ReturnProjection: Return filtered results
    ReturnProjection --> Frontend: Send to ng app
```

## Design Pattern Visualization

```mermaid
graph TB
    subgraph "Architectural Patterns"
        A["Service Layer Pattern<br/>(AssetService)"]
        B["Repository Pattern<br/>(Spring Data)"]
        C["DTO Pattern<br/>(Request/Response)"]
    end
    
    subgraph "Behavioral Patterns"
        D["Adapter Pattern<br/>(FabricLedgerService)"]
        E["Factory Pattern<br/>(FabricGatewayConfig)"]
        F["Builder Pattern<br/>(Gateway.newInstance)"]
    end
    
    subgraph "Data Patterns"
        G["Event Sourcing<br/>(eventHistory[])"]
        H["CQRS Pattern<br/>(Read from MongoDB,<br/>Write to Blockchain)"]
        I["Value Object Pattern<br/>(AssetState)"]
    end
    
    subgraph "Concurrency Patterns"
        J["Eventual Consistency<br/>(ledgerSynced flag)"]
        K["Dual-Write Pattern<br/>(MongoDB + Fabric)"]
    end
    
    A --> B
    B --> C
    D --> E
    E --> F
    G --> H
    H --> I
    I --> J
    J --> K
```

## Middleware Layer Class Diagram

```mermaid
classDiagram
    class AssetController {
        -assetService: AssetService
        +getAssets(): List<AssetSummaryResponse>
        +postAsset(CreateAssetRequest): AssetResponse
        +getAsset(id): AssetSummaryResponse
        +postTransfer(id, TransferOwnershipRequest): AssetResponse
        +postInspection(id, AddInspectionReportRequest): InspectionReportResponse
        +getHistory(id): AssetHistoryResponse
        +getInspections(id): List<InspectionReportDetailResponse>
    }
    
    class AssetService {
        -assetRepository: AssetRepository
        -inspectionReportRepository: InspectionReportRepository
        -fabricLedgerService: FabricLedgerService
        -inspectionHashService: InspectionHashService
        +registerAsset(CreateAssetRequest): AssetResponse
        +transferOwnership(id, TransferOwnershipRequest): AssetResponse
        +addInspectionReport(id, AddInspectionReportRequest): InspectionReportResponse
        +listAssets(): List<AssetSummaryResponse>
        +getAsset(id): AssetSummaryResponse
        +getInspectionReports(id): List<InspectionReportDetailResponse>
        -upsertAssetDocument(AssetResponse, hash)
        -ensureLedgerAssetExists(id)
    }
    
    class FabricLedgerService {
        -contract: Contract
        -objectMapper: ObjectMapper
        +registerAsset(CreateAssetRequest): AssetResponse
        +transferOwnership(id, newOwner): AssetResponse
        +addInspectionReport(id, hash): AssetResponse
        +getAssetHistory(id): AssetHistoryResponse
        +assetExists(id): boolean
        -submit(method, args...): byte[]
        -evaluate(method, args...): byte[]
        -mapFabricException(method, exception): RuntimeException
    }
    
    class InspectionHashService {
        +generateHash(AddInspectionReportRequest): String
    }
    
    class AssetRepository {
        +findByAssetId(id): Optional<AssetDocument>
        +findAllByOrderByUpdatedAtDesc(): List<AssetDocument>
        +save(document): AssetDocument
        +delete(document)
    }
    
    class InspectionReportRepository {
        +findByAssetId(id): List<InspectionReportDocument>
        +findFirstByAssetIdOrderByInspectedAtDesc(id): Optional<InspectionReportDocument>
        +deleteByAssetId(id)
        +save(document): InspectionReportDocument
        +countByAssetId(id): long
    }
    
    class AssetDocument {
        -id: ObjectId
        -assetId: String
        -type: String
        -brand: String
        -owner: String
        -latestInspectionHash: String
        -createdAt: Instant
        -updatedAt: Instant
    }
    
    class InspectionReportDocument {
        -id: ObjectId
        -assetId: String
        -reportHash: String
        -inspector: String
        -status: String
        -location: String
        -notes: String
        -inspectedAt: Instant
        -metadata: Map
        -storedAt: Instant
        -ledgerSynced: boolean
        -ledgerSyncedAt: Instant
    }
    
    AssetController --> AssetService
    AssetService --> FabricLedgerService
    AssetService --> InspectionHashService
    AssetService --> AssetRepository
    AssetService --> InspectionReportRepository
    AssetRepository --> AssetDocument
    InspectionReportRepository --> InspectionReportDocument
```

## Chaincode State Transition Diagram

```mermaid
stateDiagram-v2
    [*] --> NonExistent: Asset not created
    
    NonExistent --> Registered: registerAsset(id, type, brand, owner)
    
    Registered --> OwnershipChange: transferOwnership(newOwner)
    OwnershipChange --> OwnershipChange: transferOwnership(newOwner)
    
    Registered --> InspectionAdded: addInspectionReport(hash)
    InspectionAdded --> InspectionAdded: addInspectionReport(hash)
    
    OwnershipChange --> InspectionAdded: addInspectionReport(hash)
    InspectionAdded --> OwnershipChange: transferOwnership(newOwner)
```

## Fabric Network Topology

```mermaid
graph TB
    subgraph "Fabric Channel"
        direction TB
        
        subgraph "Org1"
            P1["Peer1"]
            P2["Peer2"]
            A1["Anchor Peer"]
        end
        
        subgraph "Org2"
            P3["Peer3"]
            P4["Peer4"]
            A2["Anchor Peer"]
        end
        
        subgraph "Org3"
            P5["Peer5"]
            P6["Peer6"]
            A3["Anchor Peer"]
        end
        
        O["Orderer<br/>(Consensus)"]
    end
    
    subgraph "Client Layer"
        MW["Middleware<br/>(Java Client)"]
        GW["Fabric Gateway SDK"]
    end
    
    P1 -.Endorsed.-> O
    P2 -.Endorsed.-> O
    P3 -.Endorsed.-> O
    P4 -.Endorsed.-> O
    P5 -.Endorsed.-> O
    P6 -.Endorsed.-> O
    
    O -->|Broadcast Block| P1
    O -->|Broadcast Block| P2
    O -->|Broadcast Block| P3
    O -->|Broadcast Block| P4
    O -->|Broadcast Block| P5
    O -->|Broadcast Block| P6
    
    MW --> GW
    GW -->|gRPC| A1
    GW -->|gRPC| A2
    GW -->|gRPC| A3
```

## Error Handling Flow

```mermaid
graph TD
    A["API Request"]
    B["Validate Request"]
    C{"Valid?"}
    D["Call Business Logic"]
    E["Execute Chaincode"]
    F["Blockchain Response"}
    
    B --> C
    C -->|Invalid| G["BadRequestException"]
    C -->|Valid| D
    D --> E
    E --> F
    
    F -->|Success| H["200 OK"]
    F -->|Duplicate| I["DuplicateAssetException<br/>→ 409 Conflict"]
    F -->|Not Found| J["ResourceNotFoundException<br/>→ 404 Not Found"]
    F -->|Other Error| K["FabricClientException<br/>→ 502 Bad Gateway"]
    
    G -->|GlobalExceptionHandler| L["400 Bad Request"]
    I -->|GlobalExceptionHandler| M["409 Conflict"]
    J -->|GlobalExceptionHandler| N["404 Not Found"]
    K -->|GlobalExceptionHandler| O["502 Bad Gateway"]
    
    H --> P["Return to Client"]
    L --> P
    M --> P
    N --> P
    O --> P
    
    style H fill:#90EE90
    style L fill:#FFB6C6
    style M fill:#FFB6C6
    style N fill:#FFB6C6
    style O fill:#FFB6C6
```
