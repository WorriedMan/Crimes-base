import com.oegodf.crime.CrimesMap;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class CriminalClient {

    private static Socket sConnection;
    private static DataInputStream sInputStream;
    private static DataOutputStream sOutputStream;
    private static ConnectionWorker sConnectionWorker;

    public static void main(String[] args) throws InterruptedException {
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


        System.out.println("Connecting to server...");
        try {
            while (!establishConnectionToServer(ip, port)) {
                TimeUnit.SECONDS.sleep(5);
            }
            Observer<CrimesMap> observer = new CrimesObserver();
            sConnectionWorker = new ConnectionWorker(sInputStream, sOutputStream);
            Observable<CrimesMap> sObservable = Observable.create(sConnectionWorker)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.single()).doOnComplete(CriminalClient::closeProgram);
            sObservable.subscribe(observer);
            runMainConnection();
        } catch (Exception ignored) {

        }
    }

    private static boolean establishConnectionToServer(String address, int serverPort) throws InterruptedException {
        try {
            InetAddress ipAddress = InetAddress.getByName(address);
            sConnection = new Socket(ipAddress, serverPort);
            InputStream sin = sConnection.getInputStream();
            OutputStream sout = sConnection.getOutputStream();
            sInputStream = new DataInputStream(sin);
            sOutputStream = new DataOutputStream(sout);
            System.out.println("Connected to crimes server " + address + ":" + serverPort);
            return true;
        } catch (Exception x) {
            System.out.println("Unable to find server " + address + ":" + serverPort + ", trying again in 5 seconds...");
            return false;
        }
    }

    private static void runMainConnection() throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        while (sConnection.isConnected()) {
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
                sConnectionWorker.proceedConsoleCommand(command, arguments);
            }
        }
    }

    private static void closeProgram() {
        System.exit(0);
    }
}
