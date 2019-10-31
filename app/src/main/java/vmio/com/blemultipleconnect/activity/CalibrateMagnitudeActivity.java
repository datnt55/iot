package vmio.com.blemultipleconnect.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.EntryXComparator;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;
import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.MagnetCalibrateCalculate;
import vmio.com.blemultipleconnect.Utilities.MathUtils;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.model.BluetoothProfile;
import vmio.com.blemultipleconnect.model.SensorStore;
import vmio.com.blemultipleconnect.service.OkHttpService;
import vmio.com.mioblelib.ble.BleWrapperUiCallbacks;
import vmio.com.mioblelib.ble.Bluetooth;
import vmio.com.mioblelib.service.GenericBluetoothProfile;
import vmio.com.mioblelib.service.SensorTagMovementProfile;
import vmio.com.mioblelib.utilities.SensorTagGatt;
import vmio.com.mioblelib.view.SensorTagMovementTableRow;
import vmio.com.mioblelib.widget.Point3D;
import vmio.com.mioblelib.widget.Sensor;

import static vmio.com.blemultipleconnect.Utilities.Global.calibrationFactor;
import static vmio.com.blemultipleconnect.activity.SettingActivity.mBlueToothSenSor;

public class CalibrateMagnitudeActivity extends AppCompatActivity implements BleWrapperUiCallbacks {

