package vmio.com.blemultipleconnect.Utilities;

import android.os.Environment;

/**
 * Created by DatNT on 9/13/2017.
 */

public class Define {
    // Local Folders
    public static final String mMioDirectory = Environment.getExternalStoragePublicDirectory("Mio-Ai-Logger").getAbsolutePath();
    public static final String mMioTempDirectory = Environment.getExternalStoragePublicDirectory("Mio-Ai-Logger/temp").getAbsolutePath();
    public static final String mMioUploadDirectory = Environment.getExternalStoragePublicDirectory("Mio-Ai-Logger/uploads").getAbsolutePath();
    public static final String mMioUploadSaveDirectory = Environment.getExternalStoragePublicDirectory("Mio-Ai-Logger/Saved").getAbsolutePath();
    public static final String sdCardSaveDirectory = "Mio-Ai-Logger";
    // Server information
    public static final String URL = "http://iot.demo.miosys.vn/logger.php";
    public static final String URL_CSV = "http://iot.demo.miosys.vn/upload_logger.php";
    public static final String URL_CSV_DATA = "http://iot.demo.miosys.vn/upload_logger_csv_zip.php";
    public static final String URL_CALIBRATE = "http://ailogger-calib.aimap.jp/api/values";
    public static final String URL_GET_CALIBRATE = "http://ailogger-calib.aimap.jp/api/values/cc2650MacAddr";
    //public static final String HOST = "https://ai-con.aimap.jp"; // honban
    public static final String HOST = "https://ai-con-stg2.aimap.jp"; // staging2
    public static final String URL_LOGIN = HOST + "/api/v2/user/login";
    public static final String URL_LOGOUT = HOST+"/api/v1/user/logout";
    public static final String URL_GET_WORKER_ID = HOST + "/api/v1/user/login-uuid";
    public static final String URL_GET_SENSOR_STATUS = HOST+"/api/v2/media/ping-connect-logger";

    public static final int STATUS_NOT_CONNECT = 0;
    public static final int STATUS_CONNECTED = 1;
    public static final int STATUS_CALIBRATE = 2;
    public static final int STATUS_CALIBRATE_MAGNET = 6;
    public static final int STATUS_MEMO = 3;
    public static final int STATUS_SAVE = 4;

    public static final int REQUEST_CODE_CALIBRATE = 100;
    public static final int REQUEST_CODE_CALIBRATE_MAGNET = 103;
    public static final int RESULT_CALIBRATE_OK = 101;
    public static final int RESULT_CALIBRATE_CANCELED = 102;

    public static final int RESULT_CALIBRATE_MAGNET_OK = 104;
    public static final int RESULT_CALIBRATE_MAGNET_CANCELED = 105;

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
    public static final String BUNDLE_CHART = "bundle chart";
    public static final boolean DEBUG = false;// false;

    public static final int MAGNET_CALIB_MAXTIME_MILISEC = 60000;//60000;           // [20180905    VMio] Max time for magnetic calibration in mili sec
    public static final int MAGNET_CALIB_MAXTIME_DATA = 400;//400;//128;                // [20180905    VMio] Max calibration data about 10 datum per sec
    public static final int MAGNET_FAST_CALIB_MAXTIME_DATA = 140;//140;//128;                // [20180905    VMio] Max calibration data about 10 datum per sec
    public static final int SCANNING_SENSOR_INTERVAL = 10000;
    public static final int CHECK_ACTIVE_SENSOR_INTERVAL = 7000;
    public static final int CONNECT_SENSOR_INTERVAL = 8000;
    public static final int SAVE_CSV_INTERVAL = 800;
    public static final int SLAM_COLLECT_SENSOR_INTERVAL = 50;             // [20181207    VMio] Interval for writing SLAM CSV (MA.CSV). Write only new data.
    public static final float DEG2RAD_COEFF = (float)Math.PI / 180.0f;
    public static final float RAD2DEG_COEFF = (float)(180.0f/Math.PI);
    public static final double[][]  T_WEWe = {{0, -1, 0}, {1, 0, 0}, {0, 0, 1}};
    //public static final double[][]  T_WEWe = {{0, 1, 0}, {0, 0, -1}, {-1, 0, 0}};
}
