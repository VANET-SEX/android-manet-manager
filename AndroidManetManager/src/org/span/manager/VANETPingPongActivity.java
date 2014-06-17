package org.span.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.span.R;
import org.span.service.vanetsex.VANETEvent;
import org.span.service.vanetsex.VANETNode;
import org.span.service.vanetsex.VANETPingPongState;
import org.span.service.vanetsex.VANETService;
import org.span.service.vanetsex.VANETServiceObserver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.NeighboringCellInfo;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class VANETPingPongActivity extends Activity {

    private static final String TAG = VANETPingPongActivity.class.getSimpleName();
    
    private EditText editText_numberOfPackets;
    private EditText editText_packetPayloadSize;
    private Spinner spinner_opponentAddress;
    private Button button_startPingPong;
    private TextView textView_results;
    
    private ManetManagerApp app;
    private VANETService vanetService;
    private List<String> listNeighborIPs;
    private ArrayAdapter<String> neighborsSpinnerAdapter;
    
    public static void open(Activity parentActivity) {
        Intent it = new Intent(parentActivity, VANETPingPongActivity.class);
        parentActivity.startActivity(it);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vanet_activity_ping_pong);
        
        app = (ManetManagerApp) getApplication();
        
        editText_numberOfPackets = (EditText) findViewById(R.id.editText_numberOfPackets);
        editText_packetPayloadSize = (EditText) findViewById(R.id.editText_packetPayloadSize);
        spinner_opponentAddress = (Spinner) findViewById(R.id.spinner_opponentAddress);
        button_startPingPong = (Button) findViewById(R.id.button_startPingPong);
        textView_results = (TextView) findViewById(R.id.textView_results);
        
        // Init spinner will be done after vanetService is available.
        
        // Init button.
        button_startPingPong.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if(vanetService != null) {
                    try {
                        int selectedPosition = spinner_opponentAddress.getSelectedItemPosition();
                        int numberOfPackets = Integer.parseInt(editText_numberOfPackets.getText().toString());
                        int packetPayloadSize = Integer.parseInt(editText_packetPayloadSize.getText().toString());
                        vanetService.doPingPong(neighborsSpinnerAdapter.getItem(selectedPosition), numberOfPackets, packetPayloadSize);
                        
                    } catch (Exception e) {
                        Toast.makeText(VANETPingPongActivity.this, "Input data error!", Toast.LENGTH_SHORT);
                    }
                    
                }
            }
        });
        
        button_startPingPong.setEnabled(false);
        
        if (app.isServiceRunning(VANETService.class)) {
            bindService(new Intent(this, VANETService.class), vanetServiceConnection, 0);
        } else {
            Toast.makeText(this, "ERROR: VANETService is not running", Toast.LENGTH_SHORT).show();
            finish();
        }
        
    }
    
    /*
     * 
     * 
     * 
     */
    
    private ServiceConnection vanetServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "Connected to VANETService ....................................");
            
            // Reference to the vanet service.
            VANETService.VANETServiceBinder b = (VANETService.VANETServiceBinder) binder;
            vanetService = b.getService();
            
            vanetService.registerObserver(vanetServiceObserver);
            
            // Init spinner with neighbor list.
            Set<String> nset = vanetService.getNeighbourNodesMap().keySet();
            List<String> neighbors = new ArrayList<String>(nset);
            java.util.Collections.sort(neighbors);
            listNeighborIPs = neighbors;
            
            neighborsSpinnerAdapter = new ArrayAdapter<String>(VANETPingPongActivity.this,
                    android.R.layout.simple_spinner_item, listNeighborIPs);
            neighborsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_opponentAddress.setAdapter(neighborsSpinnerAdapter);
            
            // Enable button_startPingPong
            button_startPingPong.setEnabled(true);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Disonnected from VANETService ...................................");

            vanetService.unregisterObserver(vanetServiceObserver);
            
            // Disable button_startPingPong
            button_startPingPong.setEnabled(false);
        }
    };
    
    /*
     * 
     * 
     * 
     */
    VANETServiceObserver vanetServiceObserver = new VANETServiceObserver() {
        
        @Override
        public void onPingPongStatistics(VANETPingPongState pingPongState) {
            if(pingPongState != null) {
                if(pingPongState.isFinished()) {
                    button_startPingPong.setText(R.string.pingPong_button_startPingPong);
                    button_startPingPong.setEnabled(true);
                } else {
                    button_startPingPong.setText("Ping Pong in process...");
                    button_startPingPong.setEnabled(false);
                }
                
                String resultsText = getString(R.string.pingPong_results,
                        pingPongState.getPacketsSent(),
                        pingPongState.getNumberOfPacketsToSend(),
                        pingPongState.getPacketsReceived(),
                        pingPongState.getAverageResponseTime(),
                        pingPongState.getMinResponseTime(),
                        pingPongState.getMaxResponseTime());
                textView_results.setText(Html.fromHtml(resultsText));
                
            } else {
                button_startPingPong.setText(R.string.pingPong_button_startPingPong);
                button_startPingPong.setEnabled(true);
            }
        }
        
        @Override
        public void onEventListChanged(List<VANETEvent> events) {
            // Nothing to do
        }
    };
}
