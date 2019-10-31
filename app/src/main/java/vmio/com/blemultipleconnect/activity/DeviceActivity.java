
package vmio.com.blemultipleconnect.activity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.platypii.baseline.GPSSensorStateListener;
import com.platypii.baseline.location.MyLocationListener;
import com.platypii.baseline.measurements.MLocation;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.CSVHelper;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.DecimalStandardFormat;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.FileUtil;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.KMLHelper;
import vmio.com.blemultipleconnect.Utilities.KalmanFilter;
import vmio.com.blemultipleconnect.Utilities.MagnetCalibrateCalculate;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.gps.GPXHelper;
import vmio.com.blemultipleconnect.gps.Services;
import vmio.com.blemultipleconnect.model.BleDeviceInfo;
import vmio.com.blemultipleconnect.model.BluetoothProfile;
import vmio.com.blemultipleconnect.model.TableView;
import vmio.com.blemultipleconnect.model.Title;
import vmio.com.blemultipleconnect.service.FlashLightHelper;
import vmio.com.blemultipleconnect.service.ServiceManager;
import vmio.com.blemultipleconnect.thread.CSVGAThread;
import vmio.com.blemultipleconnect.thread.CSVHAThread;
import vmio.com.blemultipleconnect.thread.CSVMAThread;
import vmio.com.blemultipleconnect.thread.CSVNAThread;
import vmio.com.blemultipleconnect.thread.CSVSAThread;
import vmio.com.blemultipleconnect.thread.CSVSensorDataThread;
import vmio.com.blemultipleconnect.thread.CSVSensorDataThread.GPS;
import vmio.com.blemultipleconnect.thread.GoProService;
import vmio.com.blemultipleconnect.thread.RealtimeGPSDataThread;
import vmio.com.blemultipleconnect.thread.RealtimeSensorDataThread;
import vmio.com.blemultipleconnect.thread.SaveCSVThread;
import vmio.com.blemultipleconnect.widget.LayoutGPS;
import vmio.com.blemultipleconnect.widget.LayoutSensor;
import vmio.com.blemultipleconnect.widget.TimerCounter;
import vmio.com.mioblelib.ble.BleDevice;
import vmio.com.mioblelib.ble.BleWaitingQueue;
import vmio.com.mioblelib.ble.BleWrapperUiCallbacks;
import vmio.com.mioblelib.ble.Bluetooth;
import vmio.com.mioblelib.model.SensorValue;
import vmio.com.mioblelib.service.GenericBluetoothProfile;
import vmio.com.mioblelib.service.SensorTagAmbientTemperatureProfile;
import vmio.com.mioblelib.service.SensorTagBarometerProfile;
import vmio.com.mioblelib.service.SensorTagHumidityProfile;
import vmio.com.mioblelib.service.SensorTagIRTemperatureProfile;
import vmio.com.mioblelib.service.SensorTagMovementProfile;
import vmio.com.mioblelib.utilities.SensorTagGatt;
import vmio.com.mioblelib.view.SensorTagMovementTableRow;
import vmio.com.mioblelib.widget.Point3D;
import vmio.com.mioblelib.widget.Sensor;

import static vmio.com.blemultipleconnect.Utilities.Define.BLE_MULTI_SENSOR_NAME;
import static vmio.com.blemultipleconnect.Utilities.Define.mMioTempDirectory;
import static vmio.com.blemultipleconnect.Utilities.Global.SLAMTimeStamp;
import static vmio.com.blemultipleconnect.Utilities.Global.sensorValues;
import static vmio.com.blemultipleconnect.activity.MainActivity.mConnectedBLEs;
import static vmio.com.blemultipleconnect.activity.MainActivity.mDeviceInfos;
import static vmio.com.blemultipleconnect.activity.MainActivity.mGPS;
import static vmio.com.mioblelib.utilities.Define.BLE_SERVICE;

