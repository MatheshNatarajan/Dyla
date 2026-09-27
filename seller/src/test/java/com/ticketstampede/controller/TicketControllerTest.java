package com.ticketstampede.controller;

import com.ticketstampede.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import com.ticketstampede.repository.PurchaseRepository;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
public class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @MockBean
    private PurchaseRepository purchaseRepository;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Test
    public void testBuyEndpoint() throws Exception {
        when(ticketService.reserveTicket("user1")).thenReturn("RESERVED:uuid-1234");

        mockMvc.perform(post("/buy").param("userId", "user1"))
                .andExpect(status().isOk())
                .andExpect(content().string("RESERVED:uuid-1234"));
    }

    @Test
    public void testConfirmEndpoint() throws Exception {
        when(ticketService.confirmTicket("uuid-1234", "req-1")).thenReturn("SUCCESS: Ticket 42");

        mockMvc.perform(post("/confirm")
                .param("reservationId", "uuid-1234")
                .param("requestId", "req-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("SUCCESS: Ticket 42"));
    }
}
