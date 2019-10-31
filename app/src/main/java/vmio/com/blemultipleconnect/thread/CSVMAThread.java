package vmio.com.blemultipleconnect.thread;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.widget.ZipManager;
import vmio.com.mioblelib.model.SensorValue;
import vmio.com.mioblelib.widget.Point3D;
import vmio.com.mioblelib.widget.Sensor;

/**
 * Created by DatNT on 12/14/2017.
 */

public class CSVMAThread {
    private Context mThis;
    private File uploadFolder;
    private File tempFolder;
    private Thread mThread;
    private boolean stopFlag;
    private File csvFileTS, tempCSV;
    private int TimeTagTS, tempCount = 0;
    private Integer SyncTag = 0;
    private SensorValue value;
    private SensorValue oldValue;
    private final Handler handler, handlerTempCSV;
    private static final String TAG = "CSVMAThread";
    private static final String TAG1 = "CSVMAThread1";
    private FileWriter writerMA, writerTemp;
    private MASaveFileListener listener;
    private String marker = "";

    public CSVMAThread(Context context, File uploadFolder, String timeStamp, MASaveFileListener listener) {
        this.mThis = context;
        this.stopFlag = false;
        this.listener = listener;
        //mThread = new Thread(this);
        this.uploadFolder = uploadFolder;
        tempFolder = new File(Define.mMioTempDirectory);
        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        final HandlerThread handlerThreadTemp = new HandlerThread(TAG);
        handlerThreadTemp.start();
        handlerTempCSV = new Handler(handlerThreadTemp.getLooper());

        csvFileTS = new File(uploadFolder, "MA_" + timeStamp + ".csv");
        createCSVFile();
        tempCSV = new File(tempFolder, "MA_" + new SharePreference(mThis).getId() + "_" + tempCount + ".csv");
        createCSVTempFile();
    }

