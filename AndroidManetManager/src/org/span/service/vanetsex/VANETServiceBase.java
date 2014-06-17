package org.span.service.vanetsex;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.span.manager.ManetManagerApp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.util.Log;
import android.widget.Toast;

public abstract class VANETServiceBase extends Service {

    private static final String TAG = VANETServiceBase.class.getSimpleName();
    
    public static final int UDP_MAX_PACKET_SIZE = 1024 * 1024;

    public static final int BEACONS_UDP_RX_PORT = 5656;
    public static final int BEACONS_HISTORY_SIZE = 1000;
    public static final int BEACONS_CHECK_TIMEOUT_PERIODS = 2;
    
    public static final int DATA_UDP_RX_PORT = 5657;
    
    public static final int TIMER_UPDATE_UI_PERIOD = 1000;
    
    /*
     * 
     * Private and protected field members.
     * 
     */
    
    private ManetManagerApp app;
    private Handler handler;
    protected String hostAddress;
    
    // beacon related members
    private long beaconPeriod;
    private Map<String, VANETNode> neighborNodesMap;
    private Map<String, VANETNode> neighborNodesMapCopy;
    private List<VANETMessage> messages;
    private List<VANETMessage> MessagesDiff;
    private List<VANETMessage> messagesDiffCopy;
    private VANETBeaconListenerThread listenerThread = null;
    private Timer beaconBroadcastTimer;
    private VANETBeaconBroadcastTimerTask beaconBroadcastTimerTask;
    private VANETMessageListenerThread messageListenerThread;
    private VANETMessageSendingThread messageSendingThread;
    
    protected Set<VANETServiceBaseObserver> observers;
    
    private VANETUpdateUiTimerTask updateUiTimerTask;
    private VANETStatisticsData statisticsData;
    private VANETStatisticsData statisticsDataCopy;

    private LocationManager myLocationManager;
    private MyLocationListener myLocationListener;
    private final int minTimeRefreshInterval = 500; // 0,5 seconds
    private final int minLocationDistance = 2; // 2 meters
    private Location currentLocation = null;
    
    /*
     * 
     * Public interface.
     * 
     */
    public abstract void onCreateVANETService();
    
    public abstract void onStartVANETService();
    
    public abstract void onDestroyVANETService();
    
    public abstract void onMessage(VANETMessage dataMessage);
    
    public abstract void onBeaconReceived(VANETMessage beaconMessage, VANETNode neighbor);
    
    public abstract void onBeaconSent(VANETMessage beaconMessage);
    
    public abstract void onNeighborAppeared(VANETNode node);
    
    public abstract void onNeighborDisappeared(VANETNode node);
    
    public abstract void onLocationChanged(Location location);
    
    public void sendMessage(VANETMessage message) {
        messageSendingThread.sendMessage(message);
    }

    public void startBeacon() {
        
        Log.i(TAG, "startBeacon() - start listener thread");
        
        if(listenerThread == null) {
            
            beaconPeriod = app.vanetPrefs.get_beacon_period();
            
            listenerThread = new VANETBeaconListenerThread();
            listenerThread.start();
        }
        
        Log.i(TAG, "startBeacon() - start periodic broadcast timer task");
        
        beaconBroadcastTimer = new Timer();
        beaconBroadcastTimerTask = new VANETBeaconBroadcastTimerTask();
        beaconBroadcastTimer.schedule(beaconBroadcastTimerTask, 100, beaconPeriod);
        
//        app.vanetPrefs.edit().put_beacon_started(true).commit();
        
        for(VANETServiceBaseObserver o : observers) {
            o.onBeaconStateChanged(true);
        }
    }
    
    public void stopBeacon() {
        // ...
        Log.i(TAG, "stopBeacon() - stop listener thread");
        
//        app.vanetPrefs.edit().put_beacon_started(false).commit();
        
        if(listenerThread != null) {
            listenerThread.terminate();
            listenerThread = null;
        }
        
        Log.i(TAG, "stopBeacon() - stop periodic broadcast timer task");
        
        if(beaconBroadcastTimer != null) {
            beaconBroadcastTimer.cancel();
        }
        
        for(VANETServiceBaseObserver o : observers) {
            o.onBeaconStateChanged(false);
        }
    }
    
