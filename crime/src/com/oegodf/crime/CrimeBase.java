package com.oegodf.crime;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class CrimeBase implements Serializable, Comparable  {
    protected int mPosition;
    protected UUID mId;
    protected String mTitle;
    protected Calendar mCalendar;
    protected boolean mSolved;
    protected boolean mNeedPolice;
    protected String mSuspect;
    protected boolean mNewCrime;

    public CrimeBase() {
        this (UUID.randomUUID(),false);
    }

    public CrimeBase(UUID id) {
        this(id, false);
    }

    public CrimeBase(UUID id, boolean newCrime) {
        mId = id;
        mCalendar = Calendar.getInstance();
        mNewCrime = newCrime;
    }

    public CrimeBase(CrimeBase c) {
        mId = c.getId();
        mTitle = c.getTitle();
        mSolved = c.isSolved();
        mNeedPolice = c.isNeedPolice();
        mCalendar = Calendar.getInstance();
        mNewCrime = false;
    }

    public void setNewCrime(boolean newCrime) {
        mNewCrime = newCrime;
    }

    public boolean isNeedPolice() {
        return mNeedPolice;
    }

    public void setNeedPolice(boolean needPolice) {
        mNeedPolice = needPolice;
    }

    public UUID getId() {
        return mId;
    }

    public void setId(UUID id) {
        mId = id;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public Date getDate() {
        return mCalendar.getTime();
    }

    public void setDate(long date) {
        this.setDate(new Date(date));
    }

    public void setDate(Date date) {
        mCalendar.setTime(date);
    }

    public boolean isSolved() {
        return mSolved;
    }

    public void setSolved(boolean solved) {
        mSolved = solved;
    }

    public boolean isNewCrime() {
        return mNewCrime;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public String getSuspect() {
        return mSuspect;
    }

    public void setSuspect(String suspect) {
        mSuspect = suspect;
    }

    @Override
    public int compareTo(Object o) {
        CrimeBase crime = (CrimeBase) o;
        return this.getPosition()-crime.getPosition();
    }
}