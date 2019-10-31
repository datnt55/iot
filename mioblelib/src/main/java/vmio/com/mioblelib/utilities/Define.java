package vmio.com.mioblelib.utilities;

import android.os.Environment;

/**
 * Created by DatNT on 9/13/2017.
 */

public class Define {
    // Local Folders
    public static final String mMioDirectory = Environment.getExternalStoragePublicDirectory("Mio-Ai-Logger").getAbsolutePath();
    public static final String mMioUploadDirectory = Environment.getExternalStoragePublicDirectory("Mio-Ai-Logger/uploads").getAbsolutePath();
    public static final String mMioUploadSaveDirectory = Environment.getExternalStoragePublicDirectory("Mio-Ai-Logger/Saved").getAbsolutePath();
    public static final String sdCardSaveDirectory = "Mio-Ai-Logger";
    // Server information
    public static final String URL = "http://iot.demo.miosys.vn/logger.php";
    public static final String URL_CSV = "http://iot.demo.miosys.vn/upload_logger.php";

    public static final int STATUS_NOT_CONNECT = 0;
    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_CALIBRATE = 2;
    public static final int STATUS_MEMO = 3;
    public static final int STATUS_SAVE = 4;

    public static final int REQUEST_CODE_CALIBRATE = 100;
    public static final int RESULT_CALIBRATE_OK = 101;
    public static final int RESULT_CALIBRATE_CANCELED = 102;

    public static final float GRAVITY = 9.80665f;
    public static final int MIN_STORAGE_MB = 100;                           // Minimum storage for running app
    public static final int BLE_SERVICE = 14;                               // Number of BLE services
    public static final int MAX_SCAN_TIME_MILISECOND = 60000 * 5;           // 3 minutes
    public static final String APPLICATION_NAME = "Data Logger";
    public static final String BLUETOOTH_GPS_NAME = "GNS 2000";             // Name of GPS Bluetooth 2.1
    public static final String BLE_MULTI_SENSOR_NAME = "CC2650 SensorTag";  // Name of TI Smart bluetooth
    public static final int GPS_STATUS_UPDATE_INTERVAL = 1000;
    public static final String BUNDLE_EDIT_CALIBRATE = "bundle edit calibrate";
    public static final String BUNDLE_BODY_POSITION = "bundle body position";
    public static final boolean DEBUG = false;

    public static final int SENSOR_MAGNET_FILTER_TYPE = 0; // [20181123    VMio] Add Magnet filter type. 0: Low pass, 1: Average last three
    public static final int SENSOR_ACCEL_FILTER_TYPE = 2;  // [20181123    VMio] 0: Get gravity after Low pass filter, 1: Remove gravity. 2: Get averate last three. Default is 2.
}
