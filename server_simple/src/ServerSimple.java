import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerSimple {
    public static void main(String[] ar) {
        int port = 8080;
        try {
            ExecutorService executor = Executors.newFixedThreadPool(5);
            ServerSocket ssock = new ServerSocket(port);
            System.out.println("Listening");
            CrimesLib crimesLib = CrimesLib.getInstance();
            KeysUtils keys = new KeysUtils();
            while (true) {
                Thread.sleep(500);
                Socket sock = ssock.accept();
                System.out.println("Connected " + sock.getInetAddress());
                Runnable clientConnection = new ClientConnection(sock, keys);
                executor.execute(clientConnection);
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
