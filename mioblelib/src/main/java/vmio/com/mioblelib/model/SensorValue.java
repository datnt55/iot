package vmio.com.mioblelib.model;


import android.hardware.SensorManager;

import org.joda.time.DateTime;

import vmio.com.mioblelib.utilities.Define;
import vmio.com.mioblelib.widget.Point3D;

/**
 * Created by DatNT on 9/13/2017.
 * // [20181127 VMio] add explanation of this class for improve accuracy 
 * This class use for accumulate accel, gyros and magnet then buffer them for getting stable value
 * set functions insert into internal buffer.
 * get functions get from average/stable internal values
 */

public class SensorValue {
    private String name;
    private Point3D accel = new Point3D(0, 0, 0);
    private Point3D magnet = new Point3D(0, 0, 0);
    private Point3D gyros = new Point3D(0, 0, 0);
    private double ambientTemp = 0;
    private double objectTemp = 0;
    private double pressure = 0;
        private double humidity = 0;
    private DateTime time;

    // [20181123    VMio] Add Gravity calculated from accelerator throw low pass filter
    Point3D gravity = new Point3D(0, 0, 0);
    // Last 3 values of Magnet for average
    Point3D[] lastMagnet = new Point3D[] { new Point3D(0,0,0), new Point3D(0,0,0), new Point3D(0,0,0)};
    // [20181127 VMio] buffer last three values 
	Point3D[] lastAccel = new Point3D[] { new Point3D(0,0,0), new Point3D(0,0,0), new Point3D(0,0,0)};
    Point3D[] lastGyros = new Point3D[] { new Point3D(0,0,0), new Point3D(0,0,0), new Point3D(0,0,0)};

    public SensorValue(String name) {
        this.name = name;
        this.ambientTemp = 0;
        this.pressure = 0;
    }

    public SensorValue() {
    }

