import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

class KeysUtils {

    private PublicKey mPublicKey;
    private PrivateKey mPrivateKey;
    private Key mClientSecret;
    private boolean mEnabled;

    KeysUtils() {
        try {
            Path privateFile = Paths.get("private.kkey");
            Path publicFile = Paths.get("public.kkey");
            if (!Files.exists(privateFile) || !Files.exists(publicFile)) {
                generateKeys();
            } else {
                mPublicKey = loadPublicKey();
                mPrivateKey = loadPrivateKey();
            }
            mEnabled = true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unable to load key files! Encryption is disabled");
            mEnabled = false;
        }
    }

    private void generateKeys() throws IOException, NoSuchAlgorithmException {
        Path privateFile = Paths.get("private.kkey");
        Path publicFile = Paths.get("public.kkey");
        if (Files.exists(privateFile)) {
            Files.delete(privateFile);
        }
        if (Files.exists(publicFile)) {
            Files.delete(publicFile);
        }
        Files.createFile(privateFile);
        Files.createFile(publicFile);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        mPublicKey = keyPair.getPublic();
        mPrivateKey = keyPair.getPrivate();
        Files.write(publicFile, mPublicKey.getEncoded());
        Files.write(privateFile, mPrivateKey.getEncoded());
    }

    private static PublicKey loadPublicKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        Path file = Paths.get("public.kkey");
        byte[] privateArray = Files.readAllBytes(file);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(privateArray);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(publicKeySpec);
    }

    private PrivateKey loadPrivateKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        Path file = Paths.get("private.kkey");
        byte[] privateArray = Files.readAllBytes(file);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateArray);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(privateKeySpec);
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

    void setPrivateKey(PrivateKey privateKey) {
        mPrivateKey = privateKey;
    }

    void setPublicKey(PublicKey publicKey) {
        mPublicKey = publicKey;
    }

    void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public byte[] encryptServer(byte[] bytes) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, mPublicKey);

        return cipher.doFinal(bytes);
    }

    public byte[] decryptServer(byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, mPrivateKey);

        return cipher.doFinal(encrypted);
    }

    private Key getClientSecret() {
        return mClientSecret;
    }

    void setClientSecret(Key clientSecret) {
        mClientSecret = clientSecret;
    }

    private byte[] getFullBuffer(byte[] data) {
        int length = data.length;
        if (length != 16 && length % 16 != 0) {
            int bbSize;
            byte[] zerosBytes;
            System.out.println("It's bad");
            if (length < 16) {
                zerosBytes = new byte[16 - length];
                bbSize = 16;
            } else {
                Double whole = (double) length / 16;
                System.out.println("whole " + whole);
                int wholeInt = whole.intValue();
                System.out.println("whole int " + wholeInt);
                int diff = length - wholeInt * 16;
                System.out.println("Diff " + diff);
                int add = 16 - diff;
                System.out.println("Add " + add);
                bbSize = length + add;
                zerosBytes = new byte[add];
            }
            System.out.println("Bbsize " + bbSize);
            ByteBuffer byteBuffer = ByteBuffer.allocate(bbSize).put(zerosBytes).put(data);
            return byteBuffer.array();
        } else {
            System.out.println("Everything is good");
            return data;
        }
    }

    byte[] encrypt(byte[] dataToSend) {
        try {
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec k =
                    new SecretKeySpec(getClientSecret().getEncoded(), "AES");
            c.init(Cipher.ENCRYPT_MODE, k);
            return c.doFinal(dataToSend);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    byte[] decrypt(byte[] encryptedData) {
        try {
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec k =
                    new SecretKeySpec(getClientSecret().getEncoded(), "AES");
            c.init(Cipher.DECRYPT_MODE, k);
            return c.doFinal(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}
