import javax.crypto.Cipher;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Objects;

public class ConnectionWorker implements Runnable {
    private final String ANSI_RED_BACKGROUND = "\u001B[41m";
    private DataInputStream mInputStream;
    private DataOutputStream mOutputStream;
    private boolean mAnswerWaiting;
    private final byte MODE_DEFAULT = 0;
    private final byte MODE_EDIT_CRIME = 1;
    private byte mMode = 0;
    private ArrayList<Crime> mCrimes;
    private Crime editingCrime;
    private ClientKeysUtils mClientKeys;

    ConnectionWorker(DataInputStream inStream, DataOutputStream outStream) {
        mInputStream = inStream;
        mOutputStream = outStream;
        mClientKeys = new ClientKeysUtils();
    }

    @Override
    public void run() {
        sendCommand("HELLO");
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
            try {
                if (mInputStream.available() > 0) {
                    proceedMessage();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void proceedMessage() throws IOException {
        byte[] commandByte = new byte[6];
        mInputStream.read(commandByte);
        commandByte = CriminalUtils.trimBytes(commandByte);
        String command = new String(commandByte, "UTF-8");
        if (!Objects.equals(command, "PING")) {
//            System.out.println("Command REC: " + command);
        }
        try {
            proceedServerCommand(command);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void proceedServerCommand(String command) throws IOException, ClassNotFoundException {
        switch (command) {
            case "HELLOK":
                proceedKeysExchange();
                break;
            case "HELLO":
                System.out.println("Server said that encryption is disabled");
                readyToEnterCommand();
                break;
            case "KTEST":
                checkTestKeys();
                break;
            case "CRIMEN":
                getCrimesFromServer(false);
                break;
            case "CRIMES":
                getCrimesFromServer(true);
                break;
            case "CRIME":
                printCrime();
                break;
            case "CSEND":
                mAnswerWaiting = false;
                break;
            case "SEND":
                mAnswerWaiting = false;
                break;
            case "PING":
                sendCommand("PONG");
                break;
        }
    }

    private void checkTestKeys() throws IOException, ClassNotFoundException {
        byte[] lengthHeader = new byte[4];
        mInputStream.read(lengthHeader);
        int dataSize = ByteBuffer.wrap(lengthHeader).getInt();
        byte[] body = new byte[dataSize];
        mInputStream.read(body);
        body = mClientKeys.decrypt(body);
        String message = (String) CriminalUtils.deserialize(body);
        if (message != null && Objects.equals(message, "SUCCESS")) {
            System.out.println("Keys test success. Encryption enabled!");
            mClientKeys.setEnabled(true);
        } else {
            mClientKeys.setEnabled(false);
            System.out.println("Keys test failed. Encryption disabled.");
            sendCommand("ENCDIS");
        }
        readyToEnterCommand();
    }

    private void proceedKeysExchange() throws IOException, ClassNotFoundException {
        byte[] lengthHeader = new byte[4];
        mInputStream.read(lengthHeader);
        int dataSize = ByteBuffer.wrap(lengthHeader).getInt();
        byte[] body = new byte[dataSize];
        mInputStream.read(body);
        PublicKey serverPublicKey = (PublicKey) CriminalUtils.deserialize(body);
        mClientKeys.setServerPublicKey(serverPublicKey);

        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, mClientKeys.getServerPublicKey());
            byte[] cipherData = cipher.doFinal(mClientKeys.getKey().getEncoded());
            sendBytes("PKEY", cipherData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getCrimesFromServer(boolean show) throws IOException {
        mCrimes = new ArrayList<>();
        byte[] commandByte = new byte[6];
        mInputStream.read(commandByte);
        commandByte = CriminalUtils.trimBytes(commandByte);
        String message = new String(commandByte, "UTF-8");
        while (!Objects.equals(message, "CSEND")) {
            printCrime();
            commandByte = new byte[6];
            mInputStream.read(commandByte);
            commandByte = CriminalUtils.trimBytes(commandByte);
            message = new String(commandByte, "UTF-8");
        }
        if (mMode != MODE_EDIT_CRIME && show) {
            printAllCrimes();
            readyToEnterCommand();
        }
        mAnswerWaiting = false;
    }

    private void printCrime() throws IOException {
        Crime crime = CriminalUtils.readCrime(mInputStream, mClientKeys);
        if (crime != null) {
            mCrimes.add(crime);
        }
    }

    private void printAllCrimes() {
        System.out.println("~~~~~~~~~~~~~~CRIMES LIST~~~~~~~~~~~~~~");
        System.out.println("#_|_______________________Id_______________________|_________Title________|____Date of crime_____");
        mCrimes.forEach((crime) -> {
            String dateString = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(crime.getDate());
            StringBuilder crimeString = new StringBuilder();
            if (crime.needPolice()) {
                crimeString.append(ANSI_RED_BACKGROUND);
            }
            crimeString.append(mCrimes.indexOf(crime)).append(" | ");
            crimeString.append("CRIME ID: ").append(crime.getId().toString()).append(" | TITLE: ").append(crime.getTitle()).append(" | ").append(dateString);
            System.out.println(crimeString);
        });
        System.out.println("~~~~~~~~~~~~~~END OF LIST~~~~~~~~~~~~~~");
    }

    void proceedConsoleCommand(String command, String arguments) {
        if (mAnswerWaiting) {
            return;
        }
        if (mMode == MODE_EDIT_CRIME) {
            switch (command) {
                case "title":
                    editTitle(arguments);
                    break;
                case "time":
                    editTime(arguments);
                    break;
                case "solved":
                    editSolved(arguments);
                    break;
                case "police":
                    editPolice(arguments);
                    break;
                case "save":
                    saveEditedCrime();
                    break;
                case "close":
                    closeEditMode();
                    break;
                case "help":
                    System.out.println("Editing crime commands:");
                    System.out.println("title [new title] - shows title or changes title to new");
                    System.out.println("time [timestamp] - shows time or sets time to timestamp");
                    System.out.println("solved [0-1] - shows solved state or sets it");
                    System.out.println("police [0-1] - shows police needing or sets it");
                    System.out.println("save - updates crime on server and closes editing mode");
                    System.out.println("close - discard changes and closes editing mode");
                default:
                    readyToEnterCommand();
            }
        } else {
            switch (command) {
                case "shutdown":
                case "bye":
                    sendCommand("BYE");
                    System.exit(0);
                    break;
                case "create":
                    createCrime(arguments);
                    break;
                case "edit":
                    startEditMode(arguments);
                    break;
                case "delete":
                    deleteCrime(arguments);
                    break;
                case "crimes":
                    System.out.println("Loading crimes...");
                    sendCommand("CRIMES");
                    mAnswerWaiting = true;
                    break;
                case "help":
                    System.out.println("Criminal client commands:");
                    System.out.println("crimes - get all crimes from server");
                    System.out.println("create [crime title] - creates new crime");
                    System.out.println("edit [crime #] - stars edit crime mode");
                    System.out.println("delete [crime #] - deletes crime");
                    System.out.println("bye - shutdown");
                default:
                    readyToEnterCommand();

            }
        }
    }

    private void readyToEnterCommand() {
        if (mMode == MODE_DEFAULT) {
            System.out.print("Command: ");
        } else {
            System.out.print("> ");
        }
    }

    private void createCrime(String arguments) {
        if (Objects.equals(arguments, "")) {
            System.out.println("Please specify crime title");
            readyToEnterCommand();
            return;
        }
        Crime crime = new Crime();
        crime.setTitle(arguments);
        sendCommand("ADD", crime);
        mAnswerWaiting = true;
    }

    private void deleteCrime(String arguments) {
        try {
            Integer crimeIndex = Integer.parseInt(arguments);
            Crime crime = mCrimes.get(crimeIndex);
            if (crime != null) {
                sendCommand("DELETE", crime);
                mAnswerWaiting = true;
            }
        } catch (NumberFormatException e) {
            System.out.println("Please specify crime id");
            readyToEnterCommand();
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Crime not found, did you asked crimes from server?");
            readyToEnterCommand();
        }
    }

    // Edit mode

    private void startEditMode(String arguments) {
        try {
            Integer crimeIndex = Integer.parseInt(arguments);
            Crime crime = mCrimes.get(crimeIndex);
            if (crime != null) {
                mMode = MODE_EDIT_CRIME;
                System.out.println("Editing crime \"" + crime.getTitle() + "\"(#" + crimeIndex + ")");
                System.out.println("Type \"help\" to get help");
                editingCrime = new Crime(crime);
                readyToEnterCommand();
            }
        } catch (NumberFormatException e) {
            System.out.println("Please specify crime id");
            readyToEnterCommand();
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Crime not found, did you asked crimes from server?");
            readyToEnterCommand();
        }
    }


    private void saveEditedCrime() {
        sendCommand("UPDATE", editingCrime);
        System.out.println("Crime saved");
        closeEditMode();
    }

    private void closeEditMode() {
        mMode = MODE_DEFAULT;
        editingCrime = null;
        System.out.println("Editing crime mode closed");
        readyToEnterCommand();
    }

    private void editTitle(String arguments) {
        if (Objects.equals(arguments, "")) {
            System.out.println("Title: " + editingCrime.getTitle());
            readyToEnterCommand();
        } else {
            editingCrime.setTitle(arguments);
            System.out.println("Title set");
            readyToEnterCommand();
        }
    }

    private void editTime(String arguments) {
        if (Objects.equals(arguments, "")) {
            String dateString = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(editingCrime.getDate());
            System.out.println("Date and time: " + dateString);
            readyToEnterCommand();
        } else {
            try {
                Long crimeTimestamp = Long.parseLong(arguments);
                editingCrime.setDate(crimeTimestamp * 1000);
                System.out.println("Timestamp set");
                readyToEnterCommand();
            } catch (NumberFormatException e) {
                System.out.println("Wrong timestamp");
                readyToEnterCommand();
            }
        }
    }

    private void editSolved(String arguments) {
        if (Objects.equals(arguments, "")) {
            System.out.println("Solved: " + (editingCrime.isSolved() ? "true" : "false"));
            readyToEnterCommand();
        } else {
            try {
                Byte solvedByte = Byte.parseByte(arguments);
                if (solvedByte != 0 && solvedByte != 1) {
                    throw new NumberFormatException();
                }
                editingCrime.setSolved(solvedByte == 1);
                System.out.println("Solved state set");
                readyToEnterCommand();
            } catch (NumberFormatException e) {
                System.out.println("Wrong solved state (must be either 0 or 1)");
                readyToEnterCommand();
            }
        }
    }

    private void editPolice(String arguments) {
        if (Objects.equals(arguments, "")) {
            System.out.println("Police needed: " + (editingCrime.needPolice() ? "true" : "false"));
            readyToEnterCommand();
        } else {
            try {
                Byte policeByte = Byte.parseByte(arguments);
                if (policeByte != 0 && policeByte != 1) {
                    throw new NumberFormatException();
                }
                editingCrime.setPolice(policeByte == 1);
                System.out.println("Police needing state set");
                readyToEnterCommand();
            } catch (NumberFormatException e) {
                System.out.println("Wrong police needing state (must be either 0 or 1)");
                readyToEnterCommand();
            }
        }
    }

    // Send command

    private void sendCommand(String command) {
        final byte[] bodyBytes;
        try {
            bodyBytes = command.getBytes("UTF-8");
            ByteBuffer headerBuffer = ByteBuffer.allocate(6).put(bodyBytes);
            byte[] message = headerBuffer.array();
            mOutputStream.write(message);
            mOutputStream.flush();
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
            if (mClientKeys.isEnabled()) {
                sendBytes(command, mClientKeys.encrypt(crimeBytes));
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
            mOutputStream.write(message);
            mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
