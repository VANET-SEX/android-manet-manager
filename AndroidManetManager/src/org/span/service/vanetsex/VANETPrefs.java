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
    
    // KEY_message_id_generator_value
    private static final String KEY_message_id_generator_value = "mvanet.message_id_generator_value";
    private static final int DEF_message_id_generator_value = 0;
    public int get_message_id_generator_value() {
        return prefs.getInt(KEY_message_id_generator_value, DEF_message_id_generator_value);
    }
    
    // KEY_event_id_generator_value
    private static final String KEY_event_id_generator_value = "mvanet.event_id_generator_value";
    private static final int DEF_event_id_generator_value = 0;
    public int get_event_id_generator_value() {
        return prefs.getInt(KEY_event_id_generator_value, DEF_event_id_generator_value);
    }

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
        
        // KEY_beacon_period
        public VANETPrefsEditor put_beacon_period(int value) {
            edit.putLong(KEY_beacon_period, value);
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
        
        // KEY_message_id_generator_value
        public VANETPrefsEditor put_message_id_generator_value(int value) {
            edit.putInt(KEY_message_id_generator_value, value);
            return vanetPrefsEditor;
        }
        
        // KEY_event_id_generator_value
        public VANETPrefsEditor put_event_id_generator_value(int value) {
            edit.putInt(KEY_event_id_generator_value, value);
            return vanetPrefsEditor;
        }
    }
}
