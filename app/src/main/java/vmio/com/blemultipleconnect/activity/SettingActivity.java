package vmio.com.blemultipleconnect.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.IntentUtils;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.adapter.ConfigBleAdapter;
import vmio.com.blemultipleconnect.gps.Services;
import vmio.com.blemultipleconnect.model.BleDeviceInfo;
import vmio.com.blemultipleconnect.model.ButtonDevice;
import vmio.com.blemultipleconnect.model.ConfigBleDeviceInfo;
import vmio.com.blemultipleconnect.model.SensorStore;
import vmio.com.blemultipleconnect.thread.CheckBleActiveThread;
import vmio.com.blemultipleconnect.thread.ScanBleThread;
import vmio.com.mioblelib.ble.BleDevice;
import vmio.com.mioblelib.ble.BleWrapperUiCallbacks;
import vmio.com.mioblelib.ble.Bluetooth;
import vmio.com.mioblelib.service.SensorTagSimpleKeysProfile;
import vmio.com.mioblelib.utilities.SensorTagGatt;
import vmio.com.mioblelib.widget.Point3D;

import static com.platypii.baseline.bluetooth.BluetoothService.BT_CONNECTED;
import static vmio.com.blemultipleconnect.Utilities.Define.BLE_MULTI_SENSOR_NAME;
import static vmio.com.blemultipleconnect.Utilities.Define.CONNECT_SENSOR_INTERVAL;
import static vmio.com.blemultipleconnect.Utilities.Define.GPS_STATUS_UPDATE_INTERVAL;
import static vmio.com.blemultipleconnect.Utilities.Define.SCANNING_SENSOR_INTERVAL;
import static vmio.com.blemultipleconnect.Utilities.Define.STATUS_CONNECTED;
import static vmio.com.blemultipleconnect.Utilities.Define.STATUS_NOT_CONNECT;
import static vmio.com.blemultipleconnect.activity.MainActivity.mConnectedBLEs;

public class SettingActivity extends AppCompatActivity implements BleWrapperUiCallbacks, ConfigBleAdapter.ItemClickListener, View.OnClickListener, ScanBleThread.ReScanBleCallback, CheckBleActiveThread.CheckBleActiveCallback {
    private ArrayList<ConfigBleDeviceInfo> mDevices = new ArrayList<>();
    private RecyclerView listBle;
    private ConfigBleAdapter adapter;
    private Bluetooth mBLEManager;
    private boolean isScanning = false, isReConnect = false;
    private ArrayList<ButtonDevice> btnDevice = new ArrayList<>();
    private ProgressDialog dialog, dialogReConnect;
    public static BleDevice mBlueToothSenSor;
    private Context mContext;
    private SettingActivity mThis;
    private SharePreference preference;
    private ArrayList<SensorStore> sensorStored = new ArrayList<>();
    private String[] arrayPosition = new String[]{"頭部", "左腕", "右腕", "左脚", "右脚", "胸部", "臀部"};
    private String[] arrayEdit = new String[]{"加速度センサーの再較正", "地磁気センサーの再較正", "メモの編集", "装着位置の編集"};
    private int lastGPSStatus = -1, indexReConnect = 1;
    private FrameLayout btnScan;
    private Handler handler;
    private Runnable runnable;
    private Switch switchGps;
    private ScanBleThread scanBleThread;
    private CheckBleActiveThread checkBleActiveThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        mContext = this;
        mThis = this;
        preference = new SharePreference(this);
        btnScan = (FrameLayout) findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(this);
        listBle = (RecyclerView) findViewById(R.id.list_device);
        listBle.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        listBle.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(listBle.getContext(), layoutManager.getOrientation());
        listBle.addItemDecoration(dividerItemDecoration);

        // Init bluetooth manager
        mBLEManager = Bluetooth.getInstance(this, this);
        if (!mBLEManager.initialize()) {
            Log.d("MainActivity", "Error initalize BLE");
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("デバイス設定");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        // Get gps sensor have been connected from preference
        scanBleThread = new ScanBleThread(this);
        scanBleThread.start();

        checkBleActiveThread = new CheckBleActiveThread(this);
        checkBleActiveThread.start();

        sensorStored = preference.getConfigFromStorage();
        setBleConnectedStatus();

        Services.bluetooth.start(this);
        Services.bluetooth.preferenceEnabled = true;
        adapter = new ConfigBleAdapter(this, mDevices);
        listBle.setAdapter(adapter);
        adapter.setOnItemClickListener(this);

        switchGps = (Switch) findViewById(R.id.switch_gps);
        switchGps.setChecked(!preference.getMustEnableGPS());
        switchGps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                preference.saveMustEnableGPS(!isChecked);
            }
        });
    }

    private void scanBle() {
        if (isScanning) {
            isScanning = false;
            mBLEManager.stopScanning();
            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    mBLEManager.startScanning();
                    isScanning = true;
                }
            };
            handler.postDelayed(runnable, 1000);
            return;
        }
        mBLEManager.startScanning();
        isScanning = true;
        ((TextView) findViewById(R.id.txt_scan)).setText("スキャン中...");
        final ImageView animationTarget = (ImageView) this.findViewById(R.id.img_rescan);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.rotate);
        animationTarget.startAnimation(animation);
        // Update list ble
