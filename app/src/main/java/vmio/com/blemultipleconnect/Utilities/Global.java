package vmio.com.blemultipleconnect.Utilities;

import java.io.File;
import java.util.ArrayList;

import vmio.com.mioblelib.model.SensorValue;
import vmio.com.mioblelib.widget.Point3D;
import org.joda.time.DateTime;
/**
 * Created by DatNT on 6/19/2017.
 */

public class Global {
    public static boolean Connected = false;
    public static Point3D accel = new Point3D(0, 0, 0);
    public static Point3D magnet = new Point3D(0, 0, 0);
    public static Point3D gyros = new Point3D(0, 0, 0);
    // [20181207    VMio] Flag for notify new sensor data
    public static boolean isNewSensorData = false;
    public static final Object synObject = new Object();
    // [20181207    VMio] add timestamp right at sensor data arrived
    public static DateTime SLAMTimeStamp = DateTime.now();
    public static String uuid = "CC2650 SensorTag", temp = "", ir_temp = "";
    public static Boolean startThread = false;
    public static Boolean startThreadScanBle = false;
    public static Boolean startThreadCSV = false;
    public static boolean isConnectSensor = false;
    public static double longitude = 0f;
    public static double latitude = 0f;
    public static DateTime GPSDateTime;
    public static DateTime GPSFirstTime;
    public static double androidLongitude = 0f;
    public static double androidLatitude = 0f;

    public static double gnsLongitude = 0f;
    public static double gnsLatitude = 0f;

    public static float GNSAccuracy = 0f;
    public static float AndroidAccuracy = 0f;

    public static float GPSAccuracy = 0f;       // [20190125    VMio] Add GPS accuracy, mean diameter of deviation circle current location [meter]
    public static boolean GPSIsAndroid = false; // [20190125    VMio] Add GPS source Android or Bluetooth/NMEA
    public static double GLlongitude = 0;
    public static double GLlatitude = 0;
    public static boolean BleReconnectRequest = false;
    public static final long KILOBYTE = 1024;
    public static final long MIN_FREE_STORAGE = 100;
    public static ArrayList<SensorValue> sensorValues = new ArrayList<>();
    public static File fileLog;
    public static File file;
    public static double[][] calibrationFactor;
    public static int WIDTH_SCREEN;
    public static int HEIGHT_SCREEN;
    public static String ApplicationVersion = "";
    public static String DeviceId = "";
    // public static boolean FirstTime = true;
}
