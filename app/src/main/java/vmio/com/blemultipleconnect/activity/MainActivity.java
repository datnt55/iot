package vmio.com.blemultipleconnect.activity;

import android.app.Activity;
import android.app.FragmentManager;
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
import android.content.UriPermission;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Response;
import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.FileUtil;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.adapter.BleAdapter;
import vmio.com.blemultipleconnect.gps.Services;
import vmio.com.blemultipleconnect.listener.SimpleItemTouchHelperCallback;
import vmio.com.blemultipleconnect.model.BleDeviceInfo;
import vmio.com.blemultipleconnect.model.ButtonDevice;
import vmio.com.blemultipleconnect.model.ConfigBleDeviceInfo;
import vmio.com.blemultipleconnect.model.SensorStore;
import vmio.com.blemultipleconnect.service.FlashLightHelper;
import vmio.com.blemultipleconnect.service.OkHttpService;
import vmio.com.blemultipleconnect.widget.BottomNavigation;
import vmio.com.blemultipleconnect.widget.GoProCheckDialog;
import vmio.com.blemultipleconnect.widget.QuickActionView;
import vmio.com.mioblelib.ble.BleDevice;
import vmio.com.mioblelib.ble.BleWaitingQueue;
import vmio.com.mioblelib.ble.BleWrapperUiCallbacks;
import vmio.com.mioblelib.ble.Bluetooth;
import vmio.com.mioblelib.service.SensorTagBatteryProfile;
import vmio.com.mioblelib.service.SensorTagSimpleKeysProfile;
import vmio.com.mioblelib.utilities.SensorTagGatt;
import vmio.com.mioblelib.widget.Point3D;

import static com.platypii.baseline.bluetooth.BluetoothService.BT_CONNECTED;
import static com.platypii.baseline.bluetooth.BluetoothService.BT_STOPPED;
import static vmio.com.blemultipleconnect.Utilities.Define.GPS_STATUS_UPDATE_INTERVAL;
import static vmio.com.blemultipleconnect.Utilities.Define.STATUS_NOT_CONNECT;

