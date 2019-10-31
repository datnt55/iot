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
import java.util.ArrayList;
import java.util.List;

import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.DecimalStandardFormat;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.MahonyAHRS;
import vmio.com.blemultipleconnect.Utilities.MathUtils;
import vmio.com.mioblelib.model.SensorValue;
import vmio.com.mioblelib.widget.Point3D;
import vmio.com.blemultipleconnect.Utilities.MadgwickAHRS;


/**
 * Created by DatNT on 12/14/2017.
 */

public class CSVNAThread {
    private Context  mThis;
    private File uploadFolder;
    private Thread mThread;
    private boolean stopFlag;
    private File csvFileNA;
    private int TimeTagNA;
    private SensorValue value;
    private SensorValue oldValue;
    private Handler handler;
    private static final String TAG = "CSVNAThread";
    private ArrayList<SensorValue> dataSensorInOneSecond;
    private MahonyAHRS ahrs= new MahonyAHRS(10,20,0);
    public CSVNAThread(Context context, File uploadFolder, String timeStamp) {
        this.mThis = context;
        this. stopFlag = false;
        //mThread = new Thread(this);
        this.uploadFolder = uploadFolder;
        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        dataSensorInOneSecond = new ArrayList<>();
        Vx=0;Vy=0;Vz=0;
        createCSVFileNA(timeStamp);
    }


