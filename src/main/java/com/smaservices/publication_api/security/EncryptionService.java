package com.smaservices.publication_api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM =
            "AES/GCM/NoPadding";

    private static final int GCM_TAG_LENGTH_BITS =
            128;

    private static final int IV_LENGTH_BYTES =
            12;

    private static final String VERSION_PREFIX =
            "v1:";

    private final SecretKeySpec secretKey;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public EncryptionService(
            @Value("${app.encryption-key}")
            String encryptionKeyBase64) {

        byte[] keyBytes;

        try {
            keyBytes =
                    Base64.getDecoder()
                            .decode(
                                    encryptionKeyBase64
                            );
        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "APP_ENCRYPTION_KEY doit être encodée en Base64.",
                    exception
            );
        }

        if (keyBytes.length != 32) {

            throw new IllegalStateException(
                    "APP_ENCRYPTION_KEY doit contenir exactement 32 octets après décodage Base64."
            );
        }

        this.secretKey =
                new SecretKeySpec(
                        keyBytes,
                        "AES"
                );
    }

    public String encrypt(
            String rawData) {

        if (rawData == null) {
            throw new IllegalArgumentException(
                    "Les données à chiffrer ne peuvent pas être nulles."
            );
        }

        try {

            byte[] iv =
                    new byte[IV_LENGTH_BYTES];

            secureRandom.nextBytes(iv);

            Cipher cipher =
                    Cipher.getInstance(
                            ALGORITHM
                    );

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH_BITS,
                            iv
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] encryptedData =
                    cipher.doFinal(
                            rawData.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            byte[] payload =
                    ByteBuffer
                            .allocate(
                                    iv.length
                                            + encryptedData.length
                            )
                            .put(iv)
                            .put(encryptedData)
                            .array();

            return VERSION_PREFIX
                    + Base64.getEncoder()
                    .encodeToString(payload);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Erreur lors du chiffrement AES-GCM.",
                    exception
            );
        }
    }

    public String decrypt(
            String encryptedData) {

        if (encryptedData == null ||
                encryptedData.isBlank()) {

            throw new IllegalArgumentException(
                    "Les données chiffrées sont absentes."
            );
        }

        if (!encryptedData.startsWith(
                VERSION_PREFIX)) {

            throw new IllegalStateException(
                    "Format de chiffrement non supporté. Reconnectez le compte externe."
            );
        }

        try {

            String encodedPayload =
                    encryptedData.substring(
                            VERSION_PREFIX.length()
                    );

            byte[] payload =
                    Base64.getDecoder()
                            .decode(
                                    encodedPayload
                            );

            if (payload.length
                    <= IV_LENGTH_BYTES) {

                throw new IllegalStateException(
                        "Données chiffrées invalides."
                );
            }

            ByteBuffer buffer =
                    ByteBuffer.wrap(
                            payload
                    );

            byte[] iv =
                    new byte[
                            IV_LENGTH_BYTES
                            ];

            buffer.get(iv);

            byte[] cipherText =
                    new byte[
                            buffer.remaining()
                            ];

            buffer.get(cipherText);

            Cipher cipher =
                    Cipher.getInstance(
                            ALGORITHM
                    );

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(
                            GCM_TAG_LENGTH_BITS,
                            iv
                    );

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] decrypted =
                    cipher.doFinal(
                            cipherText
                    );

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (IllegalStateException exception) {
            throw exception;

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Erreur lors du déchiffrement AES-GCM.",
                    exception
            );
        }
    }
}