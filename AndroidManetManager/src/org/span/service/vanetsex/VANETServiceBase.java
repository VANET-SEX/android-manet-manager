package org.span.service.vanetsex;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.span.manager.ManetManagerApp;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.Parcel;
import android.util.Log;

public abstract class VANETServiceBase extends Service {

    private static final String TAG = VANETServiceBase.class.getSimpleName();
    
    public static final int UDP_MAX_PACKET_SIZE = 1024;

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
    private String hostAddress;
    
    // beacon related members
    private long beaconPeriod;
    private Map<String, VANETNode> neighborNodesMap;
    private Map<String, VANETNode> neighborNodesMapCopy;
    private List<VANETMessage> beaconMessages;
    private List<VANETMessage> beaconMessagesDiff;
    private List<VANETMessage> beaconMessagesDiffCopy;
    private VANETBeaconListenerThread listenerThread = null;
    private Timer beaconBroadcastTimer;
    private VANETBeaconBroadcastTimerTask beaconBroadcastTimerTask;
    
    private Set<VANETObserver> observers;
    
    private VANETUpdateUiTimerTask updateUiTimerTask;
    private VANETStatisticsData statisticsData;
    private VANETStatisticsData statisticsDataCopy;

    /*
     * 
     * Public interface.
     * 
     */
    public abstract void onCreateVANETService();
    
    public abstract void onStartVANETService();
    
    public abstract void onDestroyVANETService();
    
    public abstract void onData(VANETMessage dataMessage);
    
    public abstract void onBeaconReceived(VANETMessage beaconMessage);
    
    public abstract void onBeaconSent(VANETMessage beaconMessage);
    
    public void sendMessage(VANETMessage message) {
        // ...
    }

