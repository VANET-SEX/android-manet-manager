package org.span.service.vanetsex;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class VANETExchangeEventsEventList implements Parcelable {
	
	private List<VANETEvent> events;

	public VANETExchangeEventsEventList() {
		events = new ArrayList<VANETEvent>(10);
	}
	
	public VANETExchangeEventsEventList(Parcel in) {
		this.events = new ArrayList<VANETEvent>(in.readInt());
		in.readTypedList(this.events, VANETEvent.CREATOR);
	}
	
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {

		dest.writeInt(events.size());
		dest.writeTypedList(events);
		
	}
	
    public static final Parcelable.Creator<VANETExchangeEventsEventList> CREATOR = new Parcelable.Creator<VANETExchangeEventsEventList>() {

        @Override
        public VANETExchangeEventsEventList createFromParcel(Parcel source) {
            return new VANETExchangeEventsEventList(source);
        }

        @Override
        public VANETExchangeEventsEventList[] newArray(int size) {
        	throw new UnsupportedOperationException();
        }
    };
	
	/*
	 * 
	 * Getters and setters.
	 * 
	 */
	public List<VANETEvent> getEvents() {
		return events;
	}

	public void setEvents(List<VANETEvent> events) {
		this.events = events;
	}

}
