import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by oegod on 14.09.2017.
 */

class CrimesMap extends HashMap<UUID, Crime> {
    Crime getCrimeByPosition(int position) {
        for (Entry<UUID, Crime> entry : this.entrySet()) {
            Crime crime = entry.getValue();
            if (crime.getPosition() == position) {
                return crime;
            }
        }
        return null;
    }

    int getNewCrimeId() {
        final Integer[] max = {0};
        this.forEach((id, crime) -> {
            if (crime.getPosition() > max[0]) {
                max[0] = crime.getPosition();
            }
        });
        return max[0]+1;
    }

    List<Crime> getSortedList() {
        List<Crime> list = new ArrayList<>(this.values());
        list.sort(Crime::compareTo);
        return list;
    }


}
