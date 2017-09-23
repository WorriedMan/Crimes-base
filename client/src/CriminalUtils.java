import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public final class CriminalUtils {
    private CriminalUtils() throws Exception {
        throw new Exception();
    }

    static byte[] trimBytes(byte[] array) {
        ArrayList<Byte> list = new ArrayList<>(Arrays.asList(toObjects(array)));
        list.removeIf(nextByte -> nextByte == 0);
        Byte[] cleanBytes = list.toArray(new Byte[list.size()]);
        return toPrimitives(cleanBytes);
    }

    private static byte[] toPrimitives(Byte[] oBytes)
    {
        byte[] bytes = new byte[oBytes.length];

        for(int i = 0; i < oBytes.length; i++) {
            bytes[i] = oBytes[i];
        }

        return bytes;
    }

    private static Byte[] toObjects(byte[] bytesPrim) {
        Byte[] bytes = new Byte[bytesPrim.length];

        int i = 0;
        for (byte b : bytesPrim) bytes[i++] = b; // Autoboxing

        return bytes;
    }

    private static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    static byte[] serialize(Crime crime) throws IOException {
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

    static Crime readCrime(DataInputStream stream) {
        try {
            byte[] lengthHeader = new byte[4];
            stream.read(lengthHeader);
            int dataSize = ByteBuffer.wrap(lengthHeader).getInt();
            byte[] body = new byte[dataSize];
            stream.read(body);
            Crime crime = (Crime) CriminalUtils.deserialize(body);
            return crime;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}