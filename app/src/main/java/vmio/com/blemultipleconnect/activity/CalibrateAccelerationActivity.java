package vmio.com.blemultipleconnect.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kircherelectronics.fsensor.filter.averaging.MeanFilter;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.model.BluetoothProfile;
import vmio.com.blemultipleconnect.model.SensorStore;
import vmio.com.blemultipleconnect.model.TableView;
import vmio.com.blemultipleconnect.widget.GaugeAcceleration;
import vmio.com.mioblelib.ble.BleWrapperUiCallbacks;
import vmio.com.mioblelib.ble.Bluetooth;
import vmio.com.mioblelib.model.SensorValue;
import vmio.com.mioblelib.service.GenericBluetoothProfile;
import vmio.com.mioblelib.service.SensorTagAmbientTemperatureProfile;
import vmio.com.mioblelib.service.SensorTagMovementProfile;
import vmio.com.mioblelib.utilities.SensorTagGatt;
import vmio.com.mioblelib.view.SensorTagMovementTableRow;
import vmio.com.mioblelib.widget.Point3D;
import vmio.com.mioblelib.widget.Sensor;

import static vmio.com.blemultipleconnect.activity.SettingActivity.mBlueToothSenSor;

public class CalibrateAccelerationActivity extends AppCompatActivity implements BleWrapperUiCallbacks {