public class MainActivity extends AppCompatActivity
        implements BleWrapperUiCallbacks, BleAdapter.ItemClickListener, BleWaitingQueue.BleNextActionCallbacks, BottomNavigation.BottomNaviCallback {

    private String TAG = "MUI";
    public static Bluetooth mBLEManager;
    public static ArrayList<BleDeviceInfo> mDeviceInfos = new ArrayList<>();    // list of both BLE and Legacy BT devices bound with UI
    private ArrayList<BleDeviceInfo> mDevicesConfig = new ArrayList<>();        // list of already configured device
    public static ArrayList<BleDevice> mConnectedBLEs = new ArrayList<>();      // list of connected BLE devices
    public static ArrayList<BleDeviceInfo> mGPS = new ArrayList<>();
    private RecyclerView listBle;
    private BleAdapter adapter;
    private String[] mDeviceFilter = null;
    private int num = 0;
    private Activity mActivity;
    private File desCopyFolder;
    private ProgressDialog dialog;
    private ProgressDialog dialogService = null;
    private boolean isScanning = false;
    private SharePreference preference;
    private BottomNavigation bottomNavigation;
    private ArrayList<ButtonDevice> btnDevice = new ArrayList<>();
    private ArrayList<SensorStore> sensorStored = new ArrayList<>();        // list of stored BLE sensor in shared preference
    private boolean mKeepConnected = false;                                  // Connection behavior when goto other Activities
    // Stored profiles in memory, for later use without discovery services again. Need to be re-assigned when update new GATT connection
    private List<SensorTagSimpleKeysProfile> mKeyPressProfiles = new ArrayList<>();  // Key Profile of CC2650
    private List<SensorTagBatteryProfile> mBatteryProfiles = new ArrayList<>();  // Battery Profile of CC2650
    private boolean isBluetoothStopping = false;                            // Blue is stop then not process any UI events
    private int lastGPSStatus = -1;                                         // Last GPS status
    // BLE timing parameters
    final int BLE_DELAY_DISCOVER_AFTER_CONNECTED = 100;
    final int BLE_DELAY_RECONNECTED_AFTER_LOST = 5000;
    final int BLE_DELAY_RECONNECT_WHEN_SERVICE_LOW = 10000;
    final int BLE_DELAY_SCAN_NEXT_DEVICES_AFTER_CONFIG = 200;
    // Queue for serialize all BLE sensors action
    private BleWaitingQueue mBleWaitingQueue = new BleWaitingQueue(this);
    private int mLastConfigureId = -1;
    private int mLastDeconfigureId = -1;
    // Move to DeviceActivity handler
    Handler mStartRecordHandler = null;
    Runnable mStartRecordRunnable = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;
        // 20171212 Ductx Vmio: Commentout dixlog show sensor position setting
//        initBodyDialog();
        preference = new SharePreference(this);
        TextView txtName = findViewById(R.id.txt_name);
        txtName.setText(preference.getId() +" ("+preference.getWorkerName()+")");
        mDeviceFilter = getResources().getStringArray(R.array.device_filter);
        listBle = (RecyclerView) findViewById(R.id.list_ble);
        listBle.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        listBle.setLayoutManager(layoutManager);
        // Display app name + version
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(Define.APPLICATION_NAME + " v." + Global.ApplicationVersion);
        bottomNavigation = findViewById(R.id.bottom);
        bottomNavigation.setCallback(this);
        // Get gps sensor have been connected from preference
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Services.bluetooth.preferenceDeviceId = null;
        Services.bluetooth.preferenceDeviceName = null;
        // If not exists then will scan later
//        if (Services.bluetooth.preferenceDeviceId != null && Services.bluetooth.getState() == BT_STOPPED) {
//            Services.bluetooth.preferenceEnabled = true;
//            Services.bluetooth.start(this);
//            ALog.d(TAG, "Start Bluetooth");
//        }
        // Init BLE manager
        mBLEManager = Bluetooth.getInstance(this, this);
        if (!mBLEManager.isInitialized()) {
            if (!mBLEManager.initialize()) {
                ALog.d(TAG, "Error initalize BLE");
            } else {
                ALog.d(TAG, "Start BLE");
            }
        }
        Intent intent = new Intent();
        String packageName = getPackageName();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(packageName)){
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        // 20171212 Ductx Vmio: Commentout dialog show sensor position setting
//        hideNavigationBar();
        //  Reset Flags to default
        mKeepConnected = false;
        isBluetoothStopping = false;
        lastGPSStatus = -1;
        // Clear all device lists
        mDeviceInfos.clear();
        mGPS.clear();
        btnDevice.clear();
        // Update list BLE and BT
        adapter = new BleAdapter(this, mDeviceInfos);
        listBle.setAdapter(adapter);
        adapter.setOnItemClickListener(this);
        SimpleItemTouchHelperCallback callback = new SimpleItemTouchHelperCallback(mActivity, adapter);
        adapter.setItemTouchCallBack(callback);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(listBle);

        // Re-direct BLE callback
        mBLEManager.setDependencies(this, this);
        for (BleDevice bleDevice : mConnectedBLEs) {
            bleDevice.setCallback(this);
        }
        // Get already setting BLE devices
        sensorStored = preference.getConfigFromStorage();
        // Connect already setting BLE devcies
        ALog.d(TAG, "Registered BLE Devices: " + sensorStored.size() + " devices");
        // First, if sensors connected then Discover them
        for (SensorStore storedDevice : sensorStored) {
            // Check connected BLE devices
            BleDevice ble = getConnectedBLE(storedDevice.getAddress());
            ALog.d(TAG, storedDevice.getPosition() + ")[" + storedDevice.getAddress() + (ble == null ? "] Disconnected" : "] Connected"));
            if (ble != null) {
                BleDeviceInfo bleDeviceInfo = new BleDeviceInfo(ble.getDevice(), -1000);
                bleDeviceInfo.setPosition(storedDevice.getPosition());
                bleDeviceInfo.setConnected(true);
                mDeviceInfos.add(bleDeviceInfo);
                adapter.notifyDataSetChanged();
                // ble.startServicesDiscovery();
                // discoverService(ble);
                mBleWaitingQueue.addProcess(ble, BleWaitingQueue.WaitAction.WAIT_FOR_DISCOVER, 5000);
                continue;
            }
        }
        // Second, if sensors not connected then Scan them
        for (SensorStore storedDevice : sensorStored) {
            // Check connected BLE devices
            BleDevice ble = getConnectedBLE(storedDevice.getAddress());
            if (ble == null) {
                // Registered BLE but not connected
                BleDevice newBle = new BleDevice(mBLEManager, storedDevice.getAddress(), this, this);
                addDeviceToList(storedDevice, newBle);
                // If connect without scan then one of un-comment bellow line, and connectNextWaitBLE function
                // newBle.connectInOtherThread();
                // newBle.connect();
                if (newBle.getDevice() != null && newBle.getDevice().getName() != null && newBle.getDevice().getName().equals(Define.BLE_MULTI_SENSOR_NAME))
                    mBleWaitingQueue.addProcess(newBle, BleWaitingQueue.WaitAction.WAIT_FOR_SCAN, 5000);
            }
        }
        // Process first BLE task item in queue
        mBleWaitingQueue.processNext();
        // Search paired Legacy Bluetooth devices
        searchPairedBTDevices();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        if (!mKeepConnected) {
            isBluetoothStopping = true;
            // Stop scan for make sure
            stopScanBle();
            // Stop BLE devices
            for (BleDevice ble : mConnectedBLEs) {
                ble.disconnect();
                ble.close();
            }
            ALog.d(TAG, "All BLE Closed");
            // Stop legacy Bluetooth device
            if (Services.bluetooth.preferenceDeviceId == null) {
                Services.bluetooth.stop();
            }
            ALog.d(TAG, "All Legacy Bluetooth Closed");
        }
    }

    public void onClickSaveSDListener(View v) {
        final File[] appsDir = ContextCompat.getExternalFilesDirs(mActivity, null);
        if (CommonUtils.externalMemoryAvailable(this)) {
            if (android.os.Build.VERSION.SDK_INT < 21) {
                Toast.makeText(mActivity, "Support android 5.0 and above", Toast.LENGTH_SHORT).show();
                return;
            }
            final ArrayList<File> extRootPaths = new ArrayList<>();
            for (final File f : appsDir)
                extRootPaths.add(f.getParentFile().getParentFile().getParentFile().getParentFile());
            if (CommonUtils.calculateSdcard(extRootPaths.get(1).getAbsolutePath()) < Global.MIN_FREE_STORAGE) {
                Toast.makeText(mActivity, "Can't save log data because external device is full", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(intent, 42);
            }
        } else {
            Toast.makeText(mActivity, "Can't save log data because there is no external devices", Toast.LENGTH_SHORT).show();
        }
    }

    // Discover connected BLE. Get from cached services if already discovered before time
    private void discoverService(BleDevice ble) {
        // set ti discovering state
        BleDeviceInfo info = getDeviceInfo(ble.getAddress());
        if (info == null) {
            ALog.d(TAG, "[" + ble.getAddress() + "] Discover with Address Not registered!");
            return;
        }
        if (info.getIsDiscovering()) {
            ALog.d(TAG, "[" + ble.getAddress() + "] Discover already start!");
            return;
        }
        List<BluetoothGattService> services = ble.getCachedServices();
        // reset Profiles before discovery
        clearProfiles(ble.getAddress());
        // Check that service already gotten then process from cache if exist
        if (services == null) {
            ALog.d(TAG, "[" + ble.getAddress() + "] Start discovery");
            info.setIsDiscovering(true);
            ble.startServicesDiscovery();
        } else {
            ALog.d(TAG, "[" + ble.getAddress() + "] Process discovered " + services.size() + " services");
            uiAvailableServices(ble.getGatt(), ble.getDevice(), services);
        }
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
                mGPS.add(new BleDeviceInfo(d, 0));
                mDeviceInfos.add(new BleDeviceInfo(d, 0));
                updateGPSStatus(d);
            }
        }
    }

    // Update GPS status every 1 second
    void updateGPSStatus(final BluetoothDevice device) {
        if (isFinishing() ||isBluetoothStopping || mGPS.size() <= 0)
            return;
        BleDeviceInfo devInfo = getDeviceInfo(device.getAddress());
        if (devInfo == null)
            return;
        // update status in UI
        int currentStatus = Services.bluetooth.getState();
        // Update status in list GPS
        for (BleDeviceInfo ble : mGPS)
            if (ble.getBluetoothDevice().getAddress().equals(device.getAddress()))
                ble.setConnected(currentStatus == BT_CONNECTED);
        if (currentStatus != lastGPSStatus) {
            lastGPSStatus = currentStatus;
            devInfo.setConnected(currentStatus == BT_CONNECTED);
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

    // Start scan BLE with in duration MAX_SCAN_TIME_MILISECOND.
    // This process drain baterry then need to be stop as soon as found all registered devices
    private void startScanBle() {
        if (isScanning) return;
        // Start to scan new ble
        mBLEManager.startScanning();
        isScanning = true;
        ALog.d(TAG, "START BLE SCAN");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanBle();
            }
        }, Define.MAX_SCAN_TIME_MILISECOND);
    }

    // Stop scan if scanning
    private void stopScanBle() {
        if (!isScanning) return;
        mBLEManager.stopScanning();
        isScanning = false;
        ALog.d(TAG, "STOP BLE SCAN");
    }

    public static boolean deviceInfoExists(String address) {
        for (int i = 0; i < mDeviceInfos.size(); i++) {
            if (mDeviceInfos.get(i).getBluetoothDevice().getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    // Start record click
    public void onClickStartRecord() {
        stopScanBle();
        boolean gnsConnected = false;
        for (BleDeviceInfo sensor : mDeviceInfos) {
            if (sensor.getBluetoothDevice().getName() != null)
                if (sensor.getBluetoothDevice().getName().contains("GNS 2000"))
                    if (sensor.isConnected()) {
                        mDevicesConfig.add(sensor);
                        gnsConnected = true;
                        continue;
                    }
            if (sensor.isConnected())
                mDevicesConfig.add(sensor);

        }

        if (!gnsConnected) {
            boolean check = preference.getMustEnableGPS();
            if (preference.getMustEnableGPS()) {
                DialogUtils.showMessageWithoutCancel(this, "お知らせ", "緯度経度情報を正しく取得出来ません。GNSデバイスの状態を確認の後、再度お試しください", null);
                return;
            }
        }

//        if (preference.getSensorAttachedCamera() == null || preference.getSensorAttachedCamera().equals("")) {
//            DialogUtils.showMessageWithoutCancel(this, "Warning", "Please select sensor is attached to camera", null);
//            return;
//        }
        BleDeviceInfo device = null;
        for (BleDeviceInfo bleDeviceInfo : mDevicesConfig)
            if (!bleDeviceInfo.getBluetoothDevice().getName().contains("GNS 2000")) {
                device = bleDeviceInfo;
                break;
            }
        if ( device != null) {
            if (preference.getSensorAttachedCamera() == null || preference.getSensorAttachedCamera().equals("")) {
                preference.saveSensorAttachedCamera(device.getBluetoothDevice().getAddress());
                for (BleDeviceInfo d : mDeviceInfos)
                    if (d.getBluetoothDevice().getAddress().equals(device.getBluetoothDevice().getAddress())) {
                        d.setPosition(0);
                    }
            } else {
                boolean existSensorAttachedCamera = false;
                for (BleDeviceInfo bleDeviceInfo : mDevicesConfig)
                    if (bleDeviceInfo.getBluetoothDevice().getAddress().equals(preference.getSensorAttachedCamera())) {
                        existSensorAttachedCamera = true;
                        break;
                    }
                if (!existSensorAttachedCamera) {
                    preference.saveSensorAttachedCamera(device.getBluetoothDevice().getAddress());
                    for (BleDeviceInfo d : mDeviceInfos)
                        if (d.getBluetoothDevice().getAddress().equals(device.getBluetoothDevice().getAddress())) {
                            d.setPosition(0);
                        }
                }
            }
        }
        if (mDevicesConfig.size() == 0) {
            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(mActivity);
            builder.setTitle("お知らせ")
                    .setMessage("センサーは登録されていません。 レジセンサーの設定にしてください")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert).show();
            return;
        }
//        boolean checkCalibrate = true;
//        for (BleDeviceInfo bleDeviceInfo : mDevicesConfig)
//            if (!bleDeviceInfo.getBluetoothDevice().getName().contains("GNS 2000")) {
//                if (preference.getCalibrateMagnet(bleDeviceInfo.getBluetoothDevice().getAddress()) == null) {
//                    DialogUtils.showAlertDialog(mActivity, "Notice", "Sensor " + bleDeviceInfo.getBluetoothDevice().getAddress() + " is not calibrated magnitude", new DialogUtils.YesNoListener() {
//                        @Override
//                        public void onYes() {
//
//                        }
//                    });
//                    checkCalibrate = false;
//                    break;
//                }
//            }
//        if (!checkCalibrate)
//            return;
        // Deconfigurate service already configured for not fire event at other Activity
        for (BleDevice ble : mConnectedBLEs)
            mBleWaitingQueue.addProcess(ble, BleWaitingQueue.WaitAction.WAIT_FOR_DECONFIG, 2000);

        // Deconfig first sensor
        mBleWaitingQueue.processNext();

        mKeepConnected = true;  //  move to DeviceActivety without disconnect sensors
        // Wair for start DeviceActivity after timeout or deconfigure finished

//        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//        float percent = 0.7f;
//        int seventyVolume = (int) (maxVolume);
//        audio.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);
//        AssetFileDescriptor afd = null;
//        try {
//            afd = getAssets().openFd("record.wav");
//            MediaPlayer player = new MediaPlayer();
//            player.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
//            player.prepare();
//            player.start();
//            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    mStartRecordHandler = new Handler();
//                    mStartRecordHandler.postDelayed(mStartRecordRunnable, 0);
//                }
//            });
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        GoProCheckDialog dialog = new GoProCheckDialog(this);
        dialog.setOnCallback(new GoProCheckDialog.GoProDialogCallback() {
            @Override
            public void onStart() {
                new FlashLightHelper().flash(mActivity, new FlashLightHelper.FlashLightCallback() {
                    @Override
                    public void onFlashFinish() {
                        mStartRecordHandler = new Handler();
                        mStartRecordHandler.postDelayed(mStartRecordRunnable, 50);
                        mStartRecordHandler = null;
                        //mStartRecordHandler.postDelayed(mStartRecordRunnable, 0);
                    }
                });
            }
        });
        dialog.show();

    }

    // Deconfig all profile belong to this BLE by address
    private void deconfigDevice(String address) {
        for (SensorTagSimpleKeysProfile p : mKeyPressProfiles) {
            if (p.getDevice().getAddress().equals(address)) {
                mLastDeconfigureId = p.deConfigureService();
            }
        }
        for (SensorTagBatteryProfile p : mBatteryProfiles) {
            if (p.getDevice().getAddress().equals(address)) {
                mLastDeconfigureId = p.deConfigureService();
            }
        }
    }

    public void tracksListener(View view) {
        Intent intent = new Intent(MainActivity.this, FileNotSyncActivity.class);
        startActivity(intent);
    }

    // Add registered device to UI List
    private void addDeviceToList(SensorStore storedDevice, BleDevice ble) {
        BleDeviceInfo bleDeviceInfo = new BleDeviceInfo(ble.getDevice(), -1000);
        bleDeviceInfo.setPosition(storedDevice.getPosition());
        bleDeviceInfo.setConnected(false);
        mDeviceInfos.add(bleDeviceInfo);
        adapter.notifyDataSetChanged();
        if (storedDevice.getStatus() != Define.STATUS_NOT_CONNECT) {
            for (ButtonDevice btn : btnDevice)
                if (btn.getmBluetooth().getAddress().equals(ble.getAddress()))
                    return;
            btnDevice.add(new ButtonDevice(bleDeviceInfo, ble));
        }
    }

    BleDevice getConnectingBLE(String address) {
        for (ButtonDevice btn : btnDevice)
            if (btn.getmBluetooth().getAddress().equals(address))
                return btn.getmBluetooth();
        return null;
    }

    BleDevice getConnectedBLE(String address) {
        for (BleDevice ble : mConnectedBLEs) {
            if (ble.getAddress().equals(address))
                return ble;
        }
        return null;
    }

    SensorStore getRegisteredDevice(String address) {
        for (SensorStore store : sensorStored) {
            if (store.getAddress().equals(address))
                return store;
        }
        return null;
    }

    private BleDeviceInfo getDeviceInfo(String address) {
        for (int i = 0; i < mDeviceInfos.size(); i++) {
            if (mDeviceInfos.get(i).getBluetoothDevice().getAddress().equals(address))
                return mDeviceInfos.get(i);
        }
        return null;
    }

    @Override
    public void uiDeviceFound(BluetoothDevice _device, int rssi, byte[] record) {
        if (isBluetoothStopping)
            return;
        final BluetoothDevice device = _device;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // One device name in registered list can be found multiple of time then use isScanning for filter it
                if (device.getName() == null || !isScanning)
                    return;
                // Do Filters
                if (!deviceInfoExists(device.getAddress()))
                    return;
                SensorStore stored = getRegisteredDevice(device.getAddress());
                if (stored == null)
                    return;

                ALog.d(TAG, "[" + device.getAddress() + "][uiDeviceFound]");
                BleDevice ble = getConnectedBLE(device.getAddress());
                if (ble == null) ble = getConnectingBLE(device.getAddress());
                // Connect found device after scan. Stop scan for keep connect more stable
                if (ble != null && !ble.isConnected()) {
                    stopScanBle();  // This stop make isScanning = false then next uiDeviceFound will be ignored
                    ble.connect();
                } else {
                    ALog.e(TAG, "[" + device.getAddress() + "] Error device not Found!");
                }
            }
        });
    }

    @Override
    public void uiDeviceConnected(BluetoothGatt gatt, final BluetoothDevice device) {
        if (isBluetoothStopping)
            return;
        ALog.d(TAG, "[" + device.getAddress() + "][uiDeviceConnected]");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update text of ble connected
                for (final ButtonDevice btn : btnDevice) {
                    if (btn.getDevice().getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        BleDevice ble = getConnectedBLE(device.getAddress());
                        if (ble == null) {
                            // Add connected BLE
                            mConnectedBLEs.add(btn.getmBluetooth());
                        }
                        BleDeviceInfo bleInfo = getDeviceInfo(btn.getDevice().getBluetoothDevice().getAddress());
                        if (bleInfo != null) {
                            bleInfo.setConnected(true);
                            adapter.notifyDataSetChanged();
                        }
                        //Toast.makeText(mActivity, String.format("Sensor %s connected", device.getAddress()), Toast.LENGTH_SHORT).show();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Discover procedure
                                discoverService(btn.getmBluetooth());
                            }
                        }, BLE_DELAY_DISCOVER_AFTER_CONNECTED);
                    }
                }
            }
        });
    }

    @Override
    public void uiDeviceConnectTimeout(BluetoothGatt gatt, final BluetoothDevice device) {
        ALog.d(TAG, "[" + device.getAddress() + "][uiDeviceConnectTimeout]");
    }

    @Override
    public void uiDeviceDisconnected(BluetoothGatt gatt, final BluetoothDevice device) {
        if (isBluetoothStopping)
            return;
        ALog.d(TAG, "[" + device.getAddress() + "][uiDeviceDisconnected]");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Iterator<ButtonDevice> it = btnDevice.iterator();
                while (it.hasNext()) {
                    final ButtonDevice d = it.next();
                    if (d.getDevice().getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        BleDeviceInfo bleInfo = getDeviceInfo(device.getAddress());
                        if (bleInfo != null) {
                            bleInfo.setConnected(false);
                            adapter.notifyDataSetChanged();
                        }
                        ALog.d(TAG, "[" + device.getAddress() + "] lost connection");
                        Toast.makeText(mActivity, String.format("Sensor [%s] lost connection", device.getAddress()), Toast.LENGTH_SHORT).show();
                        // re-connect after 3 seconds
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                d.getmBluetooth().connect();
                                ALog.d(TAG, "[" + device.getAddress() + "] Re-connect after " + BLE_DELAY_RECONNECTED_AFTER_LOST / 1000 + " seconds");
                                Toast.makeText(mActivity, String.format("Sensor [%s]: Re-connecting ...", device.getAddress()), Toast.LENGTH_SHORT).show();
                            }
                        }, BLE_DELAY_RECONNECTED_AFTER_LOST);
                        break;
                    }
                }
                if (mConnectedBLEs.size() == 0) {
                    Toast.makeText(mActivity, "全センサーデバイスが未接続になりました。", Toast.LENGTH_SHORT).show();
                    if (dialogService != null)
                        if (dialogService.isShowing())
                            dialogService.dismiss();
                }
            }

        });

    }

    @Override
    public void uiDeviceForceDisconnected(BluetoothGatt gatt, final BluetoothDevice device) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (BleDeviceInfo dConnected : mDeviceInfos)
                    if (dConnected.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        Toast.makeText(mActivity, "Sensor Tag(" + device.getAddress() + ")との接続が切断されました。", Toast.LENGTH_SHORT).show();
                        dConnected.setConnected(false);
                    }
                Iterator<BleDevice> ble = mConnectedBLEs.iterator();
                while (ble.hasNext()) {
                    BleDevice d = ble.next();
                    if (d.getDevice().getAddress().equals(device.getAddress())) {
                        ble.remove();
                        d.close();
                        break;
                    }
                }

                boolean existOtherSensor = false;
                if (device.getAddress().equals(preference.getSensorAttachedCamera())) {
                    for (BleDeviceInfo d : mDeviceInfos)
                        if (!d.getBluetoothDevice().getName().contains("GNS 2000")) {
                            if (d.isConnected()) {
                                existOtherSensor = true;
                                preference.saveSensorAttachedCamera(d.getBluetoothDevice().getAddress());
                                DialogUtils.showMessageWithoutCancel(mActivity, "お知らせ", "カメラセンサーが" + d.getBluetoothDevice().getAddress() + "に変更されます", new DialogUtils.YesNoListener() {
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
            }
        });
        ALog.d(TAG, "[" + device.getAddress() + "] Forced disconnected");
       /* for (int i = 0; i < mDeviceInfos.size(); i++) {
            if (mDeviceInfos.get(i).getBluetoothDevice().getAddress().equals(device.getAddress()))
                mDeviceInfos.get(i).setConnected(false);
        }
        adapter.notifyDataSetChanged();*/
    }

    @Override
    public void uiAvailableServices(final BluetoothGatt gatt, final BluetoothDevice device, final List<BluetoothGattService> services) {
        // set to discover finish
        BleDeviceInfo info = getDeviceInfo(device.getAddress());
        if (info != null) info.setIsDiscovering(false);

        if (isBluetoothStopping)
            return;

        final BleDevice bleDevice = getConnectedBLE(device.getAddress());
        if (bleDevice == null) return;

        ALog.d(TAG, "[" + device.getAddress() + "][uiAvailableServices]: count" + services.size());
        runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        // number of service is too low. try to re-connect
                        if (services.size() < Define.BLE_SERVICE) {
                            ALog.d(TAG, "[" + bleDevice.getAddress() + "] Service = " + services.size() + " is too Low. Try Re-connect device...");
                            bleDevice.close();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    bleDevice.connect();
                                }
                            }, BLE_DELAY_RECONNECT_WHEN_SERVICE_LOW);
                            return;
                        }
                        // Config battery and Keypress services
                        for (final BluetoothGattService service : services) {
                            // Check characteristic SimpleKey
                            if (SensorTagSimpleKeysProfile.isCorrectService(service)) {
                                SensorTagSimpleKeysProfile keyPressProfile = new SensorTagSimpleKeysProfile(mActivity, device, service, bleDevice);
                                mKeyPressProfiles.add(keyPressProfile);
                                ALog.d(TAG, "[" + device.getAddress() + "] configure KeyPress");
                                mLastConfigureId = keyPressProfile.configureService();

                                for (BleDeviceInfo info : mDeviceInfos)
                                    if (info.getBluetoothDevice().getAddress().equals(device.getAddress()))
                                        info.setProfile(keyPressProfile);
                            }
                            // Check characteristic battery
                            if (SensorTagBatteryProfile.isCorrectService(service)) {
                                SensorTagBatteryProfile batteryProfile = new SensorTagBatteryProfile(mActivity, device, service, bleDevice);
                                mBatteryProfiles.add(batteryProfile);
                                ALog.d(TAG, "[" + device.getAddress() + "] configure Battery");
                                mLastConfigureId = batteryProfile.configureService();
                            }
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
    public void uiNewValueForCharacteristic(BluetoothGatt gatt, final BluetoothDevice device, BluetoothGattService service, final BluetoothGattCharacteristic ch, final String strValue, int intValue, byte[] rawValue, String timestamp) {
        if (isBluetoothStopping)
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Update battery level percent to ble
                if (ch.getUuid().toString().equals(SensorTagGatt.UUID_BATTERY_DATA.toString())) {
                    int batteryLevel = ch.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    ALog.d(TAG, "[" + device.getAddress() + "][uiNewValueForCharacteristic] Battery: " + batteryLevel);
                    if (dialogService != null)
                        if (dialogService.isShowing())
                            dialogService.dismiss();
                    for (BleDeviceInfo info : mDeviceInfos) {
                        if (info.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                            info.setBattery(batteryLevel);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        });


    }

    @Override
    public void uiGotNotification(BluetoothGatt gatt, final BluetoothDevice device, BluetoothGattService service, final BluetoothGattCharacteristic characteristic) {
        if (isBluetoothStopping)
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Key press data event
                if (characteristic.getUuid().toString().equals(SensorTagGatt.UUID_KEY_DATA.toString())) {
                    byte[] value = characteristic.getValue();
                    if (value == null || value.length == 0)
                        return;
                    ALog.d(TAG, "[" + device.getAddress() + "][uiGotNotification] Key: " + value[0]);
                    for (BleDeviceInfo info : mDeviceInfos)
                        if (info.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                            info.setKey(value[0]);
                            adapter.notifyDataSetChanged();

                            // 20171212 Ductx Vmio: Commentout dialog show sensor position setting
//                            switch (info.getKey()) {
//                                case 0x1:
//                                    showSignal(device.getAddress());
//                                    break;
//                                case 0x2:
//                                    showSignal(device.getAddress());
//                                    break;
//                                case 0x3:
//                                    showSignal(device.getAddress());
//                                    break;
//                                case 0x4:
//                                    refreshBodyDialog();
//                                    break;
//                                case 0x5:
//                                    showSignal(device.getAddress());
//                                    break;
//                                case 0x6:
//                                    showSignal(device.getAddress());
//                                    break;
//                                case 0x7:
//                                    showSignal(device.getAddress());
//                                    break;
//                                default:
//                                    refreshBodyDialog();
//                                    break;
//                            }
                        }
                }
            }
        });
    }

    // 20171212 Ductx Vmio: Commentout dialog show sensor position setting
