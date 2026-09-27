package com.ticketstampede.controller;

import com.ticketstampede.entity.Purchase;
import com.ticketstampede.repository.PurchaseRepository;
import com.ticketstampede.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final PurchaseRepository purchaseRepository;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/reset")
    public String reset(@RequestParam int ticketCount) {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        purchaseRepository.deleteAll(); // In production, we'd truncate tables safely

        // Populate tickets in Redis
        for (int i = 1; i <= ticketCount; i++) {
            redisTemplate.opsForSet().add("tickets:available", String.valueOf(i));
        }
        return "RESET_SUCCESSFUL";
    }

    @PostMapping("/buy")
    public String buy(@RequestParam String userId) {
        return ticketService.reserveTicket(userId);
    }

    @PostMapping("/confirm")
    public String confirm(@RequestParam String reservationId, @RequestParam String requestId) {
        return ticketService.confirmTicket(reservationId, requestId);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        List<Purchase> purchases = purchaseRepository.findAll();
        
        long confirmed = purchases.stream()
                .filter(p -> p.getStatus() == Purchase.PurchaseStatus.CONFIRMED)
                .count();
                
        Map<Integer, String> ticketAllocations = purchases.stream()
                .filter(p -> p.getStatus() == Purchase.PurchaseStatus.CONFIRMED)
                .collect(Collectors.toMap(
                        p -> p.getTicket().getTicketNumber(),
                        p -> p.getUser().getExternalUserId()
                ));

        Long waitlistCount = redisTemplate.opsForList().size("queue:waitlist");
        
        return Map.of(
            "tickets_sold", confirmed,
            "waitlist_size", waitlistCount != null ? waitlistCount : 0,
            "allocations", ticketAllocations
        );
    }
}
