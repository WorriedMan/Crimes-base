import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

class KeysUtils {
    private PublicKey mPublicKey;
    private PrivateKey mPrivateKey;
    private boolean mEnabled;

    KeysUtils() {
        try {
            mPublicKey = loadPublicKey();
            mPrivateKey = loadPrivateKey();
            mEnabled = true;
        } catch (IOException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            System.out.println("Unable to load key files! Encryption is disabled");
            mEnabled = false;
        }
    }

    private static PublicKey loadPublicKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        Path file = Paths.get("public.kkey");
        PublicKey key;
        if (Files.exists(file)) {
            byte[] privateArray = Files.readAllBytes(file);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(privateArray);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SUN");
            key = keyFactory.generatePublic(publicKeySpec);
        } else {
            Files.createFile(file);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            key = keyPair.getPublic();
            Files.write(file, key.getEncoded());
        }
        return key;
    }

    private static PrivateKey loadPrivateKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        Path file = Paths.get("private.kkey");
        PrivateKey key;
        if (Files.exists(file)) {
            byte[] privateArray = Files.readAllBytes(file);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateArray);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SUN");
            key = keyFactory.generatePrivate(privateKeySpec);
        } else {
            Files.createFile(file);
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            key = keyPair.getPrivate();
            Files.write(file, key.getEncoded());
        }
        return key;
    }

    boolean isEnabled() {
        return mEnabled;
    }

    PublicKey getPublic() {
        return mPublicKey;
    }

    PrivateKey getPrivate() {
        return mPrivateKey;
    }
}
