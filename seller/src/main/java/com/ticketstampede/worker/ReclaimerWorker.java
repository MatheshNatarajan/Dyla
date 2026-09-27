package com.ticketstampede.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReclaimerWorker {

    private final StringRedisTemplate redisTemplate;

    private static final String AVAILABLE_KEY = "tickets:available";
    private static final String RESERVED_KEY = "tickets:reserved";
    private static final String WAITLIST_KEY = "queue:waitlist";

    @Scheduled(fixedRate = 1000)
    public void reclaimExpiredReservations() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/reclaim.lua"));
        script.setResultType(String.class);

        long currentTimestamp = Instant.now().getEpochSecond();
        long newExpirationTimestamp = currentTimestamp + 30;
        String newReservationPrefix = UUID.randomUUID().toString();

        String result = redisTemplate.execute(script, 
                List.of(RESERVED_KEY, WAITLIST_KEY, AVAILABLE_KEY), 
                String.valueOf(currentTimestamp), String.valueOf(newExpirationTimestamp), newReservationPrefix);

        if (result != null && result.startsWith("RECLAIMED:")) {
            int reclaimedCount = Integer.parseInt(result.split(":")[1]);
            if (reclaimedCount > 0) {
                log.info("Successfully reclaimed {} expired tickets and reassigned them to the waitlist.", reclaimedCount);
            }
        }
    }
}