    public void createTempCSV() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                tempCSV = new File(tempFolder, "MA_" + new SharePreference(mThis).getId() + "_" + tempCount + ".csv");
                createCSVTempFile();
            }
        });

    }

    public String getTempCSVFile() {
        return tempCSV.getAbsolutePath();
    }

    public void createCSVFile() {
        TimeTagTS = 0;
        // MediaScannerConnection.scanFile(mThis, new String[] {csvFileTS.toString()}, null, null);
        Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri fileContentUri = Uri.fromFile(csvFileTS); // With 'permFile' being the File object
        mediaScannerIntent.setData(fileContentUri);
        mThis.sendBroadcast(mediaScannerIntent);
        TimeTagTS = 0;
        try {
            csvFileTS.createNewFile();
            writerMA = new FileWriter(csvFileTS, true);
            initWriter(writerMA);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createCSVTempFile() {
        // MediaScannerConnection.scanFile(mThis, new String[] {csvFileTS.toString()}, null, null);
        Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri fileContentUri = Uri.fromFile(tempCSV); // With 'permFile' being the File object
        mediaScannerIntent.setData(fileContentUri);
        mThis.sendBroadcast(mediaScannerIntent);
        try {
            tempCSV.createNewFile();
            writerTemp = new FileWriter(tempCSV, true);
            initWriter(writerTemp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initWriter(FileWriter writer) {
        try {
            writer.append("TimeTag");
            writer.append(',');
            writer.append("SyncTag");
            writer.append(',');
            writer.append("Hour");
            writer.append(',');
            writer.append("Minute");
            writer.append(',');
            writer.append("Second");
            writer.append(',');
            writer.append("MilliSecond");
            writer.append(',');
            writer.append("Acceleration X");
            writer.append(',');
            writer.append("Acceleration Y");
            writer.append(',');
            writer.append("Acceleration Z");
            writer.append(',');
            writer.append("Gyroscope X");
            writer.append(',');
            writer.append("Gyroscope Y");
            writer.append(',');
            writer.append("Gyroscope Z");
            writer.append(',');
            writer.append("Magnitude X");
            writer.append(',');
            writer.append("Magnitude Y");
            writer.append(',');
            writer.append("Magnitude Z");
            writer.append(',');
            writer.append("Latitude");
            writer.append(',');
            writer.append("Longitude");
            writer.append(',');
            writer.append("Accuracy");
            writer.append(',');
            writer.append("GPSSource");
            writer.append(',');
            writer.append("Year");
            writer.append(',');
            writer.append("Month");
            writer.append(',');
            writer.append("Day");
            writer.append(',');
            writer.append("Android Latitude");
            writer.append(',');
            writer.append("Android Longitude");
            writer.append(',');
            writer.append("Android Accuracy");
            writer.append(',');
            writer.append("Marker");
            writer.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setValue(final boolean isAndroid, final SensorValue value, final double lon, final double lat, final float accuracy, final DateTime v) {
        // this.value = v;
        handler.post(new Runnable() {
            @Override
            public void run() {
                saveValueTimeStamp(null, isAndroid, value, lat, lon, accuracy, v);
            }
        });
    }

    DateTime lastSavedTempCSVTime = DateTime.now();

    public void setValue(int timeTag, final boolean isAndroid, final Point3D acc, final Point3D gyr, final Point3D magn, final double lat, final double lon, final float accuracy, final DateTime v, final double aLat, final double aLon, final float androidAccuracy) {
        // [20181207    VMio] Only write to CSV when new data arrived
//        synchronized (Global.synObject) {
//            if (!Global.isNewSensorData)
//                return;
//        }
        // [20190224    VMio] save CSV temkp file at different second time only (1Hz)
        if (timeTag == 1)
            synchronized (SyncTag) {
                SyncTag = timeTag;
            }
        synchronized (SyncTag) {
            SyncTag = timeTag;
        }
        DateTime now = new DateTime();
        if (lastSavedTempCSVTime.getSecondOfMinute() != now.getSecondOfMinute()) {
            lastSavedTempCSVTime = v;
            synchronized (SyncTag) {
                SyncTag = timeTag;
            }
            if (SyncTag != 0)
                saveValueTimeStamp(writerTemp, isAndroid, acc, gyr, magn, lat, lon, accuracy, v, aLat, aLon, androidAccuracy);
        }
        if (SyncTag != 0) {
            TimeTagTS++;
            saveValueTimeStamp(writerMA, isAndroid, acc, gyr, magn, lat, lon, accuracy, v, aLat, aLon, androidAccuracy);
        }
    }

    public synchronized void start() {
        if (!mThread.isAlive())
            mThread.start();
    }

    public synchronized void saveValueTimeStamp(FileWriter writer, final boolean isAndroid, SensorValue value, double lon, double lat, final float accuracy, DateTime now) {
        if (csvFileTS == null)
            return;
        // [20181207    VMio] Only write to CSV when new data arrived
//        synchronized (Global.synObject) {
//            if (!Global.isNewSensorData)
//                return;
//        }
        // Append new csv row to current csv File
        try {
            writer.append("" + TimeTagTS);
            writer.append(',');
            writer.append("" + now.getHourOfDay());
            writer.append(',');
            writer.append("" + now.getMinuteOfHour());
            writer.append(',');
            writer.append("" + now.getSecondOfMinute());
            writer.append(',');
            writer.append("" + now.getMillisOfSecond());
            writer.append(',');
            writer.append("" + value.getAccelMPS2().x);
            writer.append(',');
            writer.append("" + value.getAccelMPS2().y);
            writer.append(',');
            writer.append("" + value.getAccelMPS2().z);
            writer.append(',');
            writer.append("" + value.getGyros().x * Math.PI / 180);
            writer.append(',');
            writer.append("" + value.getGyros().y * Math.PI / 180);
            writer.append(',');
            writer.append("" + value.getGyros().z * Math.PI / 180);
            writer.append(',');
            writer.append("" + value.getMagnet().x);
            writer.append(',');
            writer.append("" + value.getMagnet().y);
            writer.append(',');
            writer.append("" + value.getMagnet().z);
            writer.append(',');
            writer.append("" + lat);
            writer.append(',');
            writer.append("" + lon);
            writer.append(',');
            writer.append("" + accuracy);
            writer.append(',');
            writer.append("" + (isAndroid ? "0" : "1"));
            writer.append(',');
            writer.append("" + now.getYear());
            writer.append(',');
            writer.append("" + now.getMonthOfYear());
            writer.append(',');
            writer.append("" + now.getDayOfMonth());
            writer.append('\n');
            writer.flush();
//            if (alert == null || !alert.isShowing()){
//                alert = FileUtil.checkDialogFreeStorage(mThis);
//            }
            // [20181207    VMio]
            synchronized (Global.synObject) {
                Global.isNewSensorData = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public synchronized void saveValueTimeStamp(FileWriter writer, boolean isAndroid, Point3D acc, Point3D gyr, Point3D magn, double lat, double lon, final float accuracy, final DateTime now, double aLat, double aLon, float androidAccuracy) {
//        if (csvFileTS == null)
//            return;
        // Append new csv row to current csv File
        synchronized (writer) {
            try {
                writer.append("" + TimeTagTS);
                writer.append(',');
                writer.append("" + SyncTag);
                writer.append(',');
                writer.append("" + now.getHourOfDay());
                writer.append(',');
                writer.append("" + now.getMinuteOfHour());
                writer.append(',');
                writer.append("" + now.getSecondOfMinute());
                writer.append(',');
                writer.append("" + now.getMillisOfSecond());
                writer.append(',');
                writer.append("" + acc.x);
                writer.append(',');
                writer.append("" + acc.y);
                writer.append(',');
                writer.append("" + acc.z);
                writer.append(',');
                writer.append("" + gyr.x);
                writer.append(',');
                writer.append("" + gyr.y);
                writer.append(',');
                writer.append("" + gyr.z);
                writer.append(',');
                writer.append("" + magn.x);
                writer.append(',');
                writer.append("" + magn.y);
                writer.append(',');
                writer.append("" + magn.z);
                writer.append(',');
                writer.append("" + lat);
                writer.append(',');
                writer.append("" + lon);
                writer.append(',');
                writer.append("" + accuracy);
                writer.append(',');
                writer.append("" + (isAndroid ? "1" : "0"));
                writer.append(',');
                writer.append("" + now.getYear());
                writer.append(',');
                writer.append("" + now.getMonthOfYear());
                writer.append(',');
                writer.append("" + now.getDayOfMonth());
                writer.append(',');
                writer.append("" + aLat);
                writer.append(',');
                writer.append("" + aLon);
                writer.append(',');
                writer.append("" + androidAccuracy);
                if (marker != null) {
                    synchronized (marker) {
                        writer.append(',');
                        writer.append("" + marker);
                        marker = null;
                    }
                }


                writer.append('\n');
                writer.flush();
//            if (alert == null || !alert.isShowing()){
//                alert = FileUtil.checkDialogFreeStorage(mThis);
//            }
                // [20181207    VMio]
                synchronized (Global.synObject) {
                    Global.isNewSensorData = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void setMarker(String marker) {
        this.marker = marker;
    }

    public void stop() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                stop(writerMA, csvFileTS);
                //stop(writerTemp, tempCSV);
            }
        });
    }

    public void saveTempFile() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                innerSaveTempFile();
            }
        });

    }

    public void innerSaveTempFile() {
        tempCount++;
        stop(writerTemp, tempCSV);
        ZipManager zipManager = new ZipManager(new ZipManager.ZipFileListener() {
            @Override
            public void onZipComplete() {
                if (listener != null)
                    listener.onSaveFileMAComplete();
            }
        });

        String zipCSV = tempCSV.getAbsolutePath().replace("csv", "zip");
        ArrayList<String> files = new ArrayList<>();
        files.add(tempCSV.getAbsolutePath());
        zipManager.zip(files, zipCSV);
        tempCSV.delete();
    }

    public void stop(FileWriter writer, File csvFile) {
        synchronized (writer) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            MediaScannerConnection.scanFile(mThis, new String[]{csvFile.toString()}, null, null);
        }
    }

    public void release() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                handler.getLooper().quit();
            }
        });

        handlerTempCSV.post(new Runnable() {
            @Override
            public void run() {
                handler.getLooper().quit();
            }
        });
        synchronized (writerMA) {
            try {
                writerMA.flush();
                writerMA.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        synchronized (writerTemp) {
            try {
                writerTemp.flush();
                writerTemp.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public interface MASaveFileListener {
        void onSaveFileMAComplete();
    }
}