//        handler = new Handler();
//        runnable = new Runnable() {
//            @Override
//            public void run() {
//                mBLEManager.stopScanning();
//                isScanning = false;
//                ((TextView) findViewById(R.id.txt_scan)).setText("スキャン");
//                animationTarget.clearAnimation();
//            }
//        };
//        handler.postDelayed(runnable, SCANNING_SENSOR_INTERVAL);
//        new CountDownTimer(SCANNING_SENSOR_INTERVAL, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//
//            }
//
//            @Override
//            public void onFinish() {
//                mBLEManager.stopScanning();
//                handler = new Handler();
//                runnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        clearRedSensorInList();
//                        mBLEManager.startScanning();
//                    }
//                };
//                handler.postDelayed(runnable, 2000);
//            }
//        }.start();
    }

    private void setBleConnectedStatus() {
        for (BleDevice device : mConnectedBLEs) {
            device.setCallback(this);
            ConfigBleDeviceInfo bleDeviceInfo = new ConfigBleDeviceInfo(device.getDevice(), 0);
            for (SensorStore sensorStore : sensorStored)
                if (sensorStore.getAddress().equals(device.getDevice().getAddress())) {
                    bleDeviceInfo.setStatus(sensorStore.getStatus());
                    bleDeviceInfo.setDescription(sensorStore.getDescription());
                    bleDeviceInfo.setPosition(sensorStore.getPosition());
                    if (bleDeviceInfo.getBluetoothDevice().getAddress().equals(preference.getSensorAttachedCamera()))
                        bleDeviceInfo.setCameraAttached(true);
                    mDevices.add(bleDeviceInfo);
                }
        }
    }

    private boolean deviceInfoExists(String address) {
        for (int i = 0; i < mDevices.size(); i++) {
            if (mDevices.get(i).getBluetoothDevice().getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    private void showDialogConnect() {
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle("接続中");
        dialog.setMessage("センサーデバイスへの接続を試みています。少々お待ちください。...");
        dialog.show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                    dialog = null;
                    DialogUtils.showMessageWithoutCancel(mContext, "お知らせ", "接続に失敗しました。以下の事項をご確認ください。\n" +
                            "1. センサーの電池残量をご確認ください。\n" +
                            "2. センサーのソフトウェアリセットをお試しください。\n" +
                            "3. センサーのハードウェアリセットをお試しください。\n" +
                            "4. 他のスマートフォンにインストール済みのロガーアプリとペアリングされている可能性がありますので、ご確認ください。", null);
                }
            }
        }, CONNECT_SENSOR_INTERVAL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Define.REQUEST_CODE_CALIBRATE) {
            mBLEManager = Bluetooth.getInstance(this, this);
            if (!mBLEManager.initialize()) {
                Log.d("MainActivity", "Error initalize BLE");
            }
            if (resultCode == Define.RESULT_CALIBRATE_OK) {
                String address = data.getStringExtra("address");
                boolean isEdit = data.getBooleanExtra("editable", false);
                for (BleDevice device : mConnectedBLEs)
                    if (device.getAddress().equals(address))
                        device.setCallback(this);
                if (!isEdit) {
                    for (ConfigBleDeviceInfo ble : mDevices) {
                        if (ble.getBluetoothDevice().getAddress().equals(address))
                            ble.setStatus(Define.STATUS_CALIBRATE);
                    }
                    adapter.notifyDataSetChanged();
                }
            } else if (resultCode == Define.RESULT_CALIBRATE_CANCELED) {
                String address = data.getStringExtra("address");
                for (BleDevice device : mConnectedBLEs)
                    if (device.getAddress().equals(address))
                        device.setCallback(this);
                adapter.notifyDataSetChanged();
            }
        } else if (requestCode == Define.REQUEST_CODE_CALIBRATE_MAGNET) {
            mBLEManager = Bluetooth.getInstance(this, this);
            if (!mBLEManager.initialize()) {
                Log.d("MainActivity", "Error initalize BLE");
            }
            if (resultCode == Define.RESULT_CALIBRATE_MAGNET_OK) {
                String address = data.getStringExtra("address");
                boolean isEdit = data.getBooleanExtra("editable", false);
                for (BleDevice device : mConnectedBLEs)
                    if (device.getAddress().equals(address))
                        device.setCallback(this);
                if (!isEdit) {
                    for (ConfigBleDeviceInfo ble : mDevices) {
                        if (ble.getBluetoothDevice().getAddress().equals(address))
                            ble.setStatus(Define.STATUS_CALIBRATE_MAGNET);
                    }
                    adapter.notifyDataSetChanged();
                }
            } else if (resultCode == Define.RESULT_CALIBRATE_MAGNET_CANCELED) {
                String address = data.getStringExtra("address");
                for (BleDevice device : mConnectedBLEs)
                    if (device.getAddress().equals(address))
                        device.setCallback(this);
                adapter.notifyDataSetChanged();
            }
        }
    }//onActivityResult

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_setting, menu);
        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Iterator<ConfigBleDeviceInfo> ble = mDevices.iterator();
        while (ble.hasNext()) {
            ConfigBleDeviceInfo d = ble.next();
            if (d.getBluetoothDevice().getName().contains(Define.BLUETOOTH_GPS_NAME)) {
                ble.remove();
            }
        }
        searchPairedBTDevices();
    }

    // Search already paired legacy bluetooth devices
    private void searchPairedBTDevices() {
        // Add bluetooth device paired
        Set<BluetoothDevice> devices = Services.bluetooth.getDevices();
        for (BluetoothDevice d : devices) {
            // Auto add GNS 2000 as GPS sensor
            if (d.getName().contains(Define.BLUETOOTH_GPS_NAME)) {
                // Dedault GPS is GNS 2000. Add to preference if not added then start
                if (Services.bluetooth.preferenceDeviceId == null) {
                    Services.bluetooth.preferenceDeviceId = d.getAddress();
                    Services.bluetooth.preferenceDeviceName = d.getName();
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("bluetooth_device_id", Services.bluetooth.preferenceDeviceId);
                    editor.putString("bluetooth_device_name", Services.bluetooth.preferenceDeviceName);
                    editor.apply();
                    // Start GPS
                    Services.bluetooth.preferenceEnabled = true;
                    Services.bluetooth.start(this);
                }
                // Add to list view
                mDevices.add(new ConfigBleDeviceInfo(d, 0, Define.STATUS_CONNECTED));
                updateGPSStatus(d);
            }
        }
    }

    // Update GPS status every 1 second
    void updateGPSStatus(final BluetoothDevice device) {
        ConfigBleDeviceInfo devInfo = getDeviceInfo(device.getAddress());
        if (devInfo == null)
            return;
        // update status in UI
        int currentStatus = Services.bluetooth.getState();
        // Update status in list GPS
        if (currentStatus != lastGPSStatus) {
            lastGPSStatus = currentStatus;
            devInfo.setStatus(currentStatus == BT_CONNECTED ? STATUS_CONNECTED : STATUS_NOT_CONNECT);
            adapter.notifyDataSetChanged();
        }
        // next time call again
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateGPSStatus(device);
            }
        }, GPS_STATUS_UPDATE_INTERVAL);
    }

    private ConfigBleDeviceInfo getDeviceInfo(String address) {
        for (int i = 0; i < mDevices.size(); i++) {
            if (mDevices.get(i).getBluetoothDevice().getAddress().equals(address))
                return mDevices.get(i);
        }
        return null;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_pair:
                IntentUtils.openBluetoothSettings(this);
                return true;
            case android.R.id.home:
                finish();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void uiDeviceFound(BluetoothDevice device, int rssi, byte[] record) {
        // Filter devices
        if (device.getName() == null)
            return;
        if (!device.getName().equals(BLE_MULTI_SENSOR_NAME))
            return;
        if (!deviceInfoExists(device.getAddress())) {
            // New device
            ConfigBleDeviceInfo bleDeviceInfo = new ConfigBleDeviceInfo(device, rssi);
            for (SensorStore sensorStore : sensorStored)
                if (sensorStore.getAddress().equals(device.getAddress())) {
                    bleDeviceInfo.setStatus(sensorStore.getStatus());
                    bleDeviceInfo.setPosition(sensorStore.getPosition());
                    if (bleDeviceInfo.getBluetoothDevice().getAddress().equals(preference.getSensorAttachedCamera()))
                        bleDeviceInfo.setCameraAttached(true);
                }
            bleDeviceInfo.setCurrentTime(new DateTime().getMillis());
            mDevices.add(bleDeviceInfo);
            adapter.notifyDataSetChanged();
            if (bleDeviceInfo.getStatus() != Define.STATUS_NOT_CONNECT) {
                BleDevice mBlueToothSenSor = new BleDevice(mBLEManager, device.getAddress(), this, this);
                btnDevice.add(new ButtonDevice(new BleDeviceInfo(bleDeviceInfo.getBluetoothDevice(), 0), mBlueToothSenSor));
                mBlueToothSenSor.connect();
            }
        } else {
           /* // Already in list, update RSSI info
            BleDeviceInfo deviceInfo = findDeviceInfo(device);
            deviceInfo.updateRssi(rssi);*/

            for (int i = 0; i < mDevices.size(); i++) {
                if (mDevices.get(i).getBluetoothDevice().getAddress().equals(device.getAddress())) {
                    mDevices.get(i).setCurrentTime(new DateTime().getMillis());
                }
            }
        }
    }

    @Override
    public void uiDeviceConnected(BluetoothGatt gatt, final BluetoothDevice device) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (ButtonDevice btn : btnDevice) {
                    if (btn.getmBluetooth().getDevice().getAddress().equals(device.getAddress())) {
                        for (BleDevice ble : mConnectedBLEs) {
                            if (ble.getDevice().getAddress().equals(device.getAddress()))
                                return;
                        }
                        mConnectedBLEs.add(btn.getmBluetooth());
                        btn.getmBluetooth().startServicesDiscovery();
                        if (dialog != null && dialog.isShowing()) {
                            preference.saveConfigToStorage(btn.getDevice().getBluetoothDevice().getName(), btn.getDevice().getBluetoothDevice().getAddress(), "", Define.STATUS_CONNECTED, -1, new Point3D(0, 0, 0), new Point3D(0, 0, 0), new Point3D(0, 0, 0));
                            sensorStored = preference.getConfigFromStorage();
                            dialog.dismiss();
                        }
                    }
                }
                for (ConfigBleDeviceInfo ble : mDevices)
                    if (ble.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        if (ble.getStatus() < Define.STATUS_CONNECTED)
                            ble.setStatus(Define.STATUS_CONNECTED);
                    }

                adapter.notifyDataSetChanged();

                if (preference.getSensorAttachedCamera() == null || preference.getSensorAttachedCamera().equals("")) {
                    preference.saveSensorAttachedCamera(device.getAddress());
                    for (ConfigBleDeviceInfo d : mDevices)
                        if (d.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                            d.setPosition(0);
                            //preference.saveConfigToStorage(d.getBluetoothDevice().getName(), d.getBluetoothDevice().getAddress(), "", Define.STATUS_CONNECTED, 0, new Point3D(0, 0, 0), new Point3D(0, 0, 0), new Point3D(0, 0, 0));
                        }
                }else {
                    boolean existSensorAttachedCamera = false;
                    for (ConfigBleDeviceInfo d : mDevices)
                        if (d.getBluetoothDevice().getAddress().equals(preference.getSensorAttachedCamera()) && d.getStatus() != STATUS_NOT_CONNECT) {
                            existSensorAttachedCamera = true;
                            break;
                        }
                    if (!existSensorAttachedCamera) {
                        preference.saveSensorAttachedCamera(device.getAddress());
                        for (ConfigBleDeviceInfo d : mDevices)
                            if (d.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                                d.setPosition(0);
                                //preference.saveConfigToStorage(d.getBluetoothDevice().getName(), d.getBluetoothDevice().getAddress(), "", Define.STATUS_CONNECTED, 0, new Point3D(0, 0, 0), new Point3D(0, 0, 0), new Point3D(0, 0, 0));
                            }
                    }
                }

                if (dialogReConnect != null && dialogReConnect.isShowing()) {
                    dialogReConnect.dismiss();
                    Intent intent = new Intent(SettingActivity.this, CalibrateAccelerationActivity.class);
                    startActivityForResult(intent, Define.REQUEST_CODE_CALIBRATE);
                }

            }
        });
    }

    private void retryToConnectSensor(final BluetoothDevice device) {
        if (indexReConnect > 3) {
            for (ConfigBleDeviceInfo dConnected : mDevices)
                if (dConnected.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                    dConnected.setStatus(Define.STATUS_NOT_CONNECT);
                    dConnected.setPosition(-1);
                    preference.removeConfigToStorage(sensorStored, dConnected.getBluetoothDevice().getAddress());
                }
            // Remove sensor from ListView
            Iterator<ConfigBleDeviceInfo> sensor = mDevices.iterator();
            while (sensor.hasNext()) {
                ConfigBleDeviceInfo d = sensor.next();
                if (d.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                    sensor.remove();
                    break;
                }
            }
            adapter.notifyDataSetChanged();
            // Rescan if scanning is stopped
            if (!isScanning)
                scanBle();
            indexReConnect = 1;
            return;
        }

        for (BleDevice ble : mConnectedBLEs) {
            if (ble.getDevice().getAddress().equals(device.getAddress()))
                break;
        }
        mBlueToothSenSor = new BleDevice(mBLEManager, device.getAddress(), mThis, mThis);
        btnDevice.add(new ButtonDevice(new BleDeviceInfo(mBlueToothSenSor.getDevice(), 0), mBlueToothSenSor));
        mBlueToothSenSor.connect();
        Toast.makeText(mContext, device.getAddress() + "に " + indexReConnect + " 回目の再接続トライ中です。", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                indexReConnect++;
                retryToConnectSensor(device);
            }
        }, 5000);
    }

    @Override
    public void uiDeviceConnectTimeout(BluetoothGatt gatt, final BluetoothDevice device) {

    }

    @Override
    public void uiDeviceForceDisconnected(BluetoothGatt gatt, final BluetoothDevice device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Iterator<BleDevice> ble = mConnectedBLEs.iterator();
                while (ble.hasNext()) {
                    BleDevice d = ble.next();
                    if (d.getDevice().getAddress().equals(device.getAddress())) {
                        ble.remove();
                        d.close();
                        Toast.makeText(mContext, "切断された", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                // Remove to list ble connected
                Iterator<ButtonDevice> btn = btnDevice.iterator();
                while (btn.hasNext()) {
                    ButtonDevice d = btn.next();
                    if (d.getDevice().getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        btn.remove();
                        break;
                    }
                }
                for (ConfigBleDeviceInfo dConnected : mDevices)
                    if (dConnected.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        dConnected.setStatus(Define.STATUS_NOT_CONNECT);
                        dConnected.setPosition(-1);
                        preference.removeConfigToStorage(sensorStored, dConnected.getBluetoothDevice().getAddress());
                    }
                // Remove sensor from ListView
                Iterator<ConfigBleDeviceInfo> sensor = mDevices.iterator();
                while (sensor.hasNext()) {
                    ConfigBleDeviceInfo d = sensor.next();
                    if (d.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        sensor.remove();
                        break;
                    }
                }
                boolean existOtherSensor = false;
                if (device.getAddress().equals(preference.getSensorAttachedCamera())) {
                    for (ConfigBleDeviceInfo d : mDevices)
                        if (!d.getBluetoothDevice().getName().contains("GNS 2000")) {
                            if (d.getStatus() == Define.STATUS_CONNECTED) {
                                existOtherSensor = true;
                                preference.saveSensorAttachedCamera(d.getBluetoothDevice().getAddress());
                                d.setCameraAttached(true);
                                DialogUtils.showMessageWithoutCancel(mThis, "お知らせ", "カメラセンサーが" + d.getBluetoothDevice().getAddress() + "に変更されます", new DialogUtils.YesNoListener() {
                                    @Override
                                    public void onYes() {

                                    }
                                });
                                break;
                            }
                        }
                }
                if (!existOtherSensor)
                    preference.clearSensorAttachedCamera();
                adapter.notifyDataSetChanged();
                // Rescan if scanning is stopped
                mBLEManager.stopScanning();
                if (handler != null) {
                    handler.removeCallbacks(runnable);
                    handler = null;
                }
                isScanning = false;
                if (!isScanning)
                    scanBle();
            }
        });
    }

    @Override
    public void uiDeviceDisconnected(BluetoothGatt gatt, final BluetoothDevice device) {
        Log.e("TAG", "Disconnect");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Iterator<BleDevice> ble = mConnectedBLEs.iterator();
                while (ble.hasNext()) {
                    BleDevice d = ble.next();
                    if (d.getDevice().getAddress().equals(device.getAddress())) {
                        ble.remove();
                        d.close();
                        Toast.makeText(mContext, "切断された", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                Toast.makeText(mThis, "Sensor Tag(" + device.getAddress() + ")との接続が切断されました。", Toast.LENGTH_SHORT).show();
                // Remove to list ble connected
                Iterator<ButtonDevice> btn = btnDevice.iterator();
                while (btn.hasNext()) {
                    ButtonDevice d = btn.next();
                    if (d.getDevice().getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        btn.remove();
                        break;
                    }
                }
                for (ConfigBleDeviceInfo dConnected : mDevices)
                    if (dConnected.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        dConnected.setStatus(Define.STATUS_NOT_CONNECT);
                        dConnected.setPosition(-1);
                        preference.removeConfigToStorage(sensorStored, dConnected.getBluetoothDevice().getAddress());
                    }
                // Remove sensor from ListView
                Iterator<ConfigBleDeviceInfo> sensor = mDevices.iterator();
                while (sensor.hasNext()) {
                    ConfigBleDeviceInfo d = sensor.next();
                    if (d.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        sensor.remove();
                        break;
                    }
                }
                boolean existOtherSensor = false;
                if (device.getAddress().equals(preference.getSensorAttachedCamera())) {
                    for (ConfigBleDeviceInfo d : mDevices)
                        if (!d.getBluetoothDevice().getName().contains("GNS 2000")) {
                            if (d.getStatus() == Define.STATUS_CONNECTED) {
                                existOtherSensor = true;
                                preference.saveSensorAttachedCamera(d.getBluetoothDevice().getAddress());
                                d.setCameraAttached(true);
                                DialogUtils.showMessageWithoutCancel(mThis, "お知らせ", "カメラセンサーが" + d.getBluetoothDevice().getAddress() + "に変更されます", new DialogUtils.YesNoListener() {
                                    @Override
                                    public void onYes() {

                                    }
                                });
                                break;
                            }
                        }
                }
                if (!existOtherSensor)
                    preference.clearSensorAttachedCamera();
                adapter.notifyDataSetChanged();
                // Rescan if scanning is stopped
                mBLEManager.stopScanning();
                if (handler != null) {
                    handler.removeCallbacks(runnable);
                    handler = null;
                }
                isScanning = false;
                if (!isScanning)
                    scanBle();
            }
        });

    }

    @Override
    public void uiAvailableServices(BluetoothGatt gatt, BluetoothDevice device, List<BluetoothGattService> services) {
        BleDevice bleDevice = null;
        for (BleDevice ble : mConnectedBLEs)
            if (device.getAddress().equals(ble.getDevice().getAddress()))
                bleDevice = ble;

        for (BluetoothGattService service : services) {
            // Check characteristic SimpleKey
            if (SensorTagSimpleKeysProfile.isCorrectService(service)) {
                SensorTagSimpleKeysProfile hum = new SensorTagSimpleKeysProfile(mContext, device, service, bleDevice);
                hum.configureService();

                for (ConfigBleDeviceInfo info : mDevices)
                    if (info.getBluetoothDevice().getAddress().equals(device.getAddress()))
                        info.setProfile(hum);
            }
        }
    }

    @Override
    public void uiCharacteristicForService(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, List<BluetoothGattCharacteristic> chars) {

    }

    @Override
    public void uiCharacteristicsDetails(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void uiNewValueForCharacteristic(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {

    }

    @Override
    public void uiGotNotification(BluetoothGatt gatt, final BluetoothDevice device, BluetoothGattService service, final BluetoothGattCharacteristic characteristic) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (characteristic.getUuid().toString().equals(SensorTagGatt.UUID_KEY_DATA.toString())) {
                    for (ConfigBleDeviceInfo info : mDevices)
                        if (info.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                            byte[] value = characteristic.getValue();
                            info.setKey(value[0]);
                            adapter.notifyDataSetChanged();
                        }

                }
            }
        });
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
    public void onItemClick(final int position) {
        switch (mDevices.get(position).getStatus()) {
            case Define.STATUS_NOT_CONNECT:
                mBlueToothSenSor = new BleDevice(mBLEManager, mDevices.get(position).getBluetoothDevice().getAddress(), this, this);
                btnDevice.add(new ButtonDevice(new BleDeviceInfo(mBlueToothSenSor.getDevice(), 0), mBlueToothSenSor));
                mBlueToothSenSor.connect();
                showDialogConnect();
                break;
            case Define.STATUS_CONNECTED:
                boolean connected = false;
                for (BleDevice ble : mConnectedBLEs)
                    if (ble.getDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                        mBlueToothSenSor = ble;
                        connected = true;
                    }
                if (!connected) {
                    mBlueToothSenSor = new BleDevice(mBLEManager, mDevices.get(position).getBluetoothDevice().getAddress(), this, this);
                    mBlueToothSenSor.connect();
                    dialogReConnect = new ProgressDialog(this);
                    dialogReConnect.setCancelable(false);
                    dialogReConnect.setCanceledOnTouchOutside(false);
                    dialogReConnect.setTitle("接続中");
                    dialogReConnect.setMessage("センサーデバイスへの接続を試みています。少々お待ちください。...");
                    dialogReConnect.show();
                } else {
                    Intent intent = new Intent(SettingActivity.this, CalibrateAccelerationActivity.class);
                    intent.putExtra(Define.BUNDLE_BODY_POSITION, mDevices.get(position).getPosition());
                    startActivityForResult(intent, Define.REQUEST_CODE_CALIBRATE);
                }
                break;
            case Define.STATUS_CALIBRATE:
                connected = false;
                for (BleDevice ble : mConnectedBLEs)
                    if (ble.getDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                        mBlueToothSenSor = ble;
                        connected = true;
                    }
                if (!connected) {
                    mBlueToothSenSor = new BleDevice(mBLEManager, mDevices.get(position).getBluetoothDevice().getAddress(), this, this);
                    mBlueToothSenSor.connect();
                    dialogReConnect = new ProgressDialog(this);
                    dialogReConnect.setCancelable(false);
                    dialogReConnect.setCanceledOnTouchOutside(false);
                    dialogReConnect.setTitle("接続中");
                    dialogReConnect.setMessage("センサーデバイスへの接続を試みています。少々お待ちください。...");
                    dialogReConnect.show();
                } else {
                    Intent intent = new Intent(SettingActivity.this, CalibrateMagnitudeActivity.class);
                    intent.putExtra(Define.BUNDLE_BODY_POSITION, mDevices.get(position).getPosition());
                    startActivityForResult(intent, Define.REQUEST_CODE_CALIBRATE_MAGNET);
                }
                break;
            case Define.STATUS_CALIBRATE_MAGNET:
                showDialogDescription(position);
                break;
            case Define.STATUS_MEMO:
                break;
            case Define.STATUS_SAVE:
                AlertDialog.Builder builderPosition = new AlertDialog.Builder(mContext);
                builderPosition.setTitle("どの操作を行いますか？")
                        .setSingleChoiceItems(arrayEdit, -1, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:
                                        boolean connected = false;
                                        for (BleDevice ble : mConnectedBLEs)
                                            if (ble.getDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                                                mBlueToothSenSor = ble;
                                                connected = true;
                                            }
                                        if (!connected) {
                                            mBlueToothSenSor = new BleDevice(mBLEManager, mDevices.get(position).getBluetoothDevice().getAddress(), mThis, mThis);
                                            mBlueToothSenSor.connect();
                                            dialogReConnect = new ProgressDialog(mThis);
                                            dialogReConnect.setCancelable(false);
                                            dialogReConnect.setCanceledOnTouchOutside(false);
                                            dialogReConnect.setTitle("接続中");
                                            dialogReConnect.setMessage("センサーデバイスへの接続を試みています。少々お待ちください。...");
                                            dialogReConnect.show();
                                        } else {
                                            Intent intent = new Intent(SettingActivity.this, CalibrateAccelerationActivity.class);
                                            intent.putExtra(Define.BUNDLE_EDIT_CALIBRATE, "edit");
                                            intent.putExtra(Define.BUNDLE_BODY_POSITION, mDevices.get(position).getPosition());
                                            startActivityForResult(intent, Define.REQUEST_CODE_CALIBRATE);
                                        }

                                        dialog.dismiss();
                                        break;
                                    case 1:
                                        boolean bleConnected = false;
                                        for (BleDevice ble : mConnectedBLEs)
                                            if (ble.getDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                                                mBlueToothSenSor = ble;
                                                bleConnected = true;
                                            }
                                        if (!bleConnected) {
                                            mBlueToothSenSor = new BleDevice(mBLEManager, mDevices.get(position).getBluetoothDevice().getAddress(), mThis, mThis);
                                            mBlueToothSenSor.connect();
                                            dialogReConnect = new ProgressDialog(mThis);
                                            dialogReConnect.setCancelable(false);
                                            dialogReConnect.setCanceledOnTouchOutside(false);
                                            dialogReConnect.setTitle("接続中");
                                            dialogReConnect.setMessage("センサーデバイスへの接続を試みています。少々お待ちください。...");
                                            dialogReConnect.show();
                                        } else {
                                            Intent intent = new Intent(SettingActivity.this, CalibrateMagnitudeActivity.class);
                                            intent.putExtra(Define.BUNDLE_EDIT_CALIBRATE, "edit");
                                            intent.putExtra(Define.BUNDLE_BODY_POSITION, mDevices.get(position).getPosition());
                                            startActivityForResult(intent, Define.REQUEST_CODE_CALIBRATE_MAGNET);
                                        }

                                        dialog.dismiss();
                                        break;
                                    case 2:
                                        showDialogDescription(position);
                                        dialog.dismiss();
                                        break;
                                    case 3:
                                        boolean haveOtherCamera = false;
                                        for (ConfigBleDeviceInfo bleDeviceInfo : mDevices) {
                                            if (bleDeviceInfo.getPosition() == 0 && !bleDeviceInfo.getBluetoothDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                                                haveOtherCamera = true;
                                                break;
                                            }
                                        }
                                        if (haveOtherCamera)
                                            showDialogAssignBody(position);
                                        dialog.dismiss();
                                        break;
                                }

                            }
                        });
                AlertDialog alertPosition = builderPosition.create();
                alertPosition.show();
                break;
        }
    }

    private void showDialogAssignBody(final int position) {
        sensorStored = preference.getConfigFromStorage();
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("装着位置を選択してください。")
                .setSingleChoiceItems(arrayPosition, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (SensorStore store : sensorStored)
                            if (store.getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                                preference.saveConfigToStorage(mDevices.get(position).getBluetoothDevice().getName(), mDevices.get(position).getBluetoothDevice().getAddress(), mDevices.get(position).getDescription(), Define.STATUS_SAVE, which, store.getAccel(), store.getGyro(), store.getMagnet());
                                mDevices.get(position).setStatus(Define.STATUS_SAVE);
                                mDevices.get(position).setPosition(which);
                                if (which != 0) {
                                    if (mDevices.get(position).isCameraAttached()) {
                                        mDevices.get(position).setCameraAttached(false);
                                        for (ConfigBleDeviceInfo bleDeviceInfo : mDevices) {
                                            if (bleDeviceInfo.getPosition() == 0 && !bleDeviceInfo.getBluetoothDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                                                bleDeviceInfo.setCameraAttached(true);
                                                preference.saveSensorAttachedCamera(bleDeviceInfo.getBluetoothDevice().getAddress());
                                            }
                                        }
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                dialog.dismiss();
                            }
                    }
                });
        builder.setCancelable(false);
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showDialogDescription(final int position) {
        sensorStored = preference.getConfigFromStorage();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("メモ");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String description = input.getText().toString();
                for (SensorStore store : sensorStored)
                    if (store.getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                        preference.saveConfigToStorage(mDevices.get(position).getBluetoothDevice().getName(), mDevices.get(position).getBluetoothDevice().getAddress(), description, Define.STATUS_SAVE, mDevices.get(position).getPosition(), store.getAccel(), store.getGyro(), store.getMagnet());
                        mDevices.get(position).setDescription(description);
                        mDevices.get(position).setStatus(Define.STATUS_SAVE);

                        adapter.notifyDataSetChanged();
                        dialog.dismiss();
                        if (mDevices.get(position).getPosition() == -1) {
                            showDialogAssignBody(position);
                        }
                    }
            }
        });

        builder.setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    public void onConfigClick(int position) {
        String address = mDevices.get(position).getBluetoothDevice().getAddress();
        /*for (BleDevice ble : mConnectedBLEs)
            if (ble.getDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                ble.disconnect();
            }*/
        // Remove to list ble connected
        Iterator<BleDevice> ble = mConnectedBLEs.iterator();
        while (ble.hasNext()) {
            BleDevice d = ble.next();
            if (d.getDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                ble.remove();
                d.close();
                Toast.makeText(mContext, "接続が切断されました。", Toast.LENGTH_SHORT).show();
                break;
            }
        }
        // Remove to list ble connected
        Iterator<ButtonDevice> btn = btnDevice.iterator();
        while (btn.hasNext()) {
            ButtonDevice d = btn.next();
            if (d.getDevice().getBluetoothDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                btn.remove();
                break;
            }
        }
        for (ConfigBleDeviceInfo dConnected : mDevices)
            if (dConnected.getBluetoothDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                dConnected.setStatus(Define.STATUS_NOT_CONNECT);
                dConnected.setPosition(-1);
                preference.removeConfigToStorage(sensorStored, dConnected.getBluetoothDevice().getAddress());
            }
        // Remove sensor from ListView
        Iterator<ConfigBleDeviceInfo> sensor = mDevices.iterator();
        while (sensor.hasNext()) {
            ConfigBleDeviceInfo d = sensor.next();
            if (d.getBluetoothDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                sensor.remove();
                break;
            }
        }
        boolean existOtherSensor = false;
        if (address.equals(preference.getSensorAttachedCamera())) {
            for (ConfigBleDeviceInfo d : mDevices)
                if (!d.getBluetoothDevice().getName().contains("GNS 2000")) {
                    if (d.getStatus() != Define.STATUS_NOT_CONNECT) {
                        existOtherSensor = true;
                        preference.saveSensorAttachedCamera(d.getBluetoothDevice().getAddress());
                        d.setCameraAttached(true);
                        DialogUtils.showMessageWithoutCancel(mThis, "お知らせ", "カメラセンサーが" + d.getBluetoothDevice().getAddress() + "に変更されます", new DialogUtils.YesNoListener() {
                            @Override
                            public void onYes() {

                            }
                        });
                        break;
                    }
                }
        }
        if (!existOtherSensor)
            preference.clearSensorAttachedCamera();
        adapter.notifyDataSetChanged();
        // Rescan if scanning is stopped
        mBLEManager.stopScanning();
//        handler.removeCallbacks(runnable);
        handler = null;
        isScanning = false;
        if (!isScanning)
            scanBle();

       /* mDeviceInfos.get(position).setStatus(Define.STATUS_NOT_CONNECT);
        mDeviceInfos.get(position).setPosition(-1);
        adapter.notifyDataSetChanged();
        preference.removeConfigToStorage(sensorStored, mDeviceInfos.get(position).getBluetoothDevice().getAddress());*/
    }

    @Override
    public void onFastCalibrate(int position) {
        boolean connected = false;
        for (BleDevice ble : mConnectedBLEs)
            if (ble.getDevice().getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                mBlueToothSenSor = ble;
                connected = true;
            }
        if (!connected) {
            mBlueToothSenSor = new BleDevice(mBLEManager, mDevices.get(position).getBluetoothDevice().getAddress(), this, this);
            mBlueToothSenSor.connect();
            dialogReConnect = new ProgressDialog(this);
            dialogReConnect.setCancelable(false);
            dialogReConnect.setCanceledOnTouchOutside(false);
            dialogReConnect.setTitle("接続中");
            dialogReConnect.setMessage("センサーデバイスへの接続を試みています。少々お待ちください。...");
            dialogReConnect.show();
        } else {
            Intent intent = new Intent(SettingActivity.this, FastCalibrateMagnitudeActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanBleThread.stop();
        checkBleActiveThread.stop();
        mBLEManager.stopScanning();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (isScanning)
                    Toast.makeText(this, "センサーデバイスのスキャンを開始しました。", Toast.LENGTH_SHORT).show();
                else {
                    scanBle();
                }
                break;
        }
    }

    @Override
    public void onRescan() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scanBle();
            }
        });

    }

    @Override
    public void onBleCheckActive() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                long currentTime = new DateTime().getMillis();
                Iterator<ConfigBleDeviceInfo> ble = mDevices.iterator();
                while (ble.hasNext()) {
                    ConfigBleDeviceInfo d = ble.next();
                    if (d.getBluetoothDevice().getName().contains(BLE_MULTI_SENSOR_NAME) && d.getStatus() == STATUS_NOT_CONNECT && currentTime - d.getCurrentTime() > SCANNING_SENSOR_INTERVAL) {
                        ble.remove();
                    }
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}
