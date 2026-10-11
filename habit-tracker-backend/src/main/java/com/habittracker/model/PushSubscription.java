package com.habittracker.model;

import jakarta.persistence.*;
import java.time.Instant;

/** One browser/phone that agreed to receive alarm notifications for an owner. */
@Entity
@Table(name = "push_subscriptions",
       uniqueConstraints = @UniqueConstraint(columnNames = "endpoint_hash"),
       indexes = @Index(columnList = "owner_id"))
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false, length = 64)
    private String ownerId;

    // SHA-256 of the endpoint, used as the unique key (the endpoint itself is long)
    @Column(name = "endpoint_hash", nullable = false, length = 64)
    private String endpointHash;

    @Column(nullable = false, length = 1000)
    private String endpoint;

    @Column(nullable = false, length = 200)
    private String p256dh;

    @Column(nullable = false, length = 100)
    private String auth;

    @Column(length = 64)
    private String timezone;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getEndpointHash() { return endpointHash; }
    public void setEndpointHash(String endpointHash) { this.endpointHash = endpointHash; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getP256dh() { return p256dh; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }
    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
