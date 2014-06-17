package org.span.service.vanetsex;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;

import org.span.manager.ManetManagerApp;

import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


public class VANETService extends VANETServiceBase {
    
    private static final String TAG = VANETService.class.getSimpleName();
    
    private static final long GUI_UPDATE_PERIOD = 1000;
    
    private ManetManagerApp app;
    
    private final VANETServiceBinder binder = new VANETServiceBinder();
    
    private SimpleDateFormat dateFormat;
    
    protected Set<VANETServiceObserver> observers;
    
    private Map<Integer, VANETEvent> mapEvents;
    private List<VANETEvent> listEvents;
    
    private Location currentLocation = null;
    StringBuilder sb;
    
    private VANETPingPongState pingPongState;
    
    
    /*
     * Listeners / Observers interfaces
     */
    
    /*
     * 
     * VENETService event handling methods.
     * 
     */
    
    @Override
    public void onCreateVANETService() {
        Log.i(TAG, "onCreateVANETService()");
        
        app = ManetManagerApp.getInstance();
        
        observers = new HashSet<VANETServiceObserver>();
        mapEvents = new HashMap<Integer, VANETEvent>();
        listEvents = new LinkedList<VANETEvent>();
        dateFormat = new SimpleDateFormat("HH:mm:ss");
        pingPongState = null;
        
        // init observer
        for(VANETServiceObserver o : observers) {
            o.onEventListChanged(listEvents);
        }
    }

    @Override
    public void onStartVANETService() {
        Log.i(TAG, "onStartVANETService()");
        
        
    }

