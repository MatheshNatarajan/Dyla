# Ticket Stampede (State Machine & CQRS Architecture)

This repository contains the solution to **Problem 1: Ticket Stampede**, fulfilling all core requirements and crushing the "Take it further" constraints (Waitlist State Machine & Distributable Polyglot Architecture).

## Architecture Overview
To handle 50,000 concurrent requests arriving in a single 60-second window, this system utilizes a **CQRS / Write-Behind** pattern:

*   **Seller API (Spring Boot / Java 17):** Exposes the required `/reset`, `/buy`, and `/status` endpoints.
*   **The Traffic Cop (Redis):** Handles the initial stampede lock-free. Atomic Lua scripts manage the Waitlist State Machine in memory, shielding the primary database from connection exhaustion.
*   **The System of Record (PostgreSQL):** A fully normalized SQL database utilizing `UNIQUE` constraints to enforce idempotency and prevent overselling mathematically.
*   **The Attacker (Go Client):** A highly concurrent load client that spawns 500+ goroutines to adversarially attack the server and prove the invariants hold.

### The Waitlist State Machine
When a user calls `/buy`, they are given a 30-second reservation. A Spring Boot `@Scheduled` worker wakes up every 1000ms, sweeps Redis for expired timestamps, and atomically hands those abandoned tickets to the next person in the Redis waitlist queue. This design guarantees **zero race conditions**, even if multiple Spring Boot instances are deployed behind a load balancer.

## How to run it (in under 5 minutes)

### Prerequisites
*   Docker & Docker Compose installed.
*   Go 1.20+ (Only required if you want to modify the load client, otherwise the pre-compiled script or `run.ps1` invokes it natively if found).

### Execution
You can spin up the entire architecture (Postgres, Redis, and the Spring Boot Seller API) and launch the Go load client with a single command. 

If you are on Windows, simply run:
```powershell
.\run.ps1
```

If you are on Linux/Mac, or prefer to run it manually:
1. **Start the Infrastructure (Builds the API via Docker Multi-stage):**
   ```bash
   docker-compose up -d --build
   ```
2. **Wait ~20 seconds** for the databases and Spring Boot to fully boot.
3. **Run the Go Load Client:**
   ```bash
   cd buyer
   go run main.go
   ```

## Included Documentation
*   `DECISIONS.md`: A 2-page write-up covering architectural tradeoffs, rejected ideas, and scaling limits.
*   `API_DOCS.md`: Manual testing payloads for Postman/cURL.
*   `TEST_REPORT.md`: Comprehensive breakdown of Unit, Invariant, and Stress testing.
*   `/logs`: Contains the full AI session transcripts, explicitly showing how the tools were directed and corrected.
