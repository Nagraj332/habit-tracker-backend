package com.habittracker.push;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Web Push building blocks using only the JDK:
 * VAPID keys + JWT (RFC 8292) and message encryption (RFC 8291, aes128gcm / RFC 8188).
 */
public final class WebPushCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private WebPushCrypto() {}

    public static String b64(byte[] b) { return B64.encodeToString(b); }

    public static byte[] unb64(String s) { return B64D.decode(s.replace('+', '-').replace('/', '_').replace("=", "")); }

    private static ECParameterSpec curve() throws Exception {
        AlgorithmParameters p = AlgorithmParameters.getInstance("EC");
        p.init(new ECGenParameterSpec("secp256r1"));
        return p.getParameterSpec(ECParameterSpec.class);
    }

    private static byte[] fixed32(BigInteger n) {
        byte[] raw = n.toByteArray();
        byte[] out = new byte[32];
        if (raw.length > 32) System.arraycopy(raw, raw.length - 32, out, 0, 32);
        else System.arraycopy(raw, 0, out, 32 - raw.length, raw.length);
        return out;
    }

    /** Uncompressed P-256 point: 0x04 || X || Y (65 bytes). */
    private static byte[] encodePublic(ECPublicKey key) {
        byte[] out = new byte[65];
        out[0] = 4;
        System.arraycopy(fixed32(key.getW().getAffineX()), 0, out, 1, 32);
        System.arraycopy(fixed32(key.getW().getAffineY()), 0, out, 33, 32);
        return out;
    }

    private static PublicKey decodePublic(byte[] raw) throws Exception {
        if (raw.length != 65 || raw[0] != 4) throw new IllegalArgumentException("Bad public key");
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(raw, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(raw, 33, 65));
        return KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), curve()));
    }

    private static PrivateKey decodePrivate(byte[] raw) throws Exception {
        return KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, raw), curve()));
    }

    /** Returns { publicKey, privateKey }, both base64url. */
    public static String[] generateVapidKeys() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair kp = g.generateKeyPair();
        return new String[] {
                b64(encodePublic((ECPublicKey) kp.getPublic())),
                b64(fixed32(((ECPrivateKey) kp.getPrivate()).getS()))
        };
    }

    // ---------- VAPID (RFC 8292) ----------

    /** Value for the Authorization header: "vapid t=<jwt>, k=<public key>". */
    public static String vapidAuthorization(String audience, String subject, String publicKeyB64, String privateKeyB64) throws Exception {
        long exp = System.currentTimeMillis() / 1000 + 12 * 3600;
        String header = b64("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String claims = b64(("{\"aud\":\"" + audience + "\",\"exp\":" + exp + ",\"sub\":\"" + subject + "\"}").getBytes(StandardCharsets.UTF_8));
        String unsigned = header + "." + claims;
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(decodePrivate(unb64(privateKeyB64)));
        s.update(unsigned.getBytes(StandardCharsets.US_ASCII));
        String jwt = unsigned + "." + b64(derToRaw(s.sign()));
        return "vapid t=" + jwt + ", k=" + publicKeyB64;
    }

    /** Java signs ECDSA as DER; JWT wants the raw R||S (64 bytes). */
    private static byte[] derToRaw(byte[] der) {
        int pos = 2;
        if ((der[1] & 0x80) != 0) pos += (der[1] & 0x7f);
        int rLen = der[pos + 1] & 0xff;
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(der, pos + 2, pos + 2 + rLen));
        pos += 2 + rLen;
        int sLen = der[pos + 1] & 0xff;
        BigInteger sv = new BigInteger(1, Arrays.copyOfRange(der, pos + 2, pos + 2 + sLen));
        byte[] out = new byte[64];
        System.arraycopy(fixed32(r), 0, out, 0, 32);
        System.arraycopy(fixed32(sv), 0, out, 32, 32);
        return out;
    }

    // ---------- Message encryption (RFC 8291) ----------

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(key, "HmacSHA256"));
        return m.doFinal(data);
    }

    /** HKDF with SHA-256 for outputs of up to 32 bytes. */
    private static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int len) throws Exception {
        byte[] prk = hmac(salt, ikm);
        byte[] in = new byte[info.length + 1];
        System.arraycopy(info, 0, in, 0, info.length);
        in[info.length] = 1;
        return Arrays.copyOf(hmac(prk, in), len);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) o.writeBytes(p);
        return o.toByteArray();
    }

    /** Encrypts the payload for one subscription. The result is the HTTP request body. */
    public static byte[] encrypt(byte[] payload, String p256dhB64, String authB64) throws Exception {
        byte[] uaPublic = unb64(p256dhB64);
        byte[] authSecret = unb64(authB64);

        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair as = g.generateKeyPair();
        byte[] asPublic = encodePublic((ECPublicKey) as.getPublic());

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(as.getPrivate());
        ka.doPhase(decodePublic(uaPublic), true);
        byte[] ecdh = ka.generateSecret();

        byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
        byte[] ikm = hkdf(authSecret, ecdh, keyInfo, 32);

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        byte[] cek = hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

        byte[] padded = concat(payload, new byte[] {2}); // 0x02 marks the last (and only) record
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] cipher = c.doFinal(padded);

        byte[] header = concat(salt, new byte[] {0, 0, 0x10, 0}, new byte[] {(byte) asPublic.length}, asPublic); // rs = 4096
        return concat(header, cipher);
    }
}
