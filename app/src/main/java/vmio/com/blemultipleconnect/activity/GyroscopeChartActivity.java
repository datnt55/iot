package vmio.com.blemultipleconnect.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.MagnetCalibrateCalculate;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.model.BleDeviceInfo;
import vmio.com.blemultipleconnect.model.BluetoothProfile;
import vmio.com.mioblelib.ble.BleDevice;
import vmio.com.mioblelib.ble.BleWrapperUiCallbacks;
import vmio.com.mioblelib.ble.Bluetooth;
import vmio.com.mioblelib.model.SensorValue;
import vmio.com.mioblelib.service.GenericBluetoothProfile;
import vmio.com.mioblelib.service.SensorTagMovementProfile;
import vmio.com.mioblelib.utilities.SensorTagGatt;
import vmio.com.mioblelib.view.SensorTagMovementTableRow;
import vmio.com.mioblelib.widget.Point3D;
import vmio.com.mioblelib.widget.Sensor;

import static vmio.com.blemultipleconnect.activity.MainActivity.mConnectedBLEs;
import static vmio.com.blemultipleconnect.activity.MainActivity.mDeviceInfos;
import static vmio.com.blemultipleconnect.activity.SettingActivity.mBlueToothSenSor;

public class GyroscopeChartActivity extends AppCompatActivity implements BleWrapperUiCallbacks {

    String TAG = "GyroscopeChartActivity";
    public GyroscopeChartActivity mThis;
    private LinearLayout layoutRoot;
    private Bluetooth mBLEManager;
    private List<BluetoothProfile> mProfiles = new ArrayList<>();
    public boolean isChangePeriod = false;
    private ProgressDialog dialog;
    private DateTime dataTime;
    private static int TIME_OUT_GET_DATA = 2000;
    private SharePreference preference;
    private int bodyPosition = -1;
    private LineChart gyrosChart;
    private ArrayList<Point3D> listAcceleration = new ArrayList<>();
    private ArrayList<Point3D> listGyroscope = new ArrayList<>();
    private ArrayList<Point3D> listMagnitude = new ArrayList<>();
    private MagnetCalibrateCalculate mMagnetCalibrator = null;
    private boolean isCalibrate = false;
    private String address;
    private BleDevice mBlueToothSenSor;
    private CountDownTimer countDown;
    private SensorValue mSensor = null;	// [20181127] Add buffer for all sensor movement data

