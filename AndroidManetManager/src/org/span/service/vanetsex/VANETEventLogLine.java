package org.span.service.vanetsex;

import java.text.SimpleDateFormat;
import java.util.Date;

public class VANETEventLogLine {
	
	private int eventId;
	private int eventType;
	private double srcLat;
	private double srcLong;
	private long srcTime;
	private float distance;
	private long delay;

	public VANETEventLogLine(int eventId, int eventType, double srcLat, double srcLong, long srcTime, float distance, long delay) {
		this.eventId = eventId;
		this.eventType = eventType;
		this.srcLat = srcLat;
		this.srcLong = srcLong;
		this.srcTime = srcTime;
		this.distance = distance;
		this.delay = delay;
	}

	public String toString() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");
		String strDateTime = formatter.format(new Date(srcTime));
		
		String strDelay = VANETUtils.formatTimeInterval(delay);
		
		return String.format("%d;%d;%.6f;%.6f;%s;%d;%s;%n", eventId, eventType, srcLat, srcLong, strDateTime, (int)distance, strDelay);
	}
	
	public static String getHeaderLogLine() {
		return "EventID;EventType;EventLat;EventLong;EventDate;Distance;Delay;\n";
	}

	
	/*
	 * 
	 * Getters
	 * 
	 */
	public int getEventId() {
		return eventId;
	}

	public int getEventType() {
		return eventType;
	}

	public double getSrcLat() {
		return srcLat;
	}

	public double getSrcLong() {
		return srcLong;
	}

	public long getSrcTime() {
		return srcTime;
	}

	public float getDistance() {
		return distance;
	}

	public long getDelay() {
		return delay;
	}
}
