import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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

    ConnectionWorker(DataInputStream inStream, DataOutputStream outStream) {
        mInputStream = inStream;
        mOutputStream = outStream;
    }

    @Override
    public void run() {
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
            System.out.println("Command: " + command);
        }
        proceedServerCommand(command);
    }

    private void proceedServerCommand(String command) throws IOException {
        switch (command) {
            case "CRIMES":
                getCrimesFromServer();
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

    private void getCrimesFromServer() throws IOException {
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
        if (mMode != MODE_EDIT_CRIME) {
            printAllCrimes();
        }
        mAnswerWaiting = false;
    }

    private void printCrime() throws IOException {
        Crime crime = CriminalUtils.readCrime(mInputStream);
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
                case "help":
                    System.out.println("Editing crime commands:");
                    System.out.println("title [new title] - shows title or changes title to new");
                    System.out.println("time [timestamp] - shows time or sets time to timestamp");
                    System.out.println("solved [0-1] - shows solved state or sets it");
                    System.out.println("police [0-1] - shows police needing or sets it");
                    System.out.println("save - updates crime on server and closes editing mode");
                    System.out.println("close - discard changes and closes editing mode");
                    break;
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
                    System.out.println("delete [crime #] - deletes crime");
                    System.out.println("bye - shutdown");
                    break;

            }
        }
    }

    private void createCrime(String arguments) {
        if (Objects.equals(arguments, "")) {
            System.out.println("Please specify crime title");
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
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Crime not found, did you asked crimes from server?");
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
            }
        } catch (NumberFormatException e) {
            System.out.println("Please specify crime id");
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Crime not found, did you asked crimes from server?");
        }
    }

    private void editTitle(String arguments) {
        if (Objects.equals(arguments, "")) {
            System.out.println("Title: " + editingCrime.getTitle());
        } else {
            editingCrime.setTitle(arguments);
            System.out.println("Title set");
        }
    }

    private void editTime(String arguments) {
        if (Objects.equals(arguments, "")) {

        } else
            try {
                Long crimeIndex = Long.parseLong(arguments);

            } catch (NumberFormatException e) {
                System.out.println("Please specify crime id");
            } catch (IndexOutOfBoundsException e) {
                System.out.println("Crime not found, did you asked crimes from server?");
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
        final byte[] bodyBytes;
        try {
            bodyBytes = command.getBytes("UTF-8");
            ByteBuffer headerBuffer = ByteBuffer.allocate(6).put(bodyBytes);
            headerBuffer.position(0);
            byte[] crimeBytes = CriminalUtils.serialize(crime);
            ByteBuffer lengthBuffer = ByteBuffer.allocate(4).putInt(crimeBytes.length);
            lengthBuffer.position(0);
            byte[] header = headerBuffer.array();
            byte[] message = ByteBuffer.allocate(10 + crimeBytes.length).put(header).put(lengthBuffer.array()).put(crimeBytes).array();
            mOutputStream.write(message);
            mOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
