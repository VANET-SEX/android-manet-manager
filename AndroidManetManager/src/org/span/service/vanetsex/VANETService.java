package org.span.service.vanetsex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.span.manager.ManetManagerApp;

import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


public class VANETService extends VANETServiceBase {
    
    private static final String TAG = VANETService.class.getSimpleName();
    
    private static final long GUI_UPDATE_PERIOD = 1000;
    
    private ManetManagerApp app;
    
    private Handler handler;
    
    private final VANETServiceBinder binder = new VANETServiceBinder();
    
    private SimpleDateFormat dateFormat;
    
    protected Set<VANETServiceObserver> observers;
    
    private Map<Integer, VANETEvent> mapEvents;
    private List<VANETEvent> listEvents;
    private boolean eventsChangeFired = false;
    
    private Location currentLocation = null;
//    StringBuilder sb;
    
    private List<VANETEventLogLine> listEventLogLines;
    private FileOutputStream eventLogFileOutputStream;
	private ExecutorService eventLogExecutor;
    
    
    
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
        handler = new Handler();
        
        observers = new HashSet<VANETServiceObserver>();
        mapEvents = new HashMap<Integer, VANETEvent>();
        listEvents = new LinkedList<VANETEvent>();
        dateFormat = new SimpleDateFormat("HH:mm:ss");
        
        prepareEventLogFile();
        
        // init observer
        fireEventListChanged();
    }

    @Override
    public void onStartVANETService() {
        Log.i(TAG, "onStartVANETService()");
        
        
    }

    @Override
    public void onDestroyVANETService() {
        Log.i(TAG, "onDestroyVANETService()");
        
        closeEventLogFile();
        
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
        Log.d(TAG, "onMessage() - from: " + dataMessage.getStringAddressSource() + "; msg.id: " + dataMessage.getMessageId());
        
        // Handle received event message.
        if(dataMessage.getType() == VANETMessage.TYPE_EVENT) {
            VANETEvent event = (VANETEvent)dataMessage.getData();
            
            // If event is first time received, add it to list, update gui and rebroadcast it
            if(!mapEvents.containsKey(event.getId())) {
            	// Time
                long receivedAt = System.currentTimeMillis();
            	
                mapEvents.put(event.getId(), event);
                listEvents.add(event);
                
                // Calculate distance to event's location.
                float dist[] = new float[1];
                Location.distanceBetween(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude(), event.getLatitude(), event.getLongitude(), dist);
                event.setDistance(dist[0]);
                event.setDelay(receivedAt - event.getTime());
                
                // update gui
                fireEventListChanged();
                
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
                
                writeLineToEventLogFile(new VANETEventLogLine(event.getId(), event.getType(), event.getLatitude(), event.getLongitude(), event.getTime(), event.getDistance(), receivedAt - event.getTime()));
            }
        } else if(dataMessage.getType() == VANETMessage.TYPE_EXCHANGE_EVENTS_ID_LIST) {
        	
        	Log.i(TAG, "onMessage() - VANETMessage.TYPE_EXCHANGE_EVENTS_ID_LIST");
        	
        	
        	
        	// Exchange events - Event IDs received.
        	// Collect those events which I have and sender doesn't have,
        	// and send collected events back to the sender.
        	
        	// IDs of events in sender's buffer.
        	final VANETExchangeEventsIDList eventIDs = (VANETExchangeEventsIDList)dataMessage.getData();
        	final String destinationAddres = dataMessage.getStringAddressSource();
        	
        	final List<VANETEvent> listEventsCopy = new LinkedList<VANETEvent>(listEvents);
        	
        	new Thread() {
            	public void run() {
            		final List<VANETExchangeEventsEventList> diffEventList = new LinkedList<VANETExchangeEventsEventList>();
                	
                	long diffSearchStartedAt = System.nanoTime() / 1000000;
                	
                	// For each event in my buffer...
                	for(VANETEvent event : listEventsCopy) {
                		// Check if it is containd also in sender's buffer.
                		boolean eventFound = false;
                		for(int i=0; i< eventIDs.getEventIDs().length; i++) {
                			if(eventIDs.getEventIDs()[i] == event.getId()) {
                				eventFound = true;
                				break;
                			}
                		}
                		
                		// If not contained (not found), put event in the diff list
                		// that will be sent back to the sender.
                		if(eventFound == false) {
                			VANETExchangeEventsEventList exchangeEventsObj = new VANETExchangeEventsEventList();
                			exchangeEventsObj.getEvents().add(event);
                			diffEventList.add(exchangeEventsObj);
                		}
                	}
                	
                	long diffSearchFinishedAt = System.nanoTime() / 1000000;
                	
                	Log.d(TAG, "Nr. diff events: " + diffEventList.size() + "; searchDuration: " + (diffSearchFinishedAt - diffSearchStartedAt) + "; Nr. my events: " + listEvents.size() + "; Nr. sender's events: " + eventIDs.getEventIDs().length);
                	
                	// Send diff event list back to the sender.
                	// Call send method from UI thread.
                	if(diffEventList.size() > 0) {
                		handler.post(new Runnable() {
            				
            				@Override
            				public void run() {
            					sendExchangeEvents(destinationAddres, diffEventList);
            				}
            			});
                	}
                	
            	};
        	}.start();
        	
        	
        } else if(dataMessage.getType() == VANETMessage.TYPE_EXCHANGE_EVENTS_EVENT_LIST) {
        	
        	// Exchange events - Diff event list received back.
        	// Store diff events to my buffer of events.
        	
        	VANETExchangeEventsEventList events = (VANETExchangeEventsEventList)dataMessage.getData();
        	
        	Log.i(TAG, "onMessage() - VANETMessage.TYPE_EXCHANGE_EVENTS_EVENT_LIST - nr. diff events RCVD!: " + events.getEvents().size());

        	boolean changed = false;
        	long receivedAt = System.currentTimeMillis();
        	float dist[] = new float[1];
        	for(VANETEvent event : events.getEvents()) {
        		if(!mapEvents.containsKey(event.getId())) {
                    mapEvents.put(event.getId(), event);
                    listEvents.add(event);
                    
                    // Calculate distance to event's location.
                    Location.distanceBetween(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude(), event.getLatitude(), event.getLongitude(), dist);
                    event.setDistance(dist[0]);
                    
                    writeLineToEventLogFile(new VANETEventLogLine(event.getId(), event.getType(), event.getLatitude(), event.getLongitude(), event.getTime(), event.getDistance(), receivedAt - event.getTime()));
                    
                    changed = true;
        		}
        	}
        	
        	if(changed) {
        		fireEventListChanged();
        	}
        }
    }

    @Override
    public void onBeaconReceived(VANETMessage beaconMessage, VANETNode neighbor) {
//        Log.d(TAG, "onBeaconReceived() - from: " + beaconMessage.getStringAddressSource() + "; msg.id: " + beaconMessage.getMessageId());

    }
    
    @Override
    public void onBeaconSent(VANETMessage beaconMessage) {
//        Log.d(TAG, "onBeaconSent()");
        
    }
    
    @Override
    public void onNeighborAppeared(VANETNode node) {
        
    	sendExchangeEventsEventIDs(node);
    }

	@Override
    public void onNeighborDisappeared(VANETNode node) {
        
    }
    
    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        
        // Calculate new distances to locations of events.
