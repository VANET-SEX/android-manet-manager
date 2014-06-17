package org.span.service.vanetsex;

import android.os.Parcel;
import android.os.Parcelable;

public class VANETPingPongPacket implements Parcelable {
    
    private int counter;
    private byte[] dummyData;

    public VANETPingPongPacket() {
    }
    
    /*
     * Parcelable protocol.
     * TODO: See TODO in VANETMessage.java.
     */
    public VANETPingPongPacket(Parcel in) {
        this.counter = in.readInt();
        in.readByteArray(this.dummyData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(counter);
        dest.writeByteArray(dummyData);
    }
    
    public static final Parcelable.Creator<VANETPingPongPacket> CREATOR = new Parcelable.Creator<VANETPingPongPacket>() {

        @Override
        public VANETPingPongPacket createFromParcel(Parcel source) {
            return new VANETPingPongPacket(source);
        }

        @Override
        public VANETPingPongPacket[] newArray(int size) {
            return new VANETPingPongPacket[size];
        }
    };
    
    /*
     * Getters and setters.
     */
    
    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
    
    public byte[] getDummyData() {
        return dummyData;
    }

    public void setDummyData(byte[] dummyData) {
        this.dummyData = dummyData;
    }

    public static Parcelable.Creator<VANETPingPongPacket> getCreator() {
        return CREATOR;
    }

}
