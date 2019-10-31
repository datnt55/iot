package vmio.com.blemultipleconnect.thread;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import vmio.com.blemultipleconnect.Utilities.FileUtil;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.mioblelib.model.SensorValue;

/**
 * Created by DatNT on 12/14/2017.
 */

public class CSVGAThread {
    private Context mThis;
    private File uploadFolder;
    private Thread mThread;
    private boolean stopFlag;
    private File csvFileGA;
    private int TimeTagGA;
    private SensorValue value;
    private SensorValue oldValue;
    private DateTime now;
    private Handler handler;
    private static final String TAG = "CSVGAThread";
    public CSVGAThread(Context context, File uploadFolder, String timeStamp) {
        this.mThis = context;
        this.stopFlag = false;
        //mThread = new Thread(this);
        this.uploadFolder = uploadFolder;
        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        createCSVFileGA(timeStamp);
    }


    public void createCSVFileGA(String timeStamp) {
        csvFileGA = new File(uploadFolder, "GA_" + timeStamp + ".csv");
        //MediaScannerConnection.scanFile(mThis, new String[]{csvFileGA.toString()}, null, null);
        Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri fileContentUri = Uri.fromFile(csvFileGA); // With 'permFile' being the File object
        mediaScannerIntent.setData(fileContentUri);
        mThis.sendBroadcast(mediaScannerIntent);
        TimeTagGA = 0;
        try {
            csvFileGA.createNewFile();
            FileWriter writer = new FileWriter(csvFileGA, true);
            writer.append("TimeTag");
            writer.append(',');
            writer.append("DataStatus");
            writer.append(',');
            writer.append("ProductType");
            writer.append(',');
            writer.append("SerialNumber");
            writer.append(',');
            writer.append("SoftwareVersion");
            writer.append(',');
            writer.append("PLDVersion");
            writer.append(',');
            writer.append("Year");
            writer.append(',');
            writer.append("Month");
            writer.append(',');
            writer.append("Day");
            writer.append(',');
            writer.append("Hour");
            writer.append(',');
            writer.append("Minute");
            writer.append(',');
            writer.append("Second");
            writer.append(',');
            writer.append("OffsetUTC [ms]");
            writer.append(',');
            writer.append("OutDataImu");
            writer.append(',');
            writer.append("OutDataAhrs");
            writer.append(',');
            writer.append("OutDataIns");
            writer.append(',');
            writer.append("OutDataNavAccuracy");
            writer.append(',');
            writer.append("OutDataProvision");
            writer.append(',');
            writer.append("OutDataSensor");
            writer.append(',');
            writer.append("OutDataGps");
            writer.append(',');
            writer.append("CalcMethod");
            writer.append(',');
            writer.append("UseSensorInertialLo");
            writer.append(',');
            writer.append("UseSensorInertialHi");
            writer.append(',');
            writer.append("UseSensorMag");
            writer.append(',');
            writer.append("UseSensorTemp");
            writer.append(',');
            writer.append("UseSensorPress");
            writer.append(',');
            writer.append("UseSensorGps");
            writer.append(',');
            writer.append("AlarmCalibTime");
            writer.append(',');
            writer.append("AlignmentStatus");
            writer.append(',');
            writer.append("GpsStatus");
            writer.append(',');
            writer.append("InitializeAccelerateStatus");
            writer.append(',');
            writer.append("FailureStatus");
            writer.append(',');
            writer.append("AttachAngleConf");
            writer.append('\n');
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setValue(final SensorValue v, DateTime now) {
        //this.value = v;
        if (handler.getLooper().getThread().isAlive()) {
            this.now = now;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    saveValueGA(v);
                }
            });
        }
    }

    public synchronized void start() {
        if (!mThread.isAlive())
            mThread.start();
    }

//    @Override
//    public void run() {
//        PowerManager pm = (PowerManager) mThis.getSystemService(Context.POWER_SERVICE);
//        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BackgroundThreadTag01");
//        wl.acquire();
//        while (Global.Connected && !stopFlag) {
//            try {
//                if (value != null) {
//                    if (oldValue == null) {
//                        saveValueHA(value);
//                    } else if (!value.compair2SensorValue(oldValue)) {
//                        saveValueHA(value);
//                    }
//                    if (oldValue == null)
//                        oldValue = new SensorValue();
//                    oldValue.assign(value);
//                    ;
//                }
//                if (stopFlag) break;
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//        synchronized (Global.startThreadCSV) {
//            Global.startThreadCSV = false;
//        }
//        handler.getLooper().quit(); // release handler
//        wl.release();
//    }

    public int getCurrentTimezoneOffset() {

        TimeZone tz = TimeZone.getDefault();
        Calendar cal = GregorianCalendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());

        return offsetInMillis;
    }

    public synchronized void saveValueGA(SensorValue value) {
        if (csvFileGA == null)
            return;
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        TimeTagGA++;
        // Append new csv row to current csv File
        try {
            FileWriter writer = new FileWriter(csvFileGA, true);
            writer.append("" + TimeTagGA);
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("20");
            writer.append(',');
            writer.append("100006");
            writer.append(',');
            writer.append("256");
            writer.append(',');
            writer.append("48");
            writer.append(',');
            writer.append("" + now.getYear());
            writer.append(',');
            writer.append("" + now.getMonthOfYear());
            writer.append(',');
            writer.append("" + now.getDayOfMonth());
            writer.append(',');
            writer.append("" + now.getHourOfDay());
            writer.append(',');
            writer.append("" + now.getMinuteOfHour());
            writer.append(',');
            writer.append("" + now.getSecondOfMinute());
            writer.append(',');
            writer.append("" + getCurrentTimezoneOffset());
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("769");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("1");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("2");
            writer.append('\n');
            writer.flush();
            writer.close();

//            if (alert == null || !alert.isShowing()){
//                alert = FileUtil.checkDialogFreeStorage(mThis);
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void stop() {
        MediaScannerConnection.scanFile(mThis, new String[]{csvFileGA.toString()}, null, null);
        release();
    }

    public void release(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                handler.getLooper().quit();
            }
        });
    }

}
