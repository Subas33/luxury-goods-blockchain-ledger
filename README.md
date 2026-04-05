# Luxury Goods Blockchain Prototype

Complete demo prototype for registering luxury goods on Hyperledger Fabric, managing off-chain inspection reports in MongoDB, exposing the workflow through a Java Spring Boot middleware, and presenting the demo through an Angular luxury-goods storefront.

## Architecture

The prototype is split into five runtime pieces:

1. Hyperledger Fabric test network
   - Runs the ledger, peers, orderer, and Fabric Gateway service.
   - Stores the current asset state and immutable key history on-chain.
2. Java chaincode
   - Implements `registerAsset`, `transferOwnership`, `addInspectionReport`, and `getAssetHistory`.
   - Persists asset metadata plus an embedded event trail inside each asset state.
3. Spring Boot middleware
   - Validates REST requests.
   - Hashes inspection report payloads with SHA-256.
   - Stores the full inspection document in MongoDB.
   - Invokes Fabric chaincode through the Fabric Gateway Java SDK.
4. MongoDB
   - Stores the latest off-chain asset projection.
   - Stores the full inspection reports that are only represented on-chain by their hash.
5. Angular frontend
   - Presents a demo ecommerce-style collection view and asset dossier page.
   - Calls the Spring Boot APIs directly from the browser.
   - Lets a demo user register assets, seed showcase inventory, transfer ownership, and add inspections without leaving the UI.

### Flow

```text
Angular UI / API client -> Spring Boot API -> MongoDB (full inspection report / asset projection)
                                     \
                                      -> Fabric Gateway SDK -> Fabric chaincode -> Ledger
```

## Project Structure

```text
.
|-- .mvn/
|-- chaincode/
|   |-- pom.xml
|   `-- src/main/java/com/luxurygoods/blockchain/chaincode/
|-- frontend/
|   |-- angular.json
|   `-- src/app/
|-- middleware/
|   |-- pom.xml
|   `-- src/main/java/com/luxurygoods/blockchain/middleware/
|-- scripts/
|-- docker-compose.yml
|-- mvnw
|-- pom.xml
`-- README.md
```

The root `pom.xml` aggregates both Java modules, and the repository-level `mvnw` wrapper is the default build entry point.

## Frontend Experience

The Angular app is designed to make the blockchain flow easy to demo live.

- Home page
  - Loads the asset catalog from `GET /assets`.
  - Shows search, asset-type filters, summary metrics, and luxury product cards.
  - Includes a register-asset form so you can create new assets without using `curl`.
  - Includes a "Load Showcase Inventory" action that seeds three high-end demo assets and their first inspection reports.
- Asset detail page
  - Loads `GET /assets/{id}`, `GET /assets/{id}/history`, and `GET /assets/{id}/inspections` together.
  - Shows ownership, latest inspection state, the immutable Fabric timeline, and the full off-chain inspection dossier.
  - Includes forms to transfer ownership and to add a fresh inspection report from the browser.

## On-Chain Data Model

Each asset is stored on Fabric as a single JSON document:

```json
{
  "assetId": "LV-001",
  "type": "watch",
  "brand": "Rolex",
  "owner": "Alice",
  "createdAt": "2026-04-04T15:42:31Z",
  "updatedAt": "2026-04-04T15:42:31Z",
  "eventHistory": [
    {
      "eventType": "REGISTERED",
      "timestamp": "2026-04-04T15:42:31Z",
      "previousOwner": null,
      "newOwner": "Alice",
      "reportHash": null
    }
  ]
}
```

`getAssetHistory(assetId)` returns the Fabric key history for that asset, including transaction IDs, timestamps, deleted flag, and the full asset snapshot at each point in time.

## MongoDB Schema

### `assets` collection

Used as an off-chain projection for fast application reads and operational metadata.

Fields:

- `assetId` unique business identifier
- `type`
- `brand`
- `owner`
- `latestInspectionHash`
- `createdAt`
- `updatedAt`

### `inspection_reports` collection

Stores the full inspection content that should not live on-chain.

Fields:

- `assetId`
- `reportHash`
- `inspector`
- `status`
- `location`
- `notes`
- `inspectedAt`
- `metadata`
- `storedAt`
- `ledgerSynced`
- `ledgerSyncedAt`

## REST API

### `GET /assets`

Returns the current catalog from MongoDB, filtered to assets that still exist on-chain.

### `GET /assets/{id}`

Returns the current summary view for a single asset.

### `POST /assets`

Registers an asset on Fabric and mirrors the latest asset state to MongoDB.

Example body:

```json
{
  "assetId": "LV-001",
  "type": "watch",
  "brand": "Rolex",
  "owner": "Alice"
}
```

### `POST /assets/{id}/transfer`

Transfers the asset to a new owner.

Example body:

```json
{
  "newOwner": "Bob"
}
```

### `POST /assets/{id}/inspection`

Hashes the request payload, stores the full report in MongoDB, and stores only the hash on-chain.

Example body:

```json
{
  "inspector": "QualityLab-7",
  "status": "PASSED",
  "location": "Milan Service Hub",
  "notes": "Case, clasp, and serial number verified.",
  "inspectedAt": "2026-04-04T18:00:00Z",
  "metadata": {
    "serialMatch": "true",
    "condition": "excellent"
  }
}
```

### `GET /assets/{id}/history`

Returns the full Fabric asset history for the given asset.

### `GET /assets/{id}/inspections`

Returns the full off-chain inspection reports for the asset from MongoDB.

## Deployment Instructions

### Prerequisites

- Git
- Docker and Docker Compose
- Java 17 or newer
- Node.js and npm
- Internet access for downloading Maven dependencies and Fabric samples

### Maven Build

This repository now uses Maven for both Java modules. A local Maven install is optional because the repo includes `./mvnw`.

