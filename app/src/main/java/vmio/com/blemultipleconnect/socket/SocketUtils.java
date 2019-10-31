package vmio.com.blemultipleconnect.socket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build.VERSION;
import android.util.Log;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

/**
 * NOTE : Some function can not be use in this class  !
 * 
 * @author Thucnd
 *
 */
public class SocketUtils {
	private static final String TAG = "SocketUtils";
	public static final String SIGNATURE_KEY = "miosys_wifidirect";
	public static final String SENDCONNECT_KEY = "miosys_wificonnect";
	public static final int BROADCAST_PORT = 7309;//1976;//2562;
	public static final int STREAMING_PORT = 7390;//1991;//2336;
	public static final int TIMEOUT_MS = 500;
	public static final int TIMEOUT_FIND_PEERS_MS = 3000;
	private static WifiManager mWifi;
	private static String mWifiMacAddress;
	
	/**
	 * Constructor
	 * @param context
	 */
	public SocketUtils(Context context) {
		initWifi(context);
	}
	
	/**
	 * initWifi
	 * @param context
	 */
	@SuppressWarnings("static-access")
	public static void initWifi(Context context)
	{
		if(mWifi == null) {
			mWifi = (WifiManager)context.getSystemService(context.WIFI_SERVICE);
			if (!mWifi.isWifiEnabled()){
				String msg = "Wifi is disabled, do you want enable?";
				((InterfaceActivity) context).displayAlert(msg);
			}
		}
	}
	
	/**
	 * turnOnWifi
	 */
	public static void turnOnWifi(){
		mWifi.setWifiEnabled(true);
		mWifiMacAddress = getWifiMACAddress();
	}
	
	/**
	 * 	get security message for hand shake
	 * @return
	 */
	public static String getHandShakeRequestMessage() {
		return String.format("%s", getSignature(SIGNATURE_KEY));
	}
	
	/**
	 * getHandShakeConnectMessage
	 * @return
	 */
	public static String getHandShakeConnectMessage() {
		return String.format("%s", getSignature(SENDCONNECT_KEY));
	}
	
	/**
	 * 	get security message for hand shake
	 * @return
	 */
	public static String getHandShakeResponseMessage() {
		return String.format("%s", getSignature(SIGNATURE_KEY));
	}
	
	/**
	 * 	check security message for hand shake
	 * @param peerMessage
	 * @return
	 */
	public static boolean checkHandShakeMessage(String peerMessage) {
		return (peerMessage == getSignature(SIGNATURE_KEY));
	}
	
