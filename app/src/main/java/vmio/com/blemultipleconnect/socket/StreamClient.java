package vmio.com.blemultipleconnect.socket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import vmio.com.blemultipleconnect.activity.SendDataViaSocketActivity;


/**
 * Solution: HMD Inspection System
 * Device Project: Camera Probe
 * Package: jp.miosys.camera.socket
 * Module: StreamClient
 * @author Datnt VMIO
 * @date 2014/09/05
 * @author Thucnd VMIO
 * @date 2014/10/08 Fix code : (maintain and add comment )
 * Implement Streaming communication between client and server for send Frame/receive OK/NG/NF message
 * 
 */

public class StreamClient 
{
	public boolean mRun = false; // check run client
	//CONSTANT
	public static int LENGTH = 0;
	public static final int OK = 0;
	public static final int ERROR = -1;
	public static final int TIME_OUT = 5000;
	public static final int SLEEP = 1000;
	public static final int JOIN = 3000;
	
	OutputStream out; 	// Sending Stream
	InputStream in; 	// Reading Stream
	public InetAddress serverAddr;
	public long elapsedTime = 0;
	InterfaceActivity mMainActivity;
	OutputStream outStream;
	Bitmap sndBitmap;
	Socket clientsocket;
	
	int count = 0;
	// Data 
	private byte[] frameData;
	String timeStamp="";
	InputStream inStream;
	// Receive
	ServerSocket serverSocket;
	// TAG 
	public static final String TAG ="StreamClient";
	private Thread threadReceive;
	// Check connect 
	public static boolean isConnect = false;
	private boolean receiveFlag = true;
	/**
	 * Constructor of the class. OnMessagedReceived listens for the messages
	 * received from server
	 */
	public StreamClient(InterfaceActivity ma, InetAddress serAddress) {
		mMainActivity = ma;
		serverAddr = serAddress;
	}
	
	/**
	 * Connect to server
	 * 		+ Connect to TCP socket server
	 * 		+ Timeout is 5000ms
	 */
	public boolean Connect() 
	{
		try {
			clientsocket = new Socket();
			clientsocket.connect(new InetSocketAddress(serverAddr, SocketUtils.STREAMING_PORT), TIME_OUT);
			outStream = clientsocket.getOutputStream();
			inStream = clientsocket.getInputStream();
		} catch (IOException exIO) {
			//HMDInspectionActivity.writeLogSystemFolders("IOException Conected in Connect function"); //Write log
			outStream = null;
			inStream = null;
			exIO.printStackTrace();
			isConnect = false ;
			return false;
			
		}
		isConnect = true;
		return true;
	}

	/**
	 * Sends the message entered by client to the server message text entered by
	 * client
	 */
	public void sendMessage(String message) {
		if (out != null) {
			try {
				out.flush();
			} catch (IOException exIO) {
				// TODO Auto-generated catch block
				//HMDInspectionActivity.writeLogSystemFolders("IOException sendMessage in Stream Client !"); //Write log
				exIO.printStackTrace();
			}
		}
	}
	
	/**
	 * Set data frameData
	 * @param select
	 */
	@SuppressLint("DefaultLocale")
	public void setFrameData( byte[] data ) 
	{
		this.frameData = new byte[data.length];
		System.arraycopy(data, 0, this.frameData, 0, data.length);
	}
	
	/**
	 * 	Set time Stamp
	 * 
	 */
	public void setTimeStamp( String timeStamp ) {
		this.timeStamp = timeStamp;
	}
	
	/**
	 * 	stopClient
	 */
	public void stopClient() 
	{
		try {
			receiveFlag = false;
			Thread.sleep(SLEEP);
			closeSocketClient();
			if(threadReceive != null)
				threadReceive.join(JOIN);
		} catch (InterruptedException exI) {
			// TODO Auto-generated catch block
			//HMDInspectionActivity.writeLogSystemFolders("InterruptedException in stopClient!"); //Write log
			exI.printStackTrace();
		}
	}
	
	
	/**
	 * Send data synchronously
	 * @return -1 : ERROR
	 * 			0 : OK
	 */
	public int sendFrameData()
	{
		byte[] pakageFrame = packageSendFrameData();
		
		int length = pakageFrame.length;
		if(length == 0)
			return  ERROR; //[180404 dungtv]
		byte arrlen[] = int32ToBytes(length);

		//	write header -> length
		try {
			outStream.write(arrlen, 0, 4);
			outStream.write(pakageFrame, 0, length);
			//HMDInspectionActivity.writeLogSystemFolders(" sendFrameData() ");//Write Log
		} catch (IOException exIO) {
			// TODO Auto-generated catch block
			//HMDInspectionActivity.writeLogSystemFolders(" sendFrameData() Error IOException ");//Write Log
			exIO.printStackTrace();
			return ERROR;
		}
		
		return OK;
	}
	
