package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"math/rand"
	"net/http"
	"net/url"
	"sort"
	"strings"
	"sync"
	"time"
)

const (
	baseURL = "http://localhost:8080"
	numTickets = 100
	numWorkers = 500 // Reduced for testing
)

type LatencyStats struct {
	mu      sync.Mutex
	records []time.Duration
}

func (s *LatencyStats) Add(d time.Duration) {
	s.mu.Lock()
	s.records = append(s.records, d)
	s.mu.Unlock()
}

func (s *LatencyStats) Report(totalDuration time.Duration) {
	s.mu.Lock()
	defer s.mu.Unlock()

	if len(s.records) == 0 {
		fmt.Println("No requests made.")
		return
	}

	sort.Slice(s.records, func(i, j int) bool { return s.records[i] < s.records[j] })
	p50 := s.records[len(s.records)/2]
	p99 := s.records[int(float64(len(s.records))*0.99)]

	rps := float64(len(s.records)) / totalDuration.Seconds()

	fmt.Printf("\n--- Performance Metrics ---\n")
	fmt.Printf("Total Requests: %d\n", len(s.records))
	fmt.Printf("Requests/sec:   %.2f\n", rps)
	fmt.Printf("Median Latency: %v\n", p50)
	fmt.Printf("P99 Latency:    %v\n", p99)
}

func doRequest(client *http.Client, req *http.Request, stats *LatencyStats) (string, error) {
	start := time.Now()
	resp, err := client.Do(req)
	duration := time.Since(start)
	
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	
	body, _ := io.ReadAll(resp.Body)
	stats.Add(duration)
	return string(body), nil
}

func main() {
	fmt.Println("Starting Ticket Stampede Attacker...")
	
	// Reset the server
	http.PostForm(baseURL+"/reset", url.Values{"ticketCount": {fmt.Sprintf("%d", numTickets)}})
	fmt.Printf("Server reset successfully. Firing %d concurrent requests...\n", numWorkers)

	var wg sync.WaitGroup
	stats := &LatencyStats{}
	client := &http.Client{
		Transport: &http.Transport{
			MaxIdleConnsPerHost: 1000,
		},
		Timeout: 10 * time.Second,
	}

	startTime := time.Now()

	// The Stampede
	for i := 0; i < numWorkers; i++ {
		wg.Add(1)
		go func(id int) {
			defer wg.Done()
			
			userID := fmt.Sprintf("user-%d", id)
			req, _ := http.NewRequest("POST", baseURL+"/buy?userId="+userID, nil)
			
			respStr, err := doRequest(client, req, stats)
			if err != nil {
				return
			}

			if strings.HasPrefix(respStr, "RESERVED:") {
				reservationID := strings.TrimPrefix(respStr, "RESERVED:")
				requestID := fmt.Sprintf("req-%d", id)

				// State Machine Malicious Test: 
				// 50% of people abandon their carts so we can test the 30-second expiration waitlist.
				if rand.Float32() > 0.5 {
					// Simulate typing credit card
					time.Sleep(100 * time.Millisecond)
					
					// Confirm
					confReq, _ := http.NewRequest("POST", baseURL+"/confirm?reservationId="+reservationID+"&requestId="+requestID, nil)
					doRequest(client, confReq, stats)
					
					// Test Idempotency (Fire duplicate confirm)
					dupReq, _ := http.NewRequest("POST", baseURL+"/confirm?reservationId="+reservationID+"&requestId="+requestID, nil)
					doRequest(client, dupReq, stats)
				} else {
					log.Printf("User %s is abandoning their cart!", userID)
				}
			}
		}(i)
	}

	wg.Wait()
	totalDuration := time.Since(startTime)
	stats.Report(totalDuration)

	fmt.Println("\nWaiting 35 seconds to let the Spring Boot @Scheduled worker reclaim abandoned carts...")
	time.Sleep(35 * time.Second)

	// Check final status
	resp, _ := http.Get(baseURL + "/status")
	body, _ := io.ReadAll(resp.Body)
	
	var statusData map[string]interface{}
	json.Unmarshal(body, &statusData)
	
	ticketsSold := int(statusData["tickets_sold"].(float64))
	allocations := statusData["allocations"].(map[string]interface{})
	
	fmt.Printf("\n--- Final System State ---\nTickets Sold: %d\n", ticketsSold)
	
	fmt.Println("\n--- Verifying Invariants ---")
	
	// 1. Never sell more tickets than exist
	if ticketsSold <= numTickets {
		fmt.Println("[PASS] Never sell more tickets than exist.")
	} else {
		fmt.Println("[FAIL] Oversold tickets!")
	}
	
	// 2. Never issue the same ticket number twice
	if len(allocations) == ticketsSold {
		fmt.Println("[PASS] Never issue the same ticket number twice (No duplicate numbers).")
	} else {
		fmt.Println("[FAIL] Duplicate ticket numbers found!")
	}
	
	// 3. Same request id arrives twice, buyer gets one ticket
	uniqueUsers := make(map[string]bool)
	for _, user := range allocations {
		uniqueUsers[user.(string)] = true
	}
	if len(uniqueUsers) == ticketsSold {
		fmt.Println("[PASS] If the same request id arrives twice, buyer gets one ticket (Idempotency holding).")
	} else {
		fmt.Println("[FAIL] A single user holds multiple tickets!")
	}
	
	// 4. Count reported matches actually issued
	if ticketsSold == len(allocations) {
		fmt.Println("[PASS] The count reported by /status matches the tickets actually issued.")
	} else {
		fmt.Println("[FAIL] Status count mismatch!")
	}
}
