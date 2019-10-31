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

/**
 * Created by DatNT on 12/14/2017.
 */

public class RealtimeSensorDataThread implements Runnable {
    private DeviceActivity  mThis;
    private boolean stopFlag;
    private DateTime sendServerTime;
    private static final int SLEEP_TIME_BETWEEN_SEND_REALTIME  = 3000;  // every 3 seconds
    private Thread mThread;
    public RealtimeSensorDataThread(DeviceActivity context) {
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
        Log.e("Connected", Global.Connected+"");
        try {
            while (Global.Connected && !stopFlag) {
                if (sendServerTime != null) {
                    long differentTime = new DateTime().getMillis() - sendServerTime.getMillis();
                    if (differentTime / 60000 >= 2) {
                        mThis.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Global.BleReconnectRequest = true;
                                final Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        for (BleDevice bluetoothDevice : mConnectedBLEs)
                                            bluetoothDevice.disconnect();
                                    }
                                }, 3000);
                            }
                        });
                        synchronized (Global.startThread) {
                            Global.startThread = false;
                        }
                        return;
                    }
                }
                sendRealtimeDataToServer();
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
    private void sendRealtimeDataToServer() {

        for (SensorValue value : sensorValues) {
            if (!value.haveValue())
                continue;
            RequestParams params;
            params = new RequestParams();
            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
            params.put("sensor_uuid", value.getName());
            params.put("sensor_name", Global.uuid);
            params.put("sensor_accel_x",value.getAccel().x);
            params.put("sensor_accel_y",value.getAccel().y);
            params.put("sensor_accel_z",value.getAccel().z);
            params.put("sensor_gyro_x", value.getGyros().x);
            params.put("sensor_gyro_y", value.getGyros().y);
            params.put("sensor_gyro_z", value.getGyros().z);
            params.put("sensor_mag_x", value.getMagnet().x);
            params.put("sensor_mag_y", value.getMagnet().y);
            params.put("sensor_mag_z",value.getMagnet().z);
            params.put("sensor_ambient_temp", value.getAmbientTemp());
            params.put("sensor_object_temp",value.getObjectTemp());
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
            params.put("device_type", 1);
            params.put("sensor_timestamp", fmt.print(new DateTime(DateTimeZone.UTC)));
            Log.e("PARAMS", params.toString());
            BaseService.getHttpClient().post(Define.URL, params, new AsyncHttpResponseHandler() {

                @Override
                public void onStart() {

                }

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                    // called when response HTTP status is "200 OK"
                    Log.e("JSON", new String(responseBody));
                    sendServerTime = new DateTime();
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e("JSON", "sensor failure");
                }

                @Override
                public void onRetry(int retryNo) {
                }
            });
        }
    }

}