	/**
	 *	Package Frame data with time stamp
	 */
	public byte[] packageSendFrameData()
	{
		byte arrTimeStamp[] = new byte[256];
		Log.i("SEVER", arrTimeStamp.length + "");
		System.arraycopy(timeStamp.getBytes(), 0, arrTimeStamp, 0, timeStamp.getBytes().length);
		arrTimeStamp[timeStamp.length()] = 0;
		
		// Copy time stamp and data to total_array
		byte[] arrPakage = new byte[frameData.length + arrTimeStamp.length];
		// Copy time Stamp to total_array
		System.arraycopy(arrTimeStamp, 0, arrPakage, 0, arrTimeStamp.length);
		// Copy data to total_array
		System.arraycopy(frameData, 0, arrPakage, arrTimeStamp.length, frameData.length);
		return arrPakage;
	}
	

	/**
	 * Function Receive
	 * Call in Activity asynchronously 
	 */
	public void receiveAsync() 
	{
		threadReceive = new Thread(new ReceivedThread());
		threadReceive.start();
	}
	

	
	/**
	 * 	Receive until finish of fragment data streaming
	 *  @param1: dest: destination buffer
	 *  @param2: offset: offset in dest to receive
	 *  @param3: length: length of buffer in byte
	 * 	return Total byte received 
	 * 			 0 : OK
	 * 			-1 : if ERROR
	 */
	public int receiveUntilFinish(byte[] dest, int offset, int length) 
	{
		int byteRead, totalByte = 0;
		try {
			//	Receive until length
			while(receiveFlag && totalByte < length) {
				byteRead = inStream.read(dest, offset, length - totalByte);
				if(byteRead == ERROR) {
					return ERROR;
				}
				totalByte += byteRead;
			}
		} 
		catch (Exception ex)
		{
			ex.printStackTrace();
			return ERROR;
		}
		return totalByte;
	}
	
	/**
	 * ReceivedThread 
	 * Call in function receive
	 */
	class ReceivedThread implements Runnable {
		@Override
		public void run() {
			while(receiveFlag)
			{
				try {
					Log.e(TAG, " Go to the Received ");
					if(inStream != null) {
						//	Read header first
						byte[] head = new byte[2];
						if(ERROR == receiveUntilFinish(head, 0, 2)){
							return;
						}
						// Get String from bite  ; Get String of head
						String stringHead = new String(head);
						
						//HMDInspectionActivity.writeLogSystemFolders(" Receive NG/NF/OK Succesfull");//Write Log
						
						/**
						 * If Not Good 
						 * Receive
						 * FrameId: 20byte (Same from device sent to )
						 * 4 byte:   X (integer)
						 * 4 byte:   Y (integer)
						 */

						mMainActivity.display(stringHead,null,0,0);
					}
	
				} catch (Exception ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
					//HMDInspectionActivity.writeLogSystemFolders("Exception Receive NG/NF/OK");//Write Log
				}
			}
		}
	}
	
	/*****************************************************************************************
	 * 	UTILITY Functions
	 *****************************************************************************************
	/**
	 * Convert int32 To Byte Array
	 * @param i
	 * @return byte array
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
	 * Convert File To Byte Array
	 * @param file
	 * @return byte
	 */
	byte[] FileToBytes(File file) {
		int size = (int) file.length();
		byte[] bytes = new byte[size];
		try {
			BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
			buf.read(bytes, 0, bytes.length);
			buf.close();
		} catch (FileNotFoundException exFNF) {
			// TODO Auto-generated catch block
			exFNF.printStackTrace();
		} catch (IOException exIO) {
			// TODO Auto-generated catch block
			exIO.printStackTrace();
		}
		return bytes;
	}
	
	/**
	 * Close socket Client
	 */
	@SuppressWarnings("deprecation")
	private void closeSocketClient() {
		if (clientsocket != null) {
			try {
				clientsocket.close();
				Log.e(TAG, "CLOSE CLIENT ...........! ");
				//HMDInspectionActivity.writeLogSystemFolders("Close client Socket");//Write Log
			} catch (IOException exIO) {
				// TODO Auto-generated catch block
				exIO.printStackTrace();
				threadReceive.stop();
				threadReceive = null;
			}
		}
	}

	/**
	 * Get client Socket 
	 */
	public Socket getClientSocket(){
		return this.clientsocket;
	}
	/**
	 *  Convert from bitmap to byte array
	 *  @param bitmap
	 *  @return byte array
	 */
	public byte[] getBytesFromBitmap(Bitmap bitmap) {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.JPEG, 70, stream);
		return stream.toByteArray();
	}
	
	/**
	 * ImageView To Bitmap
	 * @param ImageView iv
	 * @return Bitmap
	 */
	public static Bitmap imageViewToBitmap(ImageView iv)
	{
		BitmapDrawable drawable = (BitmapDrawable) iv.getDrawable();
		return drawable.getBitmap();
	}
	
	/**
	 * Get Real Path From URI
	 * @param context
	 * @param contentUri
	 * @return Real Path
	 */
	public String getRealPathFromURI(Context context, Uri contentUri)
	{
		Cursor cursor = null;
		try {
			String[] proj = {};
			cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			return cursor.getString(column_index);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
	
	/**
	 * Convert byte array of 4 to integer
	 * @param b
	 * @return integer value
	 */
	public static int byteArrayToInt(byte[] b) {
		return b[0] & 0xFF | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24;
	}
}