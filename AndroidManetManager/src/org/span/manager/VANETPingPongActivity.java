package org.span.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.span.R;
import org.span.service.vanetsex.VANETService;
import org.span.service.vanetsex.VANETUtils;
import org.span.service.vanetsex.pingpong.E_VANETPingPongDistance;
import org.span.service.vanetsex.pingpong.E_VANETPingPongMessageSize;
import org.span.service.vanetsex.pingpong.E_VANETPingPongSmartphonePosition;
import org.span.service.vanetsex.pingpong.VANETPingPongService;
import org.span.service.vanetsex.pingpong.VANETPingPongState;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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
    
    private TextView textView_title;
    private EditText editText_resultFile;
    private EditText editText_numberOfPackets;
    private EditText editText_pause;
    private Spinner spinner_opponentAddress;
    private Spinner spinner_packetPayloadSize;
    private Spinner spinner_distance;
    private Spinner spinner_smartphonePosition;
    private Button button_startPingPong;
    private Button button_manageResults;
    private TextView textView_results;
    
    private ManetManagerApp app;
    private VANETPingPongService pingpongService;
    private VANETService vanetService;
    private List<String> listNeighborIPs;
    private ArrayAdapter<String> neighborsSpinnerAdapter;
    private ArrayAdapter<String> packetPayloadSizeSpinnerAdapter;
    private ArrayAdapter<String> distanceSpinnerAdapter;
    private ArrayAdapter<String> smartphonePositionSpinnerAdapter;
    
    public static void open(Activity parentActivity) {
        Intent it = new Intent(parentActivity, VANETPingPongActivity.class);
        parentActivity.startActivity(it);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.vanet_activity_ping_pong);
        
        app = (ManetManagerApp) getApplication();
        
        textView_title = (TextView) findViewById(R.id.textView_title);
        editText_resultFile = (EditText) findViewById(R.id.editText_resultFile);
        editText_numberOfPackets = (EditText) findViewById(R.id.editText_numberOfPackets);
        editText_pause = (EditText) findViewById(R.id.editText_pause);
        spinner_opponentAddress = (Spinner) findViewById(R.id.spinner_opponentAddress);
        spinner_packetPayloadSize = (Spinner) findViewById(R.id.spinner_packetPayloadSize);
        spinner_distance = (Spinner) findViewById(R.id.spinner_distance);
        spinner_smartphonePosition = (Spinner) findViewById(R.id.spinner_smartphonePosition);
        button_startPingPong = (Button) findViewById(R.id.button_startPingPong);
        button_manageResults = (Button) findViewById(R.id.button_manageResults);
        textView_results = (TextView) findViewById(R.id.textView_results);
        
        textView_title.setText(VANETUtils.removeExcessBlankLines(Html.fromHtml(getString(R.string.pingPong_title))));
        
        editText_numberOfPackets.setText("1000");
        editText_pause.setText("100");
        
        textView_results.setText(Html.fromHtml(getString(R.string.pingPong_results, 0, 0, 0, 0, 0, 0, 0)));
        
        // Init neighbors pinner will be done after vanetService is available.
        
        packetPayloadSizeSpinnerAdapter = new ArrayAdapter<String>(VANETPingPongActivity.this,
                android.R.layout.simple_spinner_item, E_VANETPingPongMessageSize.names());
        spinner_packetPayloadSize.setAdapter(packetPayloadSizeSpinnerAdapter);
        
        distanceSpinnerAdapter = new ArrayAdapter<String>(VANETPingPongActivity.this,
                android.R.layout.simple_spinner_item, E_VANETPingPongDistance.names());
        spinner_distance.setAdapter(distanceSpinnerAdapter);
        
        smartphonePositionSpinnerAdapter = new ArrayAdapter<String>(VANETPingPongActivity.this,
                android.R.layout.simple_spinner_item, E_VANETPingPongSmartphonePosition.names());
        spinner_smartphonePosition.setAdapter(smartphonePositionSpinnerAdapter);
        
        // Init button.
        button_startPingPong.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
            	Log.i(TAG, "onClick() - button_startPingPong");
                if(vanetService != null) {
                    try {
                    	String resultFile = editText_resultFile.getText().toString();
                    	int numberOfPackets = Integer.parseInt(editText_numberOfPackets.getText().toString());
                    	int pause = Integer.parseInt(editText_pause.getText().toString());
                        String opponentAddress = neighborsSpinnerAdapter.getItem(spinner_opponentAddress.getSelectedItemPosition());
                        E_VANETPingPongMessageSize size = E_VANETPingPongMessageSize.values()[spinner_packetPayloadSize.getSelectedItemPosition()];
                        E_VANETPingPongDistance distance = E_VANETPingPongDistance.values()[spinner_distance.getSelectedItemPosition()];
                        E_VANETPingPongSmartphonePosition smartphonePosition = E_VANETPingPongSmartphonePosition.values()[spinner_smartphonePosition.getSelectedItemPosition()];
                        // start ping pong
                        pingpongService.startPingPong(resultFile, numberOfPackets, pause, opponentAddress, size, distance, smartphonePosition);
                        
                    } catch (Exception e) {
                        Toast.makeText(VANETPingPongActivity.this, "Input data error!", Toast.LENGTH_SHORT);
                    }
                    
                }
            }
        });
        
        button_startPingPong.setEnabled(false);
        
        button_manageResults.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
			}
		});
        
        Log.i(TAG, "onCreate() --------------------- 1");
        
        // Connect to already started vanet service.
        if (app.isServiceRunning(VANETService.class)) {
        	Log.i(TAG, "onCreate() --------------------- 1 bind VANETService ...");
            bindService(new Intent(this, VANETService.class), vanetServiceConnection, 0);
        } else {
        	Log.i(TAG, "onCreate() --------------------- 1 - finish");
            Toast.makeText(this, "ERROR: VANETService is not running", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        Log.i(TAG, "onCreate() --------------------- 2");
        
        // Start if needed and connect to the ping pong service.
        if (app.isServiceRunning(VANETPingPongService.class) == false) {
        	Log.i(TAG, "onCreate() --------------------- 2 START VANETPingPongService ...");
        	startService(new Intent(this, VANETPingPongService.class));
        }
        Log.i(TAG, "onCreate() --------------------- 2 bind to VANETPingPongService ...");
        bindService(new Intent(this, VANETPingPongService.class), pingPongServiceConnection, 0);
        
        Log.i(TAG, "onCreate() --------------------- 3 ...");
        
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	
    	if(vanetService != null) {
    		unbindService(vanetServiceConnection);
    	}
    	
    	if(pingpongService != null) {
    		unbindService(pingPongServiceConnection);
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
            
            // Init spinner with neighbor list.
            Set<String> nset = vanetService.getNeighbourNodesMap().keySet();
            List<String> neighbors = new ArrayList<String>(nset);
            java.util.Collections.sort(neighbors);
            listNeighborIPs = neighbors;
            
            neighborsSpinnerAdapter = new ArrayAdapter<String>(VANETPingPongActivity.this,
                    android.R.layout.simple_spinner_item, listNeighborIPs);
            neighborsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner_opponentAddress.setAdapter(neighborsSpinnerAdapter);
            
            // Enable button_startPingPong if both services are connected
            if(pingpongService != null) {
            	button_startPingPong.setEnabled(true);
            }
            
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Disonnected from VANETService ...................................");

            // Disable button_startPingPong
            button_startPingPong.setEnabled(false);
        }
    };
    
    private ServiceConnection pingPongServiceConnection = new ServiceConnection() {
		
    	@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
    		Log.d(TAG, "Connected to VANETPingPongService ...................................");
			VANETPingPongService.VANETPingPongServiceBinder b = (VANETPingPongService.VANETPingPongServiceBinder) binder;
			pingpongService = b.getService();
			pingpongService.registerObserver(pingPongObserver);
			
			// Enable button_startPingPong if both services are connected
            if(vanetService != null) {
            	button_startPingPong.setEnabled(true);
            }
		}
    	
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "Disonnected from VANETPingPongService ...................................");
			pingpongService.unregisterObserver(pingPongObserver);
			// Disable button_startPingPong
            button_startPingPong.setEnabled(false);
		}
	};
    
    /*
     * 
     * 
     * 
     */
    VANETPingPongService.Observer pingPongObserver = new VANETPingPongService.Observer() {
        
        @Override
        public void onPingPongStateUpdate(VANETPingPongState pingPongState) {
            if(pingPongState != null) {
                if(pingPongState.isRunning()) {
                	button_startPingPong.setText("Ping Pong in process (" + pingPongState.getPacketsSent() + "/" + pingPongState.getNumberOfPacketsToSend() + ") ...");
                    button_startPingPong.setEnabled(false);
                    
                } else {
                    button_startPingPong.setText(R.string.pingPong_button_startPingPong);
                    button_startPingPong.setEnabled(true);
                }
                
                String resultsText = getString(R.string.pingPong_results,
                        pingPongState.getPacketsSent(),
                        pingPongState.getNumberOfPacketsToSend(),
                        pingPongState.getPacketsReceived(),
                        pingPongState.getAverageResponseTime(),
                        pingPongState.getMinResponseTime(),
                        pingPongState.getMaxResponseTime(),
                        pingPongState.getDuration());
                textView_results.setText(Html.fromHtml(resultsText));
                
            } else {
                button_startPingPong.setText(R.string.pingPong_button_startPingPong);
                button_startPingPong.setEnabled(true);
            }
        }

    };

}
