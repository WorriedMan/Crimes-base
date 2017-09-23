import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

public class ServerCommands {
    private static ServerCommands instance;
    private ArrayList<Crime> mCrimes;

    private ServerCommands() {
        mCrimes = new ArrayList<>();
        Date startDate = new Date();
        startDate.setTime(1451606400);
        Date endDate = new Date();
        endDate.setTime(1483228800);
        for (int i = 1; i < 3; i++) {
            Crime crime = new Crime();
            long random = ThreadLocalRandom.current().nextLong(startDate.getTime(), endDate.getTime());
            crime.setDate(random);
            crime.setTitle("Преступление #" + i);
            crime.setSolved(ThreadLocalRandom.current().nextBoolean());
            mCrimes.add(crime);
        }
    }

    public static ServerCommands getInstance() {
        if (instance == null) {
            instance = new ServerCommands();
        }
        return instance;
    }

    private ArrayList<Crime> getCrimes() {
        return mCrimes;
    }

    public void proceedCommand(DataOutputStream out, String command) throws IOException {
        switch (command) {
            case "crimes":
                sendCrimes(out);
            case "test":
                sendTest(out);
        }
    }

    private void sendCrimes(DataOutputStream out) throws IOException {
        for (Crime crime : ServerCommands.getInstance().getCrimes()) {
            byte[] crimeBytes = getSerializedCrime(crime);
            byte[] crimeSize = ByteBuffer.allocate(4).putInt(crimeBytes.length).array();
            out.write(crimeSize);
            out.flush();
            out.write(crimeBytes);
            out.flush();
            System.out.println("Crime (" + Arrays.toString(crimeSize) + ") " + crimeBytes.length + " | "+(byte) crimeBytes.length);
        }
        System.out.println("Crimes sended");
    }

    private void sendTest(DataOutputStream out) throws IOException {
//        testMessage[]
        String body = "message";
        final byte[] bodyBytes = body.getBytes("UTF-8");
        ByteBuffer headerBuffer = ByteBuffer.allocate(32).putInt(bodyBytes.length);
        headerBuffer.position(0);
        byte[] header = headerBuffer.array();
        byte[] message = ByteBuffer.allocate(32+bodyBytes.length).put(header).put(bodyBytes).array();
            out.write(message);
            out.flush();
//            out.write(crimeBytes);
//            out.flush();
            System.out.println("Header bytes (" + header.length + ") " + Arrays.toString(header));
            System.out.println("Body bytes (" + bodyBytes.length + ") " + Arrays.toString(bodyBytes));
            System.out.println("Test message (" + message.length + ") " + Arrays.toString(message));
        System.out.println("Crimes sended");
    }

    private static byte[] getSerializedCrime(Crime crime) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(crime);
            out.flush();
            return bos.toByteArray();
        } finally {
            bos.close();
        }
    }


}
