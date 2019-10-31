package vmio.com.blemultipleconnect.thread;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import vmio.com.mioblelib.model.SensorValue;

/**
 * Created by DatNT on 12/14/2017.
 */

public class CSVHAThread {
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
    private static final String TAG = "CSVHAThread";
    public CSVHAThread(Context context, File uploadFolder, String timeStamp) {
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
        csvFileGA = new File(uploadFolder, "HA_" + timeStamp + ".csv");
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
            writer.append("Humidity");
            writer.append(',');
            writer.append("GPS Accuracy");
            writer.append('\n');
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setValue(final SensorValue v, final float accuracy) {
        //this.value = v;
        if (handler.getLooper().getThread().isAlive()) {
            this.now = now;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    saveValueHA(v,accuracy);
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

    public synchronized void saveValueHA(SensorValue value, float accuracy) {
        if (csvFileGA == null)
            return;
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        TimeTagGA++;
        // Append new csv row to current csv File
        try {
            FileWriter writer = new FileWriter(csvFileGA, true);
            writer.append("" + TimeTagGA);
            writer.append(',');
            writer.append(value.getHumidity()+"");
            writer.append(',');
            writer.append(accuracy+"");
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
