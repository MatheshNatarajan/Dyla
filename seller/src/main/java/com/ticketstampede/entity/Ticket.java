package com.ticketstampede.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_number", unique = true, nullable = false)
    private Integer ticketNumber;

    public Ticket(Integer ticketNumber) {
        this.ticketNumber = ticketNumber;
    }
}
