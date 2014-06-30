package org.span.service.vanetsex;

import android.os.Parcel;
import android.os.Parcelable;

public class VANETEvent implements Parcelable {

    public static byte TYPE_EVENT_A = 1;
    public static byte TYPE_EVENT_B = 2;
    public static byte TYPE_EVENT_C = 3;
    public static byte TYPE_EVENT_D = 4;
    
    private byte type;
    private int id;
    private double latitude;
    private double longitude;
    private long time;
    private String stringOriginatorAddress;
    private String text;
    private byte[] dummyData;
    /* Not serialized */
    private float distance;
    private long delay;
    
    public VANETEvent() {
    }
    
    /*
     * Parcelable protocol.
     * TODO: See TODO in VANETMessage.java.
     */
    public VANETEvent(Parcel in) {
        this.type = in.readByte();
        this.id = in.readInt();
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.time = in.readLong();
        this.stringOriginatorAddress = in.readString();
        this.text = in.readString();
        dummyData = new byte[in.readInt()];
        in.readByteArray(dummyData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(type);
        dest.writeInt(id);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeLong(time);
        dest.writeString(stringOriginatorAddress);
        dest.writeString(text);
        dest.writeInt(dummyData.length);
        dest.writeByteArray(dummyData);
    }
    
    public static final Parcelable.Creator<VANETEvent> CREATOR = new Parcelable.Creator<VANETEvent>() {

        @Override
        public VANETEvent createFromParcel(Parcel source) {
            return new VANETEvent(source);
        }

        @Override
        public VANETEvent[] newArray(int size) {
            return new VANETEvent[size];
        }
    };
    
    /*
     * Getters and setters.
     */

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getStringOriginatorAddress() {
        return stringOriginatorAddress;
    }

    public void setStringOriginatorAddress(String stringOriginatorAddress) {
        this.stringOriginatorAddress = stringOriginatorAddress;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    public byte[] getDummyData() {
		return dummyData;
	}

	public void setDummyData(byte[] dummyData) {
		this.dummyData = dummyData;
	}

	public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }
    
    public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public static Parcelable.Creator<VANETEvent> getCreator() {
        return CREATOR;
    }
    
    /*
     * Helper methods.
     */
    public String getTypeTitle() {
        if(type == TYPE_EVENT_A) {
            return "Ev.Small";
        } else if(type == TYPE_EVENT_B) {
            return "Ev.Medium";
        } else if(type == TYPE_EVENT_C) {
            return "Ev.Large";
        } else if(type == TYPE_EVENT_D) {
            return "Event-D";
        } else {
            return "<unknown_type:" + type + ">";
        }
    }

}
