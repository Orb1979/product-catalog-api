package org.example.productcatalogapi.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.example.productcatalogapi.model.SyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SyncStateRepository extends JpaRepository<SyncState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from SyncState state where state.name = 'pricing'")
    Optional<SyncState> lockPricingState();

}
