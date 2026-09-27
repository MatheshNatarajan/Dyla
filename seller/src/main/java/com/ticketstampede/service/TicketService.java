package com.ticketstampede.service;

import com.ticketstampede.entity.Purchase;
import com.ticketstampede.entity.Ticket;
import com.ticketstampede.entity.User;
import com.ticketstampede.repository.PurchaseRepository;
import com.ticketstampede.repository.TicketRepository;
import com.ticketstampede.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PurchaseRepository purchaseRepository;

    private static final String AVAILABLE_KEY = "tickets:available";
    private static final String RESERVED_KEY = "tickets:reserved";
    private static final String WAITLIST_KEY = "queue:waitlist";

    public String reserveTicket(String userId) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/reserve.lua"));
        script.setResultType(String.class);

        long expirationTimestamp = Instant.now().getEpochSecond() + 30;
        String reservationId = UUID.randomUUID().toString();

        return redisTemplate.execute(script, 
                List.of(AVAILABLE_KEY, RESERVED_KEY, WAITLIST_KEY), 
                userId, String.valueOf(expirationTimestamp), reservationId);
    }

    @Transactional
    public String confirmTicket(String reservationId, String requestId) {
        // Idempotency Check in Postgres
        if (purchaseRepository.existsByRequestId(requestId)) {
            return "ALREADY_PROCESSED";
        }

        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/confirm.lua"));
        script.setResultType(String.class);

        long currentTimestamp = Instant.now().getEpochSecond();
        String result = redisTemplate.execute(script, List.of(RESERVED_KEY), reservationId, String.valueOf(currentTimestamp));

        if (result != null && result.startsWith("CONFIRMED:")) {
            String[] parts = result.split(":");
            Integer ticketNumber = Integer.parseInt(parts[1]);
            String userId = parts[2];

            // Save permanent state to Postgres
            User user = userRepository.findByExternalUserId(userId)
                    .orElseGet(() -> userRepository.save(new User(userId)));

            Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                    .orElseGet(() -> ticketRepository.save(new Ticket(ticketNumber)));

            Purchase purchase = new Purchase();
            purchase.setUser(user);
            purchase.setTicket(ticket);
            purchase.setRequestId(requestId);
            purchase.setStatus(Purchase.PurchaseStatus.CONFIRMED);
            purchase.setConfirmedAt(Instant.now());
            
            purchaseRepository.save(purchase);
            return "SUCCESS: Ticket " + ticketNumber;
        }

        return result; // EXPIRED or NOT_FOUND
    }
}
