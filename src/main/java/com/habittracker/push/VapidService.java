package com.habittracker.push;

import com.habittracker.model.VapidKey;
import com.habittracker.repository.VapidKeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Holds the VAPID key pair that identifies this server to the push services.
 * Uses VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY if set, otherwise creates a pair once and keeps it in the database.
 */
@Service
public class VapidService {

    private final VapidKeyRepository repo;
    private final String envPublic;
    private final String envPrivate;
    private final String subject;

    private String publicKey;
    private String privateKey;

    public VapidService(VapidKeyRepository repo,
                        @Value("${VAPID_PUBLIC_KEY:}") String envPublic,
                        @Value("${VAPID_PRIVATE_KEY:}") String envPrivate,
                        @Value("${VAPID_SUBJECT:mailto:habit-tracker@example.com}") String subject) {
        this.repo = repo;
        this.envPublic = envPublic;
        this.envPrivate = envPrivate;
        this.subject = subject;
    }

    public String subject() { return subject; }

    public synchronized String publicKey() {
        load();
        return publicKey;
    }

    public synchronized String privateKey() {
        load();
        return privateKey;
    }

    private void load() {
        if (publicKey != null) return;
        if (!envPublic.isBlank() && !envPrivate.isBlank()) {
            publicKey = envPublic.trim();
            privateKey = envPrivate.trim();
            return;
        }
        Optional<VapidKey> saved = repo.findById(1L);
        if (saved.isPresent()) {
            publicKey = saved.get().getPublicKey();
            privateKey = saved.get().getPrivateKey();
            return;
        }
        try {
            String[] pair = WebPushCrypto.generateVapidKeys();
            repo.save(new VapidKey(1L, pair[0], pair[1]));
            publicKey = pair[0];
            privateKey = pair[1];
        } catch (Exception e) {
            throw new IllegalStateException("Could not create push keys", e);
        }
    }
}