    String TAG = "CalibrateAccelerationActivity";
    public static CalibrateAccelerationActivity mThis;
    private LinearLayout layoutRoot;
    private Bluetooth mBLEManager;
    private List<BluetoothProfile> mProfiles = new ArrayList<>();
    private TableView mTable;
    public boolean isChangePeriod = false;
    private ProgressDialog dialog;
    private DateTime dataTime;
    private static int TIME_OUT_GET_DATA = 2000;
    private SharePreference preference;
    public SensorValue sensorValues;
    private ArrayList<SensorStore> sensorStored = new ArrayList<>();
    private GaugeAcceleration gaugeAcceleration;
    private MeanFilter meanFilterAccelerationSmoothing;
    private boolean isEditCalibrate = false;
    private int bodyPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);
        layoutRoot = (LinearLayout) findViewById(R.id.layout_root);
        mBLEManager = Bluetooth.getInstance(this, this);
        mBLEManager.initialize();
        mThis = this;
        preference = new SharePreference(this);
        Global.Connected = true;
        sensorStored = preference.getConfigFromStorage();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("加速度センサ較正");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mBlueToothSenSor.setCallback(this);
        mBlueToothSenSor.startServicesDiscovery();
        gaugeAcceleration = (GaugeAcceleration) findViewById(R.id.gaugeAcceleration);

        if (getIntent().hasExtra(Define.BUNDLE_EDIT_CALIBRATE)) {
            isEditCalibrate = true;
        }
        bodyPosition = getIntent().getIntExtra(Define.BUNDLE_BODY_POSITION, -1);
        // Add View
        TextView title = (TextView) findViewById(R.id.title);
        title.setText(mBlueToothSenSor.getDevice().getName() + " - " + mBlueToothSenSor.getDevice().getAddress());
        TableLayout tableLayout = (TableLayout) findViewById(R.id.table);
        tableLayout.removeAllViews();
        mTable = new TableView(tableLayout);
        sensorValues = new SensorValue(mBlueToothSenSor.getDevice().getAddress());

        dialog = new ProgressDialog(this);
        dialog.setTitle("Discovering...");
        dialog.setMessage("Getting data from sensor...");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialog != null && dialog.isShowing()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mThis);
                    builder.setTitle("お知らせ")
                            .setMessage("このセンサーの特性はありません")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    Intent intent = new Intent(CalibrateAccelerationActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            })
                            .show();
                }
            }
        }, 30000);
        meanFilterAccelerationSmoothing = new MeanFilter();
    }

    public void addRowToTable(ArrayList<GenericBluetoothProfile> row) {
        for (GenericBluetoothProfile profile : row) {
            if (mTable.isFirst()) {
                mTable.getRow().removeAllViews();
                mTable.getRow().addView(profile.getTableRow());
                mTable.getRow().requestLayout();
                mTable.setFirst(false);
            } else {
                if (mTable.getRow().indexOfChild(profile.getTableRow()) == -1) {
                    mTable.getRow().addView(profile.getTableRow());
                    mTable.getRow().requestLayout();
                }
            }
            profile.enableService();
            profile.onResume();
        }
    }

    private void addProfileBluetooth(BluetoothDevice device, GenericBluetoothProfile bluetooth) {
        for (BluetoothProfile ble : mProfiles) {
            if (ble.getAddress().equals(device.getAddress())) {
                ble.getProfile().add(bluetooth);
                return;
            }
        }
        ArrayList<GenericBluetoothProfile> profiles = new ArrayList<>();
        profiles.add(bluetooth);
        mProfiles.add(new BluetoothProfile(device.getAddress(), profiles));

    }

    public void calibrateListener(View view) {
        Toast.makeText(mThis, "アクセラレータの較正が成功する", Toast.LENGTH_SHORT).show();
        if (isEditCalibrate) {
            SensorStore value = preference.getConfigFromStorageByAddress(mProfiles.get(0).getAddress());
            preference.saveConfigToStorage(mBlueToothSenSor.getDevice().getName(), mBlueToothSenSor.getDevice().getAddress(), value.getDescription(), Define.STATUS_SAVE, bodyPosition, sensorValues.getAccel(), sensorValues.getGyros(), sensorValues.getMagnet());
        }else
            preference.saveConfigToStorage(mBlueToothSenSor.getDevice().getName(), mBlueToothSenSor.getDevice().getAddress(), mBlueToothSenSor.getDevice().getName(), Define.STATUS_CALIBRATE, bodyPosition, sensorValues.getAccel(), sensorValues.getGyros(), sensorValues.getMagnet());
        Intent returnIntent = new Intent();
        returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
        returnIntent.putExtra("editable", isEditCalibrate);
        setResult(Define.RESULT_CALIBRATE_OK, returnIntent);
        finish();
    }

    @Override
    public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {

    }

    @Override
    public void uiDeviceConnected(BluetoothGatt gatt, BluetoothDevice device) {

    }

    @Override
    public void uiDeviceConnectTimeout(BluetoothGatt gatt, BluetoothDevice device) {

    }

    @Override
    public void uiDeviceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {

    }

    @Override
    public void uiDeviceForceDisconnected(BluetoothGatt gatt, BluetoothDevice device) {

    }

    @Override
    public void uiAvailableServices(BluetoothGatt gatt, final BluetoothDevice device, final List<BluetoothGattService> services) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (services.size() != Define.BLE_SERVICE) {
                    ALog.d(TAG, "[" + device.getAddress() + "] service: " + services.size() + ". Discovered service again");
                    mBlueToothSenSor.startServicesDiscovery();
                    return;
                }

                ALog.d(TAG, "[" + device.getAddress() + "] Discovered service is: " + services.size());
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }

                for (BluetoothGattService service : services) {

                    if (SensorTagMovementProfile.isCorrectService(service)) {
                        SensorTagMovementProfile move = new SensorTagMovementProfile(mThis, device, service, mBlueToothSenSor);
                        addProfileBluetooth(device, move);
                        move.configureService();
                    }
                    if (SensorTagAmbientTemperatureProfile.isCorrectService(service)) {
                        SensorTagAmbientTemperatureProfile ambient = new SensorTagAmbientTemperatureProfile(mThis, device, service, mBlueToothSenSor);
                        //addProfileBluetooth(device,ambient);
                        ambient.configureService();
                    }
                    for (final BluetoothProfile p : mProfiles)
                        if (p.getAddress().equals(device.getAddress()))
                            addRowToTable(p.getProfile());
                }
            }
        });

    }

    @Override
    public void uiCharacteristicForService(BluetoothGatt gatt, final BluetoothDevice device, final BluetoothGattService service, final List<BluetoothGattCharacteristic> chars) {

    }

    @Override
    public void uiCharacteristicsDetails(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void uiNewValueForCharacteristic(BluetoothGatt gatt, final BluetoothDevice device, BluetoothGattService service, final BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProfiles == null)
                    return;
                if (dataTime != null) {
                    DateTime current = new DateTime();
                    long diff = current.getMillis() - dataTime.getMillis();
                    if (diff > TIME_OUT_GET_DATA) {
                        for (BluetoothProfile profile : mProfiles)
                            for (final GenericBluetoothProfile gbp : profile.getProfile())
                                if (gbp instanceof SensorTagMovementProfile) {
                                    if (sensorValues.haveValue()) {
                                        gbp.didUpdateNullValueForCharacteristic(sensorValues);
                                    }
                                }
                    }
                }
                for (BluetoothProfile profile : mProfiles)
                    for (final GenericBluetoothProfile gbp : profile.getProfile()) {
                        if (ch.getUuid().equals(SensorTagGatt.UUID_MOV_DATA) && gbp instanceof SensorTagMovementProfile) {
                            if (!isChangePeriod) {
                                isChangePeriod = true;
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        SensorTagMovementTableRow movement = (SensorTagMovementTableRow) gbp.getGenericCharacteristicTableRow();
                                        Log.e("UPDATE PERIOD", "");
                                        movement.updatePeriod(10);
                                    }
                                }, 5000);
                            }
                            dataTime = new DateTime();
                            sensorValues.setAccel(Sensor.MOVEMENT_ACC.convert(ch.getValue()));
                            sensorValues.setGyros(Sensor.MOVEMENT_GYRO.convert(ch.getValue()));
                            sensorValues.setMagnet(Sensor.MOVEMENT_MAG.convert(ch.getValue()));
                            Point3D acceleration = Sensor.MOVEMENT_ACC.convert(ch.getValue());
                            Point3D gyros = Sensor.MOVEMENT_GYRO.convert(ch.getValue());
                            ALog.e("Gyr",String.format("%.02f,%.02f,%.02f",gyros.x, gyros.y,gyros.z));
                            float[] arrayAccel = {(float) acceleration.x, (float) acceleration.y};
                            float[] filterAccel = meanFilterAccelerationSmoothing.filter(arrayAccel);
                            gaugeAcceleration.updatePoint(filterAccel[0], filterAccel[1]);
                            if (gaugeAcceleration.checkCalibrate()) {
                                TextView txtCalibrate = (TextView) findViewById(R.id.txt_calibrate);
                                txtCalibrate.setVisibility(View.VISIBLE);
                            } else {
                                TextView txtCalibrate = (TextView) findViewById(R.id.txt_calibrate);
                                txtCalibrate.setVisibility(View.GONE);
                            }
                        }
                        if (gbp instanceof SensorTagMovementProfile)
                            ((SensorTagMovementProfile) gbp).didUpdateValueForCharacteristic(sensorValues);
                        else
                            gbp.didUpdateValueForCharacteristic(ch);
                    }
                if (sensorValues.haveValueAccel()) {
                    //ALog.d(TAG, "Sensors collect enough movement data");
                    if (dialog != null) {
                        dialog.dismiss();
                        dialog = null;
                    }
                }

            }
        });


    }

    @Override
    public void uiGotNotification(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void uiSuccessfulWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description, int queueId) {

    }

    @Override
    public void uiFailedWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description, int queueId) {

    }

    @Override
    public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, int rssi) {

    }

    @Override
    public void uiDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, int queueId) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        for (BluetoothProfile p : mProfiles) {
            for (GenericBluetoothProfile profile : p.getProfile()) {
                if (profile.isConfigured != true) profile.configureService();
                if (profile.isEnabled != true) profile.enableService();
                profile.onResume();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        for (BluetoothProfile p : mProfiles) {
            for (GenericBluetoothProfile profile : p.getProfile()) {
                profile.onPause();
            }
        }
        mTable.getRow().removeAllViews();

        this.mProfiles = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (dialog != null && dialog.isShowing())
                return true;
            DialogUtils.showMessageDialog(this, "お知らせ", "加速度センサー較正は完了していません。較正を終了しますか？", new DialogUtils.YesNoListener() {
                @Override
                public void onYes() {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
                    setResult(Define.RESULT_CALIBRATE_CANCELED, returnIntent);
                    finish();
                }
            });
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                DialogUtils.showMessageDialog(this, "お知らせ", "加速度センサー較正は完了していません。較正を終了しますか？", new DialogUtils.YesNoListener() {
                    @Override
                    public void onYes() {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
                        setResult(Define.RESULT_CALIBRATE_CANCELED, returnIntent);
                        finish();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

