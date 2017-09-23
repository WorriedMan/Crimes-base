import java.io.*;
import java.util.Date;
import java.util.UUID;

public class Crime implements Serializable {
    private UUID mId;
    private String mTitle;
    private Date mDate;
    private boolean mSolved;
    private boolean mNeedPolice;

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
        return false;
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
}