    public enum Chart {
        ACCELERATION,
        GYROSCOPE,
        MAGNITUDE
    }
    private Chart chartType = Chart.ACCELERATION;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyroscope_chart);
        ALog.d("APP", "================================= CALIBRATE MAGNETIC =================================");
        layoutRoot = (LinearLayout) findViewById(R.id.layout_root);
        mBLEManager = Bluetooth.getInstance(this, this);
        mBLEManager.initialize();
        mThis = this;
        preference = new SharePreference(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Calibrate Gyroscope");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        if (getIntent().hasExtra(Define.BUNDLE_CHART)) {
            address = getIntent().getStringExtra(Define.BUNDLE_CHART);
            for (BleDevice bleDevice : mConnectedBLEs) {
                if (bleDevice.getAddress().equals(address))
                    mBlueToothSenSor = bleDevice;
            }
            mBlueToothSenSor.setCallback(this);
        }
        initChart();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (listAcceleration.size() < 5){
                    if (countDown != null)
                        countDown.cancel();
                    DialogUtils.showMessageWithoutCancel(mThis, "Warning", "Sensor is not responsed. Please reset it then try again!", new DialogUtils.YesNoListener() {
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
        },5000);
    }

    private void initChart(){
        gyrosChart = (LineChart) findViewById(R.id.line_chart);
        // set an alternative background color
        gyrosChart.setBackgroundColor(Color.LTGRAY);

        gyrosChart.setTouchEnabled(false);
        gyrosChart.setDragEnabled(false);
        gyrosChart.setScaleEnabled(false);
        gyrosChart.setPinchZoom(false);
        gyrosChart.setDrawGridBackground(false);
        // chart.getLegend().setEnabled(false);
//        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) chart.getLayoutParams();
//        params.height = Global.WIDTH_SCREEN;
//        chart.setLayoutParams(params);
        gyrosChart.getDescription().setEnabled(false);
        gyrosChart.setNoDataText("");

        YAxis leftAxis = gyrosChart.getAxisLeft();
        leftAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
//        leftAxis.setAxisMaximum(300);
//        leftAxis.setAxisMinimum(300);
        //leftAxis.setYOffset(20f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(true);

        YAxis rightAxis = gyrosChart.getAxisRight();
        rightAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
//        rightAxis.setAxisMaximum(300);
//        rightAxis.setAxisMinimum(-300);
        //leftAxis.setYOffset(20f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(true);

        XAxis xAxis = gyrosChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);

        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);


        mBlueToothSenSor.startServicesDiscovery();
    }

    private void invalidateChart(Chart type){
        ArrayList<Entry> yVals1 = new ArrayList<Entry>();
        ArrayList<Entry> yVals2 = new ArrayList<Entry>();
        ArrayList<Entry> yVals3 = new ArrayList<Entry>();
        int start = 0;
        if (type == Chart.ACCELERATION) {
            getSupportActionBar().setTitle("Acceleration Chart");
            YAxis leftAxis = gyrosChart.getAxisLeft();
            leftAxis.setAxisMaximum(9);
            leftAxis.setAxisMinimum(-9);

            YAxis rightAxis = gyrosChart.getAxisRight();
            rightAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
            rightAxis.setAxisMaximum(9);
            rightAxis.setAxisMinimum(-9);
            if (listAcceleration.size() <= 0)
                return;
            if (listAcceleration.size() > 100)
                start = listAcceleration.size() - 100;
            for (int i = start; i < listAcceleration.size(); i++) {
                yVals1.add(new Entry(i, (float) listAcceleration.get(i).x));
                yVals2.add(new Entry(i, (float) listAcceleration.get(i).y));
                yVals3.add(new Entry(i, (float) listAcceleration.get(i).z));
            }
        }else if (type == Chart.GYROSCOPE) {
            getSupportActionBar().setTitle("Gyroscope Chart");
            YAxis leftAxis = gyrosChart.getAxisLeft();
            leftAxis.setAxisMaximum(300);
            leftAxis.setAxisMinimum(-300);

            YAxis rightAxis = gyrosChart.getAxisRight();
            rightAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
            rightAxis.setAxisMaximum(300);
            rightAxis.setAxisMinimum(-300);

            if (listGyroscope.size() <= 0)
                return;
            if (listGyroscope.size() > 100)
                start = listGyroscope.size() - 100;
            for (int i = start; i < listGyroscope.size(); i++) {
                yVals1.add(new Entry(i, (float) listGyroscope.get(i).x));
                yVals2.add(new Entry(i, (float) listGyroscope.get(i).y));
                yVals3.add(new Entry(i, (float) listGyroscope.get(i).z));
            }
        }else if (type == Chart.MAGNITUDE) {
            getSupportActionBar().setTitle("Magnitude Chart");
            YAxis leftAxis = gyrosChart.getAxisLeft();
            leftAxis.setAxisMaximum(255);
            leftAxis.setAxisMinimum(-255);

            YAxis rightAxis = gyrosChart.getAxisRight();
            rightAxis.removeAllLimitLines(); // reset all limit lines to avoid overlapping lines
            rightAxis.setAxisMaximum(300);
            rightAxis.setAxisMinimum(-300);

            if (listMagnitude.size() <= 0)
                return;
            if (listMagnitude.size() > 100)
                start = listMagnitude.size() - 100;
            for (int i = start; i < listMagnitude.size(); i++) {
                yVals1.add(new Entry(i, (float) listMagnitude.get(i).x));
                yVals2.add(new Entry(i, (float) listMagnitude.get(i).y));
                yVals3.add(new Entry(i, (float) listMagnitude.get(i).z));
            }
        }

        LineDataSet set1, set2, set3;


            // create a dataset and give it a type
            set1 = new LineDataSet(yVals1, "X");

            set1.setAxisDependency(YAxis.AxisDependency.LEFT);
            set1.setColor(ColorTemplate.getHoloBlue());
            set1.setCircleColor(Color.WHITE);
            set1.setLineWidth(2f);
            set1.setCircleRadius(3f);
            set1.setFillAlpha(65);
            set1.setDrawCircles(false);
            set1.setFillColor(ColorTemplate.getHoloBlue());
            set1.setHighLightColor(Color.rgb(244, 117, 117));
            set1.setDrawCircleHole(false);
            set1.setDrawFilled(true);
            //set1.setFillFormatter(new MyFillFormatter(0f));
            //set1.setDrawHorizontalHighlightIndicator(false);
            //set1.setVisible(false);
            //set1.setCircleHoleColor(Color.WHITE);

            // create a dataset and give it a type
            set2 = new LineDataSet(yVals2, "Y");
            set2.setAxisDependency(YAxis.AxisDependency.RIGHT);
            set2.setColor(Color.RED);
            //set2.setCircleColor(Color.WHITE);
            set2.setLineWidth(2f);
            set2.setCircleRadius(3f);
            set2.setFillAlpha(65);
            set2.setDrawCircles(false);
            set2.setFillColor(Color.RED);
            set2.setDrawCircleHole(false);
            set2.setDrawFilled(true);
            set2.setHighLightColor(Color.rgb(244, 117, 117));
            //set2.setFillFormatter(new MyFillFormatter(900f));

            set3 = new LineDataSet(yVals3, "Z");
            set3.setAxisDependency(YAxis.AxisDependency.RIGHT);
            set3.setColor(Color.YELLOW);
            //set3.setCircleColor(Color.WHITE);
            set3.setLineWidth(2f);
            set3.setCircleRadius(3f);
            set3.setFillAlpha(65);
            set3.setDrawCircles(false);
            set3.setFillColor(ColorTemplate.colorWithAlpha(Color.YELLOW, 200));
            set3.setDrawCircleHole(false);
            set3.setDrawFilled(true);
            set3.setHighLightColor(Color.rgb(244, 117, 117));

            // create a data object with the datasets
            LineData data = new LineData(set1, set2, set3);
            data.setValueTextColor(Color.WHITE);
            data.setValueTextSize(9f);

            // set data
            gyrosChart.setData(data);
            gyrosChart.invalidate();
    }

    private void invalidateChart(Point3D magnet){
        //invalidateChart();
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
                        // [20181127] New sensor service detected then new Calibrator
						mMagnetCalibrator = new MagnetCalibrateCalculate(mThis,device.getAddress());
                        // Get Calibrator factors from shared preference
						mMagnetCalibrator.getFactor(device.getAddress());
                        SensorTagMovementProfile move = new SensorTagMovementProfile(mThis, device, service, mBlueToothSenSor);
                        addProfileBluetooth(device, move);
                        move.configureService();
						// [20181127] New sensor values buffer
                        mSensor = new SensorValue(device.getAddress());
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
							// [20181127] Set current values to buffer
                            Point3D accel = mSensor.setAccel(Sensor.MOVEMENT_ACC.convert(ch.getValue()));
                            Point3D gyros = mSensor.setGyros(Sensor.MOVEMENT_GYRO.convert(ch.getValue()));
                            Point3D magnet = mSensor.setMagnet(Sensor.MOVEMENT_MAG.convert(ch.getValue()));
                            // [20181126    VMio] Have to clone before adding to list for avoid reference same value
                            listAcceleration.add(accel.clone());
                            listGyroscope.add(gyros.clone());
                            listMagnitude.add(mMagnetCalibrator.correctMagnetic(magnet));
                            invalidateChart(chartType);
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

    public void calibrate (View view){
        final TextView txtTimer = (TextView) findViewById(R.id.txt_timer);
        RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.layout_guide);
        relativeLayout.setVisibility(View.GONE);
        mBlueToothSenSor.startServicesDiscovery();
        countDown = new CountDownTimer(20000, 1000) {

            public void onTick(long millisUntilFinished) {
                Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/digital-7.ttf");
                txtTimer.setTypeface(custom_font);
                txtTimer.setText(CommonUtils.timerFormat(millisUntilFinished));
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                txtTimer.setText(CommonUtils.timerFormat(0));
                DialogUtils.showAlertDialog(mThis, "Notice", "Gyroscope Calibrate process is failure", new DialogUtils.YesNoListener() {
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (dialog != null && dialog.isShowing())
                return true;
            if (countDown != null)
                countDown.cancel();
            DialogUtils.showMessageDialog(this, "Notice", "Gyroscope Calibrate process is not complete. Do you want to exit?", new DialogUtils.YesNoListener() {
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

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sensor_menu, menu);
        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                DialogUtils.showMessageDialog(this, "Notice", "Gyroscope Calibrate do not complete. Do you want to exit?", new DialogUtils.YesNoListener() {
                    @Override
                    public void onYes() {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("address", mBlueToothSenSor.getDevice().getAddress());
                        setResult(Define.RESULT_CALIBRATE_MAGNET_CANCELED, returnIntent);
                        finish();
                    }
                });
                return true;
            case R.id.action_acceleration:
                chartType = Chart.ACCELERATION;
                listMagnitude.clear();
                listGyroscope.clear();
                listAcceleration.clear();
                return true;
            case R.id.action_gyroscope:
                chartType = Chart.GYROSCOPE;
                listMagnitude.clear();
                listGyroscope.clear();
                listAcceleration.clear();
                return true;
            case R.id.action_magnitude:
                chartType = Chart.MAGNITUDE;
                listMagnitude.clear();
                listGyroscope.clear();
                listAcceleration.clear();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}