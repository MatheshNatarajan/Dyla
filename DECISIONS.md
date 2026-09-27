# Architecture Decisions

## Architecture Chosen vs. Rejected
**Chosen Architecture (CQRS / Write-Behind)**: A polyglot system using a **Spring Boot** API for the Seller, **Redis** as the high-speed state engine, **PostgreSQL** as the permanent System of Record, and **Go** for the adversarial Load Client. We specifically targeted the "Take it further" constraints of building a **Waitlist State Machine** and surviving a datastore crash across a distributed setup (3 instances). 
- To avoid race conditions across instances and handle the 60-second 50,000 request stampede, we delegated all state transitions (reserving, waiting) to atomic Lua scripts in Redis. Redis acts purely as our traffic cop.
- Once a ticket is confirmed, we save it permanently to a fully normalized PostgreSQL database (using `users`, `tickets`, and a `purchases` mapping table). This ensures long-term ACID durability.
- We built the Load Client in Go because its lightweight goroutines effortlessly simulate heavy concurrent "users" mimicking real-world checkout behavior (e.g., abandoning carts).

**Rejected Architectures**: 
- *Postgres with Pessimistic Locking*: Rejected for the initial `POST /buy` stampede because handling 50,000 concurrent connections requires heavy connection pooling and yields high latency/timeouts.
- *Message Queues (Kafka/RabbitMQ)*: Rejected because the prompt requires a synchronous response for `POST /buy`. Holding HTTP connections open while a background worker processes the queue causes severe socket exhaustion.

## Trade-offs Made
1. **Simplicity vs. Reliability**: We chose to introduce both Redis and PostgreSQL. While this increases the operational complexity (more infrastructure to deploy), it perfectly balances the need for extreme speed during the burst (Redis) with the need for permanent, structured data storage (PostgreSQL).
2. **Memory vs. Ease of Background Tasks**: We chose Spring Boot over a lighter framework for the API. While spinning up instances consumes significant RAM, it allowed us to use `@Scheduled` to easily manage the "Reclaimer" worker (which sweeps expired 30-second reservations in Redis and hands them to the waitlist).
3. **Deployment Friction vs. Portability**: To make the app run instantly on any machine without requiring a local Java/Maven setup, we traded raw local execution for a Docker Multi-Stage build. The entire stack (Spring Boot, Postgres, Redis) is completely containerized.

## Testing Methodology
We built a custom Go CLI load tester to adversarially attack the Spring Boot backend. The Go client simulates a real-world stampede:
- It fires concurrent `POST /buy` requests using goroutines.
- It maliciously splits the behavior of the "lucky 100" who get reservations: some users simulate payments and confirm within 30 seconds, while others intentionally abandon their carts.
- It dynamically parses the `/status` JSON and mathematically verifies that Postgres correctly holds exactly 100 confirmed tickets, proving the Spring Boot background worker successfully stripped expired tickets and reassigned them to the Waitlist queue.

## Where it Breaks
The current architecture scales incredibly well because Redis processes Lua scripts so quickly. However, at extreme scale (e.g., 1 Million+ concurrent requests), the Nginx load balancer and Tomcat socket queue will succumb to TCP socket exhaustion, and the single-threaded nature of Redis will bottleneck. 

## Next Steps (If given 2 more weeks)
If this system needed to scale to millions of concurrent requests, I would abandon the synchronous `POST /buy` pattern entirely. I would implement an asynchronous, event-driven architecture using Kafka and WebSockets, and build an Edge-level Virtual Waiting Room (via Cloudflare Workers) to throttle ingress traffic before it ever hits the application servers.
