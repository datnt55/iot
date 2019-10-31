package vmio.com.blemultipleconnect.Utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.media.MediaScannerConnection;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

public class CSVHelper {
    private File csvFile, csvFileNA, csvFileSA, csvFileGA;
    private File uploadFolder;
    private int csvRowCount;
    private int MAX_CSV_ROW_COUNT = 1000; // max row for send csv to server
    private int MAX_CSV_ELAPSE_TIME_IN_SECOND = 600; // max seconds for send csv to server
    private DateTime mLastSaveDataTime;
    private AlertDialog alert;
    private Context mThis;
    private CSVFileCreateListener listener;
    private int TimeTagSA, TimeTagGA, TimeTagNA;

    public CSVHelper(Context context, CSVFileCreateListener listener, File uploadFolder) {
        this.uploadFolder = uploadFolder;
        this.mThis = context;
        this.listener = listener;
    }

    public File getCsvFile(){
        return this.csvFile;
    }

    public int getCsvRowCount(){
        return csvRowCount;
    }

    public void createCSVFile(){
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        csvFile = new File(uploadFolder, "DataLogger_" + fmt.print(new DateTime()) + ".csv");
        mLastSaveDataTime = new DateTime();
        if (listener != null)
            listener.onCSVCreate(csvFile);
        csvRowCount = 0; // reset row count
        try {
            csvFile.createNewFile();
            FileWriter writer = new FileWriter(csvFile,true);
            writer.append("sensor_uuid");
            writer.append(',');
            writer.append("sensor_name");
            writer.append(',');
            writer.append("sensor_ambient_temp");
            writer.append(',');
            writer.append("sensor_object_temp");
            writer.append(',');
            writer.append("sensor_accel_x");
            writer.append(',');
            writer.append("sensor_accel_y");
            writer.append(',');
            writer.append("sensor_accel_z");
            writer.append(',');
            writer.append("sensor_gyro_x");
            writer.append(',');
            writer.append("sensor_gyro_y");
            writer.append(',');
            writer.append("sensor_gyro_z");
            writer.append(',');
            writer.append("sensor_mag_x");
            writer.append(',');
            writer.append("sensor_mag_y");
            writer.append(',');
            writer.append("sensor_mag_z");
            writer.append(',');
            writer.append("sensor_timestamp");
            writer.append(',');
            writer.append("sensor_gps_long");
            writer.append(',');
            writer.append("sensor_gps_lat");
            writer.append(',');
            writer.append("sensor_firmware_ver");
            writer.append(',');
            writer.append("sensor_firmware_date");
            writer.append(',');
            writer.append("device_os_ver");
            writer.append(',');
            writer.append("device_uuid");
            writer.append(',');
            writer.append("sensor_status_key1");
            writer.append(',');
            writer.append("sensor_status_key2");
            writer.append(',');
            writer.append("device_type");
            writer.append(',');
            writer.append("app_ver");
            writer.append(',');
            writer.append("sensor_timestamp_miliseconds");
            writer.append('\n');
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveValue(String type, SensorValue value) {
        if (csvFile == null)
            return;
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        DateTime now = new DateTime(DateTimeZone.UTC);
        String current  = fmt.print(now);
        int mili = now.getMillisOfSecond();
        String version = Global.ApplicationVersion;
        String deviceUuid = Global.DeviceId;

        int apiVersion = android.os.Build.VERSION.SDK_INT;
        // Append new csv row to current csv File
        try {
            FileWriter writer = new FileWriter(csvFile, true);
            if (type.equals("SensorTag")) {
                writer.append("" + value.getName());
                writer.append(',');
                writer.append(Global.uuid);
                writer.append(',');
                writer.append("" + value.getAmbientTemp());
                writer.append(',');
                writer.append("" + value.getObjectTemp());
                writer.append(',');
                writer.append("" + value.getAccelMPS2().x);
                writer.append(',');
                writer.append("" + value.getAccelMPS2().y);
                writer.append(',');
                writer.append("" + value.getAccelMPS2().z);
                writer.append(',');
                writer.append("" + value.getGyros().x);
                writer.append(',');
                writer.append("" + value.getGyros().y);
                writer.append(',');
                writer.append("" + value.getGyros().z);
                writer.append(',');
                writer.append("" + value.getMagnetNanoTesla().x);
                writer.append(',');
                writer.append("" + value.getMagnetNanoTesla().y);
                writer.append(',');
                writer.append("" + value.getMagnetNanoTesla().z);
                writer.append(',');
                writer.append(current);
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append(apiVersion + "");
                writer.append(',');
                writer.append(deviceUuid);
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("1");
                writer.append(',');
                writer.append(version);
                writer.append(',');
                writer.append(mili + "");
                writer.append('\n');
            }
            if (type.equals("GPS")){
                writer.append("" +deviceUuid);
                writer.append(',');
                writer.append("GNS 2000 plus");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append(current);
                writer.append(',');
                writer.append("" + Global.longitude);
                writer.append(',');
                writer.append("" + Global.latitude);
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append(apiVersion+"");
                writer.append(',');
                writer.append(deviceUuid);
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("");
                writer.append(',');
                writer.append("1");
                writer.append(',');
                writer.append(version);
                writer.append(',');
                writer.append(mili+"");
                writer.append('\n');
            }
            writer.flush();
            writer.close();

            csvRowCount ++;
            // Over time or reach 100 rows then Move to uploads folder and create new file
            if(csvRowCount >= MAX_CSV_ROW_COUNT || (mLastSaveDataTime != null && new DateTime().getMillis() - mLastSaveDataTime.getMillis() >= 1000 * MAX_CSV_ELAPSE_TIME_IN_SECOND)) {
               /* // Move to background upload folder
                if(CommonUtils.MoveFile(csvFile, uploadFolder.getAbsolutePath())) {
                    // Create new CSV file
                    createCSVFile();
                    //Log.e(TAG, "New CSV File created");
                    mLastSaveDataTime = new DateTime(); // update last write row data
                }*/
                createCSVFile();
                //Log.e(TAG, "New CSV File created");
            }
            if (alert == null || !alert.isShowing()){
                alert = FileUtil.checkDialogFreeStorage(mThis);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }



    public void createCSVFileSA(){
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        csvFileSA = new File(uploadFolder, "SA_" + fmt.print(new DateTime()) + ".csv");
        MediaScannerConnection.scanFile(mThis, new String[] {csvFileSA.toString()}, null, null);
        mLastSaveDataTime = new DateTime();
        csvRowCount = 0; // reset row count
        TimeTagSA = 0;
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
            writer.append("Latitude [rad]");
            writer.append(',');
            writer.append("Longitude [rad]");
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

    public synchronized void saveValueSA(String type, SensorValue value) {
        if (csvFileSA == null)
            return;
        TimeTagSA ++;
        // Append new csv row to current csv File
        try {
            FileWriter writer = new FileWriter(csvFileSA, true);
            if (type.equals("SensorTag")) {
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
                writer.append(""+value.getPressure());
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
                writer.append("0");
                writer.append(',');
                writer.append("0");
                writer.append(',');
                writer.append("0");
                writer.append(',');
                writer.append("0");
                writer.append('\n');
            }
            writer.flush();
            writer.close();

            if (alert == null || !alert.isShowing()){
                alert = FileUtil.checkDialogFreeStorage(mThis);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void createCSVFileGA(){
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        csvFileGA = new File(uploadFolder, "GA_" + fmt.print(new DateTime()) + ".csv");
        MediaScannerConnection.scanFile(mThis, new String[] {csvFileGA.toString()}, null, null);
        mLastSaveDataTime = new DateTime();
        csvRowCount = 0; // reset row count
        TimeTagGA = 0;
        try {
            csvFileGA.createNewFile();
            FileWriter writer = new FileWriter(csvFileGA,true);
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

    public int getCurrentTimezoneOffset() {

        TimeZone tz = TimeZone.getDefault();
        Calendar cal = GregorianCalendar.getInstance(tz);
        int offsetInMillis = tz.getOffset(cal.getTimeInMillis());

        return offsetInMillis;
    }

    public synchronized void saveValueGA(String type, SensorValue value) {
        if (csvFileGA == null)
            return;
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        DateTime now = new DateTime();
        TimeTagGA ++;
        // Append new csv row to current csv File
        try {
            FileWriter writer = new FileWriter(csvFileGA, true);
            if (type.equals("SensorTag")) {
                writer.append(""+TimeTagGA);
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
                writer.append(""+now.getYear());
                writer.append(',');
                writer.append(""+now.getMonthOfYear());
                writer.append(',');
                writer.append(""+now.getDayOfMonth());
                writer.append(',');
                writer.append(""+now.getHourOfDay());
                writer.append(',');
                writer.append(""+now.getMinuteOfHour());
                writer.append(',');
                writer.append(""+now.getSecondOfMinute());
                writer.append(',');
                writer.append(""+getCurrentTimezoneOffset());
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
            }
            writer.flush();
            writer.close();

            if (alert == null || !alert.isShowing()){
                alert = FileUtil.checkDialogFreeStorage(mThis);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void createCSVFileNA(){
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        csvFileNA = new File(uploadFolder, "NA_" + fmt.print(new DateTime()) + ".csv");
        MediaScannerConnection.scanFile(mThis, new String[] {csvFileNA.toString()}, null, null);
        mLastSaveDataTime = new DateTime();
        csvRowCount = 0; // reset row count
        TimeTagNA = 0;
        try {
            csvFileNA.createNewFile();
            FileWriter writer = new FileWriter(csvFileNA,true);
            writer.append("TimeTag");
            writer.append(',');
            writer.append("StatIMU");
            writer.append(',');
            writer.append("StatAHRS");
            writer.append(',');
            writer.append("StatINS");
            writer.append(',');
            writer.append("RateP [rad/s]");
            writer.append(',');
            writer.append("RateQ [rad/s]");
            writer.append(',');
            writer.append("RateR [rad/s]");
            writer.append(',');
            writer.append("AccelerateX [m/s2]");
            writer.append(',');
            writer.append("AccelerateY [m/s2]");
            writer.append(',');
            writer.append("AccelerateZ [m/s2]");
            writer.append(',');
            writer.append("Roll [rad]");
            writer.append(',');
            writer.append("Pitch [rad]");
            writer.append(',');
            writer.append("Heading [rad]");
            writer.append(',');
            writer.append("Latitude [rad]");
            writer.append(',');
            writer.append("Longitude [rad]");
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

    public synchronized void saveValueNA(String type, SensorValue value) {
        if (csvFileNA == null)
            return;
        DateTime now = new DateTime(DateTimeZone.UTC);
        TimeTagNA ++;
        try {
            FileWriter writer = new FileWriter(csvFileNA, true);
            if (type.equals("SensorTag")) {
                writer.append(""+TimeTagNA);
                writer.append(',');
                writer.append("1");
                writer.append(',');
                writer.append("1");
                writer.append(',');
                writer.append("1");
                writer.append(',');
                writer.append("0"); // Unknown value
                writer.append(',');
                writer.append("0"); // Unknown value
                writer.append(',');
                writer.append("0"); // Unknown value
                writer.append(',');
                writer.append(""+ value.getAccelMPS2().x);
                writer.append(',');
                writer.append(""+ value.getAccelMPS2().y);
                writer.append(',');
                writer.append(""+ value.getAccelMPS2().z);
                writer.append(',');
                writer.append("0"); // Todo: calculate roll
                writer.append(',');
                writer.append("0"); // Todo: calculate pitch
                writer.append(',');
                writer.append("0");// Todo: calculate heading
                writer.append(',');
                writer.append("" + Global.latitude);
                writer.append(',');
                writer.append("" + Global.longitude);
                writer.append(',');
                writer.append("0"); // Todo : calculate altitude
                writer.append(',');
                writer.append("0"); // Todo : calculate speed NS
                writer.append(',');
                writer.append("0"); // Todo : calculate speed EW
                writer.append(',');
                writer.append("0"); // Todo : calculate speed DU
                writer.append('\n');
            }
            writer.flush();
            writer.close();

            if (alert == null || !alert.isShowing()){
                alert = FileUtil.checkDialogFreeStorage(mThis);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public interface CSVFileCreateListener{
        void onCSVCreate(File csvFile);
    }
}