Build everything:

```bash
./mvnw clean package
```

Run only middleware tests:

```bash
./mvnw -pl middleware test
```

Build the Angular storefront:

```bash
cd frontend
npm install --legacy-peer-deps --cache .npm-cache
npm run build
```

### 1. Start MongoDB

```bash
docker compose up -d
```

### 2. Start Hyperledger Fabric test network

This project ships with a helper script that downloads `fabric-samples` into `.fabric/` and starts the test network with CAs.

```bash
./scripts/start-fabric.sh
```

Defaults:

- Fabric samples directory: `./.fabric/fabric-samples`
- Channel: `mychannel`
- Org/user used by middleware: `Org1MSP`, `User1@org1.example.com`

You can override versions or directories if needed:

```bash
FABRIC_VERSION=2.5.15 CA_VERSION=1.5.17 ./scripts/start-fabric.sh
```

### 3. Deploy the Java chaincode

```bash
./scripts/deploy-chaincode.sh
```

The script:

- builds the Java chaincode with `./mvnw -pl chaincode -am clean package`
- deploys it to the Fabric test network via `network.sh deployCC`
- uses chaincode name `luxuryasset` by default

Optional overrides:

```bash
CHAINCODE_NAME=luxuryasset CHAINCODE_VERSION=1.0 CHAINCODE_SEQUENCE=1 ./scripts/deploy-chaincode.sh
```

### 4. Run the Spring Boot middleware

```bash
./scripts/run-middleware.sh
```

The service starts on `http://localhost:8080`.

### 5. Run the Angular storefront

```bash
./scripts/run-frontend.sh
```

The UI starts on `http://localhost:4200`.

The helper script:

- installs frontend dependencies on first run
- disables Angular CLI first-run prompts for a smoother demo startup
- starts the dev server bound to `0.0.0.0`

### 6. Run a quick demo in the UI

1. Open `http://localhost:4200`
2. Click `Load Showcase Inventory`
3. Open any asset card to view:
   - the Mongo-backed inspection dossier
   - the Fabric-backed ownership and history timeline
4. Use the forms on the detail page to transfer ownership or add another inspection

### 7. Run a quick demo with the API

Register asset:

```bash
curl -X POST http://localhost:8080/assets \
  -H "Content-Type: application/json" \
  -d '{
    "assetId":"LV-001",
    "type":"watch",
    "brand":"Rolex",
    "owner":"Alice"
  }'
```

Transfer ownership:

```bash
curl -X POST http://localhost:8080/assets/LV-001/transfer \
  -H "Content-Type: application/json" \
  -d '{"newOwner":"Bob"}'
```

Add inspection:

```bash
curl -X POST http://localhost:8080/assets/LV-001/inspection \
  -H "Content-Type: application/json" \
  -d '{
    "inspector":"QualityLab-7",
    "status":"PASSED",
    "location":"Milan Service Hub",
    "notes":"Case, clasp, and serial number verified.",
    "inspectedAt":"2026-04-04T18:00:00Z",
    "metadata":{"serialMatch":"true","condition":"excellent"}
  }'
```

Fetch ledger history:

```bash
curl http://localhost:8080/assets/LV-001/history
```

Fetch storefront catalog data:

```bash
curl http://localhost:8080/assets
```

Fetch off-chain inspection reports for one asset:

```bash
curl http://localhost:8080/assets/LV-001/inspections
```

### 8. Stop the Fabric network

```bash
./scripts/stop-fabric.sh
```

## Design Decisions

- Java chaincode instead of Node or Go
  - Matches the requirement and keeps the contract implementation close to the Java middleware model.
- Embedded event history in the asset state
  - Makes the latest asset state self-describing for demo reads.
- Fabric key history for immutable timeline
  - `getAssetHistory()` uses Fabric history queries so the API can expose every committed version.
- MongoDB for full inspection documents
  - Large or private report payloads stay off-chain while the ledger stores only a deterministic SHA-256 hash.
- Fabric Gateway SDK in the middleware
  - Keeps the Spring Boot service responsible for all blockchain calls instead of shelling out to CLI tooling.
- Maven build for both modules
  - Keeps the Java toolchain consistent across the repo and avoids the Gradle and Java 25 compatibility issue we hit locally.
- Angular standalone frontend
  - Keeps the UI lightweight, demo-friendly, and easy to launch beside the backend with minimal configuration.
- Unsynced inspection persistence support
  - Inspection reports are saved before chain submission and marked with `ledgerSynced=false` until Fabric confirms the transaction.
- Mongo projection filtered against Fabric existence
  - Prevents stale UI catalog entries after a Fabric test-network reset while Mongo data remains.

## Limitations

- Single organization connection
  - The middleware is wired to Org1's `User1` identity by default.
- No authentication or authorization
  - The REST API is open for demo purposes.
- No transactional guarantee across MongoDB and Fabric
  - MongoDB and Fabric writes are coordinated best-effort only.
- No retry or background reconciliation
  - Unsynced inspection reports are flagged, but automatic replay is not implemented.
- No private data collections
  - Off-chain storage is MongoDB only in this prototype.
- No production secret management
  - Paths to Fabric credentials are configured through local files and environment variables.
- No checkout or payment workflow
  - The frontend is focused on provenance and after-sale asset operations rather than ecommerce transactions.

## Notes

- Chaincode method names are lower camel case to match the requested API: `registerAsset`, `transferOwnership`, `addInspectionReport`, `getAssetHistory`.
- The middleware configuration is in `middleware/src/main/resources/application.yml`.
- The frontend API client is in `frontend/src/app/core/services/luxury-goods-api.service.ts`.
- If your Fabric samples directory lives somewhere else, override the `FABRIC_*` environment variables before starting the Spring Boot service.
