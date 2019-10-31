package vmio.com.blemultipleconnect.thread;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.activity.DeviceActivity;
import vmio.com.blemultipleconnect.service.BaseService;
import vmio.com.mioblelib.ble.BleDevice;
import vmio.com.mioblelib.model.SensorValue;

import static vmio.com.blemultipleconnect.Utilities.Global.sensorValues;
import static vmio.com.blemultipleconnect.activity.MainActivity.mConnectedBLEs;
import static vmio.com.blemultipleconnect.activity.MainActivity.mGPS;

/**
 * Created by DatNT on 12/14/2017.
 */

public class RealtimeGPSDataThread implements Runnable {
    private DeviceActivity  mThis;
    private boolean stopFlag;
    private DateTime sendServerTime;
    private static final int SLEEP_TIME_BETWEEN_SEND_REALTIME  = 3000;  // every 3 seconds
    private Thread mThread;
    public RealtimeGPSDataThread(DeviceActivity context) {
        this.mThis = context;
        this. stopFlag = false;
        mThread = new Thread(this);
    }

    public synchronized void start() {
        if (!mThread.isAlive())

            mThread.start();
    }

    @Override
    public void run() {
        try {
            while (Global.Connected && !stopFlag) {
                if (mGPS.size() > 0)
                    sendRealtimeGPSToServer();
                Thread.sleep(SLEEP_TIME_BETWEEN_SEND_REALTIME);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (Global.startThread) {
            Global.startThread = false;
        }
    }

    public void stop(){
        this.stopFlag = true;
    }

        // Send realtime data to server for showing every 3 seconds
        private void sendRealtimeGPSToServer() {
            RequestParams params;
            params = new RequestParams();
            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
            params.put("sensor_name",mGPS.get(0).getBluetoothDevice().getName());
            if (Global.longitude != 0 && Global.latitude != 0) {
                params.put("sensor_gps_long", Global.longitude);
                params.put("sensor_gps_lat", Global.latitude);
            }
            PackageInfo pInfo = null;
            try {
                pInfo = mThis.getPackageManager().getPackageInfo(mThis.getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            String version = pInfo.versionName;
            int apiVersion = android.os.Build.VERSION.SDK_INT;
            TelephonyManager tManager = (TelephonyManager) mThis.getSystemService(Context.TELEPHONY_SERVICE);
            String deviceUuid = tManager.getDeviceId();

            params.put("app_ver", version);
            params.put("device_os_ver", apiVersion);
            params.put("device_uuid", deviceUuid);
            params.put("sensor_uuid", mGPS.get(0).getBluetoothDevice().getAddress());
            params.put("device_type", 1);
            params.put("sensor_timestamp",fmt.print(new DateTime(DateTimeZone.UTC)));
            //Log.e("PARAMS", params.toString());
            BaseService.getHttpClient().post(Define.URL, params, new AsyncHttpResponseHandler() {

                @Override
                public void onStart() {

                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                    // called when response HTTP status is "200 OK"
                    //Log.e("JSON", new String(responseBody));
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e("JSON", "gps failure");
                }

                @Override
                public void onRetry(int retryNo) {
                }
            });
        }
}