//        float dist[] = new float[1];
//        for(VANETEvent ev : listEvents) {
//            Location.distanceBetween(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude(), ev.getLatitude(), ev.getLongitude(), dist);
//            ev.setDistance(dist[0]);
//        }
//        
//        // update gui
//        fireEventListChanged();
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
        
        byte[] ddata = null;
        if(eventType == VANETEvent.TYPE_EVENT_C) {
        	ddata = new byte[63*1024 + 512];
        	
        } else if(eventType == VANETEvent.TYPE_EVENT_B) {
        	ddata = new byte[31*1024 + 512];
        	
        } else {
        	// VANETEvent.TYPE_EVENT_A
        	ddata = new byte[4];
        }
        Arrays.fill(ddata, (byte)255);
    	event.setDummyData(ddata);
        
        event.setType(eventType);
        
        msg.setData(event);
        sendMessage(msg);
        
        mapEvents.put(event.getId(), event);
        listEvents.add(event);
        
        writeLineToEventLogFile(new VANETEventLogLine(event.getId(), event.getType(), event.getLatitude(), event.getLongitude(), event.getTime(), .0f, 0));
        
        // update gui
        fireEventListChanged();
    }
    
    /*
     * 
     * Private methods.
     * 
     */
    
    private void sendExchangeEventsEventIDs(VANETNode node) {
    	
    	Log.i(TAG, "sendExchangeEventsEventIDs() - Nr. my events: " + listEvents.size());
    	
		VANETExchangeEventsIDList eventIDs = new VANETExchangeEventsIDList();
		
		eventIDs.setEventIDs(new int[listEvents.size()]);
		
		for(int i=0; i<listEvents.size(); i++) {
			eventIDs.getEventIDs()[i] = listEvents.get(i).getId();
		}
		
		// Exchange events - Send event ID list to a new neighbor.
        
        VANETMessage respMsg = new VANETMessage();
        respMsg.setStringAddressSource(hostAddress);
        respMsg.setStringAddressDestination(node.getStringAddress());
        respMsg.setType(VANETMessage.TYPE_EXCHANGE_EVENTS_ID_LIST);
        respMsg.setMessageId(app.generateNextVANETMessageID());
        respMsg.setLatitude(getCurrentLocation().getLatitude());
        respMsg.setLongitude(getCurrentLocation().getLongitude());
        
        respMsg.setData(eventIDs);
        
        sendMessage(respMsg);
	}
    
    private void sendExchangeEvents(String destinationAddress, List<VANETExchangeEventsEventList> diffEventList) {
    	
    	for(VANETExchangeEventsEventList exchangeEventsObj : diffEventList) {
    		VANETMessage respMsg = new VANETMessage();
            respMsg.setStringAddressSource(hostAddress);
            respMsg.setStringAddressDestination(destinationAddress);
            respMsg.setType(VANETMessage.TYPE_EXCHANGE_EVENTS_EVENT_LIST);
            respMsg.setMessageId(app.generateNextVANETMessageID());
            respMsg.setLatitude(getCurrentLocation().getLatitude());
            respMsg.setLongitude(getCurrentLocation().getLongitude());
            
            respMsg.setData(exchangeEventsObj);
            
            sendMessage(respMsg);
    	}
    	
    }
    
    private void fireEventListChanged() {
    	if(eventsChangeFired == false) {
    		eventsChangeFired = true;
    		handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					eventsChangeFired = false;
					// update gui event list
			        for(VANETServiceObserver o : observers) {
			            o.onEventListChanged(listEvents);
			        }
				}
			}, 500);
    	}
    }
    
    private void prepareEventLogFile() {
    	eventLogExecutor = Executors.newSingleThreadExecutor();
    	
    	eventLogExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				if(listEventLogLines == null) {
					listEventLogLines = new LinkedList<VANETEventLogLine>();
				}
				
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
				
		    	String fileName = "eventsLog_" + sdf.format(new Date()) + ".csv";
				
				String root = Environment.getExternalStorageDirectory().toString();
			    File myDir = new File(root + "/vanet_scf_results");    
			    myDir.mkdirs();
				
			    File file = new File(myDir, fileName);
			    
				// First check if file already exists.
				boolean fileExists = file.exists();
				
				// write result to the resultFile
				eventLogFileOutputStream = null;

				try {
//				  outputStream = openFileOutput(fileName, Context.MODE_APPEND | Context.MODE_WORLD_READABLE);
					eventLogFileOutputStream = new FileOutputStream(file, true);
				  
				  if(fileExists == false) {
					  // write field titles in first line
					  eventLogFileOutputStream.write(VANETEventLogLine.getHeaderLogLine().getBytes());
				  }
				  
				} catch (Exception e) {
				  Log.d(TAG, e.getMessage(), e);
				}
			}
		});
    }
    
    private void writeLineToEventLogFile(final VANETEventLogLine eventLogLine) {
		eventLogExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				listEventLogLines.add(eventLogLine);
		    	
		    	if(eventLogFileOutputStream == null) {
		    		return;
		    	}
		    	
				try {
					eventLogFileOutputStream.write(eventLogLine.toString().getBytes());
				  
				} catch (Exception e) {
				  Log.d(TAG, e.getMessage(), e);
				  
				} finally {
					try {
						eventLogFileOutputStream.flush();
					} catch (IOException e) {
					}
					
//					try {
//						eventLogFileOutputStream.close();
//					} catch (IOException e) {
//					}
				}
			}
		});
    	
	}
    
    private void closeEventLogFile() {
    	
    	eventLogExecutor.execute(new Runnable() {
			
			@Override
			public void run() {
				try {
					eventLogFileOutputStream.flush();
				} catch (IOException e) {
				}
				
				try {
					eventLogFileOutputStream.close();
				} catch (IOException e) {
				}
				
				eventLogFileOutputStream = null;
			}
		});
    	
		eventLogExecutor.shutdown();
    }
    
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
