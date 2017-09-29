import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.*;
import java.util.Objects;

public class ClientConnection implements Runnable {
    private Socket mClientSocket;
    private DataInputStream mDataInput;
    private DataOutputStream dataOutput;
    private boolean connectionOnline;
    private boolean mPingReceived;
    private long mPingSendedTime;
    private KeysUtils mKeys;
    private boolean mEncryption;
    private boolean mLl;

    ClientConnection(Socket socket, KeysUtils keys) {
        this.mClientSocket = socket;
        connectionOnline = true;
        mKeys = keys;
    }

    private void closeConnection() {
        try {
            System.out.println("Connection closed with " + mClientSocket.getInetAddress());
            mClientSocket.close();
            connectionOnline = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        try {
            InputStream sin = mClientSocket.getInputStream();
            OutputStream sout = mClientSocket.getOutputStream();
            mDataInput = new DataInputStream(sin);
            dataOutput = new DataOutputStream(sout);
            mPingSendedTime = System.currentTimeMillis();
            mPingReceived = true;
            while (connectionOnline) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {

                }
                if (mDataInput.available() > 0) {
                    byte[] commandByte = new byte[6];
                    mDataInput.read(commandByte);
                    commandByte = CriminalUtils.trimBytes(commandByte);
                    String command = new String(commandByte, "UTF-8");
                    if (!Objects.equals(command, "PONG")) {
//                        System.out.println("Command: " + command);
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
                System.out.println("Connection timeout (client: "+mClientSocket.getInetAddress()+")");
            }
        }

    }

    private void proceedCommand(String command) throws IOException {
        switch (command) {
            case "HELLO":
                if (mKeys.isEnabled()) {
                    sendCommand("HELLOK", mKeys.getPublic());
                } else {
                    sendCommand("HELLO");
                }
                break;
            case "PKEY":
                receiveClientKey();
                break;
            case "ENCDIS":
                mEncryption = false;
                break;
            case "PONG":
                mPingReceived = true;
                break;
            case "CRIMES":
                sendCrimes(true);
                break;
            case "ADD":
                Crime crime = CriminalUtils.readCrime(mDataInput, mKeys, mEncryption);
                if (crime != null) {
                    CrimesLib.getInstance().addCrime(crime);
                }
                sendCrimes(true);
                break;
            case "UPDATE":
                Crime updcrime = CriminalUtils.readCrime(mDataInput, mKeys, mEncryption);
                if (updcrime != null) {
                    CrimesLib.getInstance().updateCrime(updcrime);
                }
                sendCrimes(false);
                break;
            case "DELETE":
                Crime delcrime = CriminalUtils.readCrime(mDataInput, mKeys, mEncryption);
                if (delcrime != null) {
                    CrimesLib.getInstance().deleteCrime(delcrime);
                }
                sendCrimes(true);
                break;
            case "BYE":
                System.out.println("Client " + mClientSocket.getInetAddress() + " has said goodbye");
                mClientSocket.close();
                break;
        }
    }

    private void receiveClientKey() {
        try {
            byte[] lengthHeader = new byte[4];
            mDataInput.read(lengthHeader);
            int dataSize = ByteBuffer.wrap(lengthHeader).getInt();
            byte[] body = new byte[dataSize];
            mDataInput.read(body);
            body = mKeys.decryptServer(body);
            Key key = new SecretKeySpec(body, 0, body.length, "AES");
            mKeys.setClientSecret(key);
            mEncryption = true;
            sendCommand("KTEST", "SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
            mEncryption = false;
        }
    }

    private void sendCrimes(boolean show) throws IOException {
        if (!show) {
            sendCommand("CRIMEN");
        } else {
            sendCommand("CRIMES");
        }
        for (Crime crime : CrimesLib.getInstance().getCrimes()) {
            sendCommand("CRIME", crime);
        }
        sendCommand("CSEND");
    }

    private void sendCommand(String command) {
        final byte[] bodyBytes;
        try {
            bodyBytes = command.getBytes("UTF-8");
            ByteBuffer headerBuffer = ByteBuffer.allocate(6).put(bodyBytes);
            byte[] message = headerBuffer.array();
            dataOutput.write(message);
            dataOutput.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendCommand(String command, Crime crime) {
        sendCommand(command, (Object) crime);
    }

    private void sendCommand(String command, Object object) {
        try {
            byte[] crimeBytes = CriminalUtils.serialize(object);
            if (mEncryption) {
                sendBytes(command, mKeys.encrypt(crimeBytes));
            } else {
                sendBytes(command, crimeBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendBytes(String command, byte[] bytes) {
        try {
            final byte[] bodyBytes = command.getBytes("UTF-8");
            ByteBuffer headerBuffer = ByteBuffer.allocate(6).put(bodyBytes);
            headerBuffer.position(0);
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4).putInt(bytes.length);
            lengthBuffer.position(0);
            byte[] header = headerBuffer.array();
            byte[] message = ByteBuffer.allocate(10 + bytes.length).put(header).put(lengthBuffer.array()).put(bytes).array();
            dataOutput.write(message);
            if (!Objects.equals(command, "CRIME")) {
                dataOutput.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
