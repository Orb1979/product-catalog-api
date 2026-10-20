package org.example.productcatalogapi.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sync_state")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SyncState {

    @Id
    @Column(length = 64)
    private String name;

    @Setter
    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

}
