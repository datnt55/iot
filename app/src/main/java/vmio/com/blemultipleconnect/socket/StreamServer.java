package vmio.com.blemultipleconnect.socket;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 /**
 *  Solution: HMD Inspection System
 *  Device Project: Camera Probe
 *  Package: jp.miosys.camera.socket
 *  Module: StreamServer
 * 	NOTE : THIS CLASS DONT USE IN PROJECT  !! 
 * 	BUT THIS CANT BE USE WHEN NEED 
 * 	Server will hand-to-hand with client with once connect client output stream will
 * 	be added once Constructor stream writer to send data to Client Start Thread
 * 	socketServerReplyThread to handle data receive from client
 * **/
public class StreamServer {
	private OnMessageReceived mMessageListener = null; // Listener message
	public int count = 0;
	ServerSocket serverSocket;
	String message;
	public String sendPath;
	public long elapsedTime;
	InterfaceActivity mMainActivity;
	Bitmap sndBitmap;
	private int byteRead;
	public byte[] bitmap;
	
	/**
	 * Constructor
	 * @param ma
	 */

	public StreamServer(InterfaceActivity ma) {
		mMainActivity = ma;
	}
	
	/**
	 * startReplyThread
	 * @param clientsocket
	 * @param receive
	 * @return
	 */
	public Thread startReplyThread(Socket clientsocket,
                                   OnMessageReceived receive) {
		return new Thread(new SocketServerReplyThread(clientsocket, receive));
	}

	/**
	 * Create
	 */
	public void run() {
		try {
			// constructor serverSocket
			Socket clientsocket = null;
			serverSocket = new ServerSocket(SocketUtils.STREAMING_PORT);
			while (true) {
				if (count < 1) {
					Log.i("SERVER", "" + count);
					count++;
					clientsocket = serverSocket.accept();
					message = "#" + count + " from "
							+ clientsocket.getInetAddress() + ":"
							+ clientsocket.getPort() + "\n";
					// Start class handle data
					Log.i("SERVER", message);
					mMainActivity.getSocketClient(clientsocket);
				}else {
					clientsocket.close();
				}
			}
		} catch (IOException exIO) {
			// TODO Auto-generated catch block
			exIO.printStackTrace();
		}
	}
	
	/**
	 * int32ToBytes
	 * @param i
	 * @return
	 */
	byte[] int32ToBytes(int i) {
		byte[] result = new byte[4];

		result[3] = (byte) (i >> 24);
		result[2] = (byte) (i >> 16);
		result[1] = (byte) (i >> 8);
		result[0] = (byte) (i /* >> 0 */);

		return result;
	}

	/**
	 * Data receive from Socket Client will be handle here First step,we build
	 * simple server so that server has only mission is send all data receive
	 * from client to this client
	 */
	private class SocketServerReplyThread implements Runnable {
		InputStream inStream;
		//private static final String TAG="SocketServerReplyThread";

		SocketServerReplyThread(Socket socket, OnMessageReceived listener) {
			try {
				inStream = socket.getInputStream();
				mMessageListener = listener;
			} catch (IOException exIO) {
				// TODO Auto-generated catch block
				exIO.printStackTrace();
			}

		}

