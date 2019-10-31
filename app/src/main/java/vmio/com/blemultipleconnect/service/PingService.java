package vmio.com.blemultipleconnect.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;
import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.Global;

public class PingService extends Service {
    private CustomBinder mBinder = new CustomBinder(this);

    public class CustomBinder extends Binder {
        final PingService bubblesService;

        public CustomBinder(PingService bubblesService) {
            this.bubblesService = bubblesService;
        }

        public PingService getService() {
            return this.bubblesService;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // Send file to server and delete file if success
    public void sendDataCSVToServer(String username) {
       // while (true) {
            final File csvFile;
            final File[] childFile = (new File(Define.mMioTempDirectory)).listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return (file.getPath().endsWith(".zip"));
                }
            });
            if (childFile.length > 0) {
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
                params.put("username", username);
                new OkHttpService(OkHttpService.Method.POST, null, Define.URL_GET_SENSOR_STATUS, params, false) {
                    @Override
                    public void onFailureApi(Call call, Exception e) {
                        String result = e.getMessage();
                        ALog.e("PING", result);
                    }

                    @Override
                    public void onResponseApi(Call call, Response response) throws IOException {
                        String result = response.body().string();
                        ALog.e("PING", result);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), "データをアップロードしました。", Toast.LENGTH_SHORT).show();
                            }
                        });
                        try {
                            JSONObject jon = new JSONObject(result);
                            if (jon.has("success")) {
                                csvFile.delete();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
            }
        //}
    }

}
