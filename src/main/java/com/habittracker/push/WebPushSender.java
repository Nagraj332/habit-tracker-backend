package com.habittracker.push;

import com.habittracker.model.PushSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Sends one encrypted Web Push message to a browser's push service (FCM, Mozilla, Apple, ...). */
@Service
public class WebPushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);

    private final VapidService vapid;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public WebPushSender(VapidService vapid) {
        this.vapid = vapid;
    }

    /** Returns the HTTP status (201 = accepted, 404/410 = subscription is gone) or -1 if sending failed. */
    public int send(PushSubscription sub, byte[] payload, int ttlSeconds) {
        try {
            URI uri = URI.create(sub.getEndpoint());
            String audience = uri.getScheme() + "://" + uri.getAuthority();
            byte[] body = WebPushCrypto.encrypt(payload, sub.getP256dh(), sub.getAuth());
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", WebPushCrypto.vapidAuthorization(audience, vapid.subject(), vapid.publicKey(), vapid.privateKey()))
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("TTL", String.valueOf(ttlSeconds))
                    .header("Urgency", "high")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            int status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status >= 400) log.warn("Push service answered {} for subscription {}", status, sub.getId());
            return status;
        } catch (Exception e) {
            log.warn("Push to subscription {} failed: {}", sub.getId(), e.toString());
            return -1;
        }
    }

    public static boolean isGone(int status) {
        return status == 404 || status == 410;
    }
}