    public Map<String, VANETNode> getNeighbourNodesMap() {
        return neighborNodesMap;
    }
    
    public List<VANETMessage> getBeaconMessageHistory() {
        return messages;
    }
    
    public void setBeaconPeriod(long milliseconds) {
        this.beaconPeriod = milliseconds;
    }
    
    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }
    
    public boolean isBeaconRunning() {
        return (listenerThread != null);
    }
    
    public Location getCurrentLocation() {
        return currentLocation;
    }
    
    /*
     * 
     * Observer methods.
     * 
     */
    public void registerObserver(VANETServiceBaseObserver observer) {
        observers.add(observer);
        
        // init observer update
        observer.onBeaconStateChanged(isBeaconRunning());
        observer.onMessageHistoryInit(messages);
        observer.onNeighborListChanged(neighborNodesMapCopy);
        observer.onStatisticData(statisticsDataCopy);
    }
    
    public void unregisterObserver(VANETServiceBaseObserver observer) {
        observers.remove(observer);
    }
    
    /*
     * 
     * Lifecycle methods. They can be overridden by inherited class, but inherited class must call super class' implementation.
     * 
     */
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        
        myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        currentLocation = myLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        myLocationListener = new MyLocationListener();
        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,minTimeRefreshInterval, minLocationDistance,myLocationListener);
        
        observers = new HashSet<VANETServiceBaseObserver>();
        statisticsData = new VANETStatisticsData();
        statisticsDataCopy = new VANETStatisticsData();
        app = (ManetManagerApp)getApplication();
        handler = new Handler();
        neighborNodesMap = new HashMap<String, VANETNode>();
        neighborNodesMapCopy = new HashMap<String, VANETNode>();
        messages = new LinkedList<VANETMessage>();
        MessagesDiff = new LinkedList<VANETMessage>();
        messagesDiffCopy = new LinkedList<VANETMessage>();
        
        hostAddress = app.manetcfg.getIpAddress();
        
        // Start periodic task for gui update.
        updateUiTimerTask = new VANETUpdateUiTimerTask();
        updateUiTimerTask.startTimer(TIMER_UPDATE_UI_PERIOD, TIMER_UPDATE_UI_PERIOD);
        
        // Start message receiving thread.
        messageListenerThread = new VANETMessageListenerThread();
        messageListenerThread.start();
        
        // Start message sending thread.
        messageSendingThread = new VANETMessageSendingThread();
        messageSendingThread.start();
        
        onCreateVANETService();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        
        if(app.vanetPrefs.get_vanet_service_started() == false) {
            onStartVANETService();
            app.vanetPrefs.edit().put_vanet_service_started(true).commit();
        }
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        
        onDestroyVANETService();
        
        stopBeacon();
        
        if(messageSendingThread != null) {
            messageSendingThread.terminate();
        }
        
        if(messageListenerThread != null) {
            messageListenerThread.terminate();
        }
        
        if(updateUiTimerTask != null) {
            updateUiTimerTask.stopTimer();
        }
        
        myLocationManager.removeUpdates(myLocationListener);

        
        
        app.vanetPrefs.edit().put_vanet_service_started(false).commit();
    }
    

    
    /*
     * 
     * Private and protected
     * 
     */
    protected void handleBeaconReceived(VANETMessage receivedBeacon, float distance) {
        
        boolean nodeAppeared = false;
        long currentMillisec = System.currentTimeMillis();
        statisticsData.updateBeaconReceived(receivedBeacon.getSizeInBytes());
        
        // Update the neighbor list.
        VANETNode node = neighborNodesMap.get(receivedBeacon.getStringAddressSource());
        if(node == null) {
            node = new VANETNode();
            node.setStringAddress(receivedBeacon.getStringAddressSource());
            node.setFirstSeen(currentMillisec);
            neighborNodesMap.put(receivedBeacon.getStringAddressSource(), node);
            
            nodeAppeared = true;
            Log.d(TAG, "................. + + + + ADDING NEIGHBOR: " + receivedBeacon.getStringAddressSource());
        }
        node.setLatitude(receivedBeacon.getLatitude());
        node.setLongitude(receivedBeacon.getLongitude());
        node.setDistance(distance);
        node.setLastSeen(currentMillisec);
        node.resetCheckPeriodTimer();
        
        // Update the beacon history list.
        addToBeaconMessageHistory(receivedBeacon);
        
        onBeaconReceived(receivedBeacon, node);
        
        if(nodeAppeared) {
            onNeighborAppeared(node);
        }
    }
    
    protected void handleBeaconSent(VANETMessage sentBeacon) {
        
        statisticsData.updateBeaconSent(sentBeacon.getSizeInBytes());
        addToBeaconMessageHistory(sentBeacon);
        
        handleNodeTimeoutCheck();
        
        // Call abstract method.
        onBeaconSent(sentBeacon);
    }
    
    private void addToBeaconMessageHistory(VANETMessage beaconMsg) {
        MessagesDiff.add(beaconMsg);
    }
    
    protected void handleNodeTimeoutCheck() {
        Set<String> neighborList = neighborNodesMap.keySet();
        for(String neighborAddress : neighborList) {
            VANETNode node = neighborNodesMap.get(neighborAddress);
            if(node.incrementCheckPeriodTimer() >= BEACONS_CHECK_TIMEOUT_PERIODS) {
                Log.d(TAG, "......................REMOVING NEIGHBOR: " + neighborAddress);
                neighborNodesMap.remove(neighborAddress);
                onNeighborDisappeared(node);
            }
        }
    }
    
    /* 
     * 
     * VANETBeaconListenerThread
     * 
     */
    private class VANETBeaconListenerThread extends Thread {
        
        private final String TAG = VANETBeaconListenerThread.class.getSimpleName();
        
        private DatagramSocket socket = null;
        private volatile boolean running = true;
        
        public VANETBeaconListenerThread() {
            try {
                Log.i(TAG, "VANETBeaconListenerThread() - create socket");
                // socket = new DatagramSocket(VANETServiceBase.BEACONS_UDP_RX_PORT, InetAddress.getByName(hostAddress));
                socket = new DatagramSocket(VANETServiceBase.BEACONS_UDP_RX_PORT);
                
            } catch (SocketException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        
        public void terminate() {
            
            Log.d(TAG, "terminate()");
            
            running = false;
            if(socket != null) {
                socket.close();
            }
        }
            
        public void run() {

            Log.d(TAG, "run() ------------------------------------- ");

            byte[] buff = new byte[VANETServiceBase.UDP_MAX_PACKET_SIZE];
            DatagramPacket packet;

            while (running == true) {
                try {
                    // address Android issue where old packet lengths are
                    // erroneously
                    // carried over between packet reuse
                    // packet.setLength(buff.length);
                    packet = new DatagramPacket(buff, buff.length);

//                    Log.d(TAG, "Listen for beacon packet...");
                    socket.receive(packet); // blocking

                    String receivedHostAddress = packet.getAddress().getHostAddress();

                    Log.d(TAG, "Beacon packet received from IP: " + receivedHostAddress + "; hostAddress: "
                            + hostAddress);

                    if (hostAddress.equals(receivedHostAddress)) {
//                        Log.d(TAG, "Drop received packet that is broadcasted by myself!");
                        continue;
                    }

                    // TODO: remove debugging code
                    Log.d(TAG, "packet.getData()!=null: " + (packet.getData() != null));
                    if (packet.getData() != null) {
                        Log.d(TAG, "packet.getData().length: " + (packet.getData().length));
                    }
                    Log.d(TAG, "packet.getOffset(): " + packet.getOffset());
                    Log.d(TAG, "packet.getLength(): " + packet.getLength());

//                    byte[] data2 = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength());
//                    Log.d(TAG, "RCV-PACKET:\n" + new String(VANETUtils.bytesToHex(data2)));
                    
                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(packet.getData(), packet.getOffset(), packet.getLength());
                    parcel.setDataPosition(0);

                    final VANETMessage msg = VANETMessage.CREATOR.createFromParcel(parcel);
                    
                    parcel.recycle();
                    parcel = null;
                    
                    msg.setStringAddressSource(packet.getAddress().getHostAddress());
                    msg.setStringAddressDestination(hostAddress);
                    msg.setIncoming(true);

                    float distance = -1;
                    // CurrentLocation reference can be changed from another
                    // thread. Copy it to local currLoc to keep the same reference.
                    Location currLoc = currentLocation;
                    if(currLoc != null) {
                        float[] distanceResult = null;
                        Location.distanceBetween(currLoc.getLatitude(), currLoc.getLongitude(), msg.getLatitude(), msg.getLongitude(), distanceResult);
                        if(distanceResult != null) {
                            distance = distanceResult[0];
                        }
                    }
                    
                    final float finalDistance = distance;

                    // Update observers with change of beacon list
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
//                            Log.d(TAG, "Handle received beacon on UI main thread");
                            handleBeaconReceived(msg, finalDistance);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Exception inside listening loop: " + e.getMessage(), e);
                }
            }
        }
            
            
        }
    
    /*
     * VANETBeaconBroadcastTimerTask
     */
    private class VANETBeaconBroadcastTimerTask extends TimerTask {

        DatagramSocket socket = null;
        int incrementalId = 0;

        public VANETBeaconBroadcastTimerTask() {
            Log.d(TAG, "VANETBeaconBroadcastTimerTask() created");
        }

        @Override
        public void run() {

            Log.d(TAG, "VANETBeaconBroadcastTimerTask.run()");
            Parcel parcel = null;
            try {
                socket = new DatagramSocket();

                incrementalId++;
                
                Location beaconLocation = currentLocation;
                
                final VANETMessage bmsg = new VANETMessage();
                bmsg.setStringAddressSource(hostAddress);
                bmsg.setLatitude(currentLocation.getLatitude());
                bmsg.setLongitude(currentLocation.getLongitude());
                bmsg.setType(VANETMessage.TYPE_BEACON);
                bmsg.setMessageId(app.generateNextVANETMessageID());
                // broadcast
                bmsg.setStringAddressDestination(app.manetcfg.getIpBroadcast());
                bmsg.setIncoming(false);
                
                parcel = Parcel.obtain();
                bmsg.writeToParcel(parcel, 0);
                
                byte buff[] = parcel.marshall();

//                Log.d(TAG, "SNT-PACKET:\n" + new String(VANETUtils.bytesToHex(buff)));
                
                DatagramPacket packet = new DatagramPacket(buff, buff.length,
                        InetAddress.getByName(bmsg.getStringAddressDestination()), VANETServiceBase.BEACONS_UDP_RX_PORT);
                socket.send(packet);
                
                handler.post(new Runnable() {
                    @Override
                    public void run() {
//                        Log.d(TAG, "VANETBeaconBroadcastTimerTask.run() - update on UI thread");
                        handleBeaconSent(bmsg);
                    }
                });

            } catch (SocketException e) {
                Log.e(TAG, "VANETBeaconBroadcastTimerTask.run() - SocketException message: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "VANETBeaconBroadcastTimerTask.run() - Excep - " + e.getMessage(), e);
            } finally {
                if(parcel != null) {
                    parcel.recycle();
                }
                if (socket != null) {
                    socket.close();
                }
            }
        }

    }
    

    /*
     * 
     */
    private class VANETUpdateUiTimerTask implements Runnable {
        
        private long period;
        private boolean inRunMethod = false;

        public void startTimer(long delay, long period) {
            Log.i(TAG, "VANETUpdateUiTimerTask.startTimer() - delay: " + delay + "; period: " + period);
            handler.postDelayed(this, delay);
            this.period = period;
        }
        
        public void stopTimer() {
            Log.i(TAG, "VANETUpdateUiTimerTask.stopTimer()");
            handler.removeCallbacks(this);
        }
        
        @Override
        public void run() {
//            Log.d(TAG, "VANETUpdateUiTimerTask.run()");
            
            if(inRunMethod) {
                return;
            }
            // No need to synchronize - executing on UI thread.
            inRunMethod = true;
            
            long startTime = System.currentTimeMillis();
            
            // Copy history diff, add it to history and reset it.
            messagesDiffCopy.clear();
            messagesDiffCopy.addAll(MessagesDiff);

            // First remove oldest messages in history to make space for diff.
            int numberToRemove = messages.size() - BEACONS_HISTORY_SIZE + MessagesDiff.size();
            while(numberToRemove > 0) {
                messages.remove(0);
                numberToRemove--;
            }
            messages.addAll(MessagesDiff);
            
            MessagesDiff.clear();
            
            // Copy neighbor map and reset it.
            neighborNodesMapCopy.clear();
            neighborNodesMapCopy.putAll(neighborNodesMap);
            
            // Copy statistic data and reset it.
            statisticsDataCopy.copyFrom(statisticsData);
            
            // Notify observers.
            for(VANETServiceBaseObserver observer : observers) {
                observer.onMessageHistoryDiffUpdate(messagesDiffCopy);
                observer.onNeighborListChanged(neighborNodesMapCopy);
                observer.onStatisticData(statisticsDataCopy);
            }
            
            handler.postDelayed(this, period);
            
            inRunMethod = false;
            
            //Log.d(TAG, "VANETUpdateUiTimerTask.run() - task finished in " + (System.currentTimeMillis() - startTime) + " ms");
        }
        
    }
    
    /*
     * 
     */
    private class VANETMessageListenerThread extends Thread {
        
        private final String TAG = VANETMessageListenerThread.class.getSimpleName();
        
        private DatagramSocket socket = null;
        private volatile boolean running = true;
        
        public VANETMessageListenerThread() {
            try {
                Log.i(TAG, "VANETMessageListenerThread() - create socket");
                // socket = new DatagramSocket(VANETServiceBase.BEACONS_UDP_RX_PORT, InetAddress.getByName(hostAddress));
                socket = new DatagramSocket(VANETServiceBase.DATA_UDP_RX_PORT);
                
            } catch (SocketException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        
        
        
        public void terminate() {
            
            Log.d(TAG, "terminate()");
            
            running = false;
            if(socket != null) {
                socket.close();
            }
        }
            
        public void run() {

            Log.d(TAG, "run() ------------------------------------- ");

            byte[] buff = new byte[VANETServiceBase.UDP_MAX_PACKET_SIZE];
            DatagramPacket packet;

            while (running == true) {
                try {
                    // address Android issue where old packet lengths are
                    // erroneously
                    // carried over between packet reuse
                    // packet.setLength(buff.length);
                    packet = new DatagramPacket(buff, buff.length);

//                    Log.d(TAG, "Listen for message packet...");
                    socket.receive(packet); // blocking

                    String receivedHostAddress = packet.getAddress().getHostAddress();

                    Log.d(TAG, "Message packet received from IP: " + receivedHostAddress + "; hostAddress: "
                            + hostAddress);

                    if (hostAddress.equals(receivedHostAddress)) {
//                        Log.d(TAG, "Drop received packet that is broadcasted by myself!");
                        continue;
                    }

                    // TODO: remove debugging code
                    Log.d(TAG, "packet.getData()!=null: " + (packet.getData() != null));
                    if (packet.getData() != null) {
                        Log.d(TAG, "packet.getData().length: " + (packet.getData().length));
                    }
                    Log.d(TAG, "packet.getOffset(): " + packet.getOffset());
                    Log.d(TAG, "packet.getLength(): " + packet.getLength());

//                    byte[] data2 = Arrays.copyOfRange(packet.getData(), packet.getOffset(), packet.getOffset() + packet.getLength());
//                    Log.d(TAG, "RCV-PACKET:\n" + new String(VANETUtils.bytesToHex(data2)));
                    
                    Parcel parcel = Parcel.obtain();
                    parcel.unmarshall(packet.getData(), packet.getOffset(), packet.getLength());
                    parcel.setDataPosition(0);

                    final VANETMessage msg = VANETMessage.CREATOR.createFromParcel(parcel);
                    msg.setStringAddressSource(packet.getAddress().getHostAddress());
                    msg.setStringAddressDestination(hostAddress);
                    msg.setIncoming(true);

                    parcel.recycle();
                    parcel = null;

                    // Update observers with change of beacon list
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
//                            Log.d(TAG, "Handle received beacon on UI main thread");
                            MessagesDiff.add(msg);
                            onMessage(msg);
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Exception inside listening loop: " + e.getMessage(), e);
                }
            }
        }
        
    }
    
    /*
     * VANETBeaconBroadcastTimerTask
     */
    private class VANETMessageSendingThread extends Thread {

        private final String TAG = VANETMessageSendingThread.class.getSimpleName();
        
        DatagramSocket socket = null;
        int incrementalId = 0;
        volatile boolean running = true;

        private Queue<VANETMessage> messageQueue;
        
        public VANETMessageSendingThread() {
            Log.d(TAG, "VANETMessageSendingThread() created");
            messageQueue = new LinkedList<VANETMessage>();
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        
        public void sendMessage(VANETMessage msg) {
            msg.setIncoming(false);
            
            synchronized (messageQueue) {
                messageQueue.offer(msg);
                messageQueue.notify();
            }
        }
        
        public void terminate() {
            
            Log.d(TAG, "terminate()");
            
            running = false;
            if(socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            synchronized (messageQueue) {
                messageQueue.notify();
            }
        }

        @Override
        public void run() {

            Log.d(TAG, "VANETBeaconBroadcastTimerTask.run()");
            running = true;
            Parcel parcel = null;
            VANETMessage msg = null;
            
            while(running) {
                synchronized (messageQueue) {
                    msg = messageQueue.poll();
                    if(msg == null) {
                        try {
                            messageQueue.wait();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                }
                
                
                try {
                    parcel = Parcel.obtain();
                    msg.writeToParcel(parcel, 0);
                    
                    byte buff[] = parcel.marshall();

//                    Log.d(TAG, "SNT-PACKET:\n" + new String(VANETUtils.bytesToHex(buff)));
                    
                    DatagramPacket packet = new DatagramPacket(buff, buff.length,
                            InetAddress.getByName(msg.getStringAddressDestination()), VANETServiceBase.DATA_UDP_RX_PORT);
                    socket.send(packet);
                    
                    //
                    final VANETMessage msg2 = msg;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            MessagesDiff.add(msg2);
                        }
                    });

                } catch (SocketException e) {
                    Log.e(TAG, "run() - SocketException message: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "run() - Excep - " + e.getMessage(), e);
                } finally {
                    if(parcel != null) {
                        parcel.recycle();
                    }
                }
                
            }
            
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

    };
    
    /*
     * Location
     */
    private class MyLocationListener implements LocationListener {
        
        private final String TAG = MyLocationListener.class.getSimpleName();
        
        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network
            // location provider.
            currentLocation = location;
            
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            Log.d(TAG, "GPS :\n Lat:" + lat + "\n Long:" + lng);
            
            // Calculate new distances to neighbor nodes, regarding to the new location.
            if(neighborNodesMapCopy != null) {
                float dist[] = null;
                for(String nodeAddress : neighborNodesMapCopy.keySet()) {
                    VANETNode node = neighborNodesMapCopy.get(nodeAddress);
                    Location.distanceBetween(lat, lng, node.getLatitude(), node.getLongitude(), dist);
                    node.setDistance(dist[0]);
                }
                
                // Update GUI with a new distances in the neighbor list.
                for(VANETServiceBaseObserver o : observers) {
                    o.onNeighborListChanged(neighborNodesMapCopy);
                }
            }
            
            VANETServiceBase.this.onLocationChanged(currentLocation);
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "GPS provider disabled - VANETServiceBase.stopSelf()");
            stopSelf();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };
}