    public SensorValue(String name, Point3D accel, Point3D magnet, Point3D gyros, double ambientTemp, double objectTemp) {
        this.name = name;
        this.accel = accel;
        this.magnet = magnet;
        this.gyros = gyros;
        this.ambientTemp = ambientTemp;
        this.objectTemp = objectTemp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Point3D getAccel() {
        return accel;
    }

    //[20181126 VMio] set accel to internal buffer and return current internal accel value. It contains graviry or not depends on Define.SENSOR_ACCEL_FILTER_TYPE
    public Point3D setAccel(Point3D newAccel) {
        //[20181126 VMio] Buffer shifting have to be cloned for avoid of same reference
        if (accel.x == 0 && accel.y == 0 && accel.z == 0) {
            this.accel = newAccel.clone();
        } else {
            // Store to buffer
            lastAccel[0] = lastAccel[1].clone(); lastAccel[1] = lastAccel[2].clone(); lastAccel[2] = newAccel.clone();
            this.accel = compensateGravity(newAccel);
        }
        return this.accel;
    }

    public Point3D getMagnet() {
        return magnet;
    }

    //[20181126 VMio] set magnet to internal buffer and return current internal magnet value
    public Point3D setMagnet(Point3D newMagnet) {
        // Store to buffer
        //[20181126 VMio] Buffer shifting have to be cloned for avoid of same reference
        lastMagnet[0] = lastMagnet[1].clone(); lastMagnet[1] = lastMagnet[2].clone(); lastMagnet[2] = newMagnet.clone();
        // this.magnet = newMagnet;
        final float alpha = 0.8f;
        this.magnet.x = Define.SENSOR_MAGNET_FILTER_TYPE == 0 ? (float) (alpha * this.magnet.x + (1 - alpha) * newMagnet.x) : (lastMagnet[0].x + lastMagnet[1].x + lastMagnet[2].x) / 3.0;
        this.magnet.y = Define.SENSOR_MAGNET_FILTER_TYPE == 0 ? (float) (alpha * this.magnet.y + (1 - alpha) * newMagnet.y) : (lastMagnet[0].y + lastMagnet[1].y + lastMagnet[2].y) / 3.0;
        this.magnet.z = Define.SENSOR_MAGNET_FILTER_TYPE == 0 ? (float) (alpha * this.magnet.z + (1 - alpha) * newMagnet.z) : (lastMagnet[0].z + lastMagnet[1].z + lastMagnet[2].z) / 3.0;
        return this.magnet;
    }

    public Point3D getGyros() {
        return gyros;
    }

    //[20181126 VMio] set gyros to internal buffer and return current internal gyros value.
    public Point3D setGyros(Point3D newGyros) {
        //[20181126 VMio] Buffer shifting have to be cloned for avoid of same reference
        lastGyros[0] = lastGyros[1].clone(); lastGyros[1] = lastGyros[2].clone(); lastGyros[2] = newGyros.clone();
        this.gyros.x = (lastGyros[0].x + lastGyros[1].x + lastGyros[2].x) / 3.0;
        this.gyros.y = (lastGyros[0].y + lastGyros[1].y + lastGyros[2].y) / 3.0;
        this.gyros.z = (lastGyros[0].z + lastGyros[1].z + lastGyros[2].z) / 3.0;
        return this.gyros;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getAmbientTemp() {
        return ambientTemp;
    }

    public void setAmbientTemp(double ambientTemp) {
        this.ambientTemp = ambientTemp;
    }

    public double getObjectTemp() {
        return objectTemp;
    }

    public void setObjectTemp(double objectTemp) {
        this.objectTemp = objectTemp;
    }

    public double getPressure() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure = pressure;
    }

    public boolean haveValue() {
        if (accel.x == 0 && accel.y == 0 && accel.z == 0)
            return false;
        if (gyros.x == 0 && gyros.y == 0 && gyros.z == 0)
            return false;
        if (magnet.x == 0 && magnet.y == 0 && magnet.z == 0)
            return false;
        return true;
    }

    public boolean haveValueAccel() {
        if (accel.x == 0 && accel.y == 0 && accel.z == 0)
            return false;
        return true;
    }


    public Point3D compensateGravity(Point3D newAccel) {
        float alpha = 0.8f;
        // Isolate the force of gravity with the low-pass filter.
        //double gx = alpha * accel.x + (1 - alpha) * newAccel.x;
        //double gy = alpha * accel.x + (1 - alpha) * newAccel.y;
        //double gz = alpha * accel.x + (1 - alpha) * newAccel.z;

        // Remove the gravity contribution with the high-pass
        //accel.x = newAccel.x - gx;
        //accel.y = newAccel.y - gy;
        //accel.z = newAccel.z - gz;

        // [20181123    VMio] Fix gravity subtract functions. Have to be accumulated on gravity instead of accel
        // Ref. https://developer.android.com/reference/android/hardware/SensorEvent#values
        gravity.x = alpha * gravity.x + (1 - alpha) * newAccel.x;
        gravity.y = alpha * gravity.y + (1 - alpha) * newAccel.y;
        gravity.z = alpha * gravity.z + (1 - alpha) * newAccel.z;

        // return Gravity only
        if(Define.SENSOR_ACCEL_FILTER_TYPE == 0)
            return new Point3D(gravity.x, gravity.y, gravity.z);

        // return Linear accelerator only
        if(Define.SENSOR_ACCEL_FILTER_TYPE == 1) {
            // Remove the gravity contribution with the high-pass
            accel.x = newAccel.x - gravity.x;
            accel.y = newAccel.y - gravity.y;
            accel.z = newAccel.z - gravity.z;
            return new Point3D(accel.x, accel.y, accel.z);
        }

        // return Both after average
        return new Point3D( (lastAccel[0].x + lastAccel[1].x + lastAccel[2].x) / 3.0,
                            (lastAccel[0].y + lastAccel[1].y + lastAccel[2].y) / 3.0,
                            (lastAccel[0].z + lastAccel[1].z + lastAccel[2].z) / 3.0);
    }

    public double convertGToMPS2(double gravity) {
        return SensorManager.STANDARD_GRAVITY * gravity;
    }

    public Point3D
    getAccelMPS2() {
        Point3D acceleration = getAccel();
        double x = convertGToMPS2(acceleration.x);
        double y = convertGToMPS2(acceleration.y);
        double z = convertGToMPS2(acceleration.z);
        return new Point3D(x, y, z);
    }

    public double convertMicroToNanoMagnet(double magnetude) {
        return magnetude * 1000;
    }

    public Point3D getMagnetNanoTesla() {
        Point3D magnetude = getMagnet();
        double x = convertMicroToNanoMagnet(magnetude.x);
        double y = convertMicroToNanoMagnet(magnetude.y);
        double z = convertMicroToNanoMagnet(magnetude.z);
        return new Point3D(x, y, z);
    }

    public DateTime getTime() {
        return time;
    }

    public void setTime(DateTime time) {
        this.time = time;
    }

    public boolean compair2SensorValue(SensorValue value) {
        if (getAccel().x == value.getAccel().x && getAccel().y == value.getAccel().y && getAccel().z == value.getAccel().z)
            if(getMagnet().x == value.getMagnet().x && getMagnet().y == value.getMagnet().y && getMagnet().z == value.getMagnet().z)
                return true;
        return false;
    }

    public void assign(SensorValue value){
        this.name = value.getName();
        this.accel = new Point3D(value.getAccel().x, value.getAccel().y, value.getAccel().z);
        this.magnet = new Point3D(value.getMagnet().x, value.getMagnet().y, value.getMagnet().z);
        this.gyros = new Point3D(value.getGyros().x, value.getGyros().y, value.getGyros().z);
        this.ambientTemp = value.getAmbientTemp();
        this.objectTemp = value.getObjectTemp();
        this.pressure = value.getPressure();
        this.time = value.getTime();
    }


}
