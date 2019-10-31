package vmio.com.blemultipleconnect.model;

import vmio.com.mioblelib.widget.Point3D;

/**
 * Created by DatNT on 10/4/2017.
 */

public class SensorStore {
    private String name;
    private String address;
    private String description;
    private int status;
    private int position;
    private Point3D accel;
    private Point3D magnet;
    private Point3D gyro;

    public SensorStore(String name, String address, String description, int status, int position, Point3D accel, Point3D magnet, Point3D gyro) {
        this.name = name;
        this.address = address;
        this.description = description;
        this.status = status;
        this.position = position;
        this.accel = accel;
        this.magnet = magnet;
        this.gyro = gyro;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Point3D getAccel() {
        return accel;
    }

    public void setAccel(Point3D accel) {
        this.accel = accel;
    }

    public Point3D getMagnet() {
        return magnet;
    }

    public void setMagnet(Point3D magnet) {
        this.magnet = magnet;
    }

    public Point3D getGyro() {
        return gyro;
    }

    public void setGyro(Point3D gyro) {
        this.gyro = gyro;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
