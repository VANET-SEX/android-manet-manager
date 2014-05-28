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
    private String stringOriginatorAddress;
    private String text;
    
    /*
     * Parcelable protocol.
     * TODO: See TODO in VANETMessage.java.
     */
    public VANETEvent(Parcel in) {
        this.type = in.readByte();
        this.id = in.readInt();
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.stringOriginatorAddress = in.readString();
        this.text = in.readString();
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
        dest.writeString(stringOriginatorAddress);
        dest.writeString(text);
    }
    
    public static final Parcelable.Creator<VANETEvent> CREATOR = new Parcelable.Creator<VANETEvent>() {

        @Override
        public VANETEvent createFromParcel(Parcel source) {
            return new VANETEvent(source);
        }

        @Override
        public VANETEvent[] newArray(int size) {
            throw new UnsupportedOperationException();
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

    public static Parcelable.Creator<VANETEvent> getCreator() {
        return CREATOR;
    }
    
    /*
     * Helper methods.
     */
    public String getTypeTitle() {
        if(type == TYPE_EVENT_A) {
            return "Event-A";
        } else if(type == TYPE_EVENT_B) {
            return "Event-B";
        } else if(type == TYPE_EVENT_C) {
            return "Event-C";
        } else if(type == TYPE_EVENT_D) {
            return "Event-D";
        } else {
            return "<unknown_type:" + type + ">";
        }
    }

}
