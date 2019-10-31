package vmio.com.blemultipleconnect.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * Solution: HMD Inspection System
 * Device Project: Camera Probe
 * Package: jp.miosys.camera.socket
 * Module: BroadcastFinder
 * @author Datnt VMIO
 * @date 2014/09/05
 * @author Thucnd VMIO
 * @date 2014/10/07 Fix code : (maintain and add comment )
 * Implement Finding Peers using Broadcast UDP message 
 * 
 */
public class BroadcastFinder 
{
	//CONSTANT
	public static final int ERROR = -1;
	public static final int NOT_FOUND = 0;
	public static final int FOUND_PEER = 1;
	
	//private static final String TAG = "BroadcastFinder";
	public boolean mStop = true;
	DatagramSocket mSocket;
	InterfaceActivity mMainActivity;
	
	/**
	 * Constructor
	 * @param ma
	 */
	public BroadcastFinder(InterfaceActivity ma) {
		mMainActivity = ma;
	}
	
	/**
	 * Stop finding peer
	 * 	+ Stop all alive process by setup stop flag
	 */
	public void stopFindingPeer() {
		mStop = true;
	}


	/**
	 * Is finding in progress
	 */
	public boolean isFinding()
	{
		return mStop;
	}
		
	/**
	 * Find peer:
	 * 		+ Send discovery broadcast package
	 * 		+ Wait for echo package 
	 * 		+ If timeout or error occur -> report to message
	 * 		+ If receive echo report success and return IP of peers
	 * @return -1: ERROR
	 * 			0: NOT_FOUND
	 * 			1: FOUND_PEER
	 */
	public int findListenerPeer() 
	{
		if(mStop == false)
			return ERROR;
		mStop = false;
		try {
			mSocket = new DatagramSocket();
			mSocket.setBroadcast(true);
			mSocket.setSoTimeout(SocketUtils.TIMEOUT_FIND_PEERS_MS);
			
			String handshakeMessage = SocketUtils.getHandShakeRequestMessage();
			InetAddress internetAddress = SocketUtils.getBroadcastAddressByWifi();// getBroadcastAddress();
			if (internetAddress == null) {
				//HMDInspectionActivity.writeLogSystemFolders(" Error internetAddress is NULL ! "); //Write log
				return ERROR;
			}
			DatagramPacket sendPacket = new DatagramPacket(handshakeMessage.getBytes(), handshakeMessage.length(),internetAddress, SocketUtils.BROADCAST_PORT);
			mSocket.send(sendPacket);
			int tryCount = 0;
			while (!mStop) {
				byte[] buf = new byte[1024];
				mSocket.setBroadcast(false);
				mSocket.setSoTimeout(SocketUtils.TIMEOUT_FIND_PEERS_MS);
				DatagramPacket recvPacket = new DatagramPacket(buf, buf.length);
				try {
					mSocket.receive(recvPacket);
				} catch (SocketTimeoutException e) {
					if(!mStop && tryCount < 5) {
						mSocket.setBroadcast(true);
						mSocket.send(sendPacket);
						tryCount ++;
						continue;
					} else {
						mStop = true;
						//HMDInspectionActivity.writeLogSystemFolders(" findListenerPeer NOT FOUND ! "); //Write log
						return NOT_FOUND;
					}
				}
				String peerAddress = recvPacket.getAddress().toString().substring(1);
				mMainActivity.setPeerInfo(peerAddress);
				//String s = new String(recvPacket.getData(), 0, recvPacket.getLength());
				break;
			}
			mStop = true;
			return FOUND_PEER;
		} catch (IOException exIO) {
			exIO.printStackTrace();
			mStop = true;
			//HMDInspectionActivity.writeLogSystemFolders(" findListenerPeer ERROR IOException"); //Write log
			return ERROR;
		}
	}
}
