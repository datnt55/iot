package vmio.com.blemultipleconnect.socket;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Solution: HMD Inspection System
 * Device Project: Camera Probe
 * Package: jp.miosys.camera.socket
 * Module: BroadcastListener
 * @author Datnt VMIO
 * @date 2014/09/05
 * @author Thucnd VMIO
 * @date 2014/10/08 Fix code : (maintain and add comment )
 * Implement Listening Peers using listener Broadcast UDP message 
 * 
 */

public class BroadcastListener 
{
	//CONSTANT
	public static final int OK = 0;
	public static final int ERROR = -1;
	private static final String TAG = "Discovery";
	public static final String DISCORVERY = "8f840b9e0089675fd8c0a04d37489eed"; // Check MD5
	public static final String ECHO = "209389ac0ae62c053a51a6d3ae5a37a8";	// Check MD5
	private boolean mStop = false;
	DatagramSocket mSocket;
	InterfaceActivity mMainACtivity;
	
	/**
	 * Constructor
	 * @param ma: Interface for displaying Message from socket threads
	 */
	public BroadcastListener(InterfaceActivity ma) 
	{
		mMainACtivity = ma;
	}
	
	/**
	 * Stop listen
	 */
	public void stopListen()
	{
		mStop = true;
	}
	
	/**
	 * Send Discovery Response to peers after receive a Broadcast UDP
	 * @param peerAddress
	 * @param peerPort
	 * @return  0 : OK
	 * 		   -1 : EXCEPTION
	 */
	private int sendDiscoveryResponse(InetAddress peerAddress, int peerPort)
	{
		try {
			//	Format the handshake message as Diagram
			mSocket.setSoTimeout(SocketUtils.TIMEOUT_MS);
			String handshakeMessage = SocketUtils.getHandShakeConnectMessage();
			Log.d(TAG, "Handshake Message: " + handshakeMessage);
			DatagramPacket packet = new DatagramPacket(handshakeMessage.getBytes(),handshakeMessage.length(), peerAddress, peerPort);
			//	Send echo
			mSocket.send(packet);
			mMainACtivity.setPeerInfo(peerAddress.toString().substring(1));
			return OK;
		}
		catch(IOException eIO)
		{
			eIO.printStackTrace();
			//HMDInspectionActivity.writeLogSystemFolders("IOException in  sendDiscoveryResponse() "); //Write log
			return ERROR;
		}
	}
	
	/**
	 * Listen For Responses
	 * 		+ Listen after send Broadcast
	 * 		+ If receive Echo, get IP address and first information
	 * @return: -1 : if error 
	 * 			 0 : if success
	 */
	public int listForResponses() 
	{
		//	Create new Diagram package
		try {
			mSocket = new DatagramSocket(SocketUtils.BROADCAST_PORT);
			mSocket.setBroadcast(true);
			mSocket.setSoTimeout(SocketUtils.TIMEOUT_MS);
		} catch (SocketException exS) {
			// TODO Auto-generated catch block
			//HMDInspectionActivity.writeLogSystemFolders("SocketException in  listForResponses() "); //Write log
			exS.printStackTrace();
			
			return ERROR;
		}
		
		byte[] buf = new byte[1024];
		while(!mStop) {
			try {
				//	Receive
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				mSocket.receive(packet);
				//	Check for correct HandShake message
				String s = new String(packet.getData(), 0, packet.getLength());
				if (s.equals(DISCORVERY)) {
					//	Receive Discovery message, send Echo
					if(packet.getAddress() != null && !SocketUtils.isLocalAddress(packet.getAddress())) {
						sendDiscoveryResponse(packet.getAddress(), packet.getPort());
						Log.d(TAG, packet.getAddress() + ":" + packet.getPort() + ":"  + s);
						//HMDInspectionActivity.writeLogSystemFolders(" sendDiscoveryResponse :" + packet.getAddress() + ":" + packet.getPort() ); //Write log
					} else {
						Log.d(TAG, "Error");
						//HMDInspectionActivity.writeLogSystemFolders("Error in Receive Discovery message, send Echo "); //Write log
					}
				} else if (s.equals(ECHO)) {
					//	Receive Echo, Get Info
					if(packet.getAddress() != null) {
						mMainACtivity.setPeerInfo(packet.getAddress().toString().substring(1));
						Log.d(TAG +" 2", packet.getAddress() + ":" + packet.getPort() + ":"  + s);
						//HMDInspectionActivity.writeLogSystemFolders("Receive Echo :" + packet.getAddress() + ":" + packet.getPort()); //Write log
					} else {
						Log.d(TAG, "Ignore local Connect");
					}
				}
			} catch (SocketTimeoutException exST) {
				//HMDInspectionActivity.writeLogSystemFolders("SocketTimeoutException in  listForResponses() "); //Write log
				continue;
			} catch (IOException eIO) {
				//HMDInspectionActivity.writeLogSystemFolders("IOException in  listForResponses() "); //Write log
				eIO.printStackTrace();
				return ERROR;
			} catch (Exception ex) {
				//HMDInspectionActivity.writeLogSystemFolders("Exception in  listForResponses() "); //Write log
				ex.printStackTrace();
				return ERROR;
			}
		}
		Log.d(TAG, "Brocast Listener stopped");
		//HMDInspectionActivity.writeLogSystemFolders("Brocast Listener stopped !"); //Write log
		return OK;
	}
}
