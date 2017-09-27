import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class ClientKeysUtils {

    private boolean mEnabled;
    private Key mKey;
    private PublicKey mServerPublicKey;

    ClientKeysUtils() {
        mEnabled = false;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }


    public Key getKey() {
        if (mKey == null) {
            mKey = generateKey();
        }
        return mKey;
    }

    private Key generateKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            return generator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setKey(Key key) {
        mKey = key;
    }

    PublicKey getServerPublicKey() {
        return mServerPublicKey;
    }

    void setServerPublicKey(PublicKey serverPublicKey) {
        mServerPublicKey = serverPublicKey;
    }

    byte[] encrypt(byte[] dataToSend) {
        try {
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec k =
                    new SecretKeySpec(getKey().getEncoded(), "AES");

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
                    new SecretKeySpec(getKey().getEncoded(), "AES");
            c.init(Cipher.DECRYPT_MODE, k);
            return c.doFinal(encryptedData);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

}