	/**
	 * 	Get broadcast address by wifi manager
	 * @return
	 */
	public static InetAddress getBroadcastAddressByWifi() {
		byte[] quads = new byte[4];
		try {
			if(mWifi == null)
				return null;
			DhcpInfo dhcp = mWifi.getDhcpInfo();
			if (dhcp == null) {
				Log.d(TAG, "could not get DHCP");
				return null;
			}
			int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
			
			for (int k = 0; k < 4; k++) {
				quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
				Log.e("DISCOVERY", quads[k] + "");
			}
			return InetAddress.getByAddress(quads);
		} catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * 	Signature from plan key
	 * @param planKey
	 * @return
	 */
	public static String getSignature(String planKey) {
		 try {
	            MessageDigest md = MessageDigest.getInstance("MD5");
	            byte[] messageDigest = md.digest(planKey.getBytes());
	            BigInteger number = new BigInteger(1, messageDigest);
	            String hashtext = number.toString(16);
	            // Now we need to zero pad it if you actually want the full 32 chars.
	            while (hashtext.length() < 32) {
	                hashtext = "0" + hashtext;
	            }
	            return hashtext;
	        }
	        catch (NoSuchAlgorithmException exNSA) {
	            throw new RuntimeException(exNSA);
	        }
	}
	
	/**
	 * 	get local Ip addresses
	 * @return
	 */
	public String getLocalIpAddress() {
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                    return inetAddress.getHostAddress().toString();
	                }
	            }
	        }
	    } catch (SocketException exS) {
	    	exS.printStackTrace();
	    }
	    return null;
	}
	
	/**
	 * 	Get wifi interface Mac
	 * @return
	 */
	private static String getWifiMACAddress() {
		if(mWifi == null)
			return null;
	    WifiInfo info = mWifi.getConnectionInfo();
	    return info.getMacAddress();
	}

	
	
	/**
	 * getDeviceIPAddresses
	 * @return
	 */
	@SuppressLint("NewApi")
	public static List<String> getDeviceIPAddresses() {
		List<String> Addresses =  new ArrayList<String>();
		try {
            for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
				NetworkInterface ni = niEnum.nextElement();
				if (!ni.isLoopback() && ni.isUp()) {
            		List<InterfaceAddress> interfaceAddresses = ni.getInterfaceAddresses();
					for (InterfaceAddress interfaceAddress: interfaceAddresses) {
					    InetAddress Address = interfaceAddress.getAddress();
						if(!Address.isLoopbackAddress() && Address instanceof Inet4Address) {
							String address = interfaceAddress.getAddress().toString().substring(1);
							Addresses.add(address);
						}
					}
            	}
            }
        } catch (Exception ex) {
        	ex.printStackTrace();
        } // for now eat exceptions
        return Addresses;
	}

	
	/**
	 * getDeviceInetAddresses
	 * @return
	 */
	@SuppressLint("NewApi")
	public static List<InetAddress> getDeviceInetAddresses() {
		List<InetAddress> Addresses =  new ArrayList<InetAddress>();
		try {
            for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
				NetworkInterface ni = niEnum.nextElement();
				if (!ni.isLoopback() && ni.isUp()) {
            		List<InterfaceAddress> interfaceAddresses = ni.getInterfaceAddresses();
					for (InterfaceAddress interfaceAddress: interfaceAddresses) {
					    if(interfaceAddress.getAddress() != null)
					    	Addresses.add(interfaceAddress.getAddress());
					}
            	}
            }
        } catch (Exception ex) {
        	ex.printStackTrace();
        } // for now eat exceptions
        return Addresses;
	}
	
	
	/**
	 * Check
	 * isLocalAddress
	 * @param iaddress
	 * @return
	 */
	public static boolean isLocalAddress(InetAddress iaddress)
	{
		List<InetAddress> addressList = getDeviceInetAddresses();
		if(addressList != null) {
			return addressList.contains(iaddress);
		}
		return false;
	}
	
	/**
	 * getBroadcastAddress
	 * @return
	 */
	@SuppressLint({ "NewApi", "DefaultLocale" })
	public static InetAddress getBroadcastAddress() {
	    System.setProperty("java.net.preferIPv4Stack", "true");
        try {
		    for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
		        NetworkInterface ni = niEnum.nextElement();
		        if(VERSION.SDK_INT < 9) {
		        	if(!ni.getInetAddresses().nextElement().isLoopbackAddress()){
		                byte[] quads = ni.getInetAddresses().nextElement().getAddress();
		                quads[0] = (byte)255;
		                quads[1] = (byte)255;
							return InetAddress.getByAddress(quads);
		            }
		        } else {
			        if (!ni.isLoopback() && ni.isUp()) {
			        	byte[] mac = ni.getHardwareAddress();
			    		if(mac != null) {
				        	StringBuilder sb = new StringBuilder();
				    		for (int i = 0; i < mac.length; i++) {
				    			sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
				    		}
				    		String niMac = sb.toString().toLowerCase();
				    		if(niMac.equals(mWifiMacAddress)) {
					        	List<InterfaceAddress> interfaceAddresses = ni.getInterfaceAddresses();
					        	for (InterfaceAddress interfaceAddress: interfaceAddresses) {
									InetAddress Address = interfaceAddress.getAddress();
									if(!Address.isLoopbackAddress() && Address instanceof Inet4Address) {
										InetAddress bAddress = interfaceAddress.getBroadcast();
										return bAddress;
									}
					            }
				    		}
			    		}
			        }
		    	}
		    }
		} catch (UnknownHostException exUH) {
			// TODO Auto-generated catch block
			exUH.printStackTrace();
		} catch (SocketException exS) {
			// TODO Auto-generated catch block
			exS.printStackTrace();
		}
	    return null;
	}
	
	/**
	 * asBytes
	 * @param addr
	 * @return byte[]
	 */
	public final static byte[] asBytes(String addr) {
		int ipInt = parseNumericAddress(addr);
		if (ipInt == 0)
			return null;
		byte[] ipByts = new byte[4];

		ipByts[3] = (byte) (ipInt & 0xFF);
		ipByts[2] = (byte) ((ipInt >> 8) & 0xFF);
		ipByts[1] = (byte) ((ipInt >> 16) & 0xFF);
		ipByts[0] = (byte) ((ipInt >> 24) & 0xFF);
		return ipByts;
	}

	
	/**
	 * parse Numeric Address
	 * @param ipaddr
	 * @return 0 :  Exception
	 * 		   else  :  IpInt
	 */
	public final static int parseNumericAddress(String ipaddr) {
		if (ipaddr == null || ipaddr.length() < 7 || ipaddr.length() > 15)
			return 0;
		StringTokenizer token = new StringTokenizer(ipaddr, ".");
		if (token.countTokens() != 4)
			return 0;
		int ipInt = 0;
		while (token.hasMoreTokens()) {
			String ipNum = token.nextToken();
			try {
				int ipVal = Integer.valueOf(ipNum).intValue();
				if (ipVal < 0 || ipVal > 255)
					return 0;
				ipInt = (ipInt << 8) + ipVal;
			} catch (NumberFormatException exNF) {
				return 0;
			}
		}
		return ipInt;
	}
}
