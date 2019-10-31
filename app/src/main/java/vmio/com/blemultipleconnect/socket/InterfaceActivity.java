package vmio.com.blemultipleconnect.socket;

import java.net.Socket;

/**
 * Solution: HMD Inspection System
 * Device Project: Camera Probe
 * Package: jp.miosys.camera.socket
 * User for StreamClietn ; StreamSever ; BroadCastFinder; BroadCastListener
 * @date 15/09/2014
 * @author Thucnd 
 */
public interface InterfaceActivity {
	
	/**
	 * set PeerInfo Will implements in Activity Class
	 * @param info
	 * 
	 */
	public void setPeerInfo(String info);

	/**
	 * get Socket Client 
	 * Will implements in Activity class
	 * @param clientSocket
	 */
	public void getSocketClient(Socket clientSocket);

	/**
	 * Display in Activity
	 * @param msg
	 */
	public void displayAlert(String msg);
	
	/**
	 * Display in Activity 
	 * @param msg
	 */
	public void display(String msg1, String msg2, int msg3, int msg4);

}
