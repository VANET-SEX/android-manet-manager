package org.span.service.vanetsex.pingpong;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import org.span.manager.ManetManagerApp;
import org.span.service.vanetsex.VANETServiceBase;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.text.TextUtils;
import android.util.Log;

public class VANETPingPongService extends Service {
	
	private static final String TAG = VANETPingPongService.class.getSimpleName();

	public static final int PORT_RX_UDP = 5658;
	
	private Handler handler;
	
	private final VANETPingPongServiceBinder binder = new VANETPingPongServiceBinder();
	private ManetManagerApp app;
	
	private Observer observer;
	
	private VANETPingPongState state;
	private VANETPingPongSenderThread senderThread;
	private VANETPingPongReceiverThread receiverThread;
	private VANETPingPongResponseSenderThread responseSenderThread;

	private String hostAddress;

	/*
	 * 
	 * 
	 * 
	 */
	
	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate()");
		app = (ManetManagerApp) getApplication();
		handler = new Handler();
		hostAddress = app.manetcfg.getIpAddress();
		// Dummy state instance
		state = new VANETPingPongState("", 0, 0, "", E_VANETPingPongMessageSize.SMALL, E_VANETPingPongDistance.DISTANCE_50, E_VANETPingPongSmartphonePosition.BOARD_BOARD);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		if(receiverThread == null) {
			receiverThread = new VANETPingPongReceiverThread();
			receiverThread.start();
		}
		
		if(responseSenderThread == null) {
			responseSenderThread = new VANETPingPongResponseSenderThread();
			responseSenderThread.start();
		}
		
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if(senderThread != null) {
			senderThread.terminate();
		}
		
		if(receiverThread != null) {
			receiverThread.terminate();
		}
		