    public void startBeacon() {
        
        Log.i(TAG, "startBeacon() - start listener thread");
        
        if(listenerThread == null) {
            
            hostAddress = app.manetcfg.getIpAddress();
            beaconPeriod = app.vanetPrefs.get_beacon_period();
            
            listenerThread = new VANETBeaconListenerThread();
            listenerThread.start();
        }
        
        Log.i(TAG, "startBeacon() - start periodic broadcast timer task");
        
        beaconBroadcastTimer = new Timer();
        beaconBroadcastTimerTask = new VANETBeaconBroadcastTimerTask();
        beaconBroadcastTimer.schedule(beaconBroadcastTimerTask, 100, beaconPeriod);
        
//        app.vanetPrefs.edit().put_beacon_started(true).commit();
        
        for(VANETObserver o : observers) {
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
        
        for(VANETObserver o : observers) {
            o.onBeaconStateChanged(false);
        }
    }
    
    public Map<String, VANETNode> getNeighbourNodesMap() {
        return neighborNodesMap;
    }
    
    public List<VANETMessage> getBeaconMessageHistory() {
        return beaconMessages;
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
    
    /*
     * 
     * Observer methods.
     * 
     */
    public void registerObserver(VANETObserver observer) {
        observers.add(observer);
        
        // init observer update
        observer.onBeaconStateChanged(isBeaconRunning());
        observer.onMessageHistoryInit(beaconMessages);
        observer.onNeighborListChanged(neighborNodesMapCopy);
        observer.onStatisticData(statisticsDataCopy);
    }
    
    public void unregisterObserver(VANETObserver observer) {
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
        observers = new HashSet<VANETObserver>();
        statisticsData = new VANETStatisticsData();
        statisticsDataCopy = new VANETStatisticsData();
        app = (ManetManagerApp)getApplication();
        handler = new Handler();
        neighborNodesMap = new HashMap<String, VANETNode>();
        neighborNodesMapCopy = new HashMap<String, VANETNode>();
        beaconMessages = new LinkedList<VANETMessage>();
        beaconMessagesDiff = new LinkedList<VANETMessage>();
        beaconMessagesDiffCopy = new LinkedList<VANETMessage>();
        
        updateUiTimerTask = new VANETUpdateUiTimerTask();
        updateUiTimerTask.startTimer(TIMER_UPDATE_UI_PERIOD, TIMER_UPDATE_UI_PERIOD);
        
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
        
        if(updateUiTimerTask != null) {
            updateUiTimerTask.stopTimer();
        }
        
        app.vanetPrefs.edit().put_vanet_service_started(false).commit();
    }
    

    
    /*
     * 
     * Private and protected
     * 
     */
    protected void handleBeaconReceived(VANETMessage receivedBeacon) {
        
        long currentMillisec = System.currentTimeMillis();
        statisticsData.updateBeaconReceived(receivedBeacon.getSizeInBytes());
        
        // Update the neighbor list.
        VANETNode node = neighborNodesMap.get(receivedBeacon.getStringAddressSource());
        if(node == null) {
            node = new VANETNode();
            node.setStringAddress(receivedBeacon.getStringAddressSource());
            node.setFirstSeen(currentMillisec);
            neighborNodesMap.put(receivedBeacon.getStringAddressSource(), node);
            
            Log.d(TAG, "................. + + + + ADDING NEIGHBOR: " + receivedBeacon.getStringAddressSource());
        }
        node.setLatitude(receivedBeacon.getLatitude());
        node.setLongitude(receivedBeacon.getLongitude());
        node.setLastSeen(currentMillisec);
        node.resetCheckPeriodTimer();
        
        // Update the beacon history list.
        addToBeaconMessageHistory(receivedBeacon);
    }
    
    protected void handleBeaconSent(VANETMessage sentBeacon) {
        
        statisticsData.updateBeaconSent(sentBeacon.getSizeInBytes());
        addToBeaconMessageHistory(sentBeacon);
    }
    
    private void addToBeaconMessageHistory(VANETMessage beaconMsg) {
        
        beaconMessagesDiff.add(beaconMsg);
    }
    
    protected void handleNodeTimeoutCheck() {
        Set<String> neighborList = neighborNodesMap.keySet();
        for(String neighborAddress : neighborList) {
            VANETNode node = neighborNodesMap.get(neighborAddress);
            if(node.incrementCheckPeriodTimer() >= BEACONS_CHECK_TIMEOUT_PERIODS) {
                Log.d(TAG, "......................REMOVING NEIGHBOR: " + neighborAddress);
                neighborNodesMap.remove(neighborAddress);
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
            } /* catch (UnknownHostException e) {
                Log.e(TAG, e.getMessage(), e);
            } */
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
                            handleBeaconReceived(msg);
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
                
                final VANETMessage bmsg = new VANETMessage();
                bmsg.setStringAddressSource(hostAddress);
                bmsg.setLatitude(1);
                bmsg.setLongitude(1);
                bmsg.setType(VANETMessage.TYPE_BEACON);
                bmsg.setMessageId(-1);
                // broadcast
                bmsg.setStringAddressDestination("192.168.1.0");
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
                        handleNodeTimeoutCheck();
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
            beaconMessagesDiffCopy.clear();
            beaconMessagesDiffCopy.addAll(beaconMessagesDiff);

            // First remove oldest messages in history to make space for diff.
            int numberToRemove = beaconMessages.size() - BEACONS_HISTORY_SIZE + beaconMessagesDiff.size();
            while(numberToRemove > 0) {
                beaconMessages.remove(0);
                numberToRemove--;
            }
            beaconMessages.addAll(beaconMessagesDiff);
            
            beaconMessagesDiff.clear();
            
            // Copy neighbor map and reset it.
            neighborNodesMapCopy.clear();
            neighborNodesMapCopy.putAll(neighborNodesMap);
            
            // Copy statistic data and reset it.
            statisticsDataCopy.copyFrom(statisticsData);
            
            // Notify observers.
            for(VANETObserver observer : observers) {
                observer.onMessageHistoryDiffUpdate(beaconMessagesDiffCopy);
                observer.onNeighborListChanged(neighborNodesMapCopy);
                observer.onStatisticData(statisticsDataCopy);
            }
            
            handler.postDelayed(this, period);
            
            inRunMethod = false;
            
            //Log.d(TAG, "VANETUpdateUiTimerTask.run() - task finished in " + (System.currentTimeMillis() - startTime) + " ms");
        }
        
    }
    
}
