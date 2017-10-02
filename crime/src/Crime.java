import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Crime implements Serializable, Comparable  {
    private UUID mId;
    private String mTitle;
    private Date mDate;
    private boolean mSolved;
    private boolean mNeedPolice;
    private int mPosition;

    UUID getId() {
        return mId;
    }

    public void setId(UUID mId) {
        this.mId = mId;
    }

    String getTitle() {
        return mTitle;
    }

    void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    Date getDate() {
        return mDate;
    }

    void setDate(long time) {
        this.mDate.setTime(time);
    }

    boolean isSolved() {
        return mSolved;
    }

    void setSolved(boolean mSolved) {
        this.mSolved = mSolved;
    }

    boolean needPolice() {
        return mNeedPolice;
    }

    void setPolice(boolean police) {
        mNeedPolice = police;
    }

    Crime() {
        mId = UUID.randomUUID();
        mDate = new Date();
    }
    Crime(Crime c) {
        mId = c.getId();
        mTitle = c.getTitle();
        mDate = c.getDate();
        mSolved = c.isSolved();
        mNeedPolice = c.needPolice();
    }

    int getPosition() {
        return mPosition;
    }

    void setPosition(int position) {
        mPosition = position;
    }

    @Override
    public int compareTo(Object o) {
        Crime crime = (Crime) o;
        return this.getPosition()-crime.getPosition();
    }
}