    String TAG = "CalibrateMagnitudeActivity";
    public CalibrateMagnitudeActivity mThis;
    private ScrollView layoutRoot;
    private RelativeLayout layoutCompass;
    private Bluetooth mBLEManager;
    private List<BluetoothProfile> mProfiles = new ArrayList<>();
    public boolean isChangePeriod = false;
    private ProgressDialog dialog;
    private DateTime dataTime;
    private static int TIME_OUT_GET_DATA = 2000;
    private SharePreference preference;
    private boolean isEditCalibrate = false;
    private int bodyPosition = -1;
    private ScatterChart magnetXY, magnetYZ, magnetXZ;
    private ArrayList<Point3D> listMagnet = new ArrayList<>();
    private MagnetCalibrateCalculate magnetCalibrate;
    private boolean isCalibrate = false, iShowInstruction = true;
    private CountDownTimer countDown;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float[] Rr = new float[9];
    private float[] Ii = new float[9];
    private float azimuth;
    private float azimuthFix;
    private ImageView arrowView;
    private Point3D averageMagnet;
    private Point3D scalefactorMagnet;
    private double[][] sphereMatrixMagnet;
    private float currentAzimuth;
    private File csvCalibrate;
    private Button btnCompass, btnNext;
    private TextView txtShowCompass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magnitude_calibrate);
        ALog.d("APP", "================================= CALIBRATE MAGNETIC =================================");
        mThis = this;
        magnetCalibrate = new MagnetCalibrateCalculate(mThis, mBlueToothSenSor.getDevice().getAddress());
        if (!Define.DEBUG)
            getCalibrateFromServer();
        else
            getCalibrateFromLocal();
    }

    private void initComponents() {
        layoutRoot = (ScrollView) findViewById(R.id.layout_root);
        layoutCompass = (RelativeLayout) findViewById(R.id.layout_compass);
        layoutCompass.setVisibility(View.GONE);
        mBLEManager = Bluetooth.getInstance(this, this);
        mBLEManager.initialize();
        preference = new SharePreference(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("地磁気センサー較正");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        mBlueToothSenSor.setCallback(this);

        if (getIntent().hasExtra(Define.BUNDLE_EDIT_CALIBRATE)) {
            isEditCalibrate = true;
        }
        bodyPosition = getIntent().getIntExtra(Define.BUNDLE_BODY_POSITION, -1);
        magnetYZ = (ScatterChart) findViewById(R.id.magnet_y_z);
        magnetXY = (ScatterChart) findViewById(R.id.magnet_x_y);
        magnetXZ = (ScatterChart) findViewById(R.id.magnet_z_x);
        txtShowCompass = findViewById(R.id.txt_next_to_compass);
        btnCompass = findViewById(R.id.btn_compass);
        btnNext = findViewById(R.id.btn_next);
        btnNext.setVisibility(View.GONE);
        initChart(magnetXY);
        initChart(magnetYZ);
        initChart(magnetXZ);

        csvCalibrate = new File(Define.mMioTempDirectory, "magnetCSV.csv");
        if (isCalibrate) {
            isCalibrate = true;
            layoutCompass.setVisibility(View.VISIBLE);
            startCalibrateProgress();
            iShowInstruction = false;
            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.layout_guide);
            relativeLayout.setVisibility(View.GONE);
            layoutRoot.setVisibility(View.GONE);
            btnNext.setVisibility(View.VISIBLE);
            btnCompass.setText("Re-Calibrate");
        }
        btnCompass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btnCompass.getText().toString().equals("OK")) {
                    saveCalibrateListener(btnCompass);
                } else if (btnCompass.getText().toString().equals("Re-Calibrate")) {
                    layoutCompass.setVisibility(View.GONE);
                    iShowInstruction = true;
                    isCalibrate = false;
                    listMagnet.clear();
                    RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.layout_guide);
                    relativeLayout.setVisibility(View.VISIBLE);
                    layoutRoot.setVisibility(View.VISIBLE);
                }
            }
        });
        txtShowCompass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCompassView();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SensorStore sensorValues = preference.getConfigFromStorageByAddress(mProfiles.get(0).getAddress());
                if (isEditCalibrate)
                    preference.saveConfigToStorage(mBlueToothSenSor.getDevice().getName(), mBlueToothSenSor.getDevice().getAddress(), sensorValues.getDescription(), Define.STATUS_SAVE, bodyPosition, sensorValues.getAccel(), sensorValues.getGyro(), sensorValues.getMagnet());
                else
                    preference.saveConfigToStorage(mBlueToothSenSor.getDevice().getName(), mBlueToothSenSor.getDevice().getAddress(), mBlueToothSenSor.getDevice().getName(), Define.STATUS_CALIBRATE_MAGNET, bodyPosition, sensorValues.getAccel(), sensorValues.getGyro(), sensorValues.getMagnet());
                Intent returnIntent = new Intent();
                returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
                returnIntent.putExtra("editable", isEditCalibrate);
                setResult(Define.RESULT_CALIBRATE_MAGNET_OK, returnIntent);
                finish();
            }
        });
    }

    private void initChart(ScatterChart chart) {
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        // chart.getLegend().setEnabled(false);
//        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) chart.getLayoutParams();
//        params.height = Global.WIDTH_SCREEN;
//        chart.setLayoutParams(params);
        chart.getDescription().setEnabled(false);
        chart.setNoDataText("");
    }

    private void invalidateChartXY() {
        ArrayList<Entry> yVals1 = new ArrayList<Entry>();
        for (int i = 0; i < listMagnet.size(); i++) {
            yVals1.add(new Entry((float) listMagnet.get(i).x, (float) listMagnet.get(i).y));
            // [20181123    VMio] uncomment below lines for show calibrated from last calibration factors
            //Point3D drawPoint = magnetCalibrate.correctMagnetic(listMagnet.get(i));
            //yVals1.add(new Entry((float) drawPoint.x, (float) drawPoint.y));
        }
        Collections.sort(yVals1, new EntryXComparator());
        ScatterDataSet set1 = new ScatterDataSet(yVals1, "Magnetic XY");
        set1.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set1.setColor(ColorTemplate.COLORFUL_COLORS[0]);
        set1.setScatterShapeSize(8f);
        ArrayList<IScatterDataSet> dataSets = new ArrayList<IScatterDataSet>();
        dataSets.add(set1);

        ScatterData data = new ScatterData(dataSets);
        data.setDrawValues(false);
        magnetXY.setData(data);
        magnetXY.invalidate();

    }

    private void invalidateChartYZ() {
        ArrayList<Entry> yVals1 = new ArrayList<Entry>();
        for (int i = 0; i < listMagnet.size(); i++) {
            yVals1.add(new Entry((float) listMagnet.get(i).y, (float) listMagnet.get(i).z));
            // [20181123    VMio] uncomment below lines for show calibrated from last calibration factors
            //Point3D drawPoint = magnetCalibrate.correctMagnetic(listMagnet.get(i));
            //yVals1.add(new Entry((float) drawPoint.y, (float) drawPoint.z));
        }
        Collections.sort(yVals1, new EntryXComparator());
        ScatterDataSet set1 = new ScatterDataSet(yVals1, "Magnetic YZ");
        set1.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set1.setColor(ColorTemplate.COLORFUL_COLORS[2]);
        set1.setScatterShapeSize(8f);
        ArrayList<IScatterDataSet> dataSets = new ArrayList<IScatterDataSet>();
        dataSets.add(set1);

        ScatterData data = new ScatterData(dataSets);
        data.setDrawValues(false);
        magnetYZ.setData(data);
        magnetYZ.invalidate();
    }

    private void invalidateChartXZ() {
        ArrayList<Entry> yVals1 = new ArrayList<Entry>();
        for (int i = 0; i < listMagnet.size(); i++) {
            yVals1.add(new Entry((float) listMagnet.get(i).x, (float) listMagnet.get(i).z));
            // [20181123    VMio] uncomment below lines for show calibrated from last calibration factors
            //Point3D drawPoint = magnetCalibrate.correctMagnetic(listMagnet.get(i));
            //yVals1.add(new Entry((float)drawPoint.x, (float) drawPoint.z));
        }
        Collections.sort(yVals1, new EntryXComparator());
        ScatterDataSet set1 = new ScatterDataSet(yVals1, "Magnetic XZ");
        set1.setScatterShape(ScatterChart.ScatterShape.CIRCLE);
        set1.setColor(ColorTemplate.COLORFUL_COLORS[1]);
        set1.setScatterShapeSize(8f);
        ArrayList<IScatterDataSet> dataSets = new ArrayList<IScatterDataSet>();
        dataSets.add(set1);

        ScatterData data = new ScatterData(dataSets);
        data.setDrawValues(false);
        magnetXZ.setData(data);
        magnetXZ.invalidate();
    }

    private void invalidateChart(Point3D magnet) {
        // [20180905   VMio] Fix no magnet data case
        if (magnet.x == 0 && magnet.y == 0 && magnet.x == 0)
            return;
        if (isCalibrate) {
            invalidateChartXY();
            invalidateChartYZ();
            invalidateChartXZ();
            return;
        }
        saveValue(magnet);
        isCalibrate = magnetCalibrate.addData(magnet, Define.MAGNET_CALIB_MAXTIME_DATA);

        // calibrate == true mean calibration finish (enough data points and some conditions)
        if (isCalibrate) {
            if (countDown != null)
                countDown.cancel();
            ALog.d("APP", "================================= END CALIBRATE MAGNETIC =================================");

            // Calculate calib factors for saving to preferences
            double[][] SphereMatrix = null;
            calibrationFactor = magnetCalibrate.calibrate(SphereMatrix);

            if (!Define.DEBUG && CommonUtils.isNetworkConnectionAvailable(this))
            //if (CommonUtils.isNetworkConnectionAvailable(this))
                calibrateFromServer(csvCalibrate);
            else {
                magnetCalibrate.saveFactor(mBlueToothSenSor.getDevice().getAddress());
                showResultAfterCalibrate(SphereMatrix);
                btnNext.setVisibility(View.GONE);
                csvCalibrate.delete();
            }
            return;
        }
        invalidateChartXY();
        invalidateChartYZ();
        invalidateChartXZ();
    }

    private void showResultAfterCalibrate(double[][] SphereMatrix) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("較正完了");
        alertDialog.setMessage("地磁気センサーの較正が完了しました！");
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // Change from Plotter view to Compass View
                        invalidatePlotterAfterCalibrate();
                    }
                });
        alertDialog.show();
        // [20181122    VMio] replace Bias and Scales from already written class MagnetCalibrateCalculate
        averageMagnet = magnetCalibrate.GetBias(); // calculateMagnetAverage(); //
        scalefactorMagnet = magnetCalibrate.GetScales(); // calculateMagnetScale(); //
        sphereMatrixMagnet = magnetCalibrate.GetSphereMatrix(); //[20181221 dungtv] Get sphere matrix for transformation
    }

    private void invalidatePlotterAfterCalibrate() {
        txtShowCompass.setVisibility(View.VISIBLE);
        final TextView txtTimer = (TextView) findViewById(R.id.txt_timer);
        txtTimer.setVisibility(View.GONE);
        for (Point3D point : listMagnet)
            point.setPoint(magnetCalibrate.correctMagnetic(point));
        invalidateChartXY();
        invalidateChartYZ();
        invalidateChartXZ();
    }

    private Point3D calculateMagnetAverage() {
        if (listMagnet.size() <= 0)
            return new Point3D(0, 0, 0);

        Point3D sum = new Point3D(0, 0, 0);
        for (Point3D magnet : listMagnet) {
            sum.x += magnet.x;
            sum.y += magnet.y;
            sum.z += magnet.z;
        }
        sum.x /= listMagnet.size();
        sum.y /= listMagnet.size();
        sum.z /= listMagnet.size();

        return new Point3D(sum.x, sum.y, sum.z);
    }

    private Point3D calculateMagnetScale() {
        if (listMagnet.size() <= 0)
            return new Point3D(1, 1, 1);

        Point3D sum = new Point3D(0, 0, 0);
        for (Point3D magnet : listMagnet) {
            sum.x += Math.abs(magnet.x - averageMagnet.x);
            sum.y += Math.abs(magnet.y - averageMagnet.y);
            sum.z += Math.abs(magnet.z - averageMagnet.z);
        }
        sum.x /= listMagnet.size();
        sum.y /= listMagnet.size();
        sum.z /= listMagnet.size();

        double avg = (sum.x + sum.y + sum.z) / 3.0;

        return new Point3D(avg / sum.x, avg / sum.y, avg / sum.z);
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
        bluetooth.enableService();
        bluetooth.onResume();
        mProfiles.add(new BluetoothProfile(device.getAddress(), profiles));

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
                        magnetCalibrate.getFactor(device.getAddress());
                        SensorTagMovementProfile move = new SensorTagMovementProfile(mThis, device, service, mBlueToothSenSor);
                        addProfileBluetooth(device, move);
                        move.configureService();
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
    public void uiNewValueForCharacteristic(BluetoothGatt gatt, final BluetoothDevice device, BluetoothGattService service, final BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProfiles == null)
                    return;

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
                            byte[] value = ch.getValue();
                            Point3D magnet = Sensor.MOVEMENT_MAG.convert(ch.getValue());
                            // [20180905   VMio] Fix adding Point (0,0,0) when no data available
                            if (magnet.x != 0 || magnet.y != 0 || magnet.z != 0) {
                                // magnet = magnetCalibrate.correctMagnetic(magnet);
                                if (layoutRoot.getVisibility() == View.VISIBLE) {
                                    if (isCalibrate) {
                                        magnet = magnetCalibrate.correctMagnetic(magnet);
                                    }
                                    listMagnet.add(magnet);
                                    invalidateChart(magnet);
                                }
                                if (layoutCompass.getVisibility() == View.VISIBLE) {
                                    addDataCompass(ch);
                                    listMagnet.add(magnet);
                                    //invalidateChart(magnet);
                                }
                            }
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

    private void addDataCompass(BluetoothGattCharacteristic ch) {
        Point3D accel = Sensor.MOVEMENT_ACC.convert(ch.getValue());
        Point3D magnet = Sensor.MOVEMENT_MAG.convert(ch.getValue());

        if (isCalibrate) {
//            magnet.x = (magnet.x - averageMagnet.x) * scalefactorMagnet.x;
//            magnet.y = (magnet.y - averageMagnet.y) * scalefactorMagnet.y;
//            magnet.z = (magnet.z - averageMagnet.z) * scalefactorMagnet.z;
            //[20181221 dungtv] replace by function correctMagnetic() include Sphere matrix transformation
            magnet = magnetCalibrate.correctMagnetic(magnet);
        }
//        final float alpha = 0.99f;
//        mGravity[0] = (float) (alpha * mGravity[0] + (1 - alpha) * accel.x);
//        mGravity[1] = (float) (alpha * mGravity[1] + (1 - alpha) * accel.y);
//        mGravity[2] = (float) (alpha * mGravity[2] + (1 - alpha) * accel.z);
//
//        mGeomagnetic[0] = (float) (alpha * mGeomagnetic[0] + (1 - alpha) * magnet.x);
//        mGeomagnetic[1] = (float) (alpha * mGeomagnetic[1] + (1 - alpha) * magnet.y);
//        mGeomagnetic[2] = (float) (alpha * mGeomagnetic[2] + (1 - alpha) * magnet.z);
        boolean success = SensorManager.getRotationMatrix(Rr, null, mGravity, mGeomagnetic);
        success = false;    // [20181122	VMio] Alway use simple azimuth estimation, not use rotation matrix

        // convert to Mercator coordinate
//        double[][] accel_mercator = MathUtils.multipleMatrix(Define.T_WEWe, new double[][]{{accel.x},{accel.y},{accel.z}});
//        double[][] magnet_mercator = MathUtils.multipleMatrix(Define.T_WEWe, new double[][]{{magnet.x},{magnet.y},{magnet.z}});
//        accel = new Point3D(accel_mercator[0][0], accel_mercator[1][0], accel_mercator[2][0]);
//        magnet = new Point3D(magnet_mercator[0][0], magnet_mercator[1][0], magnet_mercator[2][0]);

        // [20181122	VMio] Calculate azimuth angle
        // azimuth is angle between North and Axis X of CC2650. (Now we use X of accel axis for all Accel, Magnet and Gyros.
        if (success) {
            float orientation[] = new float[3];
            SensorManager.getOrientation(Rr, orientation);
            // Log.d(TAG, "azimuth (rad): " + azimuth);
            azimuth = (float) Math.toDegrees(orientation[0]); // orientation
        } else {
            // [20181122	VMio] replace to YZ plane for real sensor position tight to camera
            // azimuth = (float)Math.toDegrees(Math.atan2(magnet.y, magnet.x));
            // azimuth = (float)Math.toDegrees(Math.atan2(magnet.z, magnet.y));
            // [20181206	VMio] change azimuth to target Z direction corresponding to North.

//            // z plane determined by vector gravity g, n is unit vector and O(0,0,0) is crossing point
//            Point3D n = new Point3D(accel.x, accel.y, -accel.z).normalize();
//
//            // project vector m onto z plane
//            Point3D m = projectOntoPlane(new Point3D(0, 0, 0), n, magnet.x, magnet.y, magnet.z);

            azimuth = (float) Math.toDegrees(Math.atan2(magnet.y, magnet.z));
            CommonUtils.printLog(String.format("Azimuth: %.02f", azimuth));
        }
        azimuth = (azimuth + azimuthFix + 180 + 360) % 360;

        // [20181122	VMio] Set azimuth angle to arraw. Angle between arraw and up-right vector of Android screen reflected the Azimuth angle
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adjustArrow(azimuth);
                //((TextView) findViewById(R.id.txt_degree)).setText(azimuth + "°");
            }
        });
    }

    /// <summary>
    /// Project a point P onto a plane defined by normal unit vector n and crossing point P0
    /// </summary>
    /// <param name="P0">a point lie on this plane </param>
    /// <param name="n">normal unit vector</param>
    /// <param name="P">vector for project</param>
    /// <returns>projected vector of v on plane</returns>
    public Point3D projectOntoPlane(Point3D P0, Point3D n, double x, double y, double z)
    {
        Point3D v = new Point3D(x - P0.x, y - P0.y, z - P0.z);
        double dist = n.dot(v);

        return new Point3D(x - dist * n.x, y - dist * n.y, z - dist * n.z);
    }

    private void adjustArrow(float azimuth) {
        Log.e("degree", azimuth + "");
        if (currentAzimuth - azimuth > 180)
            azimuth += 360;
        else if (currentAzimuth - azimuth < -180)
            azimuth -= 360;
        arrowView = (ImageView) findViewById(R.id.main_image_dial);
        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = azimuth;

        an.setDuration(50);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        arrowView.startAnimation(an);
    }

    public void calibrate(View view) {
        startCalibrateProgress();
        iShowInstruction = false;
        final TextView txtTimer = (TextView) findViewById(R.id.txt_timer);
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.layout_guide);
        relativeLayout.setVisibility(View.GONE);
        countDown = new CountDownTimer(Define.MAGNET_CALIB_MAXTIME_MILISEC, 1000) {

            public void onTick(long millisUntilFinished) {
                Typeface custom_font = Typeface.createFromAsset(getAssets(), "fonts/digital-7.ttf");
                txtTimer.setTypeface(custom_font);
                txtTimer.setText(CommonUtils.timerFormat(millisUntilFinished));
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                txtTimer.setText(CommonUtils.timerFormat(0));
                DialogUtils.showAlertDialog(mThis, "お知らせ", "地磁気センサー較正に失敗しました。", new DialogUtils.YesNoListener() {
                    @Override
                    public void onYes() {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
                        setResult(Define.RESULT_CALIBRATE_MAGNET_CANCELED, returnIntent);
                        finish();
                    }
                });
            }

        }.start();
    }

    private void startCalibrateProgress() {
        if (listMagnet.size() > 0)
            listMagnet.clear();
        else
            mBlueToothSenSor.startServicesDiscovery();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (listMagnet.size() < 5) {
                    if (countDown != null)
                        countDown.cancel();
                    DialogUtils.showMessageWithoutCancel(mThis, "お知らせ", "センサーが応答していません。センサーをリセットしてから再度お試しください。", new DialogUtils.YesNoListener() {
                        @Override
                        public void onYes() {
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
                            setResult(Define.RESULT_CALIBRATE_MAGNET_CANCELED, returnIntent);
                            finish();
                        }
                    });
                }
            }
        }, 5000);
    }

    public void showCompassView() {
        btnCompass.setText("OK");
        layoutCompass.setVisibility(View.VISIBLE);
        layoutRoot.setVisibility(View.GONE);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDown != null)
            countDown.cancel();
        for (BluetoothProfile p : mProfiles) {
            for (GenericBluetoothProfile profile : p.getProfile()) {
                profile.onPause();
            }
        }
        this.mProfiles = null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!iShowInstruction) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (dialog != null && dialog.isShowing())
                    return true;
                if (countDown != null)
                    countDown.cancel();
                DialogUtils.showMessageDialog(this, "お知らせ", "地磁気センサー較正は完了していません。較正を終了しますか？", new DialogUtils.YesNoListener() {
                    @Override
                    public void onYes() {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
                        setResult(Define.RESULT_CALIBRATE_MAGNET_CANCELED, returnIntent);
                        finish();
                    }
                });
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                DialogUtils.showMessageDialog(this, "お知らせ", "地磁気センサー較正は完了していません。較正を終了しますか？", new DialogUtils.YesNoListener() {
                    @Override
                    public void onYes() {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
                        setResult(Define.RESULT_CALIBRATE_MAGNET_CANCELED, returnIntent);
                        finish();
                    }
                });
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void saveCalibrateListener(View view) {
        // [20180905    VMio] Set compass view gone for not process compass data
        layoutCompass.setVisibility(View.GONE);

        magnetCalibrate.saveFactor(mProfiles.get(0).getAddress());
        SensorStore sensorValues = preference.getConfigFromStorageByAddress(mProfiles.get(0).getAddress());
        if (isEditCalibrate)
            preference.saveConfigToStorage(mBlueToothSenSor.getDevice().getName(), mBlueToothSenSor.getDevice().getAddress(), sensorValues.getDescription(), Define.STATUS_SAVE, bodyPosition, sensorValues.getAccel(), sensorValues.getGyro(), sensorValues.getMagnet());
        else
            preference.saveConfigToStorage(mBlueToothSenSor.getDevice().getName(), mBlueToothSenSor.getDevice().getAddress(), mBlueToothSenSor.getDevice().getName(), Define.STATUS_CALIBRATE_MAGNET, bodyPosition, sensorValues.getAccel(), sensorValues.getGyro(), sensorValues.getMagnet());
        Intent returnIntent = new Intent();
        returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
        returnIntent.putExtra("editable", isEditCalibrate);
        setResult(Define.RESULT_CALIBRATE_MAGNET_OK, returnIntent);
        finish();
    }

    public void saveValue(Point3D point3D) {
        try {
            FileWriter writer = new FileWriter(csvCalibrate, true);
            writer.append("" + point3D.x);
            writer.append(',');
            writer.append("" + point3D.y);
            writer.append(',');
            writer.append("" + point3D.z);
            writer.append('\n');
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void calibrateFromServer(File file) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Calibrating ...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Map<String, Object> params = new HashMap<>();
        params.put("myFile", file);
        params.put("cc2650MacAddr", mBlueToothSenSor.getDevice().getAddress() + ","+magnetCalibrate.biasAndScaleToString());
        new OkHttpService(OkHttpService.Method.POST, this, Define.URL_CALIBRATE, params, false) {
            @Override
            public void onFailureApi(Call call, Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        csvCalibrate.delete();
                        showResultAfterCalibrate(null);
                        DialogUtils.showAlertDialog(mThis, "お知らせ", "地磁気センサー較正に失敗しました。", new DialogUtils.YesNoListener() {
                            @Override
                            public void onYes() {
                                Intent returnIntent = new Intent();
                                returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
                                setResult(Define.RESULT_CALIBRATE_MAGNET_CANCELED, returnIntent);
                                finish();
                            }
                        });
                    }
                });

            }

            @Override
            public void onResponseApi(Call call, Response response) throws IOException {

                String result = response.body().string();
                csvCalibrate.delete();
                if (!result.contains("error")) {
                    result = result.replace("\"", "");
                    String[] factor = result.split(",");
                    final double[][] saveFactor = new double[5][3];
                    saveFactor[0][0] = Double.valueOf(factor[0]);
                    saveFactor[0][1] = Double.valueOf(factor[1]);
                    saveFactor[0][2] = Double.valueOf(factor[2]);

                    saveFactor[1][0] = Double.valueOf(factor[3]);
                    saveFactor[1][1] = Double.valueOf(factor[4]);
                    saveFactor[1][2] = Double.valueOf(factor[5]);

                    saveFactor[2][0] = Double.valueOf(factor[6]);
                    saveFactor[2][1] = Double.valueOf(factor[7]);
                    saveFactor[2][2] = Double.valueOf(factor[8]);

                    saveFactor[3][0] = Double.valueOf(factor[9]);
                    saveFactor[3][1] = Double.valueOf(factor[10]);
                    saveFactor[3][2] = Double.valueOf(factor[11]);

                    saveFactor[4][0] = Double.valueOf(factor[12]);
                    saveFactor[4][1] = Double.valueOf(factor[13]);
                    saveFactor[4][2] = Double.valueOf(factor[14]);
                    magnetCalibrate.setFactorFromServer(saveFactor);
                    magnetCalibrate.saveFactor(mBlueToothSenSor.getDevice().getAddress());
                    runOnUiThread(new Runnable() {
                                      @Override
                                      public void run() {
                                          progressDialog.dismiss();
                                          btnNext.setVisibility(View.GONE);
                                          showResultAfterCalibrate(saveFactor);
                                      }
                                  }
                    );
                }
            }
        };
    }

    private void getCalibrateFromLocal() {
        SharePreference preference = new SharePreference(this);
        if (preference.getCalibrateMagnet(mBlueToothSenSor.getDevice().getAddress()) == null){
            isCalibrate = false;
        }else {
            magnetCalibrate.setFactorFromServer(preference.getCalibrateMagnet(mBlueToothSenSor.getDevice().getAddress()));
            isCalibrate = true;
        }
        initComponents();
    }

    private void getCalibrateFromServer() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Processing ...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Map<String, Object> params = new HashMap<>();
        params.put("cc2650MacAddr", mBlueToothSenSor.getDevice().getAddress());

        new OkHttpService(OkHttpService.Method.GET, this, Define.URL_GET_CALIBRATE, params, false) {
            @Override
            public void onFailureApi(Call call, Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        //getCalibrateFromLocal();
                        initComponents();
                    }
                });


            }

            @Override
            public void onResponseApi(Call call, Response response) throws IOException {

                String result = response.body().string();
                result = result.replace("\"", "");
                if (result.equals("Not Found")) {
                    isCalibrate = false;
                } else {
                    isCalibrate = true;
                    String[] factor = result.split(",");
                    final double[][] saveFactor = new double[5][3];
                    saveFactor[0][0] = Double.valueOf(factor[0]);
                    saveFactor[0][1] = Double.valueOf(factor[1]);
                    saveFactor[0][2] = Double.valueOf(factor[2]);

                    saveFactor[1][0] = Double.valueOf(factor[3]);
                    saveFactor[1][1] = Double.valueOf(factor[4]);
                    saveFactor[1][2] = Double.valueOf(factor[5]);

                    saveFactor[2][0] = Double.valueOf(factor[6]);
                    saveFactor[2][1] = Double.valueOf(factor[7]);
                    saveFactor[2][2] = Double.valueOf(factor[8]);

                    saveFactor[3][0] = Double.valueOf(factor[9]);
                    saveFactor[3][1] = Double.valueOf(factor[10]);
                    saveFactor[3][2] = Double.valueOf(factor[11]);

                    saveFactor[4][0] = Double.valueOf(factor[12]);
                    saveFactor[4][1] = Double.valueOf(factor[13]);
                    saveFactor[4][2] = Double.valueOf(factor[14]);

                    SharePreference preference = new SharePreference(mThis);
                    if (preference.getCalibrateMagnet(mBlueToothSenSor.getDevice().getAddress()) == null){
                        magnetCalibrate.setFactorFromServer(saveFactor);
                        magnetCalibrate.saveFactor(mBlueToothSenSor.getDevice().getAddress());
                    }else {
                        magnetCalibrate.setFactorFromServer(preference.getCalibrateMagnet(mBlueToothSenSor.getDevice().getAddress()));
                        magnetCalibrate.setSphereMatrix(saveFactor);
                        magnetCalibrate.saveFactor(mBlueToothSenSor.getDevice().getAddress());
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        initComponents();
                    }
                });

            }
        };
    }
}