		@Override
		public void run() {
			//long startTime = System.currentTimeMillis();
			//mMainActivity.displayStatus("Receiving...");
			try {
				/*
				byte array[] = new byte[4];
				int len = 0;
				byteRead = inStream.read(array, 0, 4);
				int totalByte = 0;
				if (byteRead == 4) {
					len = byteArrayToInt(array);
					array = new byte[len];
				}
				while (totalByte < len) {
					// read(byte[] arr, int offset, int length)
					byteRead = inStream.read(array, totalByte, len - totalByte);
					totalByte += byteRead;
				}
				long endTime = System.currentTimeMillis();
				elapsedTime = endTime - startTime + 1;
				Log.e(TAG+"Total Byte", totalByte + "");

				int bps = (elapsedTime == 0) ? 0
						: (int) ((long) totalByte * 8000 / elapsedTime);
				if (mMessageListener != null) {
					mMessageListener.messageReceived(array);
				}
				*/
				
				/**
				 * Receive 
				 */
				// Create HEAD
				byte[] head = new byte[2];
				byte[] id = new byte[20];
				byte[] x = new byte[4];
				byte[] y = new byte[4];
				
				if(inStream!=null){
					/**
					 *  Get 2 byte HEAD
					 */
					byte headLength[] = new byte[2];
					int lenHead = 0;
					// Read 2 byte head 
					byteRead = inStream.read(headLength, 0, 2);
					// if not 2 byte -> Return
					if (byteRead != 2) {
						return ;
					}
					// Get length from HEAD (2 byte)
					lenHead = byteArrayToInt(headLength);
					// Create HEAD
					head = new byte[lenHead];
					// receive
					int totalByteHead = 0;
					while (totalByteHead < lenHead) {
						byteRead = inStream.read(head, totalByteHead, lenHead - totalByteHead);
						totalByteHead += byteRead;
					}
					// Get String from bite  ; Get String of head
					String stringHead = new String(head);
					
					/**
					 * 	If Not Good 
					 * Receive
					 * FrameId: 20byte (Same from device sent to )
					 * 4 byte:   X (integer)
					 * 4 byte:   Y (integer)
					 */

					/**
					 * Get Frame ID and X Y
					 * 
					 */
					if (stringHead.equals("NG")){
						//Get Frame Id
						byte frameLength[] = new byte[20];
						int lenFrame = 0;
						byteRead = inStream.read(frameLength, 0, 20);
						if (byteRead != 20) {
							return ;
						}
						lenFrame = byteArrayToInt(frameLength);
						id = new byte[lenFrame];
						int totalByteFrameId = 0;
						while (totalByteFrameId < lenFrame) {
							byteRead = inStream.read(id, totalByteFrameId, lenFrame - totalByteFrameId);
							totalByteFrameId += byteRead;
						}
						
						// Get X
						byte xLength[] = new byte[4];
						int lenX = 0;
						byteRead = inStream.read(xLength, 0, 4);
						if (byteRead != 4) {
							return ;
						}
						lenX = byteArrayToInt(xLength);
						x = new byte[lenX];
						int totalByteX = 0;
						while (totalByteFrameId < lenX) {
							byteRead = inStream.read(x, totalByteX, lenX - totalByteX);
							totalByteX += byteRead;
						}
						
						// Get Y
						byte yLength[] = new byte[4];
						int lenY = 0;
						byteRead = inStream.read(yLength, 0, 4);
						if (byteRead != 4) {
							return ;
						}
						lenY = byteArrayToInt(yLength);
						y = new byte[lenY];
						int totalByteY = 0;
						while (totalByteFrameId < lenY) {
							byteRead = inStream.read(y, totalByteY, lenY - totalByteY);
							totalByteY += byteRead;
						}

					}
				}

				if (mMessageListener != null) {
					mMessageListener.messageReceived(head,id,x,y);
				}
				
			} catch (Exception ex) {
				Log.e("TCP", "Connect Error", ex);
			} finally {
				try {
					serverSocket.close();
				} catch (IOException exIO) {
					// TODO Auto-generated catch block
					exIO.printStackTrace();
				}
			}

		}
	}
	
	/**
	 * closeSocketServer
	 */
	public void closeSocketServer() {
		try {
			serverSocket.close();
		} catch (IOException exIO) {
			exIO.printStackTrace();
		}
	}

	/**
	 *  convert byte array to int
	 * @param b
	 * @return
	 */
	public static int byteArrayToInt(byte[] b) {
		return b[0] & 0xFF | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16
				| (b[3] & 0xFF) << 24;
	}

	/**
	 *  Declare the interface. The method messageReceived(String message) will
	 * @author Thucnd
	 *	must be implemented in the MyActivity
	 *	class at on asynckTask doInBackground
	 */
	public interface OnMessageReceived {
		public void messageReceived(byte arrayHead[], byte arrayId[], byte arrayX[], byte arrayY[]);
	}

}
