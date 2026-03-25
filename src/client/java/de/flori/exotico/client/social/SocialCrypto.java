package de.flori.exotico.client.social;

import java.io.File;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

public class SocialCrypto {

    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORM = "RSA/ECB/PKCS1Padding";

    private static KeyPair myKeyPair;

    public static void init(File configDir) {
        try {
            File privFile = new File(configDir, "exotico_private.key");
            File pubFile = new File(configDir, "exotico_public.key");

            if (privFile.exists() && pubFile.exists()) {
                byte[] privBytes = Files.readAllBytes(privFile.toPath());
                byte[] pubBytes = Files.readAllBytes(pubFile.toPath());

                KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
                PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privBytes)));
                PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pubBytes)));

                myKeyPair = new KeyPair(pub, priv);
            } else {
                System.out.println("[Exotico Social] Generating new RSA-2048 E2EE keys...");
                KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
                generator.initialize(2048);
                myKeyPair = generator.generateKeyPair();

                Files.writeString(privFile.toPath(),
                        Base64.getEncoder().encodeToString(myKeyPair.getPrivate().getEncoded()));
                Files.writeString(pubFile.toPath(),
                        Base64.getEncoder().encodeToString(myKeyPair.getPublic().getEncoded()));
            }
        } catch (Exception e) {
            System.err.println("[Exotico Social] Failed to initialize E2E Crypto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getMyPublicKeyBase64() {
        if (myKeyPair == null)
            return null;
        return Base64.getEncoder().encodeToString(myKeyPair.getPublic().getEncoded());
    }

    public static PublicKey decodePublicKey(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
            return kf.generatePublic(new X509EncodedKeySpec(bytes));
        } catch (Exception e) {
            return null;
        }
    }

    public static String encryptString(String rawText, PublicKey destPub) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, destPub);
            byte[] encrypted = cipher.doFinal(rawText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return null;
        }
    }

    public static String decryptString(String encryptedBase64) {
        try {
            if (myKeyPair == null)
                return "[Crypto Error: No keys]";
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, myKeyPair.getPrivate());
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            return "[Decryption Failed]";
        }
    }
}