public class DeviceActivity extends AppCompatActivity implements
        BleWrapperUiCallbacks,
        MyLocationListener,
        BleWaitingQueue.BleNextActionCallbacks,
        TimerCounter.TimerCounterListener,
        CSVSensorDataThread.UploadCSVListener,
        CSVHelper.CSVFileCreateListener,
        GoProService.ConnectGoProListener, SaveCSVThread.SaveDataToCSVCallback, CSVMAThread.MASaveFileListener, GPSSensorStateListener, View.OnClickListener {

    private String TAG = "DEV";
    public static DeviceActivity mThis;
    private LinearLayout layoutRoot;
    private Bluetooth mBLEManager;
    private List<BluetoothProfile> mProfiles = new ArrayList<>();
    private ArrayList<TableView> mListTable = new ArrayList<>();
    public boolean isChangePeriod;
    private LayoutGPS layoutGPS;
    public double cur_longitude = 0f;
    public double cur_latitude = 0f;
    private RealtimeSensorDataThread realtimeDataThread;
    private RealtimeGPSDataThread realtimeGPSThread;
    private CSVSensorDataThread csvSensorDataThread;
    private CSVSAThread csvsaThread;
    private CSVGAThread csvgaThread;
    private CSVNAThread csvnaThread;
    private CSVHAThread csvhaThread;
    private CSVMAThread csvtsThread;
    private boolean isGetGPSStart = false;
    private byte[] lastMovement;
    private File uploadFolder, uploadDoneFolder;
    private TimerCounter timerCounter;
    private CountDownTimer timerBrightness;
    private boolean mIsStopping = false;
    private DateTime previousTime;
    private GPXHelper gpxHelper;
    // Handler Queue for serialize all Device config/deconfig actions
    private BleWaitingQueue mBleWaitingQueue = new BleWaitingQueue(this);
    private static final int QUEUE_IS_PROCESSING_CONFIGURE_SEQUENCE = 1;
    private static final int QUEUE_IS_PROCESSING_DECONFIGURE_SEQUENCE = 2;
    private int mCurrentQueueProcessing = 0; // None
    private int mLastConfigProfileId = -1;
    private int mLastDeconfigProfileId = -1;
    private CSVHelper csvHelper;
    private ArrayList<Title> mListTitle = new ArrayList<>();
    private ArrayList<MagnetCalibrateCalculate> magnetCalibrateCalculators = new ArrayList<>();
    private boolean connectedGoPro = true;
    //private GoProService goProService;
    private SLAMDataThread slamDataThread;
    private SaveCSVThread saveCSVThread;
    private boolean reconnect = false, startSlam = false;
    private KMLHelper kmlHelper;
    //Variable to store brightness value
    private int brightness;
    //Content resolver used as a handle to the system's settings
    private ContentResolver cResolver;
    private boolean isScanning = false;
    private Window window;
    public int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    private KalmanFilter kalmanGNSFilter;
    private KalmanFilter kalmanAndroidFilter;
    private ServiceManager mPingServiceManager;
    private Handler zeroHandler;
    private Runnable zeroRunnable;
    private boolean zeroCheck = false;
    private Button btnSlam;
    private TextView txtStatusLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ALog.d(TAG, "ON CREATE");
        setContentView(R.layout.activity_device);
//        this.mPingServiceManager = new ServiceManager.PingManagerInstance(this).addCallBack(new ServiceManager.OnInitializedCallback() {
//            @Override
//            public void OnInitialized() {
//                initComponents();
//            }
//        }).getmManager();
//        this.mPingServiceManager.startService();
        initComponents();
        hideFlags();
        cResolver = getContentResolver();
        //Get the current window
        window = getWindow();
        //Set the system brightness using the brightness variable value
        //System.putInt(cResolver, System.SCREEN_BRIGHTNESS, brightness);
        //Get the current window attributes
        if (checkSystemWritePermission()) {
            startCountdownToSleepScreen();
        }

        kalmanGNSFilter = new KalmanFilter(0.1f);
        kalmanAndroidFilter = new KalmanFilter(0.2f);
        zeroHandler = new Handler();
        zeroRunnable = new Runnable() {
            @Override
            public void run() {
                if (invalidData(Global.magnet) || invalidData(Global.accel) || invalidData(Global.gyros) )
                    new FlashLightHelper().ringtone(mThis, 3000, 400, 200, new FlashLightHelper.FlashLightCallback() {
                        @Override
                        public void onFlashFinish() {
                            ImageView imgStop = findViewById(R.id.img_stop);
                            imgStop.performClick();
                        }
                    });
            }
        };
    }

    private boolean invalidData(Point3D point){
        return (point.x == 0 && point.y == 0 && point.z == 0) || (point.x == -1 && point.y == -1 && point.z == -1);
    }
    private void setBrightnessMode(int mode){
        try
        {
            // To handle the auto
            Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
            //Get the current system brightness
            brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e)
        {
            //Throw an error case it couldn't be retrieved
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }
    }

    private void setBrightnessLevel(float level){
        Settings.System.putInt(cResolver,  Settings.System.SCREEN_BRIGHTNESS, brightness);
        WindowManager.LayoutParams layoutpars = window.getAttributes();
        //Set the brightness of this window
        layoutpars.screenBrightness = level;
        //Apply attribute changes to this window
        window.setAttributes(layoutpars);
    }
    private boolean checkSystemWritePermission() {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(this);
            if(retVal){

            }else{
                openAndroidPermissionsMenu();
            }
        }
        return retVal;
    }

    private void startCountdownToSleepScreen(){
        if (timerBrightness != null)
            timerBrightness.cancel();
        timerBrightness = new CountDownTimer(5000, 1000) {

            public void onTick(long millisUntilFinished) {

            }

            public void onFinish() {
                setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                setBrightnessLevel(0f);
            }

        }.start();
    }
    private void openAndroidPermissionsMenu() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
    private void initComponents() {
        mThis = this;
        btnSlam = findViewById(R.id.btn_slam);
        btnSlam.setOnClickListener(this);
        txtStatusLogger = findViewById(R.id.txt_notice_logging);
        TextView txtName = findViewById(R.id.txt_name);
        txtName.setText(new SharePreference(this).getId() +" ("+ new SharePreference(this).getWorkerName()+")");
        // delete all file in temp folder
        File[] childFile = new File(mMioTempDirectory).listFiles();
        if (childFile.length > 0){
            for (int i = 0; i < childFile.length; i++)
                childFile[i].delete();
        }
        kmlHelper = new KMLHelper();
        kmlHelper.createGPXTrack();
        //goProService = new GoProService(this, this);
        //goProService.changeToWifiNetwork(true);
        slamDataThread = new SLAMDataThread();
        layoutRoot = (LinearLayout) findViewById(R.id.layout_root);
        mBLEManager = Bluetooth.getInstance(this, this);
        mBLEManager.initialize();
        Services.start(this,this);
        Global.Connected = true;
        // Default is try to reconnect when connection lost
        Global.BleReconnectRequest = true;
        initSensorChart();
        initSensorStorage();
        // Do config all BLE sequentialy
        ALog.d(TAG, "Start Discovery and configurate sequence");
        // Add ble discover task to queue then process in serial
        for (BleDevice ble : mConnectedBLEs) {
            ble.setCallback(mThis);
            mBleWaitingQueue.addProcess(ble, BleWaitingQueue.WaitAction.WAIT_FOR_DISCOVER, 5000);
            // Get calib calculator for this BLE
            MagnetCalibrateCalculate calculate = new MagnetCalibrateCalculate(this, ble.getAddress());
            calculate.getFactor(ble.getAddress());
            magnetCalibrateCalculators.add(calculate);
        }
        // Process first
        mCurrentQueueProcessing = QUEUE_IS_PROCESSING_CONFIGURE_SEQUENCE;
        mBleWaitingQueue.processNext();
//        if (layoutGPS != null && layoutGPS.isConnected()) //[180315 dungtv] ignore gps check for debug indoor
//            checkGPSLock();
        scanBle();
        Global.GPSFirstTime = null;

        View rlayout =  findViewById(android.R.id.content);
        rlayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                    setBrightnessLevel(0.7f);
                    startCountdownToSleepScreen();
                }
                return false;
            }
        });

        ScrollView scrollView =  findViewById(R.id.mainlayout);
        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                    setBrightnessLevel(0.7f);
                    startCountdownToSleepScreen();
                }
                return false;
            }
        });
        if (mGPS.size() > 0)
            Services.location.addListener(this);
    }

    private void scanBle() {
        if (isScanning) return;
        mBLEManager.startScanning();
        isScanning = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBLEManager.stopScanning();
                isScanning = false;
            }
        }, 10*60*1000);
    }

    private void checkGPSLock() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!layoutGPS.isLockGPS()) {
                    timerCounter.cancel();
                    for (BluetoothProfile p : mProfiles) {
                        for (GenericBluetoothProfile profile : p.getProfile()) {
                            profile.onPause();
                        }
                    }
                    for (TableView table : mListTable)
                        table.getRow().removeAllViews();
                    DialogUtils.showMessageWithoutCancel(mThis, "お知らせ", "緯度経度情報を正しく取得出来ません。GNSデバイスの状態を確認の後、再度お試しください", new DialogUtils.YesNoListener() {
                        @Override
                        public void onYes() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    deleteSession();
                                    Intent intent = new Intent(DeviceActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                        }
                    });
                }
            }
        }, 1000);
    }

    private void deleteSession() {
        if (csvhaThread != null)
            csvhaThread.release();
        if (csvnaThread != null)
            csvnaThread.release();
        if (csvsaThread != null)
            csvsaThread.release();
        if (csvgaThread != null)
            csvgaThread.release();
        if (csvtsThread != null)
            csvtsThread.release();
        if (saveCSVThread != null)
            saveCSVThread.stop();

        deleteFolder(uploadFolder);
        //deleteFolder(uploadDoneFolder);
    }

    private void deleteFolder(File directory) {
        if (directory.isDirectory())
            for (File child : directory.listFiles())
                deleteFolder(child);
        directory.delete();
        MediaScannerConnection.scanFile(this, new String[]{directory.getAbsolutePath()}, null, null);
    }

    private void initSensorStorage() {
        timerCounter = new TimerCounter(this);
        uploadFolder = FileUtil.createUploadDirectory(this);
        uploadDoneFolder = FileUtil.createUploadDoneDirectory(this);
        // Start send realtime data to server thread if not started yet
//        synchronized (Global.startThread) {
//            if (!Global.startThread) {
//                realtimeDataThread = new RealtimeSensorDataThread(this);
//                realtimeDataThread.start();
//
//                realtimeGPSThread = new RealtimeGPSDataThread(this);
//                realtimeGPSThread.start();
//
//                Global.startThread = true;
//            }
//        }

        DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        DateTime now = new DateTime();
        csvsaThread = new CSVSAThread(this, uploadFolder, fmt.print(now));
        //csvsaThread.start();

        csvgaThread = new CSVGAThread(this, uploadFolder, fmt.print(now));
        //csvgaThread.start();

        csvnaThread = new CSVNAThread(this, uploadFolder, fmt.print(now));

        csvhaThread = new CSVHAThread(this, uploadFolder, fmt.print(now));

        //csvnaThread.start();
        csvtsThread = new CSVMAThread(this, uploadFolder, fmt.print(now),this);

        //csvtsThread.setValue(Global.GPSIsAndroid, Global.accel, Global.gyros, Global.magnet, Global.gnsLatitude, Global.gnsLongitude, Global.GNSAccuracy, now, Global.androidLatitude, Global.androidLongitude, Global.AndroidAccuracy, "PC0");//now);//setValue(sensor, now);
        saveCSVThread = new SaveCSVThread(this);
        saveCSVThread.start();

        //Start send background CSV data to server thread if not started yet
        synchronized (Global.startThreadCSV) {
            if (!Global.startThreadCSV) {
                csvSensorDataThread = new CSVSensorDataThread(this,this);
                csvSensorDataThread.start();
                Global.startThreadCSV = true;
            }
        }
//        csvHelper = new CSVHelper(this,this, uploadFolder);
//        csvHelper.createCSVFile();
        //csvHelper.createCSVFileSA();
        //csvHelper.createCSVFileGA();
        //csvHelper.createCSVFileNA();
        if (!isGetGPSStart) {
            //if (Global.accel.x != 0) {
            if (slamDataThread != null)
                slamDataThread.start();
            isGetGPSStart = true;
            //}
        }
    }

    private void initSensorChart() {
        sensorValues = new ArrayList<>();
        layoutGPS = new LayoutGPS(this);
        layoutGPS.createValueView();
        layoutRoot.addView(layoutGPS);
        gpxHelper = new GPXHelper();
        gpxHelper.createGPXTrack();
        if (mGPS.size() > 0) {
            for (BleDeviceInfo ble : mGPS) {
                if (!ble.isConnected()){
                    Services.bluetooth.preferenceEnabled = false;
                    layoutGPS.setLabel("(Android GPS)");
                }
                layoutGPS.setLabel(ble.getBluetoothDevice().getName() + " (GPS)");
            }
        }else {
            Services.bluetooth.preferenceEnabled = false;
            layoutGPS.setLabel("(Android GPS)");
        }
        for (BleDevice ble : mConnectedBLEs) {
//            boolean sensorCompleteCalibrate = true;
//            for (BleDeviceInfo sensor : mDeviceInfos) {
//                if (ble.getDevice().getAddress().equals(sensor.getBluetoothDevice().getAddress()))
//                    if (sensor.getPosition() == -1){
//                        sensorCompleteCalibrate = false;
//                        break;
//                    }
//            }
//            if (!sensorCompleteCalibrate)
//                continue;
            LayoutSensor layoutSensor = new LayoutSensor(this);
            for (BleDeviceInfo sensor : mDeviceInfos)
                if (ble.getDevice().getAddress().equals(sensor.getBluetoothDevice().getAddress())) {
                    layoutSensor.setTitle(ble.getDevice().getAddress() + " (" + CommonUtils.getPosition(sensor.getPosition()) + ")");
                    mListTitle.add(new Title(layoutSensor.getTitle(), ble.getDevice().getAddress()));
                }

            layoutRoot.addView(layoutSensor);
            mListTable.add(new TableView(layoutSensor.getTable()));
            SensorValue value = new SensorValue(ble.getDevice().getAddress());
            Global.sensorValues.add(value);
        }
    }


    public void StopClickListener(View view) {
        ALog.d(TAG, "[UserTab]: BUTTON STOP");
        //goProService.changeToWifiNetwork(false);
        //Save file gpx
        saveGPXFile();
        timerCounter.cancel();
        // Start de-config sequentialy. Timeout for deconfigure is 10 seconds
        if (mHandlerWaitForStartMainActivity == null) {
            mCurrentQueueProcessing = QUEUE_IS_PROCESSING_DECONFIGURE_SEQUENCE;
            mHandlerWaitForStartMainActivity = new Handler();
            mHandlerWaitForStartMainActivity.postDelayed(mRunnableWaitForStartMainActivity, 10000);
            startDeconfigure();
        }
        csvnaThread.stop();
        csvgaThread.stop();
        csvsaThread.stop();
        csvhaThread.stop();
        if (slamDataThread != null)
            slamDataThread.release();
        if (csvtsThread != null) {
            synchronized (csvtsThread) {
                csvtsThread.stop();
                csvtsThread.release();
            }
        }
    }

    // Waiting Handler for going to MainActivity after de-config all characterisrics
    private Handler mHandlerWaitForStartMainActivity = null;
    private Runnable mRunnableWaitForStartMainActivity = new Runnable() {
        @Override
        public void run() {
            mHandlerWaitForStartMainActivity = null;
            ALog.d(TAG, "Start MainActivity");
            Intent intent = new Intent(DeviceActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    };

    public void addRowToTable(ArrayList<GenericBluetoothProfile> row, int position) {
        if (position >= mListTable.size())
            return;
        TableView table = mListTable.get(position);
        for (GenericBluetoothProfile profile : row) {
            if (!reconnect) {
                if (table.isFirst()) {
                    table.getRow().removeAllViews();
                    table.getRow().addView(profile.getTableRow());
                    table.getRow().requestLayout();
                    table.setFirst(false);
                } else {
                    if (table.getRow().indexOfChild(profile.getTableRow()) == -1) {
                        table.getRow().addView(profile.getTableRow());
                        table.getRow().requestLayout();
                    }
                }
            }
            if (profile.isConfigured != true) {
                int tmp = profile.configureService();
                if (tmp != -1) mLastConfigProfileId = tmp;
            }
            if (profile.isEnabled != true) {
                int tmp = profile.enableService();
                if (tmp != -1) mLastConfigProfileId = tmp;
            }
//            final Intent intent = new Intent(ACTION_PERIOD_UPDATE);
//            int period = 1000;
//            intent.putExtra(EXTRA_SERVICE_UUID, profile.getGenericCharacteristicTableRow().uuidLabel.getText());
//            intent.putExtra(EXTRA_PERIOD,period);
//            this.sendBroadcast(intent);
            // profile.periodWasUpdated(1000);
            profile.onResume();
        }
    }

    private void addProfileBluetooth(BluetoothDevice device, GenericBluetoothProfile bluetooth) {
        for (BluetoothProfile ble : mProfiles) {
            if (ble.getAddress().equals(device.getAddress())) {
                if (ble.getProfile().get(0) instanceof SensorTagMovementProfile) {
                    ble.getProfile().add(bluetooth);
                } else if (bluetooth instanceof SensorTagMovementProfile)
                    ble.getProfile().add(0, bluetooth);
                else
                    ble.getProfile().add(bluetooth);
                return;
            }
        }
        ArrayList<GenericBluetoothProfile> profiles = new ArrayList<>();
        profiles.add(bluetooth);
        mProfiles.add(new BluetoothProfile(device.getAddress(), profiles));

    }

    private void discoverService(BleDevice ble) {
        // set ti discovering state
        List<BluetoothGattService> services = ble.getCachedServices();
        // reset Profiles before discovery
        //clearProfiles(ble.getAddress());
        // Check that service already gotten then process from cache if exist
        if (services == null) {
            ALog.d(TAG, "[" + ble.getAddress() + "] Start discovery");
            //info.setIsDiscovering(true);
            ble.startServicesDiscovery();
        } else {
            ALog.d(TAG, "[" + ble.getAddress() + "] Process discovered " + services.size() + " services");
            uiAvailableServices(ble.getGatt(), ble.getDevice(), services);
        }
    }
    BleDevice mBlueToothSenSor;
    String currentAddress="";
    @Override
    public void uiDeviceFound(final BluetoothDevice device, int rssi, byte[] record) {
        ALog.d(TAG, "[" + device.getAddress() + "]: Device found, rssi: " + rssi);
        if (device.getName() == null)
            return;
        if (!device.getName().equals(BLE_MULTI_SENSOR_NAME))
            return;
        for (BleDevice odlBle : mConnectedBLEs)
            if (currentAddress.equals("") && device.getAddress().equals(odlBle.getAddress()) && (!currentAddress.equals(device.getAddress()) && !odlBle.isConnected())) {
                currentAddress = device.getAddress();
                mBlueToothSenSor = new BleDevice(mBLEManager, device.getAddress(), mThis, mThis);
                mBlueToothSenSor.connect();
                break;
            }
    }

    @Override
    public void uiDeviceConnected(BluetoothGatt gatt, final BluetoothDevice device) {
        ALog.d(TAG, "[" + device.getAddress() + "]: Connected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mThis, String.format("Sensor %s connected", device.getAddress()), Toast.LENGTH_SHORT).show();
            }
        });
        if (mBlueToothSenSor!= null && mBlueToothSenSor.getAddress().equals(device.getAddress())) {
            //discoverService(mBlueToothSenSor);
            mBlueToothSenSor.startServicesDiscovery();
            Iterator<BleDevice> ble = mConnectedBLEs.iterator();
            while (ble.hasNext()) {
                BleDevice d = ble.next();
                if (d.getDevice().getAddress().equals(device.getAddress())) {
                    ble.remove();
                    break;
                }
            }
            reconnect = true;
            mConnectedBLEs.add(mBlueToothSenSor);
            mBlueToothSenSor = null;
        }
    }

    @Override
    public void uiDeviceConnectTimeout(BluetoothGatt gatt, final BluetoothDevice device) {
        ALog.d(TAG, "[" + device.getAddress() + "]: Connect timeout");
        currentAddress="";
    }

    @Override
    public void uiDeviceDisconnected(BluetoothGatt gatt, final BluetoothDevice device) {
        ALog.d(TAG, "[" + device.getAddress() + "]: Disconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBLEManager.startScanning();
                Global.isConnectSensor = false;
                Global.accel = new Point3D(-1,-1,-1);
                Global.gyros = new Point3D(-1,-1,-1);
                Global.magnet = new Point3D(-1,-1,-1);
                currentAddress = "";
                for (BleDeviceInfo dConnected : mDeviceInfos)
                    if (dConnected.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        Toast.makeText(mThis, "Sensor Tag(" + device.getAddress() + ")との接続が切断されました。", Toast.LENGTH_SHORT).show();
                        dConnected.setConnected(false);
                    }
                // Update title
                for (Title title : mListTitle) {
                    if (title.getAddress().equals(device.getAddress())) {
                        String tit = title.getTitle().getText().toString();
                        title.getTitle().setText(tit + " - 切断された");
                        title.getTitle().setTextColor(ContextCompat.getColor(mThis, R.color.red));
                    }
                }
//                // Remove to list ble connected
//                Iterator<BleDevice> ble = mConnectedBLEs.iterator();
//                while (ble.hasNext()) {
//                    BleDevice d = ble.next();
//                    if (d.getDevice().getAddress().equals(device.getAddress())) {
//                        // Reconnect if connection lost
//                        if (Global.BleReconnectRequest) {
//                            d.connect();
//                            ALog.d(TAG, "[" + device.getAddress() + "]: Re-connect");
//                            Toast.makeText(mThis, String.format("Sensor %s lost. Re-connecting ...", device.getAddress()), Toast.LENGTH_SHORT);
//                        } else {
//                            ble.remove();
//                            Toast.makeText(mThis, String.format("Sensor %s Disconnected", device.getAddress()), Toast.LENGTH_SHORT).show();
//                            if (csvnaThread != null)
//                                csvnaThread.release();
//                            if (csvsaThread != null)
//                                csvsaThread.release();
//                            if (csvgaThread != null)
//                                csvgaThread.release();
//                            if (csvtsThread != null)
//                                csvtsThread.release();
//                        }
//                        break;
//                    }
//                }

                // If sensor is disconnected we'll change flag check zero thread to prepare in case re-connect sensor
                zeroCheck = false;
                zeroHandler.removeCallbacks(zeroRunnable);
            }
        });

    }

    @Override
    public void uiDeviceForceDisconnected(BluetoothGatt gatt, final BluetoothDevice device) {
        Global.isConnectSensor = false;
        Global.accel = new Point3D(-1,-1,-1);
        Global.gyros = new Point3D(-1,-1,-1);
        Global.magnet = new Point3D(-1,-1,-1);
        currentAddress = "";
        ALog.d(TAG, "[" + device.getAddress() + "]: Device FORCE disconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mBLEManager.startScanning();
                for (BleDeviceInfo dConnected : mDeviceInfos)
                    if (dConnected.getBluetoothDevice().getAddress().equals(device.getAddress())) {
                        Toast.makeText(mThis, "Sensor Tag(" + device.getAddress() + ")との接続が切断されました。", Toast.LENGTH_SHORT).show();
                        dConnected.setConnected(false);
                    }
                // Update title
                for (Title title : mListTitle) {
                    if (title.getAddress().equals(device.getAddress())) {
                        String tit = title.getTitle().getText().toString();
                        title.getTitle().setText(tit + " - 切断された");
                        title.getTitle().setTextColor(ContextCompat.getColor(mThis, R.color.red));
                    }
                }
//                Iterator<BleDevice> ble = mConnectedBLEs.iterator();
//                while (ble.hasNext()) {
//                    BleDevice d = ble.next();
//                    if (d.getDevice().getAddress().equals(device.getAddress())) {
//                        ble.remove();
//                        d.close();
//                        break;
//                    }
//                }
            }
        });

    }

    @Override
    public void uiAvailableServices(BluetoothGatt gatt, final BluetoothDevice device, final List<BluetoothGattService> services) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Title title : mListTitle) {
                    if (title.getAddress().equals(device.getAddress())) {
                        title.getTitle().setTextColor(ContextCompat.getColor(mThis, R.color.dark));
                        for (BleDeviceInfo sensor : mDeviceInfos)
                            if (device.getAddress().equals(sensor.getBluetoothDevice().getAddress())) {
                                title.getTitle().setText(device.getAddress() + " (" + CommonUtils.getPosition(sensor.getPosition()) + ")");
                            }
                    }
                }
                ALog.d(TAG, "[" + device.getAddress() + "]: Discovered services: " + services.size());
                BleDevice bleDevice = null;
                for (BleDevice ble : mConnectedBLEs)
                    if (device.getAddress().equals(ble.getDevice().getAddress()))
                        bleDevice = ble;
                if (services.size() < BLE_SERVICE) {
                    //bleDevice.startServicesDiscovery();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // DialogUtils.showAlertDialog(mThis, "Notice", "Android cannot get sensor data. Please restart sensor -> restart App then start record again, or replace sensor battery", new DialogUtils.YesNoListener() {
                            DialogUtils.showAlertDialog(mThis, "Notice", "赤色センサーからの信号を検出できません。センサーのバッテリー残量をご確認のうえ、アプリを再起動の後に再度ロギングを開始して下さい。", new DialogUtils.YesNoListener() {
                                @Override
                                public void onYes() {
                                    finish();
                                }
                            });
                        }
                    });
                    return;
                }

                // Config neccessary profiles
                for (BluetoothGattService service : services) {

                    if (SensorTagMovementProfile.isCorrectService(service)) {
                        SensorTagMovementProfile move = new SensorTagMovementProfile(mThis, device, service, bleDevice);
                        addProfileBluetooth(device, move);
                        mLastConfigProfileId = move.configureService();
                    }

                    if (SensorTagHumidityProfile.isCorrectService(service)) {
                        SensorTagHumidityProfile hum = new SensorTagHumidityProfile(mThis, device, service, bleDevice);
                        addProfileBluetooth(device, hum);
                        mLastConfigProfileId = hum.configureService();
                    }

                    if (SensorTagAmbientTemperatureProfile.isCorrectService(service)) {
                        SensorTagAmbientTemperatureProfile ambient = new SensorTagAmbientTemperatureProfile(mThis, device, service, bleDevice);
                        addProfileBluetooth(device, ambient);
                        mLastConfigProfileId = ambient.configureService();
                    }

                    if (SensorTagIRTemperatureProfile.isCorrectService(service)) {
                        SensorTagIRTemperatureProfile irt = new SensorTagIRTemperatureProfile(mThis, device, service, bleDevice);
                        addProfileBluetooth(device, irt);
                        mLastConfigProfileId = irt.configureService();
                    }

                    if (SensorTagBarometerProfile.isCorrectService(service)) {
                        SensorTagBarometerProfile barometer = new SensorTagBarometerProfile(mThis, device, service, bleDevice);
                        addProfileBluetooth(device, barometer);
                        mLastConfigProfileId = barometer.configureService();
                    }
                }
                // Enable neccessary profiles
                for (final BluetoothProfile p : mProfiles) {
                    if (p.getAddress().equals(device.getAddress())) {
                        for (int i = 0; i < mConnectedBLEs.size(); i++)
                            if (device.getAddress().equals(mConnectedBLEs.get(i).getDevice().getAddress())) {
                                addRowToTable(p.getProfile(), i);
                            }
                    }
                }
                ALog.d(TAG, "Last Configure Profile: " + mLastConfigProfileId);
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
                if (mIsStopping)
                    return;
                if (mProfiles == null)
                    return;

                //[180404 dungtv] Edit to save raw data to MA file
                Point3D accelTimestamp = new Point3D(0, 0, 0);
                Point3D gyrosTimestamp = new Point3D(0, 0, 0);
                Point3D magnetTimestamp = new Point3D(0, 0, 0);

                // Ringing when no data after 5000 milis
                if (!zeroCheck) {
                    zeroCheck = true;
                    zeroHandler.postDelayed(zeroRunnable, 5000);
                }
                for (BluetoothProfile profile : mProfiles)
                    if (profile.getAddress().equals(device.getAddress())) {
                        for (final GenericBluetoothProfile gbp : profile.getProfile()) {
                            if (ch.getUuid().equals(SensorTagGatt.UUID_MOV_DATA) && gbp instanceof SensorTagMovementProfile) {
                                if (!isChangePeriod) {
                                    isChangePeriod = true;
                                    //goProService.changeToMobileNetwork();
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            SensorTagMovementTableRow movement = (SensorTagMovementTableRow) gbp.getGenericCharacteristicTableRow();
                                            Log.e("UPDATE PERIOD", "");
                                            movement.updatePeriod(100);
                                        }
                                    }, 5000);
                                }
                                for (SensorValue sensor : sensorValues)
                                    if (sensor.getName().equals(device.getAddress())) {
                                        sensor.setAccel(Sensor.MOVEMENT_ACC.convert(ch.getValue()));
                                        //accelTimestamp = /*compensateGravity*/(Sensor.MOVEMENT_ACC.convert(ch.getValue()));
                                        sensor.setGyros(Sensor.MOVEMENT_GYRO.convert(ch.getValue()));
                                        //gyrosTimestamp = (Sensor.MOVEMENT_GYRO.convert(ch.getValue()));
                                        // [20181123    VMio] Correct by calibrator before write values, set again to sensor
                                        MagnetCalibrateCalculate calculate = getCalibCalculator(device.getAddress());
                                        if (calculate != null)
                                            sensor.setMagnet(calculate.correctMagnetic(Sensor.MOVEMENT_MAG.convert(ch.getValue())));
                                        else
                                            sensor.setMagnet(Sensor.MOVEMENT_MAG.convert(ch.getValue()));
//                                        for (MagnetCalibrateCalculate calculate : magnetCalibrateCalculators) {
//                                            if (calculate.getAddress().equals(device.getAddress())) {
//                                                Point3D oldMagnet = Sensor.MOVEMENT_MAG.convert(ch.getValue());
//                                                Point3D magnet = calculate.correctMagnetic(oldMagnet); //[180315 dungtv] use raw data for debug
//                                                sensor.setMagnet(magnet);
//                                                //Point3D magnetNormal = magnet.normalize();
//                                                //magnetTimestamp = Sensor.MOVEMENT_MAG.convert(ch.getValue());
//                                                //Log.e("Magnet", (Math.atan2(magnetNormal.x, magnetNormal.y)*180/Math.PI)+""); // swap x,y
//                                            }
//                                        }
                                        sensor.setTime(DateTime.now());
                                    }
                                lastMovement = ch.getValue();
                            }
                            if (ch.getUuid().equals(SensorTagGatt.UUID_IRT_DATA) && gbp instanceof SensorTagMovementProfile) {
                                for (SensorValue sensor : sensorValues)
                                    if (sensor.getName().equals(device.getAddress())) {
                                        Point3D temp = Sensor.IR_TEMPERATURE.convert(ch.getValue());
                                        DecimalStandardFormat dTime = new DecimalStandardFormat(".####");
                                        sensor.setObjectTemp(Double.valueOf(dTime.format(temp.z)));
                                        sensor.setAmbientTemp(Double.valueOf(dTime.format(temp.x)));
                                    }
                            }
                            if (ch.getUuid().equals(SensorTagGatt.UUID_BAR_DATA) && gbp instanceof SensorTagBarometerProfile) {
                                for (SensorValue sensor : sensorValues)
                                    if (sensor.getName().equals(device.getAddress())) {
                                        Point3D barometer = Sensor.BAROMETER.convert(ch.getValue());
                                        DecimalStandardFormat dTime = new DecimalStandardFormat(".##");
                                        if (barometer.x != 0)
                                            sensor.setPressure(Double.valueOf(dTime.format(barometer.x / 100 - 9.5))); // TODO : add offset to pressure value from sensor
                                        else
                                            sensor.setPressure(barometer.x);
                                    }
                            }

                            if (ch.getUuid().equals(SensorTagGatt.UUID_HUM_DATA) && gbp instanceof SensorTagHumidityProfile) {
                                for (SensorValue sensor : sensorValues)
                                    if (sensor.getName().equals(device.getAddress())) {
                                        Point3D humidity = Sensor.HUMIDITY2.convert(ch.getValue());
                                        DecimalStandardFormat dTime = new DecimalStandardFormat(".##");
                                        sensor.setHumidity(Double.valueOf(dTime.format(humidity.x)));
                                    }
                            }
                            SharePreference preference = new SharePreference(mThis);
                            if (connectedGoPro) {
                                SLAMTimeStamp = DateTime.now();
                                if (!isGetGPSStart) {
                                    if (Global.accel.x != 0) {
                                        if (slamDataThread != null)
                                            slamDataThread.start();
                                        isGetGPSStart = true;
                                    }
                                }

                                for (SensorValue sensor : sensorValues)
                                    if (sensor.getName().equals(preference.getSensorAttachedCamera())) {
                                        //csvHelper.saveValue("SensorTag", sensor);
                                        Global.isConnectSensor = true;
                                        Global.accel = sensor.getAccel();
                                        Global.gyros = sensor.getGyros();
                                        Global.magnet = sensor.getMagnet();
                                        DateTime now = new DateTime();

                                        if (previousTime == null || now.getSecondOfDay() - previousTime.getSecondOfDay() != 0) {
                                            sensor.setTime(now);
                                            if (sensor.getPressure() != 0 && sensor.getAmbientTemp() != 0) {
                                                csvnaThread.setValue(sensor);
                                                csvsaThread.setValue(sensor);
                                                csvgaThread.setValue(sensor, now);
                                                float accuracy = Global.GPSIsAndroid ? Global.AndroidAccuracy : Global.GNSAccuracy;
                                                csvhaThread.setValue(sensor,accuracy);
                                                previousTime = now;
                                            }
                                        }
                                    }

                                synchronized (Global.synObject) {
                                    Global.isNewSensorData = true;
                                }
                            }
                            if (gbp instanceof SensorTagMovementProfile) {
                                for (SensorValue sensor : sensorValues)
                                    if (sensor.getName().equals(device.getAddress()))
                                        ((SensorTagMovementProfile) gbp).didUpdateValueForCharacteristic(sensor);
                            } else
                                gbp.didUpdateValueForCharacteristic(ch);

                        }
                    }
             /*   boolean loadSuccess = true;
                for (SensorValue sensor : sensorValues)
                    if (!sensor.haveValue())
                        loadSuccess = false;
                *//*if (loadSuccess) {
                    dialog.dismiss();
                }*/

            }
        });


    }

    @Override
    public void uiGotNotification(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void uiSuccessfulWrite(BluetoothGatt gatt, final BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description, final int queueId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // If last profile handler of this BLE already processed then process next BLE
                checkForProcessLastHandler(device.getAddress(), queueId);
            }
        });
    }

    @Override
    public void uiFailedWrite(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic ch, String description, int queueId) {
        ALog.d(TAG, "[" + device.getAddress() + "]" + "uiFailedWrite");
    }

    @Override
    public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, int rssi) {

    }

    @Override
    public void uiDescriptorWrite(final BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, final int queueId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // If last profile handler of this BLE already processed then process next BLE
                checkForProcessLastHandler(gatt.getDevice().getAddress(), queueId);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ALog.d(TAG, "ON RESUME");
        for (BluetoothProfile p : mProfiles) {
            for (GenericBluetoothProfile profile : p.getProfile()) {
                if (profile.isConfigured != true) {
                    int tmp = profile.configureService();
                    if (tmp != -1) mLastConfigProfileId = tmp;
                }
                if (profile.isEnabled != true) {
                    int tmp = profile.enableService();
                    if (tmp != -1) mLastConfigProfileId = tmp;
                }
                profile.onResume();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void checkForProcessLastHandler(final String address, final int queueId) {
        // If last profile handler of this BLE already processed then process next BLE
        if (queueId == mLastConfigProfileId || queueId == mLastDeconfigProfileId) {
            ALog.d(TAG, "[" + address + "]: Finish handler series");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ALog.d(TAG, "[" + address + "]: " + "Go to processNext BLE");
                    mBleWaitingQueue.processNext();
                }
            }, 50);
        }
    }

    @Override
    public void onLocationChanged(@NonNull MLocation loc) {
    }

    @Override
    public void onLocationChangedPostExecute(String device) {
        updateGPS(device, Services.location.lastLoc);
    }

    boolean isGNSLost = false;

    @Override
    public void onLostLocation(String device) {
        if (Services.bluetooth.preferenceEnabled && device.equals("LocationServiceBluetooth")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isGNSLost) {
                        layoutGPS.setIconGNS(R.drawable.gps_android);
                        //Toast.makeText(mThis, "GNS 2000 : data lost", Toast.LENGTH_SHORT).show();
                        Services.bluetooth.preferenceEnabled = false;
                        layoutGPS.setLabel("(Android GPS)");
//                        Global.gnsLongitude = 0f;
//                        Global.gnsLatitude = 0f;
                        isGNSLost = true;
                    }
                }
            });
        }else if (!Services.bluetooth.preferenceEnabled && device.equals("LocationProviderAndroid")){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layoutGPS.setIconGNS(R.drawable.no_gps);
                    layoutGPS.setLabel("NO GPS");
                    layoutGPS.setLatitude(Double.NaN);
                    layoutGPS.setLongitude(Double.NaN);
                    layoutGPS.setAccuracy(Double.NaN);
                    Global.androidLongitude = 0f;
                    Global.androidLatitude = 0f;
                }
            });
        }

    }

    @Override
    public void onReceiveLocation(final String device, MLocation location) {
        if (device.equals("LocationServiceBluetooth")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layoutGPS.setIconGNS(R.drawable.gns_gps);
                    if (isGNSLost) {
                        Services.bluetooth.preferenceEnabled = true;
                        for (BleDeviceInfo ble : mGPS)
                            layoutGPS.setLabel(ble.getBluetoothDevice().getName() + " (GPS)");
                        isGNSLost = false;
                    }
                }
            });

        }else if (!Services.bluetooth.preferenceEnabled && device.equals("LocationProviderAndroid")){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isGNSLost = true;
                    layoutGPS.setIconGNS(R.drawable.gps_android);
                    layoutGPS.setLabel("(Android GPS)");
                }
            });
        }
    }

    @Override
    public void onGPSDisconnect() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( Services.bluetooth.preferenceEnabled ) {
                    layoutGPS.setIconGNS(R.drawable.no_gps);
                    layoutGPS.setLabel("(Android GPS)");
                    //Toast.makeText(mThis, "GNS 2000 : data lost", Toast.LENGTH_SHORT).show();
                    Services.bluetooth.preferenceEnabled = false;
                    isGNSLost = true;
                    Global.gnsLongitude = 0f;
                    Global.gnsLatitude = 0f;
                }
            }
        });

    }

    private void updateGPS(String device, MLocation loc) {

        DateTime _startDate = new DateTime(loc.millis);
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
        //Toast.makeText(this,  dtf.print(_startDate)+"",Toast.LENGTH_SHORT).show();
        if (!layoutGPS.isConnected())
            return;
//        if (gpxHelper.checkFirstElement()){
//            if (loc.latitude == 0 && loc.longitude ==0){
//                timerCounter.cancel();
//                DialogUtils.showMessageWithoutCancel(mThis, "Warning", "緯度経度情報を正しく取得出来ません。GNSデバイスの状態を確認の後、再度お試しください", new DialogUtils.YesNoListener() {
//                    @Override
//                    public void onYes() {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                deleteSession();
//                                Intent intent = new Intent(DeviceActivity.this, MainActivity.class);
//                                startActivity(intent);
//                                finish();
//                            }
//                        });
//                    }
//                });
//                return;
//            }
//        }
        if (loc.latitude == 0 && loc.longitude == 0) {
            layoutGPS.setLatitude(Global.latitude);
            layoutGPS.setLongitude(Global.longitude);
        } else {
            DecimalStandardFormat dTime = new DecimalStandardFormat(".##########");

            if ( Services.bluetooth.preferenceEnabled )
                Global.GPSIsAndroid = false;
            else
                Global.GPSIsAndroid = true;

            if (device.equals("LocationServiceBluetooth")) {
                Global.gnsLongitude = Double.valueOf(dTime.format(CommonUtils.Conv_DegreesToRadians(loc.longitude)));
                Global.gnsLatitude = Double.valueOf(dTime.format(CommonUtils.Conv_DegreesToRadians(loc.latitude)));
                Global.GNSAccuracy = loc.hAcc;
                // [20190226    VMio] Apply kalman filter
                GPS filteredGPS = applyKalmanFilter(kalmanGNSFilter, new GPS(Global.gnsLatitude, Global.gnsLongitude, Global.GNSAccuracy, new DateTime()));
                //Global.gnsLongitude = filteredGPS.getLon();
                //Global.gnsLatitude = filteredGPS.getLat();
                layoutGPS.setLatitude(loc.latitude);
                layoutGPS.setLongitude(loc.longitude);
                layoutGPS.setAccuracy(loc.hAcc);
                if (Services.bluetooth.preferenceEnabled ) {
                    Global.longitude = Double.valueOf(dTime.format(CommonUtils.Conv_DegreesToRadians(loc.longitude)));
                    Global.latitude = Double.valueOf(dTime.format(CommonUtils.Conv_DegreesToRadians(loc.latitude)));
                    Global.GPSAccuracy = loc.hAcc;
                    Global.GPSDateTime = new DateTime();
                }
            }else if (device.equals("LocationProviderAndroid")){
                Global.androidLongitude = Double.valueOf(dTime.format(CommonUtils.Conv_DegreesToRadians(loc.longitude)));
                Global.androidLatitude = Double.valueOf(dTime.format(CommonUtils.Conv_DegreesToRadians(loc.latitude)));
                Global.AndroidAccuracy = loc.hAcc;
                // [20190226    VMio] Apply kalman filter
                GPS filteredGPS = applyKalmanFilter(kalmanAndroidFilter, new GPS(Global.androidLatitude, Global.androidLongitude, Global.AndroidAccuracy, new DateTime()));
                //Global.androidLongitude = filteredGPS.getLon();
                //Global.androidLatitude = filteredGPS.getLat();
                if (!Services.bluetooth.preferenceEnabled ) {
                    Global.longitude = Double.valueOf(dTime.format(CommonUtils.Conv_DegreesToRadians(loc.longitude)));
                    Global.latitude = Double.valueOf(dTime.format(CommonUtils.Conv_DegreesToRadians(loc.latitude)));
                    Global.GPSAccuracy = loc.hAcc;
                    Global.GPSDateTime = new DateTime();
                    layoutGPS.setLatitude(loc.latitude);
                    layoutGPS.setLongitude(loc.longitude);
                    layoutGPS.setAccuracy(loc.hAcc);
                }
            }

        }
        gpxHelper.addWayPoint(loc);
        kmlHelper.addWayPoint(loc);
        if (loc.latitude != cur_latitude || loc.longitude != cur_longitude) {
            cur_latitude = Global.latitude;
            cur_longitude = Global.longitude;
            //csvHelper.saveValue("GPS", null);
        }

    }

    @Override
    public void onTimerCount(final String tick) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.txt_timer)).setText(tick);
            }
        });
    }

    @Override
    public void onUploadSuccess(final String fileName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mThis, "Uploaded: " + fileName, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onUploadFault(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mThis, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStartUploadZipFile() {
        csvtsThread.saveTempFile();
        csvtsThread.createTempCSV();
    }


    @Override
    public void onSaveFileMAComplete() {
        String kmlFile = kmlHelper.saveKMLFile(this);
        //mPingServiceManager.ping();
        csvSensorDataThread.sendDataCSVToServer(new SharePreference(this).getId());
        kmlHelper.createGPXTrack();
    }

    private void saveGPXFile() {
        if (layoutGPS != null && layoutGPS.isConnected())
            if (gpxHelper.saveGpxFile(mThis, uploadDoneFolder + "/" + uploadDoneFolder.getName() + ".gpx"))
                ALog.d(TAG, "GPXファイルを保存してください！");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        setBrightnessLevel(0.7f);
        Services.location.removeListener(this);
        if (mProfiles != null) {
            for (BluetoothProfile p : mProfiles) {
                for (GenericBluetoothProfile profile : p.getProfile()) {
                    profile.onPause();
                }
            }
            this.mProfiles = null;
        }
        for (TableView table : mListTable)
            table.getRow().removeAllViews();

        if (realtimeDataThread != null)
            realtimeDataThread.stop();
        if (realtimeGPSThread != null)
            realtimeGPSThread.stop();
        if (csvSensorDataThread != null) {
            csvSensorDataThread.stop();
            Global.startThreadCSV = false;
        }
        mBLEManager.stopScanning();
//        mPingServiceManager.stopService();

        //deleteSession();
        // Reset longitude and latitude
        Global.longitude = 0f;
        Global.latitude = 0f;
        Global.GPSAccuracy = 0f;
        Global.GPSDateTime = null;

        Global.androidLongitude = 0f;
        Global.androidLatitude = 0f;
        Global.AndroidAccuracy = 0f;

        Global.gnsLongitude = 0f;
        Global.gnsLatitude = 0f;
        Global.GNSAccuracy = 0f;

        zeroHandler.removeCallbacks(zeroRunnable);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideFlags();
    }

    public void hideFlags() {
        //  This work only for android 4.4+
        final View decorView = getWindow().getDecorView();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(flags);
            decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        decorView.setSystemUiVisibility(flags);
                    }
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            ALog.d(TAG, "[UserTab]: KEYCODE_BACK");
//            setBrightnessMode(Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
//            setBrightnessLevel(0.7f);
            //Save file gpx
//            saveGPXFile();
//            timerCounter.cancel();
//            // Start deconfig sequentialy then back to Main, if deconfigure timeout then return Main activity after 10 seconds
//            if (mHandlerWaitForStartMainActivity == null) {
//                mCurrentQueueProcessing = QUEUE_IS_PROCESSING_DECONFIGURE_SEQUENCE;
//                mHandlerWaitForStartMainActivity = new Handler();
//                mHandlerWaitForStartMainActivity.postDelayed(mRunnableWaitForStartMainActivity, 10000);
//                startDeconfigure();
//            }
//            ImageView imgStop = findViewById(R.id.img_stop);
//            imgStop.performClick();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // De-config all device sequentialy
    private void startDeconfigure() {
        ALog.d(TAG, "Start Deconfigurate sequence");
        mIsStopping = true;
        for (final BleDevice ble : mConnectedBLEs) {
            mBleWaitingQueue.addProcess(ble, BleWaitingQueue.WaitAction.WAIT_FOR_DECONFIG, 5000);
        }
        mBleWaitingQueue.processNext();
    }

    void deConfigureDevice(String address) {
        for (int i = 0; i < mProfiles.size(); i++) {// BluetoothProfile p : mProfiles) {
            if (mProfiles.get(i).getAddress().equals(address)) {
                for (GenericBluetoothProfile profile : mProfiles.get(i).getProfile()) {
                    profile.disableService();
                    mLastDeconfigProfileId = profile.deConfigureService();
                }
            }
        }
        ALog.d(TAG, "Last De-configure id = " + mLastDeconfigProfileId);
    }

    @Override
    public void nextDeconfigure(final BleDevice ble) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ALog.d(TAG, "[" + ble.getAddress() + "]" + " Do Deconfigure");
                deConfigureDevice(ble.getAddress());
            }
        });
    }

    // Finish function is called when BLE queue empty. Current has 2 cases of process finished:
    // 1. Finish Discover and Configure sensor when start activity
    // 2. Finish De-configure sensor when stop record and gôt Main activity
    @Override
    public void finished() {
        // When queue is empty, check current processing which is finished then process corresponding next handler
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCurrentQueueProcessing == QUEUE_IS_PROCESSING_CONFIGURE_SEQUENCE) {
                    ALog.d(TAG, "Finished PROCESSING_CONFIGURE_SEQUENCE");
                    ALog.d(TAG, "Sensor data are recording ...");
                } else if (mCurrentQueueProcessing == QUEUE_IS_PROCESSING_DECONFIGURE_SEQUENCE) {
                    ALog.d(TAG, "Finished PROCESSING_DECONFIGURE_SEQUENCE");
                    if (mHandlerWaitForStartMainActivity != null) {
                        ALog.d(TAG, "Start Handler for Start MainActivity");
                        mHandlerWaitForStartMainActivity.removeCallbacks(mRunnableWaitForStartMainActivity);
                        mHandlerWaitForStartMainActivity.postDelayed(mRunnableWaitForStartMainActivity, 10);
                    }
                }
            }
        });
    }

    @Override
    public void nextScan(BleDevice ble) {
    }

    @Override
    public void nextDiscover(final BleDevice ble) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ble.startServicesDiscovery();
            }
        });
    }

    @Override
    public void nextConnect(final BleDevice ble) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ble.connect();
            }
        });
    }

    @Override
    public void onCSVCreate(final File csvFile) {
//        if (csvSensorDataThread != null)
//            csvSensorDataThread.setCurrentCSVFile(csvFile);
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(mThis, "Create file: " + csvFile.getName(), Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    @Override
    public void onConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectedGoPro = true;
                //goProService.changeToMobileNetwork();
            }
        });
    }

    @Override
    public void onSaveToCSV() {
        SharePreference preference = new SharePreference(mThis);
        if (connectedGoPro) {
            for (SensorValue sensor : sensorValues)
                if (sensor.getName().equals(preference.getSensorAttachedCamera())) {
                    //csvHelper.saveValue("SensorTag", sensor);
                    Global.accel = sensor.getAccel();
                    Global.gyros = sensor.getGyros();
                    Global.magnet = sensor.getMagnet();
                    DateTime now = new DateTime();

                    if (previousTime == null || now.getSecondOfDay() - previousTime.getSecondOfDay() != 0) {
                        sensor.setTime(now);
                        //if (sensor.getPressure() != 0 && sensor.getAmbientTemp() != 0) {
                        csvnaThread.setValue(sensor);
                        float accuracy = Global.GPSIsAndroid ? Global.AndroidAccuracy : Global.GNSAccuracy;
                        csvhaThread.setValue(sensor,accuracy);
                        csvsaThread.setValue(sensor);
                        csvgaThread.setValue(sensor, now);
                        previousTime = now;
                        //}
                    }
                }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_slam:
                startSlam = !startSlam;
                updateStatusSLAM();
                break;
        }
    }

    private void updateStatusSLAM() {
        if (startSlam) {
            btnSlam.setText("点群解析停止");
            btnSlam.setBackgroundColor(ContextCompat.getColor(this,R.color.red));
            csvtsThread.setMarker("PC0");
            txtStatusLogger.setVisibility(View.VISIBLE);
        }else {
            btnSlam.setText("点群解析開始");
            btnSlam.setBackgroundColor(ContextCompat.getColor(this,R.color.colorPrimary));
            csvtsThread.setMarker("PC1");
            txtStatusLogger.setVisibility(View.GONE);
        }

    }

    public class SLAMDataThread extends Thread {
        boolean start = true;
        public void run() {
            while (start) {
                synchronized (csvtsThread) {
                    if (csvtsThread != null) {
                        // [20181207    VMio] Using timestamp right at data arrived
                        DateTime now = new DateTime();
                        csvtsThread.setValue(csvsaThread.getTimeTagSA(), Global.GPSIsAndroid, Global.accel, Global.gyros, Global.magnet, Global.gnsLatitude, Global.gnsLongitude, Global.GNSAccuracy, now, Global.androidLatitude, Global.androidLongitude, Global.AndroidAccuracy);//now);//setValue(sensor, now);
                    }
                }
                try {
                    sleep(Define.SLAM_COLLECT_SENSOR_INTERVAL);
                } catch (InterruptedException e) {
                    start = false;
                    e.printStackTrace();
                }
            }
        }

        public synchronized void release() {
            start = false;
        }

    }

    MagnetCalibrateCalculate getCalibCalculator(String address) {
        for (MagnetCalibrateCalculate calculator : magnetCalibrateCalculators) {
            if (calculator.getAddress().equals(address)) {
                return calculator;
            }
        }
        return null;
    }

    GPS applyKalmanFilter(KalmanFilter kf, GPS inputGPS)
    {
        kf.Process(inputGPS.getLat(), inputGPS.getLon(), inputGPS.getAccuracy(), inputGPS.getTimestamp().getMillis());
        return new GPS(kf.get_lat(), kf.get_lng(), inputGPS.getAccuracy(), inputGPS.getTimestamp());//, kalmanFilter.get_accuracy(), kalmanFilter.get_TimeStamp());
    }
}
