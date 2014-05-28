package org.span.service.vanetsex;

import android.content.Context;
import android.content.SharedPreferences;


public class VANETPrefs {

    // prefs title
    private static final String PREFS_TITLE = "mvanet_prefs";

    /*
     *  KEY and DEF (default values)
     */
    
    // KEY_vanet_service_started
    private static final String KEY_vanet_service_started = "mvanet.vanet_service_started";
    private static final boolean DEF_vanet_service_started = false;
    public boolean get_vanet_service_started() {
        return prefs.getBoolean(KEY_vanet_service_started, DEF_vanet_service_started);
    }
    
    // KEY_vanet_ip_address
    private static final String KEY_vanet_ip_address = "mvanet.vanet_ip_address";
    private static final String DEF_vanet_ip_address = null;
    public String get_vanet_ip_address() {
        return prefs.getString(KEY_vanet_ip_address, DEF_vanet_ip_address);
    }
    
    // KEY_beacon_period
    private static final String KEY_beacon_period = "mvanet.beacon_period";
    private static final long DEF_beacon_period = 2000;
    public long get_beacon_period() {
        return prefs.getLong(KEY_beacon_period, DEF_beacon_period);
    }
    
    // KEY_beacon_started
//    private static final String KEY_beacon_started = "mvanet.beacon_started";
//    private static final boolean DEF_beacon_started = false;
//    public boolean get_beacon_started() {
//        return prefs.getBoolean(KEY_beacon_started, DEF_beacon_started);
//    }

    /*
     *  singleton
     */
    
    private static VANETPrefs inst = null;
    
    public static VANETPrefs create(Context ctx) {
        synchronized (PREFS_TITLE) {
            if(inst == null) {
                inst = new VANETPrefs(ctx);
            }
            return inst;
        }
    }

    // private members
    
    private SharedPreferences prefs;
    
    private VANETPrefs(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_TITLE, Context.MODE_PRIVATE);
    }
    
    // other public members
    
    public VANETPrefsEditor edit() {
        return new VANETPrefsEditor(prefs.edit());
    }
    
    
    
    /*
     * Editor
     */
    public class VANETPrefsEditor {
        
        private android.content.SharedPreferences.Editor edit;
        private VANETPrefsEditor vanetPrefsEditor;

        public VANETPrefsEditor(android.content.SharedPreferences.Editor editor) {
            edit = prefs.edit();
            vanetPrefsEditor = this;
        }
        
        // KEY_vanet_service_started
        public VANETPrefsEditor put_vanet_service_started(boolean value) {
            edit.putBoolean(KEY_vanet_service_started, value);
            return vanetPrefsEditor;
        }
        
        // KEY_vanet_ip_address
        public VANETPrefsEditor put_vanet_ip_address(String value) {
            edit.putString(KEY_vanet_ip_address, value);
            return vanetPrefsEditor;
        }
        
        // KEY_beacon_period
        public VANETPrefsEditor put_beacon_period(long beacon_period) {
            edit.putLong(KEY_beacon_period, beacon_period);
            return vanetPrefsEditor;
        }
        
        // KEY_beacon_started
//        public VANETPrefsEditor put_beacon_started(boolean beacon_started) {
//            edit.putBoolean(KEY_beacon_started, beacon_started);
//            return vanetPrefsEditor;
//        }
        
        public boolean commit() {
            this.vanetPrefsEditor = null;
            return edit.commit();
        }
        
        public VANETPrefsEditor clear() {
            edit.clear();
            return vanetPrefsEditor;
        }
        
    }
}
