package com.oegodf.crime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by oegod on 14.09.2017.
 */

public class CrimesMap extends HashMap<UUID, CrimeBase> {
    public CrimeBase getCrimeByPosition(int position) {
        for (Entry<UUID, CrimeBase> entry : this.entrySet()) {
            CrimeBase crime = entry.getValue();
            if (crime.getPosition() == position) {
                return crime;
            }
        }
        return null;
    }

    public int getNewCrimeId() {
        final Integer[] max = {0};
        this.forEach((id, crime) -> {
            if (crime.getPosition() > max[0]) {
                max[0] = crime.getPosition();
            }
        });
        return max[0]+1;
    }

    public List<CrimeBase> getSortedList() {
        List<CrimeBase> list = new ArrayList<>(this.values());
        list.sort(CrimeBase::compareTo);
        return list;
    }


}
