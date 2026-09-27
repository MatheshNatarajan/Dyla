# Session Logs

This folder contains the exported transcripts of every AI coding session used during the development of this project, as required by the submission guidelines.

### Context for the Reviewers:
The logs clearly show the evolution of the architecture. You will see us explicitly discuss and reject a Postgres approach, evaluate the risks of Message Queues for synchronous endpoints, and ultimately pivot to a **Redis-backed State Machine Waitlist** using atomic Lua scripts to safely manage concurrency and 30-second expirations across 3 distributed Spring Boot instances.
