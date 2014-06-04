package org.span.service.vanetsex;

import java.text.SimpleDateFormat;
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


public class VANETService extends VANETServiceBase {
    
    private static final String TAG = VANETService.class.getSimpleName();
    
    private static final long GUI_UPDATE_PERIOD = 1000;
    
    private ManetManagerApp app;
    
    private final VANETServiceBinder binder = new VANETServiceBinder();
    
    private SimpleDateFormat dateFormat;
    
    protected Set<VANETServiceObserver> observers;
    
    private Map<Integer, VANETEvent> mapEvents;
    private List<VANETEvent> listEvents;
    
    StringBuilder sb;
    
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
    
    public void events1000(byte eventType) {
        Log.i(TAG, "events1000() - eventType: " + eventType);
        
        for(int i = 0; i<1000; i++) {
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
            event.setStringOriginatorAddress(hostAddress);
            
            int textBytes = 1024 * 1;
            if (sb == null) {
                sb = new StringBuilder(1000);
                for (int iTB = 0; iTB <= textBytes; iTB += 2) {
                    sb.append('a');
                }
            }
            event.setText(sb.toString());
            
//            event.setText(dateFormat.format(date) + " Event A ");
            
            event.setType(eventType);
            
            msg.setData(event);
            sendMessage(msg);
            
            mapEvents.put(event.getId(), event);
            listEvents.add(event);
            
            // update gui
            if(i%50 == 0) {
                for(VANETServiceObserver o : observers) {
                    o.onEventListChanged(listEvents);
                }
            }
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
    
    /*
     * 
     * Gui update timer task.
     * 
     */
    private class GuiUpdateTimerTask extends TimerTask {
        
        @Override
        public void run() {
            
        }
        
    }
    

}