    public void createCSVFileNA(String timeStamp){
        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        csvFileNA = new File(uploadFolder, "NA_" + timeStamp + ".csv");
       // MediaScannerConnection.scanFile(mThis, new String[] {csvFileNA.toString()}, null, null);
        Intent mediaScannerIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri fileContentUri = Uri.fromFile(csvFileNA); // With 'permFile' being the File object
        mediaScannerIntent.setData(fileContentUri);
        mThis.sendBroadcast(mediaScannerIntent);
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

//            writer.append(',');
//            writer.append("Roll_old [rad]");
//            writer.append(',');
//            writer.append("Pitch_old [rad]");
//            writer.append(',');
//            writer.append("Heading_old [rad]");

            writer.append('\n');
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setValue(final SensorValue v){
        //this.value = v;
        if (handler.getLooper().getThread().isAlive()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    addValueToDataSensor(v);
                    saveValueNA(v);
                }
            });
        }
    }



    /**
     * get 3 sensor acceleration value in 1s and add to array
     *
     * @param v
     */
    private void addValueToDataSensor(SensorValue v){
        int size = dataSensorInOneSecond.size();
        if (size == 0){
            dataSensorInOneSecond.add(v);
            return;
        }
         long diff = v.getTime().getMillis() - dataSensorInOneSecond.get(0).getTime().getMillis();
       if (diff <= 1000){
           for (int i = 0 ; i < size-2 ; i++)
               dataSensorInOneSecond.get(i).assign(dataSensorInOneSecond.get(i+1)) ;
           dataSensorInOneSecond.get(size-1).assign(v);
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
//                        saveValueNA(value);
//                    } else if (!value.compair2SensorValue(oldValue)) {
//                        saveValueNA(value);
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
//        wl.release();
//    }

    // Mercator coordinate: x phai, y len tren, z di ra
    // Sensor coordinate: x xuong duoi, y phai, z di ra
    double [][] EarthDir = {{1},{1},{1}};

    public void saveValueNA(SensorValue value) {
        if (csvFileNA == null)
            return;
        TimeTagNA ++;

        Point3D DU = normalize(value.getAccelMPS2());
        Point3D NS = normalize(value.getMagnetNanoTesla());
        Point3D EW = crossProduct(DU, NS);

//
        double [][] T_s0_E={{0,1,0},{0,0,-1},{-1,0,0}};//transformation matrix from Mercator coordinate to Sensor coordinate at time t=0
//
        Point3D velocity = CalcSpeedFields(dataSensorInOneSecond);
        double[][] T_st_s0 = createRotateMatrix(EW, NS, DU); //transformation matrix from sensor coordinate at time t=0 to sensor coordinate at time t=t
        double[][] V_St = {{velocity.x}, {velocity.y}, {velocity.z}}; //V_st is velocity of sensor in sensor coordinate at time t
        double[][] V_S0 = multipleMatrix(T_st_s0,V_St); //V_s0 is velocity of sensor in sensor coordinate at time t=0;
        double[][] V_E = multipleMatrix(T_s0_E,V_S0); //V_E is velocity of sensor in Mercator coordinate

        //[2019/06/10 huenguyen] estimate the euler angles
        ahrs.Update((float)value.getGyros().x* Define.DEG2RAD_COEFF,(float)value.getGyros().y*Define.DEG2RAD_COEFF,(float)value.getGyros().z*Define.DEG2RAD_COEFF,
                        (float)value.getAccel().x,(float)value.getAccel().y,(float)value.getAccel().z,
                        (float)value.getMagnet().x,(float)value.getMagnet().y,(float)value.getMagnet().z);

        // get euler angle then convert to the Mercator coordinate
        double[] eulers = ahrs.GetEulersAngle();
        double[][] eulers_mecator = MathUtils.multipleMatrix(new double[][]{{eulers[0],eulers[1],eulers[2]}}, Define.T_WEWe);
        eulers = new double[]{eulers_mecator[0][0],eulers_mecator[0][1],eulers_mecator[0][2]};


        //[2019/06/13 huenguyen] calculate rotation matrix
        double[][] rotMat = ahrs.GetRotationMatrix();
        rotMat = MathUtils.multipleMatrix(rotMat, Define.T_WEWe);

        //[20190702 huenguyen] calculate speedNS,EW,DU
        double[] eulers_old = ConvRot2eulers(createRotateMatrix(EW, NS, DU));

        // for debug
        double[] gaze = ahrs.rotationMatrixToEulerAngles(rotMat);
        double azimuth =Math.atan2(value.getMagnet().y,value.getMagnet().z);
        //if(Define.DEBUG) {
//            double[][] rotMat = ahrs.EulerAnglesToRotationMatrix(eulers);
//            double[][] frame = MathUtils.multipleMatrix(rotMat, EarthDir);
//            gaze = new double[]{frame[0][0], frame[1][0],  [2][0]};
        //}


        DecimalStandardFormat dTime = new DecimalStandardFormat(".##");
        try {
            FileWriter writer = new FileWriter(csvFileNA, true);
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
            writer.append(""+ ((eulers[0])+2*Math.PI)%(2*Math.PI));
            writer.append(',');
            writer.append(""+ ((eulers[1])+2*Math.PI)%(2*Math.PI));
            writer.append(',');

//            writer.append(""+ (eulers[2]*180/Math.PI));
            //[20190621 HueNguyen] take heading from azimuth angle
            writer.append("" + ((azimuth) +Math.PI   +2*Math.PI)%(2*Math.PI));

            writer.append(',');
            writer.append("" + Global.latitude);
            writer.append(',');
            writer.append("" + Global.longitude);
            writer.append(',');
            writer.append("" + calculateAltitude2(value)); // Todo : calculate altitude

            writer.append(',');
            writer.append("" + Double.valueOf(dTime.format(V_E[0][0])));
            writer.append(',');
            writer.append("" + Double.valueOf(dTime.format(V_E[1][0])));
            writer.append(',');
            writer.append("" + Double.valueOf(dTime.format(V_E[2][0])));

//            writer.append(',');
//            writer.append("" + ((eulers_old[0])+2*Math.PI)%(2*Math.PI));
//            writer.append(',');
//            writer.append(""+ ((eulers_old[1])+2*Math.PI)%(2*Math.PI));
//            writer.append(',');
//            writer.append(""+ ((eulers_old[2])+2*Math.PI)%(2*Math.PI));

//            writer.append(',');
//            writer.append("0"); //Speed NS calculate and put back in Uploader
//            writer.append(',');
//            writer.append("0"); //Speed EW calculate and put back in Uploader
//            writer.append(',');
//            writer.append("0"); //Speed DU calculate and put back in Uploader

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

    private double calculateAltitude(SensorValue value){
        double pressure = Math.pow((1005/value.getPressure()),(1/5.257));
        double Kevin = value.getAmbientTemp() + 273.15;
        double constain = Math.pow((1013.25/1005), (1/5.256));
        double altitude=(((pressure-1)*Kevin )/0.0065)*constain;
        DecimalStandardFormat dTime = new DecimalStandardFormat(".####");

        return Double.isInfinite(altitude) ? 0 : Double.valueOf(dTime.format(altitude));
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

    private Point3D crossProduct( Point3D p1, Point3D p2){
        double uvi = p1.y * p2.z - p2.y * p1.z;
        double uvj = p2.x * p1.z - p1.x * p2.z;
        double uvk = p1.x * p2.y - p2.x * p1.y;
        return new Point3D(uvi, uvj, uvk);
    }

    private Point3D normalize(Point3D p){
        double m1 = Math.max(Math.abs(p.x), Math.abs(p.y));
        double max = Math.max(m1, Math.abs(p.z));
        if (max == 0)
            return new Point3D(0, 0, 0);
        double newX = p.x/ max;
        double newY = p.y/ max;
        double newZ = p.z/ max;

        return  new Point3D(newX, newY, newZ);
    }

    private double[][] createRotateMatrix (Point3D i,Point3D j, Point3D k ){
        double [][] m = new double[3][3];
        m[0][0] = i.x;
        m[1][0] = i.y;
        m[2][0] = i.z;

        m[0][1] = j.x;
        m[1][1] = j.y;
        m[2][1] = j.z;

        m[0][2] = k.x;
        m[1][2] = k.y;
        m[2][2] = k.z;

        return m;
    }

    /// <summary>
    /// Converts a given Rotation/Pose matrix to Euler angles
    /// </summary>
    /// <param name="m">3x3 rotation matrix or 4x4 pose matrix</param>
    /// <returns>Euler angles in radians: Roll, Pitch, Yaw</returns>
    private double[] ConvRot2eulers(double[][] m)
    {
        double m00 = m[0][0];
        double m02 = m[0][2];
        double m10 = m[1][0];
        double m11 = m[1][1];
        double m12 = m[1][2];
        double m20 = m[2][0];
        double m22 = m[2][2];

        double fiX, fiY, fiZ;
        if (m10 > 0.998)
        { // singularity at north pole
            fiX = 0;
            fiY = Math.PI / 2;
            fiZ = Math.atan2(m02, m22);
        }
        else if (m10 < -0.998)
        { // singularity at south pole
            fiX = 0;
            fiY = -Math.PI / 2;
            fiZ = Math.atan2(m02, m22);
        }
        else
        {
            fiX = Math.atan2(-m12, m11);
            fiY = Math.asin(m10);
            fiZ = Math.atan2(-m20, m00);
        }

        DecimalStandardFormat dTime = new DecimalStandardFormat(".####");

        return new double[] { Double.valueOf(dTime.format(fiX)), Double.valueOf(dTime.format(fiY)), Double.valueOf(dTime.format(fiZ)) };
    }

//    private Point3D CalcSpeedFields(ArrayList<SensorValue> data)
//    {
//        double x = 0, y =0, z = 0;
//        for (int i = 0 ; i < data.size(); i++){
//            if (data.get(i) != null) {
//                Point3D acceleration = data.get(i).getAccel();
//                x += acceleration.x;
//                y += acceleration.y;
//                z += acceleration.z;
//            }
//        }
//        DecimalStandardFormat dTime = new DecimalStandardFormat(".####");
//
//        return new Point3D(x,y,z);
//    }

    //[20190604 huenguyen] calculate velocity by calculate integration of acceleration from t=0(v0=0)
    Point3D gravity = new Point3D(0, 0, 0);

    double Vx = 0, Vy =0, Vz = 0;
    private Point3D CalcSpeedFields(ArrayList<SensorValue> data)
    {
        if (data.get(data.size()-1) != null) {
            Point3D acceleration = data.get(data.size()-1).getAccel();
            // Ref. https://developer.android.com/reference/android/hardware/SensorEvent#values
            float alpha = 0.8f;

            gravity.x = alpha * gravity.x + (1 - alpha) * acceleration.x;
            gravity.y = alpha * gravity.y + (1 - alpha) * acceleration.y;
            gravity.z = alpha * gravity.z + (1 - alpha) * acceleration.z;

            // Remove the gravity contribution with the high-pass
            acceleration.x = acceleration.x - gravity.x;
            acceleration.y = acceleration.y - gravity.y;
            acceleration.z = acceleration.z - gravity.z;

            Vx += acceleration.x;
            Vy += acceleration.y;
            Vz += acceleration.z;
        }
        return new Point3D(Vx,Vy,Vz);
    }


    private double[][] multipleMatrix(double[][] firstMatrix ,double[][] secondMatrix  ) {
        int r1 = firstMatrix.length, c1 = firstMatrix[0].length;
        int r2 = secondMatrix.length, c2 = secondMatrix[0].length;
        if (c1 != r2)
            return null;
        double[][] product = new double[r1][c2];
        for (int i = 0; i < r1; i++) {
            for (int j = 0; j < c2; j++) {
                for (int k = 0; k < c1; k++) {
                    product[i][j] += firstMatrix[i][k] * secondMatrix[k][j];
                }
            }
        }
        return product;
    }

    public void stop(){
        MediaScannerConnection.scanFile(mThis, new String[] {csvFileNA.toString()}, null, null);
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
