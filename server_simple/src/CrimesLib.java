import com.oegodf.crime.CrimeBase;

import java.sql.*;
import java.util.UUID;
import com.oegodf.crime.CrimesMap;

class CrimesLib {
    private static volatile CrimesLib instance;
    private volatile CrimesMap mCrimes;
    private Connection connection;

    private CrimesLib() {
        mCrimes = new CrimesMap();
        connect();
        try {
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT uuid, title, date, solved, police FROM `crimes`");
            int pos = 1;
            while (result.next()) {
                String uuid = result.getString("uuid");
                String title = result.getString("title");
                long date = result.getLong("date");
                short solved = result.getShort("solved");
                short police = result.getShort("police");
                CrimeBase crime = new CrimeBase();
                crime.setId(UUID.fromString(uuid));
                crime.setTitle(title);
                crime.setDate(date);
                crime.setSolved(solved == 1);
                crime.setNeedPolice(police == 1);
                crime.setPosition(pos++);
                mCrimes.put(crime.getId(),crime);
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
//            CrimeBase crime = new CrimeBase();
//            long random = ThreadLocalRandom.current().nextLong(startDate.getTime(), endDate.getTime());
//            crime.setDate(random);
//            crime.setTitle("Преступление #" + i);
//            crime.setSolved(ThreadLocalRandom.current().nextBoolean());
//            mCrimes.add(crime);
//            addCrime(crime);
//        }
    }

    CrimesMap getCrimes() {
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

    private CrimeBase getCrimeByUUID(UUID id) {
        return mCrimes.get(id);
    }

    void addCrime(CrimeBase crime) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO `crimes` (uuid,title,date,solved,police) VALUES (?,?,?,?,?)");
            statement.setString(1, crime.getId().toString());
            statement.setString(2, crime.getTitle());
            statement.setLong(3, crime.getDate().getTime());
            statement.setShort(4, (short) (crime.isSolved() ? 1 : 0));
            statement.setShort(5, (short) (crime.isNeedPolice() ? 1 : 0));
            crime.setPosition(mCrimes.getNewCrimeId());
            statement.execute();
            mCrimes.put(crime.getId(),crime);
            System.out.println("Saved crime " + crime.getTitle() + "(" + crime.getId() + "). Solved: " + ((byte) (crime.isSolved() ? 1 : 0)));
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Unable to add crime to database.");
        }
    }

    void deleteCrime(CrimeBase crime) {
        try {
            mCrimes.remove(crime.getId());
            PreparedStatement statement = connection.prepareStatement("DELETE FROM `crimes` WHERE uuid = ?;");
            statement.setString(1, crime.getId().toString());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Unable to delete crime from database.");
        }
    }

    void updateCrime(CrimeBase crime) {
        try {
            CrimeBase crimeOrigin = getCrimeByUUID(crime.getId());
            if (crimeOrigin != null) {
                crimeOrigin.setTitle(crime.getTitle());
                crimeOrigin.setDate(crime.getDate().getTime());
                crimeOrigin.setSolved(crime.isSolved());
                crimeOrigin.setNeedPolice(crime.isNeedPolice());
            }
            PreparedStatement statement = connection.prepareStatement("UPDATE `crimes` SET title = ?, date = ?, solved = ?, police = ? WHERE uuid = ?;");
            statement.setString(1, crime.getTitle());
            statement.setLong(2, crime.getDate().getTime());
            statement.setShort(3, (short) (crime.isSolved() ? 1 : 0));
            statement.setShort(4, (short) (crime.isNeedPolice() ? 1 : 0));
            statement.setString(5, crime.getId().toString());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Unable to update crime from database.");
        }
    }


}
