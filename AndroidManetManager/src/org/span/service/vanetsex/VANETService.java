package org.span.service.vanetsex;

import java.util.TimerTask;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


public class VANETService extends VANETServiceBase {
    
    private static final String TAG = VANETService.class.getSimpleName();
    
    private static final long GUI_UPDATE_PERIOD = 1000;
    
    private final VANETServiceBinder binder = new VANETServiceBinder();
    
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
    public void onData(VANETMessage dataMessage) {
        Log.d(TAG, "onData() - from: " + dataMessage.getStringAddressSource() + "; msg.id: " + dataMessage.getMessageId());
    }

    @Override
    public void onBeaconReceived(VANETMessage beaconMessage) {
        Log.d(TAG, "onBeaconReceived() - from: " + beaconMessage.getStringAddressSource() + "; msg.id: " + beaconMessage.getMessageId());

    }
    
    @Override
    public void onBeaconSent(VANETMessage beaconMessage) {
        Log.d(TAG, "onBeaconSent()");
        
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
