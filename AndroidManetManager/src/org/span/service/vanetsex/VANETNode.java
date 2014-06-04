package org.span.service.vanetsex;

public class VANETNode {
    
    private String stringAddress;
    private double latitude;
    private double longitude;
    private float distance;
    private long firstSeen;
    private long lastSeen;
    private int checkPeriodTimer = 0;
    
    
    public String getStringAddress() {
        return stringAddress;
    }
    public void setStringAddress(String stringAddress) {
        this.stringAddress = stringAddress;
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
    public float getDistance() {
        return distance;
    }
    public void setDistance(float distance) {
        this.distance = distance;
    }
    public long getFirstSeen() {
        return firstSeen;
    }
    public void setFirstSeen(long firstSeen) {
        this.firstSeen = firstSeen;
    }
    public long getLastSeen() {
        return lastSeen;
    }
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    public int getCheckPeriodTimer() {
        return checkPeriodTimer;
    }
    public void resetCheckPeriodTimer() {
        this.checkPeriodTimer = 0;
    }
    public int incrementCheckPeriodTimer() {
        this.checkPeriodTimer++;
        return this.checkPeriodTimer;
    }
    
    
}
