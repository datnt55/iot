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

import vmio.com.blemultipleconnect.Utilities.DecimalStandardFormat;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.mioblelib.model.SensorValue;

/**
 * Created by DatNT on 12/14/2017.
 */

public class CSVSAThread {
    private Context  mThis;
    private File uploadFolder;
    private Thread mThread;
    private boolean stopFlag;
    private File csvFileSA;
    private Integer TimeTagSA = 0;
    private SensorValue value;
    private SensorValue oldValue;
    private final Handler handler;
    private static final String TAG = "CSVSAThread";
    public CSVSAThread(Context context, File uploadFolder, String timeStamp) {
        this.mThis = context;
        this. stopFlag = false;
        //mThread = new Thread(this);
        this.uploadFolder = uploadFolder;
        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        createCSVFileSA(timeStamp);
    }


    public void createCSVFileSA(String timeStamp){
        csvFileSA = new File(uploadFolder, "SA_" + timeStamp + ".csv");
        //MediaScannerConnection.scanFile(mThis, new String[] {csvFileSA.toString()}, null, null);
        Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri fileContentUri = Uri.fromFile(csvFileSA); // With 'permFile' being the File object
        mediaScannerIntent.setData(fileContentUri);
        mThis.sendBroadcast(mediaScannerIntent);
        TimeTagSA = 1;
        try {
            csvFileSA.createNewFile();
            FileWriter writer = new FileWriter(csvFileSA,true);
            writer.append("TimeTag");
            writer.append(',');
            writer.append("StatInertialLo");
            writer.append(',');
            writer.append("StatInertialHi");
            writer.append(',');
            writer.append("StatPress");
            writer.append(',');
            writer.append("StatMag");
            writer.append(',');
            writer.append("StatTemp");
            writer.append(',');
            writer.append("StatGps");
            writer.append(',');
            writer.append("RatePLo [rad/s]");
            writer.append(',');
            writer.append("RateQLo [rad/s]");
            writer.append(',');
            writer.append("RateRLo [rad/s]");
            writer.append(',');
            writer.append("AccelerateXLo [m/s2]");
            writer.append(',');
            writer.append("AccelerateYLo [m/s2]");
            writer.append(',');
            writer.append("AccelerateZLo [m/s2]");
            writer.append(',');
            writer.append("RatePHi [rad/s]");
            writer.append(',');
            writer.append("RateQHi [rad/s]");
            writer.append(',');
            writer.append("RateRHi [rad/s]");
            writer.append(',');
            writer.append("AccelerateXHi [m/s2]");
            writer.append(',');
            writer.append("AccelerateYHi [m/s2]");
            writer.append(',');
            writer.append("AccelerateZHi [m/s2]");
            writer.append(',');
            writer.append("Pressure [hPa]");
            writer.append(',');
            writer.append("MagneticX [nT]");
            writer.append(',');
            writer.append("MagneticY [nT]");
            writer.append(',');
            writer.append("MagneticZ [nT]");
            writer.append(',');
            writer.append("Tempareture [degreeC]");
            writer.append(',');
            writer.append("Longitude [rad]");
            writer.append(',');
            writer.append("Latitude [rad]");
            writer.append(',');
            writer.append("Altitude [m]");
            writer.append(',');
            writer.append("SpeedNS [m/s]");
            writer.append(',');
            writer.append("SpeedEW [m/s]");
            writer.append(',');
            writer.append("SpeedDU [m/s]");
            writer.append('\n');
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized Integer getTimeTagSA() {
        return TimeTagSA;
    }

    public void setValue(final SensorValue v){
       // this.value = v;
        if (handler.getLooper().getThread().isAlive()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    saveValueSA(v);
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
//                        saveValueSA(value);
//                    } else if (!value.compair2SensorValue(oldValue)) {
//                        saveValueSA(value);
//                    }
//                    if (oldValue == null)
//                        oldValue = new SensorValue();
//                    oldValue.assign(value);;
//                }
//                if(stopFlag) break;
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

    public synchronized void saveValueSA(SensorValue value) {
        if (csvFileSA == null)
            return;
        // Append new csv row to current csv File
        try {
            FileWriter writer = new FileWriter(csvFileSA, true);
            writer.append(""+TimeTagSA);
            writer.append(',');
            writer.append("3");
            writer.append(',');
            writer.append("3");
            writer.append(',');
            writer.append("3");
            writer.append(',');
            writer.append("3");
            writer.append(',');
            writer.append("3");
            writer.append(',');
            writer.append("3");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append(""+ value.getAccelMPS2().x);
            writer.append(',');
            writer.append(""+ value.getAccelMPS2().y);
            writer.append(',');
            writer.append(""+ value.getAccelMPS2().z);
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append(""+ value.getAccelMPS2().x);
            writer.append(',');
            writer.append(""+ value.getAccelMPS2().y);
            writer.append(',');
            writer.append(""+ value.getAccelMPS2().z);
            writer.append(',');
            writer.append(""+ value.getPressure());
            writer.append(',');
            writer.append(""+ value.getMagnetNanoTesla().x);
            writer.append(',');
            writer.append(""+ value.getMagnetNanoTesla().y);
            writer.append(',');
            writer.append(""+ value.getMagnetNanoTesla().z);
            writer.append(',');
            writer.append(""+value.getAmbientTemp());
            writer.append(',');
            writer.append("" + Global.longitude);
            writer.append(',');
            writer.append("" + Global.latitude);
            writer.append(',');
            writer.append(""+calculateAltitude2(value));
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("0");
            writer.append(',');
            writer.append("0");
            writer.append('\n');
            writer.flush();
            writer.close();

//            if (alert == null || !alert.isShowing()){
//                alert = FileUtil.checkDialogFreeStorage(mThis);
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        TimeTagSA ++;
    }

    private double calculateAltitude(SensorValue value){
        double pressure = Math.pow((1005/value.getPressure()),(1/5.257));
        double Kevin = value.getAmbientTemp() + 273.15;
        double constain = Math.pow((1013.25/1005), (1/5.256));
        double altitude=(((pressure-1)*Kevin )/0.0065)*constain;
        return Double.isInfinite(altitude) ? 0 : altitude;
    }

    private double calculateAltitude2(SensorValue value){
//        double pressure = Math.pow((1005/value.getPressure()),(1/5.257));
//        double Kevin = value.getAmbientTemp() + 273.15;
//        double constain = Math.pow((1013.25/1005), (1/5.256));
//        double altitude=(((pressure-1)*Kevin )/0.0065)*constain;
//        return Double.isInfinite(altitude) ? 0 : altitude;

        double altitude = -9.2247 *value.getPressure() + 9381.1;
        DecimalStandardFormat dTime = new DecimalStandardFormat(".####");
        return Double.isInfinite(altitude) ? 0 : Double.valueOf(dTime.format(altitude));
    }

    public void stop(){
        MediaScannerConnection.scanFile(mThis, new String[] {csvFileSA.toString()}, null, null);
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
