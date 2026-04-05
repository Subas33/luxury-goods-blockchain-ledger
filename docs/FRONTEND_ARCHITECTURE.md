# Frontend (Angular) Architecture

## Overview

The Frontend is a modern Angular standalone application that provides a luxury goods storefront experience. It demonstrates the complete blockchain workflow through UI, enabling users to explore assets, manage ownership transfers, and add inspection reports without ever leaving the application.

## What This Component Does

The frontend provides these user-facing features:

1. **Asset Catalog Discovery**
   - Browse all registered luxury goods
   - Search by asset ID, brand, or custodian name
   - Filter by asset type (watch, bag, jewelry, etc.)
   - View summary statistics (collection size, custodian count, verification volume)

2. **Asset Registration**
   - Register new assets via form (brand, type, initial owner)
   - One-click seeding of showcase inventory for demos
   - Real-time feedback on registration success/failure

3. **Asset Detail & Management**
   - View complete asset dossier including:
     - Current ownership and creation timeline
     - Immutable blockchain history with transaction IDs
     - Off-chain inspection reports with full details
   - Transfer ownership to new custodians
   - Add new inspection reports with location, status, and metadata

4. **Blockchain Timeline Visualization**
   - Display Fabric transaction history with timestamps
   - Show asset snapshots at each historical point
   - Visualize ownership chain and inspection events

5. **Inspection Report Management**
   - View full inspection details (not just hashes)
   - Inspection status dashboard (latest status, date)
   - Inspection metadata visualization

## Role in the System

```
Browser (localhost:4200)
      ↓ HTTP REST (JSON)
Angular Frontend
      ├─ Home Page (catalog view)
      ├─ Asset Detail Page (dossier + operations)
      └─ Shared Components (asset card, error handling)
         ↓ HTTP Calls
      Middleware REST API (localhost:8080)
         ↓
      (Blockchain + MongoDB)
```

The frontend is the **user-facing presentation layer** that translates blockchain operations into intuitive workflows.

## Integration Points

### Upstream (User)
- **Browser** - Angular app served from `http://localhost:4200`
- **User Actions** - Form submissions, button clicks, navigation

### Downstream (Backend)
- **Middleware API** - REST calls to `http://localhost:8080/assets`
- **CORS Policy** - Requests must be allowed from frontend origin

## Implementation Details

### Technology Stack

- **Framework:** Angular 19+ (Standalone Components)
- **Language:** TypeScript 5.5+
- **HTTP Client:** `HttpClientModule` (RxJS-based)
- **Styling:** SCSS (CSS preprocessor)
- **Routing:** `provideRouter()` with standalone routes
- **Forms:** Reactive Forms (`FormBuilder`, `Validators`)
- **Async:** RxJS Observables with `firstValueFrom` for promise-based code
- **Build Tool:** ng CLI, bundled with webpack
- **Package Manager:** npm 10+
- **Container Runtime:** Node.js dev server or Docker

