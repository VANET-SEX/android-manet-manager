package org.span.service.vanetsex;

import android.os.Parcel;
import android.os.Parcelable;


public class VANETMessage implements Parcelable {
    
    public static final byte TYPE_BEACON = (byte)1;
    public static final byte TYPE_EVENT = (byte)2;

    private byte type;
    private String stringAddressSource;
    private String stringAddressDestination;
    private boolean incoming;
    private double latitude;
    private double longitude;
    private int messageId;
    
    private int sizeInBytes;
    
    private Parcelable data;
    
    /*
     * Constructor
     */
    public VANETMessage() {
        // TODO Auto-generated constructor stub
    }
    
    /*
     * Parcelable protocol.
     * 
     * TODO:
     * Android's Parcelable protocol is supposed to be used for interprocess
     * communication, not for communication between devices that are possibly
     * having different versions of Android and implementation of Parcelable
     * protocol. In future introduce VANETParcelable that will handle
     * serialization of data and object/memory recycling...
     */
    private VANETMessage(Parcel in) {
        sizeInBytes = in.dataSize();
        
        type = in.readByte();
        latitude = in.readDouble();
        longitude = in.readDouble();
        messageId = in.readInt();
        
        switch (type) {
        case VANETMessage.TYPE_BEACON:
            // TODO: Check if there will be some beacon specific data...
            data = null;
            break;
            
        case VANETMessage.TYPE_EVENT:
            data = VANETEvent.CREATOR.createFromParcel(in);
            break;
            
//        case VANETMessage.TYPE_EVENT_ARRAY:
//            data = VANETEvent.CREATOR.createFromParcel(in);
//            break;

        default:
            break;
        }
    }
    


    @Override
    public int describeContents() {
        return 0;
    }
    
    public static final Parcelable.Creator<VANETMessage> CREATOR = new Parcelable.Creator<VANETMessage>() {

        @Override
        public VANETMessage createFromParcel(Parcel source) {
            return new VANETMessage(source);
        }

        @Override
        public VANETMessage[] newArray(int size) {
            throw new UnsupportedOperationException();
        }
    };
    
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(type);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeInt(messageId);
        if(data != null) {
            data.writeToParcel(dest, flags);
        }
        
        sizeInBytes = dest.dataSize();
    }
    
    /*
     * Getters and setters
     */
    public byte getType() {
        return type;
    }
    public void setType(byte type) {
        this.type = type;
    }
    public String getStringAddressSource() {
        return stringAddressSource;
    }
    public void setStringAddressSource(String stringAddressSource) {
        this.stringAddressSource = stringAddressSource;
    }
    public String getStringAddressDestination() {
        return stringAddressDestination;
    }
    public void setStringAddressDestination(String stringAddressDestination) {
        this.stringAddressDestination = stringAddressDestination;
    }
    public boolean isIncoming() {
        return incoming;
    }
    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
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
    public int getMessageId() {
        return messageId;
    }
    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
    public Parcelable getData() {
        return data;
    }
    public void setData(Parcelable data) {
        this.data = data;
    }
    public int getSizeInBytes() {
        return sizeInBytes;
    }
    
}
