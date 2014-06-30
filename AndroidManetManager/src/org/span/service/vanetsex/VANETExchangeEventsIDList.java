package org.span.service.vanetsex;

import android.os.Parcel;
import android.os.Parcelable;

public class VANETExchangeEventsIDList implements Parcelable {
	
	private int[] eventIDs;
	
	public VANETExchangeEventsIDList() {
		
	}

	public VANETExchangeEventsIDList(Parcel in) {
		this.eventIDs = new int[in.readInt()];
		in.readIntArray(this.eventIDs);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {

		dest.writeInt(eventIDs.length);
		dest.writeIntArray(eventIDs);
		
	}
	
    public static final Parcelable.Creator<VANETExchangeEventsIDList> CREATOR = new Parcelable.Creator<VANETExchangeEventsIDList>() {

        @Override
        public VANETExchangeEventsIDList createFromParcel(Parcel source) {
            return new VANETExchangeEventsIDList(source);
        }

        @Override
        public VANETExchangeEventsIDList[] newArray(int size) {
        	throw new UnsupportedOperationException();
        }
    };
	
	/*
	 * 
	 * Getters and setters.
	 * 
	 */
	public int[] getEventIDs() {
		return eventIDs;
	}

	public void setEventIDs(int[] eventIDs) {
		this.eventIDs = eventIDs;
	}

}
