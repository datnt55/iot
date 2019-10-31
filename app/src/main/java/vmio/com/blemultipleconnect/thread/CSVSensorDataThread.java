package vmio.com.blemultipleconnect.thread;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;
import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.KalmanFilter;
import vmio.com.blemultipleconnect.activity.DeviceActivity;
import vmio.com.blemultipleconnect.service.BaseService;
import vmio.com.blemultipleconnect.service.OkHttpService;
import vmio.com.mioblelib.ble.BleDevice;
import vmio.com.mioblelib.model.SensorValue;

import static android.provider.Settings.Global.WIFI_SLEEP_POLICY;
import static android.provider.Settings.Global.WIFI_SLEEP_POLICY_NEVER;
import static vmio.com.blemultipleconnect.Utilities.Global.GPSDateTime;
import static vmio.com.blemultipleconnect.Utilities.Global.sensorValues;
import static vmio.com.blemultipleconnect.activity.MainActivity.mConnectedBLEs;
import static vmio.com.blemultipleconnect.activity.SettingActivity.mBlueToothSenSor;

/**
 * Created by DatNT on 12/14/2017.
 */

public class CSVSensorDataThread implements Runnable {
    private DeviceActivity  mThis;
    private boolean stopFlag;
    private String uploadFolder = Define.mMioTempDirectory;
    private File uploadDoneFolder;
    private static final int SLEEP_TIME_BETWEEN_SEND_CSV  = 30*1000;//30000  // every 3 seconds
    private static final int SLEEP_TIME_BETWEEN_SEND_OFFLINE_CSV  = 1000;  // every 1000 miliseconds
    private Thread mThread;
    private UploadCSVListener listener;
    private KalmanFilter kalmanFilter;
    public CSVSensorDataThread(DeviceActivity context,UploadCSVListener listener) {
        this.mThis = context;
        this. stopFlag = false;
        mThread = new Thread(this);
        kalmanFilter = new KalmanFilter();
        this.listener = listener;
    }
    public synchronized void start() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mThread.isAlive())
                    mThread.start();
            }
        },SLEEP_TIME_BETWEEN_SEND_CSV);

    }

    @Override
    public void run() {
        PowerManager pm = (PowerManager) mThis.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK , "myapp:mywakeupapp");
        wl.acquire();
        WifiManager wm = (WifiManager) mThis.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiManager.WifiLock wifiLock= wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "TAG");
        wifiLock.acquire();
        setNeverSleepPolicy();
        while (Global.Connected && !stopFlag) {
            try {
                if (listener != null)
                    listener.onStartUploadZipFile();
                if(stopFlag) break;
                Thread.sleep(SLEEP_TIME_BETWEEN_SEND_CSV);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        synchronized (Global.startThreadCSV) {
            Global.startThreadCSV = false;
        }
        wl.release();
        wifiLock.release();
    }

    private void setNeverSleepPolicy() {
        try {
            ContentResolver cr = mThis.getContentResolver();
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                int set = WIFI_SLEEP_POLICY_NEVER;
                Settings.System.putInt(cr, WIFI_SLEEP_POLICY, set);
            } else {
                int set = WIFI_SLEEP_POLICY_NEVER;
                Settings.System.putInt(cr, WIFI_SLEEP_POLICY, set);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        this.stopFlag = true;
    }

    // Send file to server and delete file if success
    public void sendDataCSVToServer(String username) {
        final File csvFile;
        final File[] childFile = (new File(uploadFolder)).listFiles(new FileFilter() {
            @Override
            public boolean accept(File file)
            {
                return (file.getPath().endsWith(".zip"));
            }
        });
        if (childFile.length > 0){
            boolean isConnectGPS = true;
            if (Global.longitude == 0 || Global.GPSAccuracy > 20)
                isConnectGPS = false;
            csvFile = childFile[0];
            Map<String, Object> params = new HashMap<>();
            params.put("sensor", String.valueOf(Global.isConnectSensor));
            params.put("gps", String.valueOf(isConnectGPS));
            params.put("csv", csvFile);
            if (Global.longitude != 0 && Global.latitude != 0 && Global.GPSAccuracy < 30) {
                // GPS GPSFilter = ApplyKalmanFilter(new GPS(Global.latitude, Global.longitude, Global.GPSAccuracy, Global.GPSDateTime));
                params.put("lat", String.valueOf(Global.latitude));
                params.put("lng", String.valueOf(Global.longitude));
            }
            params.put("username",username);
            new OkHttpService(OkHttpService.Method.POST, mThis, Define.URL_GET_SENSOR_STATUS, params, false) {
                @Override
                public void onFailureApi(Call call, Exception e) {
                    String result = e.getMessage();
                    ALog.e("PING",result);
                }

                @Override
                public void onResponseApi(Call call, Response response) throws IOException {
                    String result = response.body().string();
                    ALog.e("PING",result);
                    try {
                        final JSONObject jon = new JSONObject(result);
                        final String message = jon.getString("message");
                        if (jon.getInt("error") == 0){
                            mThis.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mThis, "データをアップロードしました。",Toast.LENGTH_SHORT).show();
                                }
                            });
                            csvFile.delete();
                        }
                        else {
                            mThis.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mThis, message,Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };
        }
    }

    private String TAG = "GPS";
    public GPS ApplyKalmanFilter(GPS location)
    {
        ALog.e(TAG, GPSDateTime.getMillis()+","+location.lon+","+location.lat+","+location.accuracy);
        if (Global.GPSFirstTime == null) {
            Global.GPSFirstTime = Global.GPSDateTime;
            return location;
        }
        kalmanFilter.Process(location.getLat(), location.getLon(), location.getAccuracy(), (long)(GPSDateTime.getMillis() - Global.GPSFirstTime.getMillis() ));
        ALog.e(TAG, kalmanFilter.get_lng()+","+kalmanFilter.get_lat());
        return new GPS(kalmanFilter.get_lat(), kalmanFilter.get_lng(), location.getAccuracy()/*kf.get_accuracy()*/ /*Keep raw accuracy*/, location.getTimestamp());
    }

    public interface UploadCSVListener{
        void onUploadSuccess(String fileName);
        void onUploadFault(String message);
        void onStartUploadZipFile();
    }

    static public class GPS {
        private double lat;
        private double lon;
        private double accuracy;
        private DateTime timestamp;

        public GPS(double lat, double lon, double accuracy, DateTime timestamp) {
            this.lat = lat;
            this.lon = lon;
            this.accuracy = accuracy;
            this.timestamp = timestamp;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }

        public double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(double accuracy) {
            this.accuracy = accuracy;
        }

        public DateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(DateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

}