		if(responseSenderThread != null) {
			responseSenderThread.terminate();
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
	
	/*
	 * 
	 * Public
	 * 
	 */
	public void registerObserver(Observer observer) {
		this.observer = observer;
	}
	
	public void unregisterObserver(Observer observer) {
		this.observer = null;
	}
	
	public void requestPingPongStateUpdate() {
		if(observer != null) {
			observer.onPingPongStateUpdate(state);
		}
	}
	
	public void startPingPong(String resultFile, int packets, int pause, String destinationAddress, E_VANETPingPongMessageSize size,
			E_VANETPingPongDistance distance, E_VANETPingPongSmartphonePosition smartphonePosition) {
		if(state.isRunning() == false) {
			
			Log.d(TAG, "startPingPong()");
			
			state = new VANETPingPongState(resultFile, packets, pause, destinationAddress, size, distance, smartphonePosition);
			
			senderThread = new VANETPingPongSenderThread(state);
			senderThread.start();
			
			state.pingPongStarted();
		} else {
			Log.d(TAG, "Ping pong already started!?!");
		}
	}
	
	/*
	 * 
	 * Private
	 * 
	 */
	private void updateGui() {
		observer.onPingPongStateUpdate(state);
	}
	
	private void updateAfterPingPongFinished() {
		state.pingPongFinished();
		observer.onPingPongStateUpdate(state);
		
		new Thread() {
			public void run() {
				writeLineToResultsFile(state);
			};
		}.start();
		
		File fd = getFilesDir();
		
	}
	
	private void writeLineToResultsFile(VANETPingPongState state) {
		
		String fileName = state.getResultFile();
		if(TextUtils.isEmpty(fileName)) {
			fileName = "default_results.csv";
		}
		
		String strDate = android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss", new java.util.Date(state.getStartedAt())).toString();
		
		String DELIM = ";";
		
		StringBuilder sb = new StringBuilder();
		sb.append(strDate).append(DELIM);
		sb.append(state.getOpponentAddress()).append(DELIM);
		sb.append(state.getSmartphonePosition().name()).append(DELIM);
		sb.append(state.getDistance().name()).append(DELIM);
		sb.append(state.getPause()).append(DELIM);
		sb.append(state.getSize().name()).append(DELIM);
		sb.append(state.getNumberOfPacketsToSend()).append(DELIM);
		
		sb.append(state.getPacketsSent()).append(DELIM);
		sb.append(state.getPacketsReceived()).append(DELIM);
		sb.append(state.getAverageResponseTime()).append(DELIM);
		sb.append(state.getMinResponseTime()).append(DELIM);
		sb.append(state.getMaxResponseTime()).append(DELIM);
		sb.append(state.getDuration()).append(DELIM);
		
		String root = Environment.getExternalStorageDirectory().toString();
	    File myDir = new File(root + "/pingpong_results");    
	    myDir.mkdirs();
		
	    File file = new File(myDir, fileName);
	    
		// First check if file already exists.
		boolean fileExists = file.exists();
		
		// write result to the resultFile
		FileOutputStream outputStream = null;

		try {
//		  outputStream = openFileOutput(fileName, Context.MODE_APPEND | Context.MODE_WORLD_READABLE);
			outputStream = new FileOutputStream(file, true);
		  
		  if(fileExists == false) {
			  // write field titles in first line
			  outputStream.write(("Date;OpponentAddr;SmartphonePosition;Distance;Pause;PacketSize;NrPacketsToSend;NrPacketsSent;NrPacketsRcvd;AvgRespTime;MinRespTime;MaxRespTime;TestDuration;\n").getBytes());
		  }
		  
		  outputStream.write((sb.toString() + "\n").getBytes());
		  
		} catch (Exception e) {
		  Log.d(TAG, e.getMessage(), e);
		  
		} finally {
			try {
				outputStream.flush();
			} catch (IOException e) {
			}
			
			try {
				outputStream.close();
			} catch (IOException e) {
			}
		}
	}

	/*
     * 
     * Service binder.
     * 
     */
    public class VANETPingPongServiceBinder extends Binder {
        public VANETPingPongService getService() {
            return VANETPingPongService.this;
        }
    }
    
    /*
     * 
     * 
     * 
     */
    public interface Observer {
    	public void onPingPongStateUpdate(VANETPingPongState state);
    }
    
    /*
     * 
     * 
     * 
     */
    
    private class VANETPingPongSenderThread extends Thread {

        private final String TAG = VANETPingPongSenderThread.class.getSimpleName();
        
        private DatagramSocket socket = null;
        private volatile boolean running = true;
        
        private VANETPingPongState state = null;
        
        private int packetIdx = 0;
        
        private long startedAt = -1;
        private long lastGuiUpdateAt = -1;
        
        public VANETPingPongSenderThread(VANETPingPongState state) {
            Log.d(TAG, "VANETPingPongSendingThread() created");
            
            try {
                socket = new DatagramSocket();
            } catch (SocketException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            
            this.state = state;
        }
        
        public void terminate() {
            Log.d(TAG, "terminate()");
            running = false;
        }
        
        @Override
        public void run() {

        	Log.i(TAG, "PING PONG SENDER STARTED");
        	
            running = true;
            Parcel parcel = null;

            //
            InetAddress destinationAddress;
			try {
				destinationAddress = InetAddress.getByName(state.getOpponentAddress());
			} catch (UnknownHostException e1) {
				Log.d(TAG, e1.getMessage(), e1);
				return;
			}
            
            // VANETPingPongPacket
            VANETPingPongPacket ppPacket = new VANETPingPongPacket();
            ppPacket.setData(state.getDummyData());
            ppPacket.setRequest(1);
            
            long millisec = System.currentTimeMillis();
            if((millisec - lastGuiUpdateAt) >= 1000) {
            	lastGuiUpdateAt = millisec;
            	handler.post(new Runnable() {
					
					@Override
					public void run() {
						updateGui();
					}
				});
            }
            startedAt = System.currentTimeMillis();
            
            byte buff[] = null;
            byte buff1[] = null;
            byte buff2[] = null;
            byte buff3[] = null;
            
            ByteBuffer bbuff = ByteBuffer.allocate(4);
            bbuff.order(ByteOrder.nativeOrder());
            
            while(running) {
                
                try {
                	if(buff == null) {
                		
                		if(state.getSize() == E_VANETPingPongMessageSize.RANDOM) {
                			
                			ppPacket.setPacketIdx(packetIdx);
                        	ppPacket.setData(state.getDummyData());
                        	
                            parcel = Parcel.obtain();
                            ppPacket.writeToParcel(parcel, 0);
                            buff = parcel.marshall();
                            parcel.recycle();
                            
                            //
                            ppPacket.setPacketIdx(packetIdx);
                        	ppPacket.setData(state.getDummyData1());
                        	
                            parcel = Parcel.obtain();
                            ppPacket.writeToParcel(parcel, 0);
                            buff1 = parcel.marshall();
                            parcel.recycle();
                            
                            //
                            ppPacket.setPacketIdx(packetIdx);
                        	ppPacket.setData(state.getDummyData2());
                        	
                            parcel = Parcel.obtain();
                            ppPacket.writeToParcel(parcel, 0);
                            buff2 = parcel.marshall();
                            parcel.recycle();
                            
                            //
                            ppPacket.setPacketIdx(packetIdx);
                        	ppPacket.setData(state.getDummyData3());
                        	
                            parcel = Parcel.obtain();
                            ppPacket.writeToParcel(parcel, 0);
                            buff3 = parcel.marshall();
                            parcel.recycle();
                            
                            Random r = new Random();
                            int rnd = r.nextInt(3);
                            
                            if(rnd == 0) {
                            	DatagramPacket packet = new DatagramPacket(buff1, buff1.length,
                                		destinationAddress, VANETPingPongService.PORT_RX_UDP);
                            	state.packetSent(packetIdx);
                                socket.send(packet);
                            	
                            } else if(rnd == 1) {
                            	DatagramPacket packet = new DatagramPacket(buff2, buff2.length,
                                		destinationAddress, VANETPingPongService.PORT_RX_UDP);
                            	state.packetSent(packetIdx);
                                socket.send(packet);
                            	
                            } else if(rnd == 2) {
                            	DatagramPacket packet = new DatagramPacket(buff3, buff3.length,
                                		destinationAddress, VANETPingPongService.PORT_RX_UDP);
                            	state.packetSent(packetIdx);
                                socket.send(packet);
                            	
                            }
                			
                		} else {
                			ppPacket.setPacketIdx(packetIdx);
                        	ppPacket.setData(state.getDummyData());
                        	
                            parcel = Parcel.obtain();
                            ppPacket.writeToParcel(parcel, 0);
                            buff = parcel.marshall();
                            parcel.recycle();
                            
                            DatagramPacket packet = new DatagramPacket(buff, buff.length,
                            		destinationAddress, VANETPingPongService.PORT_RX_UDP);
                            state.packetSent(packetIdx);
                            socket.send(packet);
                		}
                		
                        
                	} else {
                		
                		if(state.getSize() == E_VANETPingPongMessageSize.RANDOM) {
                			
                			Random r = new Random();
                            int rnd = r.nextInt(3);
                            
                            if(rnd == 0) {
                            	bbuff.clear();
                        		byte[] baPacketIdx = bbuff.putInt(packetIdx).array();
                        		buff1[0] = baPacketIdx[0];
                        		buff1[1] = baPacketIdx[1];
                        		buff1[2] = baPacketIdx[2];
                        		buff1[3] = baPacketIdx[3];
                            	
                            	DatagramPacket packet = new DatagramPacket(buff1, buff1.length,
                                		destinationAddress, VANETPingPongService.PORT_RX_UDP);
                            	state.packetSent(packetIdx);
                                socket.send(packet);
                            	
                            } else if(rnd == 1) {
                            	bbuff.clear();
                        		byte[] baPacketIdx = bbuff.putInt(packetIdx).array();
                        		buff2[0] = baPacketIdx[0];
                        		buff2[1] = baPacketIdx[1];
                        		buff2[2] = baPacketIdx[2];
                        		buff2[3] = baPacketIdx[3];
                            	
                            	DatagramPacket packet = new DatagramPacket(buff2, buff2.length,
                                		destinationAddress, VANETPingPongService.PORT_RX_UDP);
                            	state.packetSent(packetIdx);
                                socket.send(packet);
                            	
                            } else if(rnd == 2) {
                            	bbuff.clear();
                        		byte[] baPacketIdx = bbuff.putInt(packetIdx).array();
                        		buff3[0] = baPacketIdx[0];
                        		buff3[1] = baPacketIdx[1];
                        		buff3[2] = baPacketIdx[2];
                        		buff3[3] = baPacketIdx[3];
                            	
                            	DatagramPacket packet = new DatagramPacket(buff3, buff3.length,
                                		destinationAddress, VANETPingPongService.PORT_RX_UDP);
                            	state.packetSent(packetIdx);
                                socket.send(packet);
                            	
                            }
                			
                			
                		} else {
                			bbuff.clear();
                    		byte[] baPacketIdx = bbuff.putInt(packetIdx).array();
                    		buff[0] = baPacketIdx[0];
                    		buff[1] = baPacketIdx[1];
                    		buff[2] = baPacketIdx[2];
                    		buff[3] = baPacketIdx[3];
                    		
                    		DatagramPacket packet = new DatagramPacket(buff, buff.length,
                            		destinationAddress, VANETPingPongService.PORT_RX_UDP);
                    		state.packetSent(packetIdx);
                            socket.send(packet);
                		}
                		
                	}
                	

                    packetIdx++;
                    if((packetIdx+1) > state.getNumberOfPacketsToSend()) {
                    	running = false;
                    	
                    } else {
                    	long currMilliSec = System.currentTimeMillis();
                        if((currMilliSec - lastGuiUpdateAt) >= 1000) {
                        	lastGuiUpdateAt = currMilliSec;
                        	handler.post(new Runnable() {
            					
            					@Override
            					public void run() {
            						updateGui();
            					}
            				});
                        }
                    	
                    	sleep(state.getPause());
                    }
                    
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
            	try {
            		socket.close();
					
				} catch (Exception e) {
				}
            }
            
            handler.postDelayed(new Runnable() {
				
				@Override
				public void run() {
					updateAfterPingPongFinished();
				}
			}, 1000);
            
            Log.i(TAG, "PING PONG SENDER STOPPED");
        }

    };
    
    /*
     * 
     * 
     * 
     */
    
    private class VANETPingPongReceiverThread extends Thread {
    	
    	private DatagramSocket socket = null;
        private volatile boolean running = true;
        
        public VANETPingPongReceiverThread() {
            try {
                Log.i(TAG, "VANETPingPongReceiverThread() - create socket listening on port: " + VANETPingPongService.PORT_RX_UDP);
                socket = new DatagramSocket(VANETPingPongService.PORT_RX_UDP);
                
            } catch (SocketException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        
        public void terminate() {
            
            Log.d(TAG, "terminate()");
            
            running = false;
            if(socket != null) {
            	try {
            		socket.close();
				} catch (Exception e) {
					Log.d(TAG, "Exception while closing receiver socket: " + e.getMessage());
				}
            }
        }
    	
    	@Override
    	public void run() {
    		Log.i(TAG, "PING PONG RECEIVER STARTED");
    		
            byte[] buff = new byte[VANETServiceBase.UDP_MAX_PACKET_SIZE];
            DatagramPacket packet;
            
            byte[] buff2 = new byte[VANETServiceBase.UDP_MAX_PACKET_SIZE];
            ByteBuffer bBuff2 = ByteBuffer.wrap(buff2);
            
            ByteBuffer intByteBuff = ByteBuffer.allocate(4);
            intByteBuff.order(ByteOrder.nativeOrder());

            int last_msg_packetIdx = -1;
            
            while (running == true) {
                try {
                    packet = new DatagramPacket(buff, buff.length);

                    socket.receive(packet); // blocking
                    
                    long receivedAt = System.nanoTime() / 1000000;

                    String receivedHostAddress = packet.getAddress().getHostAddress();

                    if (hostAddress.equals(receivedHostAddress)) {
//                      Drop received packet broadcasted by myself
                        continue;
                    }
                    
                    
                    
                    intByteBuff.clear();
                    intByteBuff.put(packet.getData(), packet.getOffset() + 4, 4);
                    intByteBuff.position(0);
                    int msg_request = intByteBuff.getInt();
                    
                    intByteBuff.clear();
                    intByteBuff.put(packet.getData(), packet.getOffset(), 4);
                    intByteBuff.position(0);
                    int msg_packetIdx = intByteBuff.getInt();
                    
                    if(msg_request == 0) {
                    	// It is response received. Update statistics.
                        state.packetReceived(msg_packetIdx, receivedAt);
                        
                    } else {
                    	
                    	if(msg_packetIdx < last_msg_packetIdx || last_msg_packetIdx == -1) {
                        	// Read bytes from received packet to buff2.
                            responseSenderThread.setResponseData(packet.getData(), packet.getOffset(), packet.getLength());
                        }
                        last_msg_packetIdx = msg_packetIdx;
                        
                        responseSenderThread.sendResponse(msg_packetIdx, packet.getAddress(), packet.getLength());
                    }
                    

                } catch (Exception e) {
                    Log.e(TAG, "Exception inside listening loop: " + e.getMessage(), e);
                }
            }
    		
            handler.post(new Runnable() {
				
				@Override
				public void run() {
					receiverThread = null;
				}
			});
            
    		Log.i(TAG, "PING PONG RECEIVER STOPPED");
    	}
    	
    }
    
    private class VANETPingPongResponseSenderThread extends Thread {
    	
    	private class RespObj {
    		public int packetIdx;
    		public InetAddress address;
    		public int length;
    	}
    	
    	private Queue<RespObj> respObjQueue;
    	
    	private DatagramSocket socket = null;
        private volatile boolean running = true;
        
        byte[] buff2;
        ByteBuffer bBuff2;
        ByteBuffer intByteBuffer;
        
        public VANETPingPongResponseSenderThread() {
            try {
                Log.i(TAG, "VANETPingPongResponseSenderThread()");
                socket = new DatagramSocket();
                
            } catch (SocketException e) {
                Log.e(TAG, e.getMessage(), e);
            }
            
            buff2 = new byte[VANETServiceBase.UDP_MAX_PACKET_SIZE];
            bBuff2 = ByteBuffer.wrap(buff2);
            
            intByteBuffer = ByteBuffer.allocate(4);
            intByteBuffer.order(ByteOrder.nativeOrder());
            
            respObjQueue = new LinkedList<RespObj>();
        }
        
        public void setResponseData(byte[] buff2src, int offset, int length) {
        	
        	// Read bytes from received packet to buff2.
        	
        	synchronized (bBuff2) {
        		bBuff2.clear();
        		Arrays.fill(buff2, 8, VANETServiceBase.UDP_MAX_PACKET_SIZE, (byte) 0);
        		bBuff2.put(buff2src, offset, 8);
        		
                int msg_request = 0;
            	
                intByteBuffer.clear();
                intByteBuffer.putInt(msg_request);
            	byte[] msg_reqest_ba = intByteBuffer.array();
            	
            	buff2[4] = msg_reqest_ba[0];
            	buff2[5] = msg_reqest_ba[1];
            	buff2[6] = msg_reqest_ba[2];
            	buff2[7] = msg_reqest_ba[3];
			}
        }
        
        public void sendResponse(int packetIdx, InetAddress address, int length) {
        	RespObj respObj = new RespObj();
        	respObj.packetIdx = packetIdx;
        	respObj.address = address;
        	respObj.length = length;
        	
        	synchronized (respObjQueue) {
				respObjQueue.offer(respObj);
				respObjQueue.notify();
			}
        }
        
        public void terminate() {
            
            Log.d(TAG, "terminate()");
            
            running = false;
            if(socket != null) {
            	try {
            		socket.close();
				} catch (Exception e) {
					Log.d(TAG, "Exception while closing response sender socket: " + e.getMessage());
				}
            }
            
            synchronized (respObjQueue) {
            	respObjQueue.notify();
            }
        }
    	
    	@Override
    	public void run() {
    		Log.i(TAG, "PING PONG RESPONSE SENDER STARTED");
            
    		running = true;
    		
            ByteBuffer intByteBuff = ByteBuffer.allocate(4);
            intByteBuff.order(ByteOrder.nativeOrder());

            RespObj respObj = null;
            
            while (running == true) {
            	synchronized (respObjQueue) {
            		respObj = respObjQueue.poll();
                    if(respObj == null) {
                        try {
                        	respObjQueue.wait();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                }
            	
                try {

                    	// It is request received. Send back a response.
                    	
                    	// Set packetIdx
                    	intByteBuff.clear();
                    	intByteBuff.putInt(respObj.packetIdx);
                    	byte[] msg_packetIdx_ba = intByteBuff.array();
                    	
                    	synchronized (bBuff2) {
                    		buff2[0] = msg_packetIdx_ba[0];
                        	buff2[1] = msg_packetIdx_ba[1];
                        	buff2[2] = msg_packetIdx_ba[2];
                        	buff2[3] = msg_packetIdx_ba[3];

                            DatagramPacket packetResp = new DatagramPacket(buff2, respObj.length,
                            		respObj.address, VANETPingPongService.PORT_RX_UDP);
                            
                            socket.send(packetResp);
						}

                } catch (Exception e) {
                    Log.e(TAG, "Exception inside listening loop: " + e.getMessage(), e);
                }
            }
    		
            handler.post(new Runnable() {
				
				@Override
				public void run() {
					responseSenderThread = null;
				}
			});
            
    		Log.i(TAG, "PING PONG RESPONSE SENDER STOPPED");
    	}
    	
    }
    
}
