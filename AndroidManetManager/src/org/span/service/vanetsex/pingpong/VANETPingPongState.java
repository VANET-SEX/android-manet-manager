package org.span.service.vanetsex.pingpong;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.util.ByteArrayBuffer;

public class VANETPingPongState {

	/*
	 * Input params
	 */
	private String resultFile;
	private int numberOfPacketsToSend;
	private int pause;
	private String opponentAddress;
	private E_VANETPingPongMessageSize size;
	private E_VANETPingPongDistance distance;
	private E_VANETPingPongSmartphonePosition smartphonePosition;

	/*
	 * State
	 */
	private boolean running = false;

	private Random random;

	private int packetsSent = 0;
	private int packetsReceived = 0;

	private long startedAt = -1;
	private long finishedAt = -1;
	private long duration = -1;

	private long cummulativeResponseTime = 0;
	private long averageResponseTime = 0;
	private long minResponseTime = Long.MAX_VALUE;
	private long maxResponseTime = 0;

	private byte[] dummyData = null;
	private byte[] dummyData1 = null;
	private byte[] dummyData2 = null;
	private byte[] dummyData3 = null;

	private Map<Integer, Long> mapSentTimeToPacketIdx = new ConcurrentHashMap<Integer, Long>(
			100);

	public VANETPingPongState(String resultFile, int numberOfPacketsToSend,
			int pause, String opponentAddress, E_VANETPingPongMessageSize size,
			E_VANETPingPongDistance distance,
			E_VANETPingPongSmartphonePosition smartphonePosition) {
		random = new Random();

		this.resultFile = resultFile;
		this.numberOfPacketsToSend = numberOfPacketsToSend;
		this.pause = pause;
		this.opponentAddress = opponentAddress;
		this.size = size;
		this.distance = distance;
		this.smartphonePosition = smartphonePosition;

		if (size != E_VANETPingPongMessageSize.RANDOM) {
			// 2*4 --> 2 * int size of 4 bytes --> one int for ping packet
			// counter, second int for size of dummyData
			dummyData = new byte[size.getValue() - 3 * 4];
			Arrays.fill(dummyData, (byte) 0);

		} else {
			dummyData = new byte[E_VANETPingPongMessageSize.SMALL.getValue() - 3 * 4];
			Arrays.fill(dummyData, (byte) 0);
			
			dummyData1 = new byte[E_VANETPingPongMessageSize.SMALL.getValue() - 3 * 4];
			Arrays.fill(dummyData1, (byte) 0);

			dummyData2 = new byte[E_VANETPingPongMessageSize.MEDIUM.getValue() - 3 * 4];
			Arrays.fill(dummyData2, (byte) 0);

			dummyData3 = new byte[E_VANETPingPongMessageSize.LARGE.getValue() - 3 * 4];
			Arrays.fill(dummyData3, (byte) 0);
		}

	}

	public void pingPongStarted() {
		startedAt = System.currentTimeMillis();
		setRunning(true);
	}

	public void pingPongFinished() {
		finishedAt = System.currentTimeMillis();
		duration = finishedAt - startedAt;
		setRunning(false);
	}

	public void packetSent(int packetIdx) {
		long sentAt = System.nanoTime() / 1000000;
		packetsSent++;
		mapSentTimeToPacketIdx.put(packetIdx, sentAt);
	}

	public void packetReceived(int packetIdx, long receivedAt) {
		Long sentAt = mapSentTimeToPacketIdx.get(packetIdx);

		if (sentAt == null) {
			return;
		}

		mapSentTimeToPacketIdx.remove(packetIdx);

		long respTime = receivedAt - sentAt;

		packetsReceived++;
		cummulativeResponseTime += respTime;
		averageResponseTime = cummulativeResponseTime / packetsReceived;

		if (respTime > maxResponseTime) {
			maxResponseTime = respTime;
		}

		if (respTime < minResponseTime) {
			minResponseTime = respTime;
		}
	}

	/*
	 * Getters
	 */

	public boolean isRunning() {
		return running;
	}

	private void setRunning(boolean running) {
		this.running = running;
	}

	public String getResultFile() {
		return resultFile;
	}

	public int getPause() {
		return pause;
	}

	public String getOpponentAddress() {
		return opponentAddress;
	}

	public E_VANETPingPongMessageSize getSize() {
		return size;
	}

	public E_VANETPingPongDistance getDistance() {
		return distance;
	}

	public E_VANETPingPongSmartphonePosition getSmartphonePosition() {
		return smartphonePosition;
	}

	public int getPacketsSent() {
		return packetsSent;
	}

	public int getPacketsReceived() {
		return packetsReceived;
	}

	public long getStartedAt() {
		return startedAt;
	}

	public long getFinishedAt() {
		return finishedAt;
	}

	public long getDuration() {
		return duration;
	}

	public long getAverageResponseTime() {
		return averageResponseTime;
	}

	public long getMinResponseTime() {
		return minResponseTime;
	}

	public long getMaxResponseTime() {
		return maxResponseTime;
	}

	public byte[] getDummyData() {
		return dummyData;
	}

	public byte[] getDummyData1() {
		return dummyData1;
	}

	public byte[] getDummyData2() {
		return dummyData2;
	}

	public byte[] getDummyData3() {
		return dummyData3;
	}

	public int getNumberOfPacketsToSend() {
		return numberOfPacketsToSend;
	}
}
