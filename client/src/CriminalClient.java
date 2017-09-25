import com.sun.org.apache.xml.internal.serializer.Encodings;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CriminalClient {

    private static Socket mConnection;
    private static DataInputStream mInputStream;
    private static DataOutputStream mOutputStream;
    private static ConnectionWorker mWorker;

    public static void main(String[] args) {
        String ip = "127.0.0.1";
        int port = 8080;
        if (args.length > 0) {
            ip = args[0];
            if (args.length > 1) {
                String portString = args[1];
                if (portString != null) {
                    port = Integer.parseInt(portString);
                }
            }
        }
        boolean encryption = false;
        Path filePath = Paths.get("kfile");
        if (Files.exists(filePath)) {
            encryption = loadKeys(filePath);
        } else {
            try {
                Files.createFile(filePath);
                encryption = createKeys(filePath);
            } catch (IOException e) {
                System.out.println("Unable to load key files! Encryption is disabled");
            }
        }
        if (encryption) {
            System.out.println("Encryption is enabled");
        }
        System.out.println("Connecting to server...");
        try {
            while (!establishConnectionToServer(ip, port)) {
                TimeUnit.SECONDS.sleep(5);
            }
            mWorker = new ConnectionWorker(mInputStream, mOutputStream);
            Thread workerThread = new Thread(mWorker);
            workerThread.start();
            runMainConnection();
        } catch (Exception ignored) {

        }
    }

    private static boolean createKeys(Path filePath) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            byte[] publicKeyBytes = publicKey.getEncoded();
            byte[] privateKeyBytes = privateKey.getEncoded();
            Files.write(filePath,publicKeyBytes);
            Files.write(filePath,privateKeyBytes);
            System.out.println("cpub " + Arrays.toString(publicKey.getEncoded()));
            System.out.println("cpub len " + publicKey.getEncoded().length);
            System.out.println("cpriv " + Arrays.toString(privateKey.getEncoded()));
            System.out.println("cpriv len " + privateKey.getEncoded().length);
            return true;
        } catch (IOException | NoSuchAlgorithmException e) {
            return false;
        }
    }

    private static boolean loadKeys(Path filePath) {
        try {
            byte[] fileArray = Files.readAllBytes(filePath);
            System.out.println("Len " + fileArray.length);
            byte[] publicArray = Arrays.copyOfRange(fileArray, 0, 128);
            byte[] privateArray = Arrays.copyOfRange(fileArray, 128, 256);
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(publicArray);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateArray);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA", "SUN");
            PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            System.out.println("pub " + pubKey.getFormat());
            System.out.println("priv " + privateKey.getFormat());
            return true;
        } catch (IOException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            System.out.println("Unable to load key files! Encryption is disabled");
            return false;
        }
    }

    private static boolean establishConnectionToServer(String address, int serverPort) throws InterruptedException {
        try {
            InetAddress ipAddress = InetAddress.getByName(address);
            mConnection = new Socket(ipAddress, serverPort);
            InputStream sin = mConnection.getInputStream();
            OutputStream sout = mConnection.getOutputStream();
            mInputStream = new DataInputStream(sin);
            mOutputStream = new DataOutputStream(sout);
            System.out.println("Connected to crimes server " + address + ":" + serverPort);
            return true;
        } catch (Exception x) {
            System.out.println("Unable to find server " + address + ":" + serverPort + ", trying again in 5 seconds...");
            return false;
        }
    }

    private static void runMainConnection() throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        while (mConnection.isConnected()) {
            String line = scanner.nextLine();
            if (!Objects.equals(line, "")) {
                String[] arr = line.split(" ", 2);
                String command = arr[0];
                String arguments;
                if (arr.length > 1) {
                    arguments = arr[1];
                } else {
                    arguments = "";
                }
                mWorker.proceedConsoleCommand(command, arguments);
            }
        }
    }


}
