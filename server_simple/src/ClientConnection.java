import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class ClientConnection implements Runnable {
    private Socket clientSocket;
    private DataInputStream dataInput;
    private DataOutputStream dataOutput;
    private boolean connectionOnline;
    private boolean mPingReceived;
    private long mPingSendedTime;

    ClientConnection(Socket socket) {
        this.clientSocket = socket;
        connectionOnline = true;
    }

    private void closeConnection() {
        try {
            System.out.println("Connection closed with " + clientSocket.getInetAddress());
            clientSocket.close();
            connectionOnline = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            InputStream sin = clientSocket.getInputStream();
            OutputStream sout = clientSocket.getOutputStream();
            dataInput = new DataInputStream(sin);
            dataOutput = new DataOutputStream(sout);
            mPingSendedTime = System.currentTimeMillis();
            mPingReceived = true;
            while (connectionOnline) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {

                }
                if (dataInput.available() > 0) {
                    byte[] commandByte = new byte[6];
                    dataInput.read(commandByte);
                    commandByte = CriminalUtils.trimBytes(commandByte);
                    String command = new String(commandByte, "UTF-8");
                    if (!Objects.equals(command, "PONG")) {
                        System.out.println("Command: " + command);
                    }
                    proceedCommand(command);
                } else {
                    checkConnection();
                }
            }
        } catch (IOException e) {
            closeConnection();
        }
    }

    private void checkConnection() throws IOException {
        if (mPingSendedTime + 3000 < System.currentTimeMillis()) {
            if (mPingReceived) {
                mPingReceived = false;
                mPingSendedTime = System.currentTimeMillis();
                sendCommand("PING");
            } else {
                connectionOnline = false;
                System.out.println("Connection timeout");
            }
        }

    }

    private void proceedCommand(String command) throws IOException {
        switch (command) {
            case "CRIMES":
                sendCrimes();
                break;
            case "PONG":
                mPingReceived = true;
                break;
            case "ADD":
                Crime crime = CriminalUtils.readCrime(dataInput);
                if (crime != null) {
                    CrimesLib.getInstance().addCrime(crime);
                }
                sendCrimes();
                break;
            case "DELETE":
                Crime delcrime = CriminalUtils.readCrime(dataInput);
                if (delcrime != null) {
                    CrimesLib.getInstance().deleteCrime(delcrime);
                }
                sendCrimes();
                break;
            case "BYE":
                System.out.println("Client " + clientSocket.getInetAddress() + " has said goodbye");
                clientSocket.close();
                break;
        }
    }

    private void sendCrimes() throws IOException {
        sendCommand("CRIMES");
        for (Crime crime : CrimesLib.getInstance().getCrimes()) {
            sendCommand("CRIME", crime);
        }
        sendCommand("CSEND");
    }

    private void sendCommand(String command) throws IOException {
        final byte[] bodyBytes;
        bodyBytes = command.getBytes("UTF-8");
        ByteBuffer headerBuffer = ByteBuffer.allocate(6).put(bodyBytes);
        byte[] message = headerBuffer.array();
        dataOutput.write(message);
        dataOutput.flush();
    }

    private void sendCommand(String command, Crime crime) throws IOException {
        final byte[] bodyBytes;
        bodyBytes = command.getBytes("UTF-8");
        ByteBuffer headerBuffer = ByteBuffer.allocate(6).put(bodyBytes);
        headerBuffer.position(0);
        byte[] crimeBytes = CriminalUtils.serialize(crime);
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4).putInt(crimeBytes.length);
        lengthBuffer.position(0);
        byte[] header = headerBuffer.array();
        byte[] message = ByteBuffer.allocate(10 + crimeBytes.length).put(header).put(lengthBuffer.array()).put(crimeBytes).array();
        dataOutput.write(message);
        if (command != "CRIME") {
            dataOutput.flush();
        }
    }
}