//        public void showSignal(String address) {
//        for (int i = 0; i < bleBodyArrayList.size(); i++) {
//            if (address.equals(bleBodyArrayList.get(i).getDeviceAddress())) {
//                bleBodyArrayList.get(i).getBtnPosition().setBackgroundResource(R.drawable.action_arrow_down);
//            }
//        }
//    }

    @Override
    public void uiSuccessfulWrite(final BluetoothGatt gatt, final BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description, final int queueId) {
        ALog.d(TAG, "[" + device.getAddress() + "]" + "[uiSuccessfulWrite]: Id = " + queueId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkFinalIdForProcessNext(gatt.getDevice().getAddress(), queueId);
            }
        });
    }

    @Override
    public void uiFailedWrite(final BluetoothGatt gatt, BluetoothDevice device, final BluetoothGattService service, BluetoothGattCharacteristic ch, String description, final int queueId) {
        ALog.d(TAG, "[" + device.getAddress() + "]" + "[uiFailedWrite]: Id = " + queueId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkFinalIdForProcessNext(gatt.getDevice().getAddress(), queueId);
            }
        });
    }

    @Override
    public void uiNewRssiAvailable(BluetoothGatt gatt, final BluetoothDevice device, final int rssi) {
        if (isBluetoothStopping)
            return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (BleDeviceInfo info : mDeviceInfos)
                    if (info.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        info.updateRssi(rssi);
                        break;
                    }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void uiDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, final int queueId) {
        ALog.d(TAG, "[" + gatt.getDevice().getAddress() + "]" + "[uiSuccessfulWrite]: Id = " + queueId);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkFinalIdForProcessNext(gatt.getDevice().getAddress(), queueId);
            }
        });
    }

    @Override
    public void onItemWarningClick(ImageView btnWarning, int position) {
        // create the quick action view, passing the view anchor
        QuickActionView qa = QuickActionView.Builder(btnWarning);
        // finally show the view
        qa.show();
        SharePreference preference = new SharePreference(this);
        ArrayList<SensorStore> storeArrayList = preference.getConfigFromStorage();
        for (SensorStore store : storeArrayList) {
            if (store.getAddress().equals(mDeviceInfos.get(position).getBluetoothDevice().getAddress())) {
                if (store.getStatus() == Define.STATUS_CONNECTED)
                    qa.setWarning("Sensor isn't Calibrated. Please goto Setting for do it first");
                else if (store.getStatus() == Define.STATUS_CALIBRATE)
                    qa.setWarning("Sensor isn't Calibrated. Please goto Setting for do it first");
                else if (store.getStatus() == Define.STATUS_CALIBRATE_MAGNET)
                    qa.setWarning("Sensor isn't set Body position. Please goto Setting for do it first");
            }
        }
    }

    @Override
    public void onSwipeItem(int position) {
        String address = mDeviceInfos.get(position).getBluetoothDevice().getAddress();
        preference.removeConfigToStorage(sensorStored, mDeviceInfos.get(position).getBluetoothDevice().getAddress());
        Iterator<BleDevice> ble = mConnectedBLEs.iterator();
        while (ble.hasNext()) {
            BleDevice d = ble.next();
            if (d.getDevice().getAddress().equals(mDeviceInfos.get(position).getBluetoothDevice().getAddress())) {
                ble.remove();
                d.close();
                Toast.makeText(mActivity, "切断された", Toast.LENGTH_SHORT).show();
                break;
            }
        }
        sensorStored = preference.getConfigFromStorage();
        boolean existOtherSensor = false;
        if (address.equals(preference.getSensorAttachedCamera())) {
            for (BleDeviceInfo bleInfo : mDeviceInfos)
                if (!bleInfo.getBluetoothDevice().getName().contains("GNS 2000")) {
                    for (SensorStore d : sensorStored)
                        if (bleInfo.getBluetoothDevice().getAddress().equals(d.getAddress()))
                            if (d.getStatus() == Define.STATUS_SAVE && d.getPosition() != -1) {
                                existOtherSensor = true;
                                preference.saveSensorAttachedCamera(d.getAddress());
                                DialogUtils.showMessageWithoutCancel(mActivity, "お知らせ", "カメラセンサーが" + d.getAddress() + "に変更されます", new DialogUtils.YesNoListener() {
                                    @Override
                                    public void onYes() {

                                    }
                                });
                                break;
                            }

                }
        }
        if (!existOtherSensor) {
            preference.clearSensorAttachedCamera();
        }
        mDeviceInfos.remove(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, adapter.getItemCount());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (Define.DEBUG)
            inflater.inflate(R.menu.main_debug, menu);
        else
            inflater.inflate(R.menu.main, menu);
        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_chart:
                ArrayList<String> address = new ArrayList<>();
                for (BleDeviceInfo sensor : mDeviceInfos)
                    if (sensor.getBluetoothDevice().getName() != null)
                        if (!sensor.getBluetoothDevice().getName().contains("GNS 2000") && sensor.isConnected())
                            address.add(sensor.getBluetoothDevice().getAddress());
                if (address.size() == 0) {
                    Toast.makeText(mActivity, "センサーが接続されていない", Toast.LENGTH_SHORT).show();

                } else if (address.size() == 1) {
                    Intent intentChart = new Intent(mActivity, GyroscopeChartActivity.class);
                    intentChart.putExtra(Define.BUNDLE_CHART, address.get(0));
                    startActivity(intentChart);
                } else {
                    final String[] listSensor = new String[address.size()];
                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                    mBuilder.setTitle("Choose an item");
                    mBuilder.setSingleChoiceItems(address.toArray(listSensor), -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intentChart = new Intent(mActivity, GyroscopeChartActivity.class);
                            intentChart.putExtra(Define.BUNDLE_CHART, listSensor[i]);
                            startActivity(intentChart);
                            dialogInterface.dismiss();
                        }
                    });
                    AlertDialog mDialog = mBuilder.create();
                    mDialog.show();
                }
                return true;
            case R.id.socket:
                ArrayList<String> addressSocket = new ArrayList<>();
                for (BleDeviceInfo sensor : mDeviceInfos)
                    if (sensor.getBluetoothDevice().getName() != null)
                        if (!sensor.getBluetoothDevice().getName().contains("GNS 2000") && sensor.isConnected())
                            addressSocket.add(sensor.getBluetoothDevice().getAddress());
                if (addressSocket.size() == 0) {
                    Toast.makeText(mActivity, "センサーが接続されていない", Toast.LENGTH_SHORT).show();

                } else if (addressSocket.size() == 1) {
                    Intent intentChart = new Intent(mActivity, SendDataViaSocketActivity.class);
                    intentChart.putExtra(Define.BUNDLE_CHART, addressSocket.get(0));
                    startActivity(intentChart);
                } else {
                    final String[] listSensor = new String[addressSocket.size()];
                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                    mBuilder.setTitle("Choose an item");
                    mBuilder.setSingleChoiceItems(addressSocket.toArray(listSensor), -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intentChart = new Intent(mActivity, SendDataViaSocketActivity.class);
                            intentChart.putExtra(Define.BUNDLE_CHART, listSensor[i]);
                            startActivity(intentChart);
                            dialogInterface.dismiss();
                        }
                    });
                    AlertDialog mDialog = mBuilder.create();
                    mDialog.show();
                }
                return true;
            case R.id.action_setting_body:
                if (mDeviceInfos.size() > 0) {
                    // 20171212 Ductx Vmio: Commentout dialog show sensor position setting
//                    mBodyDialog.show();
//                    refreshBodyDialog();
                    startActivity(new Intent(mActivity, SensorPositionSettingActivity.class));
                } else {
                    Toast.makeText(mActivity, "1つ以上のセンサーデバイスを接続してください。", Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.action_setting:
                Intent intent = new Intent(mActivity, SettingActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_logout:
                logOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void logOut() {
        Map<String, Object> params = new HashMap<>();
        params.put("", "");
        new OkHttpService(OkHttpService.Method.POST, this, Define.URL_LOGOUT, params, false) {
            @Override
            public void onFailureApi(Call call, Exception e) {
                Log.e("Error", e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mActivity, "Internet error", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponseApi(Call call, Response response) throws IOException {
                String result = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        preference.saveToken("");
                        preference.saveWorkerName("");
                        preference.saveId("");
                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 42:
                if (data == null)
                    return;
                Uri treeUri = data.getData();
                desCopyFolder = new File(treeUri.getEncodedPath());
                int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                mActivity.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
                new CopyFileAsynTask().execute();
                break;
        }
    }

    @Override
    public void nextScan(final BleDevice ble) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ALog.d(TAG, "[" + ble.getAddress() + "]" + " Scan next");
                startScanBle();
            }
        });
    }

    @Override
    public void nextDiscover(final BleDevice ble) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ALog.d(TAG, "[" + ble.getAddress() + "]" + " Discover next");
                discoverService(ble);
            }
        });
    }

    @Override
    public void nextConnect(final BleDevice ble) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ALog.d(TAG, "[" + ble.getAddress() + "]" + " Connect next");
                ble.connect();
            }
        });
    }

    @Override
    public void nextDeconfigure(final BleDevice ble) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ALog.d(TAG, "[" + ble.getAddress() + "]" + " Deconfigure next");
                deconfigDevice(ble.getAddress());
            }
        });
    }

    @Override
    public void finished() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mStartRecordHandler != null) {
                    ALog.d(TAG, "Deconfigured Finished: Stop Scan if scanning then Start DeviceActivity");
                    stopScanBle();
                    mStartRecordHandler.removeCallbacks(mStartRecordRunnable);
                    mStartRecordHandler.postDelayed(mStartRecordRunnable, 50);
                    mStartRecordHandler = null;
                }
            }
        });
    }

    // If current BLE finish sequence then goto next BLE device
    private void checkFinalIdForProcessNext(String address, int queueId) {
        if (queueId == mLastConfigureId || queueId == mLastDeconfigureId) {
            ALog.d(TAG, "[" + address + "]" + "Final Id = " + queueId + ". Process Next BLE");
            mBleWaitingQueue.processNext();
        }
    }

    @Override
    public void onStartRecord() {
        onClickStartRecord();
    }

    private class CopyFileAsynTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(mActivity);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setIndeterminate(false);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setMax(100);
            dialog.setMessage("Exporting...");
            dialog.setProgress(0);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            File fileSource = Environment.getExternalStoragePublicDirectory(Define.mMioUploadSaveDirectory);//"Mio/done");
            final File[] appsDir = ContextCompat.getExternalFilesDirs(mActivity, null);
            final ArrayList<File> extRootPaths = new ArrayList<>();
            for (final File f : appsDir)
                extRootPaths.add(f.getParentFile().getParentFile().getParentFile().getParentFile());

            DocumentFile dir = getDocumentFileIfAllowedToWrite(extRootPaths.get(1), mActivity);
            File folderUpload = new File(extRootPaths.get(1), "Sensor Tag");
            if (!folderUpload.exists()) {
                dir.createDirectory("Sensor Tag");
            }

            if (fileSource.exists()) {
                File[] files = fileSource.listFiles();

                int index = 0;
                for (File file : files) {
                    publishProgress(index * 100 / files.length);
                    copy(file, extRootPaths.get(1).getAbsolutePath() + "/Sensor Tag", mActivity);
                    index++;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(mActivity, "copied successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            dialog.setProgress(values[0]);
        }
    }

    public static DocumentFile getDocumentFileIfAllowedToWrite(File file, Context con) {

        List<UriPermission> permissionUris = con.getContentResolver().getPersistedUriPermissions();

        for (UriPermission permissionUri : permissionUris) {
            Uri treeUri = permissionUri.getUri();
            DocumentFile rootDocFile = DocumentFile.fromTreeUri(con, treeUri);
            String rootDocFilePath = FileUtil.getFullPathFromTreeUri(treeUri, con);

            if (file.getAbsolutePath().startsWith(rootDocFilePath)) {

                ArrayList<String> pathInRootDocParts = new ArrayList<String>();
                while (!rootDocFilePath.equals(file.getAbsolutePath())) {
                    pathInRootDocParts.add(file.getName());
                    file = file.getParentFile();
                }

                DocumentFile docFile = null;

                if (pathInRootDocParts.size() == 0) {
                    docFile = DocumentFile.fromTreeUri(con, rootDocFile.getUri());
                } else {
                    for (int i = pathInRootDocParts.size() - 1; i >= 0; i--) {
                        if (docFile == null) {
                            docFile = rootDocFile.findFile(pathInRootDocParts.get(i));
                        } else {
                            docFile = docFile.findFile(pathInRootDocParts.get(i));
                        }
                    }
                }
                if (docFile != null && docFile.canWrite()) {
                    return docFile;
                } else {
                    return null;
                }

            }
        }
        return null;
    }

    public boolean copy(File copy, String directory, Context con) {
        FileInputStream inStream = null;
        OutputStream outStream = null;
        DocumentFile dir = getDocumentFileIfAllowedToWrite(new File(directory), con);
        String mime = mime(copy.toURI().toString());
        DocumentFile copy1 = dir.createFile(mime, copy.getName());
        try {
            inStream = new FileInputStream(copy);
            outStream = con.getContentResolver().openOutputStream(copy1.getUri());
            byte[] buffer = new byte[16384];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                copy.delete();
                inStream.close();
                outStream.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public static String mime(String URI) {
        String type = "";
        String extention = MimeTypeMap.getFileExtensionFromUrl(URI);
        if (extention != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extention);
        }
        return type;
    }

    // Clear all profiles belong to this BLE address
    void clearProfiles(String address) {
        List<SensorTagSimpleKeysProfile> tmpList1 = new ArrayList<>();
        for (SensorTagSimpleKeysProfile p : mKeyPressProfiles) {
            if (!p.getDevice().getAddress().equals(address))
                tmpList1.add(p);
        }
        mKeyPressProfiles = tmpList1;
        List<SensorTagBatteryProfile> tmpList2 = new ArrayList<>();
        for (SensorTagBatteryProfile p : mBatteryProfiles) {
            if (!p.getDevice().getAddress().equals(address))
                tmpList2.add(p);
        }
        mBatteryProfiles = tmpList2;
    }

    // 20171212 Ductx Vmio: Commentout dialog show sensor position setting
//    private Button btnPos0, btnPos1, btnPos2, btnPos3, btnPos4, btnPos5, btnPos6, btnClose;
//    private Dialog mBodyDialog;
//    private ArrayList<BleBody> bleBodyArrayList = new ArrayList<>();
//
//    private void hideNavigationBar() {
//        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
//            View decorView = getWindow().getDecorView();
//            decorView.setSystemUiVisibility(uiOptions);
//        }
//    }
//
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        hideNavigationBar();
//    }
//
//    private void initBodyDialog() {
//        hideNavigationBar();
//        // custom dialog
//        mBodyDialog = new Dialog(mActivity, android.R.style.Theme_NoTitleBar_Fullscreen);
//        mBodyDialog.setContentView(R.layout.dialog_body);
//        mBodyDialog.setCancelable(false);
//        mBodyDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
//
//        btnPos0 = (Button) mBodyDialog.findViewById(R.id.btn_pos_0);
//        btnPos1 = (Button) mBodyDialog.findViewById(R.id.btn_pos_1);
//        btnPos2 = (Button) mBodyDialog.findViewById(R.id.btn_pos_2);
//        btnPos3 = (Button) mBodyDialog.findViewById(R.id.btn_pos_3);
//        btnPos4 = (Button) mBodyDialog.findViewById(R.id.btn_pos_4);
//        btnPos5 = (Button) mBodyDialog.findViewById(R.id.btn_pos_5);
//        btnPos6 = (Button) mBodyDialog.findViewById(R.id.btn_pos_6);
//        btnClose = (Button) mBodyDialog.findViewById(R.id.btn_close);
//
//        int width = Global.WIDTH_SCREEN;
//        int height = Global.HEIGHT_SCREEN;
//        int round = CommonUtils.convertDpToPx(20, mActivity);
//
//        RelativeLayout.LayoutParams params0 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, mActivity), CommonUtils.convertDpToPx(40, mActivity));
//        params0.setMargins((width / 2) - round, (height * 12 / 100) - round, 0, 0);
//        btnPos0.setLayoutParams(params0);
//
//        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, mActivity), CommonUtils.convertDpToPx(40, mActivity));
//        params1.setMargins((width * 71 / 100) - round, (height * 56 / 100) - round, 0, 0);
//        btnPos1.setLayoutParams(params1);
//
//        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, mActivity), CommonUtils.convertDpToPx(40, mActivity));
//        params2.setMargins((width * 29 / 100) - round, (height * 56 / 100) - round, 0, 0);
//        btnPos2.setLayoutParams(params2);
//
//        RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, mActivity), CommonUtils.convertDpToPx(40, mActivity));
//        params3.setMargins((width * 69 / 100) - round, (height * 90 / 100) - round, 0, 0);
//        btnPos3.setLayoutParams(params3);
//
//        RelativeLayout.LayoutParams params4 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, mActivity), CommonUtils.convertDpToPx(40, mActivity));
//        params4.setMargins((width * 31 / 100) - round, (height * 90 / 100) - round, 0, 0);
//        btnPos4.setLayoutParams(params4);
//
//        RelativeLayout.LayoutParams params5 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, mActivity), CommonUtils.convertDpToPx(40, mActivity));
//        params5.setMargins((width / 2) - round, (height * 42 / 100) - round, 0, 0);
//        btnPos5.setLayoutParams(params5);
//
//        RelativeLayout.LayoutParams params6 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, mActivity), CommonUtils.convertDpToPx(40, mActivity));
//        params6.setMargins((width / 2) - round, (height * 58 / 100) - round, 0, 0);
//        btnPos6.setLayoutParams(params6);
//
//        btnPos0.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                listSensorDialog(0);
//            }
//        });
//        btnPos1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                listSensorDialog(1);
//            }
//        });
//        btnPos2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                listSensorDialog(2);
//            }
//        });
//        btnPos3.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                listSensorDialog(3);
//            }
//        });
//        btnPos4.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                listSensorDialog(4);
//            }
//        });
//        btnPos5.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                listSensorDialog(5);
//            }
//        });
//        btnPos6.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                listSensorDialog(6);
//            }
//        });
//        btnClose.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mBodyDialog.dismiss();
//            }
//        });
//    }
//
//    private void refreshBodyDialog() {
//        btnPos0.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
//        btnPos1.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
//        btnPos2.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
//        btnPos3.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
//        btnPos4.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
//        btnPos5.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
//        btnPos6.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
//
//        btnPos0.setText("");
//        btnPos1.setText("");
//        btnPos2.setText("");
//        btnPos3.setText("");
//        btnPos4.setText("");
//        btnPos5.setText("");
//        btnPos6.setText("");
//
//        bleBodyArrayList = new ArrayList<>();
//        sensorStored = preference.getConfigFromStorage();
//        for (SensorStore store : sensorStored) {
//            if (deviceInfoExists(store.getAddress())) {
//                if (store.getPosition() >= 0) {
//                    switch (store.getPosition()) {
//                        case 0:
//                            btnPos0.setBackgroundResource(R.drawable.sensortag2);
//                            btnPos0.setText(store.getDescription());
//                            bleBodyArrayList.add(new BleBody(btnPos0, store.getAddress()));
//                            break;
//                        case 1:
//                            btnPos1.setBackgroundResource(R.drawable.sensortag2);
//                            btnPos1.setText(store.getDescription());
//                            bleBodyArrayList.add(new BleBody(btnPos1, store.getAddress()));
//                            break;
//                        case 2:
//                            btnPos2.setBackgroundResource(R.drawable.sensortag2);
//                            btnPos2.setText(store.getDescription());
//                            bleBodyArrayList.add(new BleBody(btnPos2, store.getAddress()));
//                            break;
//                        case 3:
//                            btnPos3.setBackgroundResource(R.drawable.sensortag2);
//                            btnPos3.setText(store.getDescription());
//                            bleBodyArrayList.add(new BleBody(btnPos3, store.getAddress()));
//                            break;
//                        case 4:
//                            btnPos4.setBackgroundResource(R.drawable.sensortag2);
//                            btnPos4.setText(store.getDescription());
//                            bleBodyArrayList.add(new BleBody(btnPos4, store.getAddress()));
//                            break;
//                        case 5:
//                            btnPos5.setBackgroundResource(R.drawable.sensortag2);
//                            btnPos5.setText(store.getDescription());
//                            bleBodyArrayList.add(new BleBody(btnPos5, store.getAddress()));
//                            break;
//                        case 6:
//                            btnPos6.setBackgroundResource(R.drawable.sensortag2);
//                            btnPos6.setText(store.getDescription());
//                            bleBodyArrayList.add(new BleBody(btnPos6, store.getAddress()));
//                            break;
//                    }
//                }
//            }
//        }
//    }
//
//    private void listSensorDialog(final int position) {
//        boolean checkPosition = false;
//        sensorStored = preference.getConfigFromStorage();
//        final ArrayList<SensorStore> listSensor = new ArrayList<>();
//        final ArrayList<String> strings = new ArrayList<>();
//        for (SensorStore store : sensorStored) {
//            if (deviceInfoExists(store.getAddress())) {
//                listSensor.add(store);
//                if (store.getPosition() < 0) {
//                    if (store.getDescription() != null) {
//                        strings.add(store.getDescription());
//                    } else {
//                        strings.add(store.getAddress());
//                    }
//                } else {
//                    if (store.getDescription() != null) {
//                        strings.add(store.getDescription() + " (" + CommonUtils.getPosition(store.getPosition()) + ") ");
//                    } else {
//                        strings.add(store.getAddress() + " (" + CommonUtils.getPosition(store.getPosition()) + ") ");
//                    }
//                }
//                if (position == store.getPosition()) {
//                    checkPosition = true;
//                }
//            }
//        }
//        if (listSensor.size() < 1){
//            Toast.makeText(mActivity, "Please keep connecting at least one sensor tag", Toast.LENGTH_LONG).show();
//            return;
//        }
//        if(checkPosition){
//            strings.add("Remove Sensor");
//        }
//        String[] stockArr = new String[strings.size()];
//        stockArr = strings.toArray(stockArr);
//        final String[] finalStockArr = stockArr;
//        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
//
//        builder.setTitle("Select Sensor")
//                .setItems(stockArr, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        String selection = finalStockArr[which];
//                        if (selection.equals("Remove Sensor")) {
//                            for (SensorStore store : listSensor) {
//                                if (store.getPosition() == position) {
//                                    preference.saveConfigToStorage(store.getName(), store.getAddress(), store.getDescription(), Define.STATUS_CALIBRATE, -1, store.getAccel(), store.getGyro(), store.getMagnet());
//                                }
//                            }
//                        } else {
//                            preference.saveConfigToStorage(listSensor.get(which).getName(), listSensor.get(which).getAddress(), listSensor.get(which).getDescription(), Define.STATUS_SAVE, position, listSensor.get(which).getAccel(), listSensor.get(which).getGyro(), listSensor.get(which).getMagnet());
//                        }
//                        if (adapter != null){
//                            adapter.updateSensorStorage(preference.getConfigFromStorage());
//                            adapter.notifyDataSetChanged();
//                        }
//                        refreshDeviceList();
//                        refreshBodyDialog();
//                        dialog.dismiss();
//                    }
//                });
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//        AlertDialog alert = builder.create();
//        alert.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
//        alert.show();
//    }
//
//    private void refreshDeviceList() {
//        sensorStored = preference.getConfigFromStorage();
//        mDeviceInfos.clear();
//        for (SensorStore store : sensorStored) {
//            BleDevice ble = getConnectedBLE(store.getAddress());
//            BleDeviceInfo bleDeviceInfo = new BleDeviceInfo(ble.getDevice(), -1000);
//            bleDeviceInfo.setPosition(store.getPosition());
//            bleDeviceInfo.setConnected(true);
//            mDeviceInfos.add(bleDeviceInfo);
//        }
//    }
//
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        if (mBodyDialog.isShowing()) {
//            mBodyDialog.dismiss();
//        } else {
//            finish();
//        }
//    }
}

