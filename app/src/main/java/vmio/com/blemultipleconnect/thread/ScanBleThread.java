package vmio.com.blemultipleconnect.thread;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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

import static vmio.com.blemultipleconnect.Utilities.Define.SCANNING_SENSOR_INTERVAL;
import static vmio.com.blemultipleconnect.activity.MainActivity.mGPS;

/**
 * Created by DatNT on 12/14/2017.
 */

public class ScanBleThread implements Runnable {
    private ReScanBleCallback  callback;
    private boolean stopFlag;
    private Thread mThread;
    public ScanBleThread(ReScanBleCallback callback) {
        this.callback = callback;
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
            while (!stopFlag) {
                if (callback !=null)
                    callback.onRescan();
                Thread.sleep(SCANNING_SENSOR_INTERVAL);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        synchronized (Global.startThreadScanBle) {
//            Global.startThreadScanBle = false;
//        }
    }

    public void stop(){
        this.stopFlag = true;
    }


    public interface ReScanBleCallback{
        void onRescan();
    }
}
