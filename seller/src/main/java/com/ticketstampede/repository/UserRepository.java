package com.ticketstampede.repository;

import com.ticketstampede.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByExternalUserId(String externalUserId);
}
