import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

class CrimesLib {
    private static volatile CrimesLib instance;
    private volatile ArrayList<Crime> mCrimes;
    private Connection connection;

    private CrimesLib() {
        mCrimes = new ArrayList<>();
        connect();
        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT uuid, title, date, solved, police FROM `crimes`");
            while (result.next()) {
                String uuid = result.getString("uuid");
                String title = result.getString("title");
                long date = result.getLong("date");
                short solved = result.getShort("solved");
                short police = result.getShort("police");
                Crime crime = new Crime();
                crime.setId(UUID.fromString(uuid));
                crime.setTitle(title);
                crime.setDate(date);
                crime.setSolved(solved == 1);
                crime.setPolice(police == 1);
                mCrimes.add(crime);
            }
            System.out.println("Total loaded crimes: " + mCrimes.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }

//        Date startDate = new Date();
//        startDate.setTime(1451606400);
//        Date endDate = new Date();
//        endDate.setTime(1483228800);
//        for (int i = 1; i < 110; i++) {
//            Crime crime = new Crime();
//            long random = ThreadLocalRandom.current().nextLong(startDate.getTime(), endDate.getTime());
//            crime.setDate(random);
//            crime.setTitle("Преступление #" + i);
//            crime.setSolved(ThreadLocalRandom.current().nextBoolean());
//            mCrimes.add(crime);
//            addCrime(crime);
//        }
    }

    ArrayList<Crime> getCrimes() {
        return mCrimes;
    }

    static CrimesLib getInstance() {
        CrimesLib localInstance = instance;
        if (localInstance == null) {
            synchronized (CrimesLib.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new CrimesLib();
                }
            }
        }
        return localInstance;
    }

    private void connect() {
        try {
            String url = "jdbc:sqlite:crimes.db";
            connection = DriverManager.getConnection(url);
            Statement statement = connection.createStatement();
            statement.execute("CREATE TABLE IF NOT EXISTS `crimes` (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT, title TEXT, date INTEGER, solved SMALLINT(1), police SMALLINT(1))");
            System.out.println("Connection to SQLite has been established.");
            statement.close();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    Crime getCrimeByUUID(UUID id) {
        String idString = id.toString();
        for (Crime crime : mCrimes) {
            if (Objects.equals(crime.getId().toString(), idString)) {
                return crime;
            }
        }
        return null;
    }

    void addCrime(Crime crime) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO `crimes` (uuid,title,date,solved,police) VALUES (?,?,?,?,?)");
            statement.setString(1, crime.getId().toString());
            statement.setString(2, crime.getTitle());
            statement.setLong(3, crime.getDate().getTime());
            statement.setShort(4, (short) (crime.isSolved() ? 1 : 0));
            statement.setShort(5, (short) (crime.needPolice() ? 1 : 0));
            statement.execute();
            mCrimes.add(crime);
            System.out.println("Saved crime " + crime.getTitle() + "(" + crime.getId() + "). Solved: " + ((byte) (crime.isSolved() ? 1 : 0)));
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Unable to add crime to database.");
        }
    }

    void deleteCrime(Crime crime) {
        try {
            Crime crimeOrigin = getCrimeByUUID(crime.getId());
            System.out.println("UUID "+crime.getId().toString());
            System.out.println("Origin "+crimeOrigin);
            System.out.println("Index "+mCrimes.indexOf(crimeOrigin));
            mCrimes.remove(crimeOrigin);
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `crimes` WHERE uuid = ?;");
            statement.setString(1, crime.getId().toString());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Unable to delete crime from database.");
        }
    }

    void updateCrime(Crime crime) {
        mCrimes.remove(crime);
        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE `crimes` SET title = ?, date = ?, solved = ?, police = ? WHERE uuid = ?;");
            statement.setString(1, crime.getTitle());
            statement.setLong(2, crime.getDate().getTime());
            statement.setShort(3, (short) (crime.isSolved() ? 1 : 0));
            statement.setShort(4, (short) (crime.needPolice() ? 1 : 0));
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Unable to update crime from database.");
        }
    }


}
