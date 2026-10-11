package com.habittracker.repository;

import com.habittracker.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {
    Optional<PushSubscription> findByEndpointHash(String endpointHash);

    List<PushSubscription> findByOwnerId(String ownerId);

    long countByOwnerId(String ownerId);
}
