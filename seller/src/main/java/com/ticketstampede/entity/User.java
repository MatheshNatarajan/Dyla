package com.ticketstampede.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "external_user_id", unique = true, nullable = false)
    private String externalUserId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "email")
    private String email;

    public User(String externalUserId) {
        this.externalUserId = externalUserId;
    }
}
