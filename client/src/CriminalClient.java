import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
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
        System.out.println("Connecting to server...");
        if (args.length > 0) {
            ip = args[0];
            if (args.length > 1) {
                String portString = args[1];
                if (portString != null) {
                    port = Integer.parseInt(portString);
                }
            }
        }
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
