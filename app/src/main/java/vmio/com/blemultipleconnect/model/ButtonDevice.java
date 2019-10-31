package vmio.com.blemultipleconnect.model;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import vmio.com.mioblelib.ble.BleDevice;


/**
 * Created by DatNT on 9/12/2017.
 */

public class ButtonDevice {
    private Button btnConnect;
    private BleDeviceInfo device;
    private BleDevice mBluetooth;
    private ImageView imgSignal;
    private TextView txtBattery;

    public ButtonDevice(Button btnConnect, BleDeviceInfo device, BleDevice mBluetooth, ImageView imgSignal, TextView txtBattery) {
        this.btnConnect = btnConnect;
        this.device = device;
        this.mBluetooth = mBluetooth;
        this.imgSignal = imgSignal;
        this.txtBattery = txtBattery;
    }

    public ButtonDevice(BleDeviceInfo device, BleDevice mBluetooth) {
        this.device = device;
        this.mBluetooth = mBluetooth;
    }


    public Button getBtnConnect() {
        return btnConnect;
    }

    public void setBtnConnect(Button btnConnect) {
        this.btnConnect = btnConnect;
    }

    public BleDeviceInfo getDevice() {
        return device;
    }

    public void setDevice(BleDeviceInfo device) {
        this.device = device;
    }

    public BleDevice getmBluetooth() {
        return mBluetooth;
    }

    public void setmBluetooth(BleDevice mBluetooth) {
        this.mBluetooth = mBluetooth;
    }

    public ImageView getImgSignal() {
        return imgSignal;
    }

    public void setImgSignal(ImageView imgSignal) {
        this.imgSignal = imgSignal;
    }

    public TextView getTxtBattery() {
        return txtBattery;
    }

    public void setTxtBattery(TextView txtBattery) {
        this.txtBattery = txtBattery;
    }
}
