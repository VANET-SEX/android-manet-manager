package org.span.service.vanetsex;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;


public class VANETPingPongState {
    
    private String opponentAddress;
    
    private boolean finished = false;
    
    private int packetsSent = 0;
    private int packetsReceived = 0;
    
    private long averageResponseTime = 0;
    private long minResponseTime = 0;
    private long maxResponseTime = 0;
    
    private long lastPacketSentAt = -1;

    private byte[] dummyData = null;
    private int numberOfPacketsToSend;
    
    public VANETPingPongState(String opponentAddress, int numberOfPacketsToSend, int packetPayloadSize) {
        this.opponentAddress = opponentAddress;
        this.numberOfPacketsToSend = numberOfPacketsToSend;
        Arrays.fill(dummyData, (byte)0);
    }
    
    public void packetSent() {
        lastPacketSentAt = System.currentTimeMillis();
        packetsSent++;
    }
    
    public void packetReceived() {
        if(lastPacketSentAt == -1) {
            return;
        }

        packetsReceived++;
        
        long respTime = System.currentTimeMillis() - lastPacketSentAt;
        
        if(packetsReceived > 1) {
            averageResponseTime = (averageResponseTime + respTime) / 2;
            
            if(respTime > maxResponseTime) {
                maxResponseTime = respTime;
            }
            
            if(respTime < minResponseTime) {
                minResponseTime = respTime;
            }
            
        } else {
            averageResponseTime = respTime;
            maxResponseTime = respTime;
            minResponseTime = respTime;
        }
        
    }
    
    
    
    /*
     * Getters
     */

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getOpponentAddress() {
        return opponentAddress;
    }

    public int getPacketsSent() {
        return packetsSent;
    }

    public int getPacketsReceived() {
        return packetsReceived;
    }

    public long getAverageResponseTime() {
        return averageResponseTime;
    }

    public long getMinResponseTime() {
        return minResponseTime;
    }

    public long getMaxResponseTime() {
        return maxResponseTime;
    }

    public byte[] getDummyData() {
        return dummyData;
    }

    public int getNumberOfPacketsToSend() {
        return numberOfPacketsToSend;
    }
}