    @Override
    public void onDestroyVANETService() {
        Log.i(TAG, "onDestroyVANETService()");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return binder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onMessage(VANETMessage dataMessage) {
        Log.d(TAG, "onData() - from: " + dataMessage.getStringAddressSource() + "; msg.id: " + dataMessage.getMessageId());
        
        // Handle received event message.
        if(dataMessage.getType() == VANETMessage.TYPE_EVENT) {
            VANETEvent event = (VANETEvent)dataMessage.getData();
            
            // If event is first time received, add it to list, update gui and rebroadcast it
            if(!mapEvents.containsKey(event.getId())) {
                mapEvents.put(event.getId(), event);
                listEvents.add(event);
                
                // update gui
                for(VANETServiceObserver o : observers) {
                    o.onEventListChanged(listEvents);
                }
                
                // rebroadcast
                
                VANETMessage rebroadcastMsg = new VANETMessage();
                rebroadcastMsg.setStringAddressSource(hostAddress);
                rebroadcastMsg.setStringAddressDestination(app.manetcfg.getIpBroadcast());
                rebroadcastMsg.setType(VANETMessage.TYPE_EVENT);
                rebroadcastMsg.setMessageId(app.generateNextVANETMessageID());
                rebroadcastMsg.setLatitude(getCurrentLocation().getLatitude());
                rebroadcastMsg.setLongitude(getCurrentLocation().getLongitude());
                
                rebroadcastMsg.setData(event);
                
                sendMessage(rebroadcastMsg);
            }
        } else if(dataMessage.getType() == VANETMessage.TYPE_PING_PONG) {
            
            VANETPingPongPacket pp = (VANETPingPongPacket) dataMessage.getData();
            byte[] dummyData = pp.getDummyData();
            
            if(pingPongState != null) {
                pingPongState.packetReceived();
                dummyData = pingPongState.getDummyData();
            }
            
            if(pingPongState.getPacketsSent() < pingPongState.getNumberOfPacketsToSend()) {
                sendPingPongPacket(dataMessage.getStringAddressSource(), (pp.getCounter() + 1), dummyData);
            } else {
                
            }
        }
    }

    @Override
    public void onBeaconReceived(VANETMessage beaconMessage, VANETNode neighbor) {
        Log.d(TAG, "onBeaconReceived() - from: " + beaconMessage.getStringAddressSource() + "; msg.id: " + beaconMessage.getMessageId());

    }
    
    @Override
    public void onBeaconSent(VANETMessage beaconMessage) {
        Log.d(TAG, "onBeaconSent()");
        
    }
    
    @Override
    public void onNeighborAppeared(VANETNode node) {
        
    }
    
    @Override
    public void onNeighborDisappeared(VANETNode node) {
        
    }
    
    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        
        // Calculate new distances to locations of events.
        float dist[] = null;
        for(VANETEvent ev : listEvents) {
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), ev.getLatitude(), ev.getLongitude(), dist);
            ev.setDistance(dist[0]);
        }
        
        // update gui
        for(VANETServiceObserver o : observers) {
            o.onEventListChanged(listEvents);
        }
    }
    
    
    /*
     * 
     * VANETServiceBase overriden public methods.
     * 
     */
    @Override
    public void startBeacon() {
        Log.d(TAG, "startBeacon()");

        // Do something before beacons are started.
        
        super.startBeacon();
    }
    
    @Override
    public void stopBeacon() {
        Log.d(TAG, "stopBeacon()");
        
        // Do something before beacons are stopped.
        
        super.stopBeacon();
    }
    
    /*
     * 
     * Public methods.
     * 
     */
    public void registerObserver(VANETServiceObserver observer) {
        observers.add(observer);
        
        // init observer update
        observer.onEventListChanged(listEvents);
    }

    public void unregisterObserver(VANETServiceObserver observer) {
        observers.remove(observer);
    }
    
    public void event(byte eventType) {
        Log.i(TAG, "event() - eventType: " + eventType);
        
        Date date = new Date();
        
        // Compose VANET message - "HEADER"
        VANETMessage msg = new VANETMessage();
        msg.setStringAddressSource(hostAddress);
        msg.setStringAddressDestination(app.manetcfg.getIpBroadcast());
        msg.setType(VANETMessage.TYPE_EVENT);
        msg.setMessageId(app.generateNextVANETMessageID());
        msg.setLatitude(getCurrentLocation().getLatitude());
        msg.setLongitude(getCurrentLocation().getLongitude());
        
        // Compose VANET message - "BODY"
        // BODY has to be Parcelable. In this specific case, VANETEvent
        // implements parcelable and event is sent as body of vanet message.
        VANETEvent event = new VANETEvent();
        event.setId(app.generateNextVANETEventID());
        event.setLatitude(getCurrentLocation().getLatitude());
        event.setLongitude(getCurrentLocation().getLongitude());
        event.setTime(System.currentTimeMillis());
        event.setStringOriginatorAddress(hostAddress);
        
        event.setText("Event: " + eventType);
//        event.setText(dateFormat.format(date) + " Event A ");
        
        event.setType(eventType);
        
        msg.setData(event);
        sendMessage(msg);
        
        mapEvents.put(event.getId(), event);
        listEvents.add(event);
        
        // update gui
        for(VANETServiceObserver o : observers) {
            o.onEventListChanged(listEvents);
        }
    }
    
    public void doPingPong(String destinationAddress, int numberOfPackets, int packetPayloadSize) {
        Log.i(TAG, "startPingPong() - destinationAddress: " + destinationAddress + " numberOfPackets: " + numberOfPackets + " packetPayloadSize: " + packetPayloadSize);
        
        pingPongState = new VANETPingPongState(destinationAddress, numberOfPackets, packetPayloadSize);
        sendPingPongPacket(destinationAddress, 0, pingPongState.getDummyData());
    }
    
    private void sendPingPongPacket(String destinationAddress, int counter, byte[] dummyData) {
        
        // Compose VANET message - "HEADER"
        VANETMessage msg = new VANETMessage();
        msg.setStringAddressSource(hostAddress);
        msg.setStringAddressDestination(destinationAddress);
        msg.setType(VANETMessage.TYPE_PING_PONG);
        msg.setMessageId(app.generateNextVANETMessageID());
        msg.setLatitude(getCurrentLocation().getLatitude());
        msg.setLongitude(getCurrentLocation().getLongitude());
        
        // VANETPingPongPacket
        VANETPingPongPacket pp = new VANETPingPongPacket();
        pp.setCounter(counter);
        pp.setDummyData(dummyData);
        
        msg.setData(pp);
        sendMessage(msg);
        
        if(pingPongState != null) {
            pingPongState.packetSent();
        }
    }
    
    /*
     * 
     * Private methods.
     * 
     */
    
    /*
     * 
     * Service binder.
     * 
     */
    public class VANETServiceBinder extends Binder {
        public VANETService getService() {
            return VANETService.this;
        }
    }
    
}
