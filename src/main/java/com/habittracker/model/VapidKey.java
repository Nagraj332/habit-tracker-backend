package com.habittracker.model;

import jakarta.persistence.*;

/** The server's own Web Push key pair. Created once (id 1) and kept, so subscriptions stay valid after restarts. */
@Entity
@Table(name = "vapid_keys")
public class VapidKey {

    @Id
    private Long id;

    @Column(nullable = false, length = 120)
    private String publicKey;

    @Column(nullable = false, length = 120)
    private String privateKey;

    public VapidKey() {}

    public VapidKey(Long id, String publicKey, String privateKey) {
        this.id = id;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public Long getId() { return id; }
    public String getPublicKey() { return publicKey; }
    public String getPrivateKey() { return privateKey; }
}
