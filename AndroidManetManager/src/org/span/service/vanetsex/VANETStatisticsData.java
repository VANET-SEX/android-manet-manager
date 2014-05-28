package org.span.service.vanetsex;

public class VANETStatisticsData {

    private int beaconsReceivedPackets;
    private int beaconsReceivedBytes;
    private int beaconsSentPackets;
    private int beaconsSentBytes;
    
    private int eventsReceivedPackets;
    private int eventsReceivedBytes;
    private int eventsSentPackets;
    private int eventsSentBytes;
    
    public VANETStatisticsData() {
        beaconsReceivedPackets = 0;
        beaconsReceivedBytes = 0;
        beaconsSentPackets = 0;
        beaconsSentBytes = 0;
        eventsReceivedPackets = 0;
        eventsReceivedBytes = 0;
        eventsSentPackets = 0;
        eventsSentBytes = 0;
    }

    public int getBeaconsReceivedPackets() {
        return beaconsReceivedPackets;
    }

    public void setBeaconsReceivedPackets(int beaconsReceivedPackets) {
        this.beaconsReceivedPackets = beaconsReceivedPackets;
    }

    public int getBeaconsReceivedBytes() {
        return beaconsReceivedBytes;
    }

    public void setBeaconsReceivedBytes(int beaconsReceivedBytes) {
        this.beaconsReceivedBytes = beaconsReceivedBytes;
    }

    public int getBeaconsSentPackets() {
        return beaconsSentPackets;
    }

    public void setBeaconsSentPackets(int beaconsSentPackets) {
        this.beaconsSentPackets = beaconsSentPackets;
    }

    public int getBeaconsSentBytes() {
        return beaconsSentBytes;
    }

    public void setBeaconsSentBytes(int beaconsSentBytes) {
        this.beaconsSentBytes = beaconsSentBytes;
    }

    public int getEventsReceivedPackets() {
        return eventsReceivedPackets;
    }

    public void setEventsReceivedPackets(int eventsReceivedPackets) {
        this.eventsReceivedPackets = eventsReceivedPackets;
    }

    public int getEventsReceivedBytes() {
        return eventsReceivedBytes;
    }

    public void setEventsReceivedBytes(int eventsReceivedBytes) {
        this.eventsReceivedBytes = eventsReceivedBytes;
    }

    public int getEventsSentPackets() {
        return eventsSentPackets;
    }

    public void setEventsSentPackets(int eventsSentPackets) {
        this.eventsSentPackets = eventsSentPackets;
    }

    public int getEventsSentBytes() {
        return eventsSentBytes;
    }

    public void setEventsSentBytes(int eventsSentBytes) {
        this.eventsSentBytes = eventsSentBytes;
    }


    /*
     * Update methods.
     */
    public void updateBeaconReceived(int bytes) {
        beaconsReceivedPackets++;
        beaconsReceivedBytes += bytes;
    }
    
    public void updateBeaconSent(int bytes) {
        beaconsSentPackets++;
        beaconsSentBytes += bytes;
    }
    
    public void updateEventReceived(int bytes) {
        eventsReceivedPackets++;
        eventsReceivedBytes += bytes;
    }
    
    public void updateEventSent(int bytes) {
        eventsSentPackets++;
        eventsSentBytes += bytes;
    }
    
    /*
     * Copy
     */
    public void copyFrom(VANETStatisticsData src) {
        setBeaconsReceivedPackets(src.getBeaconsReceivedPackets());
        setBeaconsReceivedBytes(src.getBeaconsReceivedBytes());
        setBeaconsSentPackets(src.getBeaconsSentPackets());
        setBeaconsSentBytes(src.getBeaconsSentBytes());
        
        setEventsReceivedPackets(src.getEventsReceivedPackets());
        setEventsReceivedBytes(src.getEventsReceivedBytes());
        setEventsSentPackets(src.getEventsSentPackets());
        setEventsSentBytes(src.getEventsSentBytes());
    }
}
