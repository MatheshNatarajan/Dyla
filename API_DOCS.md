# Ticket Stampede API Documentation

This document contains the endpoints to manually test the State Machine & Waitlist architecture via Postman, cURL, or any other HTTP client.

---

### 1. Reset the Server (Optional)
Instantly flushes Redis and deletes everything from Postgres so you can start fresh with a clean pool of 100 tickets.

*   **URL:** `POST http://localhost:8080/reset`
*   **Content-Type:** `application/x-www-form-urlencoded`
*   **Body (Form Data):**
    *   `ticketCount`: `100`
*   **Example cURL:**
    ```bash
    curl -X POST -d "ticketCount=100" http://localhost:8080/reset
    ```

---

### 2. Buy (Reserve a Ticket)
Hit this to secure a temporary 30-second reservation. If you hit this 101 times, you will see it return `WAITLISTED`.

*   **URL:** `POST http://localhost:8080/buy`
*   **Content-Type:** `application/x-www-form-urlencoded`
*   **Body (Form Data):**
    *   `userId`: `mathesh_123` *(Can be any string)*
*   **Expected Response:** `RESERVED:abcd-1234-efgh-5678` 
*   **Example cURL:**
    ```bash
    curl -X POST -d "userId=mathesh_123" http://localhost:8080/buy
    ```

---

### 3. Confirm the Ticket
You have exactly **30 seconds** from the moment you called `/buy` to call this endpoint.

*   **URL:** `POST http://localhost:8080/confirm`
*   **Content-Type:** `application/x-www-form-urlencoded`
*   **Body (Form Data):**
    *   `reservationId`: *(paste the exact UUID you got from `/buy`)*
    *   `requestId`: `req-uuid-1` *(This is your idempotency key. Any random string works, but if you send the same string twice, it will block the duplicate).*
*   **Expected Responses:**
    *   `SUCCESS: Ticket 42` (If you were fast enough and confirmed in time)
    *   `ALREADY_PROCESSED` (If you hit confirm twice with the exact same `requestId`)
    *   `EXPIRED` (If you took longer than 30 seconds and the `@Scheduled` worker deleted your reservation)
*   **Example cURL:**
    ```bash
    curl -X POST -d "reservationId=YOUR_UUID_HERE" -d "requestId=req-123" http://localhost:8080/confirm
    ```

---

### 4. Check the Status
Hit this anytime to monitor how many tickets are permanently confirmed in Postgres, and how many people are sitting in the Redis waitlist.

*   **URL:** `GET http://localhost:8080/status`
*   **Expected Response:**
    ```json
    {
        "tickets_sold": 1,
        "waitlist_size": 0,
        "allocations": {
            "42": "mathesh_123"
        }
    }
    ```
*   **Example cURL:**
    ```bash
    curl http://localhost:8080/status
    ```
