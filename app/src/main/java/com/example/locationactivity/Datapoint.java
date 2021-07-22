package com.example.locationactivity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Datapoint implements Parcelable,Comparable<Datapoint>{

    private int mId;
    private long mTime;
    private double mLongitude;
    private double mLatitude;
    private String mPothole;

    public Datapoint (int id, long time, double longitude, double latitude, String pothole){
        mId=id;
        mTime=time;
        mLongitude=longitude;
        mLatitude=latitude;
        mPothole=pothole;
    }

    protected Datapoint(Parcel in) {
        mId = in.readInt();
        mTime = in.readLong();
        mLongitude = in.readDouble();
        mLatitude = in.readDouble();
        mPothole = in.readString();
    }

    public static final Parcelable.Creator<Datapoint> CREATOR = new Parcelable.Creator<Datapoint>() {
        @Override
        public Datapoint createFromParcel(Parcel in) {
            return new Datapoint(in);
        }

        @Override
        public Datapoint[] newArray(int size) {
            return new Datapoint[size];
        }
    };

    public int getmId() {
        return mId;
    }

    public long getmTime() {
        return mTime;
    }

    public double getmLongitude() {
        return mLongitude;
    }

    public double getmLatitude() {
        return mLatitude;
    }

    public String getmPothole() {
        return mPothole;
    }

    public void setmTime(long Time){
        mTime=Time;
    }

    public void setmLongitude(double longitude){
        mLongitude=longitude;
    }

    public void setmLatitude(double latitude){
        mLatitude=latitude;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeLong(mTime);
        dest.writeDouble(mLongitude);
        dest.writeDouble(mLatitude);
        dest.writeString(mPothole);
    }

    @Override
    public int compareTo(Datapoint o) {
        //sorting in ascending order
        return Long.compare(mTime,o.mTime);
    }

    @NonNull
    @Override
    public String toString() {
        return "Name: " + mId + " Time: " + mTime;
    }



}
