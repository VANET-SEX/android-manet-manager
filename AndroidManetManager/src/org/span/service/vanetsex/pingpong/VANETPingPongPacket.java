package org.span.service.vanetsex.pingpong;

import android.os.Parcel;
import android.os.Parcelable;

public class VANETPingPongPacket implements Parcelable {
    
    private int packetIdx;
    // If request == 0 than it is response. Otherwise it is request.
    private int request;
    private byte[] data;

    public VANETPingPongPacket() {
    }
    
    public VANETPingPongPacket(Parcel in) {
        this.packetIdx = in.readInt();
        this.request = in.readInt();
        int length = in.readInt();
        data = new byte[length];
        in.readByteArray(this.data);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(packetIdx);
        dest.writeInt(request);
        dest.writeInt(data.length);
        dest.writeByteArray(data);
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
    
    public int getPacketIdx() {
        return packetIdx;
    }

    public void setPacketIdx(int idx) {
        this.packetIdx = idx;
    }
    
    public int getRequest() {
		return request;
	}

	public void setRequest(int request) {
		this.request = request;
	}

	public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public static Parcelable.Creator<VANETPingPongPacket> getCreator() {
        return CREATOR;
    }

}
