package vmio.com.blemultipleconnect.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.DecimalStandardFormat;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.MagnetCalibrateCalculate;
import vmio.com.blemultipleconnect.Utilities.MahonyAHRS;
import vmio.com.blemultipleconnect.adapter.IPAdapter;
import vmio.com.blemultipleconnect.model.BluetoothProfile;
import vmio.com.blemultipleconnect.socket.BroadcastFinder;
import vmio.com.blemultipleconnect.socket.InterfaceActivity;
import vmio.com.blemultipleconnect.socket.SearchIPAsynTask;
import vmio.com.blemultipleconnect.socket.SocketUtils;
import vmio.com.blemultipleconnect.socket.StreamClient;
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

public class SendDataViaSocketActivity extends AppCompatActivity implements
        SearchIPAsynTask.InterfaceProgress,
        InterfaceActivity,
        IPAdapter.ItemClickListener,
        BleWrapperUiCallbacks { 
    private ArrayList<String> arrayIp = new ArrayList<>();
    private RecyclerView listIP;
    private SendDataViaSocketActivity mThis;
    private IPAdapter adapter;
    private BroadcastFinder mBroadcastFinder = null;
    private String mPeerInfo;
    private Socket clientsocket = null;
    private String message = "";
    private String ip;
    private TextView txtMyIp, txtPCIp;
    private StreamClient mstreamClient;
    private RelativeLayout layoutSeachIp, layoutSendData;
    private Button btnStart;
    String timeStampAfter;
    private SenderTask senderTask;
    private static boolean isComplete = true;
    private boolean isStartReceiveData = false;
    private Bluetooth mBLEManager;
    private List<BluetoothProfile> mProfiles = new ArrayList<>();
    public boolean isChangePeriod = false;
    private BleDevice mBlueToothSenSor;
    private String address;
	// [20181127] Add buffer and magnet Calibrator for all value for improve accuracy
    private SensorValue mSensor = null;
    private MagnetCalibrateCalculate mMagnetCalibrator = null;

    private MahonyAHRS ahrs= new MahonyAHRS(10,20,0);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_data_via_socket);
        mThis = this;
        layoutSeachIp = (RelativeLayout) findViewById(R.id.layout_search_ip);
        layoutSendData = (RelativeLayout) findViewById(R.id.layout_send_data);
        btnStart = (Button) findViewById(R.id.btn_start);
        txtMyIp = (TextView) findViewById(R.id.txtMyIp);
        txtMyIp.setText("Mine : " + getLocalIpAddress());
        txtPCIp = (TextView) findViewById(R.id.txtPcIP);
        listIP = (RecyclerView) findViewById(R.id.list_ip);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        listIP.setLayoutManager(mLayoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mThis, DividerItemDecoration.VERTICAL);
        listIP.addItemDecoration(dividerItemDecoration);

        adapter = new IPAdapter(mThis, arrayIp);
        adapter.setClickListener(this);
        listIP.setAdapter(adapter);
        SocketUtils.initWifi(this);

        mBLEManager = Bluetooth.getInstance(this, this);
        mBLEManager.initialize();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Movement Transfer");
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
        mBlueToothSenSor.startServicesDiscovery();
        Vx = 0; Vy = 0; Vz = 0;
    }

    public String getLocalIpAddress() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        @SuppressWarnings("deprecation")
        String ipAddress = Formatter.formatIpAddress(ip);
        return ipAddress;
    }

    public void searchIpListener(View v) {
        arrayIp.clear();
        adapter.notifyDataSetChanged();
        SearchIPAsynTask taskScanIP = new SearchIPAsynTask(this, this);
        taskScanIP.execute("");
    }

    public void startReceiveDataListener(View v){
        if (!isStartReceiveData) {
            if (mstreamClient == null) {
                return;
            }
            // 	Start Thread Receive From PC
            mstreamClient.receiveAsync();
            btnStart.setText("STOP");
            btnStart.setBackgroundColor(ContextCompat.getColor(mThis, R.color.red));
        }else {
            // Close socket
            if (mstreamClient != null) {
                mstreamClient.stopClient();
                mstreamClient = null;
            }
            // Reset mBroadcast
            stopFinding();
            btnStart.setText("START");
            btnStart.setBackgroundColor(ContextCompat.getColor(mThis, R.color.colorPrimary));
            layoutSeachIp.setVisibility(View.VISIBLE);
            layoutSendData.setVisibility(View.GONE);

        }
        isStartReceiveData = !isStartReceiveData;
    }

    private void stopFinding()
    {
        // Reset mBroadcast
        if(mBroadcastFinder != null) {
            if(mBroadcastFinder.isFinding())
                mBroadcastFinder.stopFindingPeer();
            mBroadcastFinder = null;
        }
    }

    private void SendData(String data){
        if (isComplete && mstreamClient !=null) {
            timeStampAfter = String.format("%d", System.currentTimeMillis());
            isComplete = false;//set isComplete = false
            mstreamClient.setFrameData(data.getBytes());		// Set data (Byte)
            mstreamClient.setTimeStamp(timeStampAfter);	// Set TimeStamp(String Id)
            senderTask = new SenderTask();
            senderTask.execute(); // Call Sender
        }
    }

    @Override
    public void doProgressPreExecute() {

    }

    @Override
    public String doProgressInBackground(String... args) {
        switch (ScanIP()) {
            case BroadcastFinder.ERROR:
                return "Error finding peer";
            case BroadcastFinder.NOT_FOUND:
                return "Not found any peers !";
            case BroadcastFinder.FOUND_PEER:
                return "Peer found !";
        }
        return "";
    }

    @Override
    public void doProgressPostExecute(String res) {
        final Toast toast = Toast.makeText(this, res, Toast.LENGTH_SHORT);
        toast.show();
    }

    public int ScanIP() {
        if (mBroadcastFinder == null) {
            mBroadcastFinder = new BroadcastFinder(this);
        }
        return mBroadcastFinder.findListenerPeer();
    }

    @Override
    public void setPeerInfo(String info) {
        mPeerInfo = info;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // mTvPeerInfo.setText(mPeerInfo);
                Boolean check = true;
                for (String ip : arrayIp) {
                    if (ip.equals(mPeerInfo))
                        check = false;
                }
                if (check)
                    arrayIp.add(mPeerInfo);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void getSocketClient(Socket clientSocket) {
        this.clientsocket = clientSocket;
    }

    @Override
    public void displayAlert(String msg) {
        message = msg;
        runOnUiThread(new Runnable() {
            @SuppressWarnings("deprecation")
            @Override
            public void run() {
                final AlertDialog alertDialog = new AlertDialog.Builder(mThis).create();
                alertDialog.setTitle("HMD Inspection");
                alertDialog.setMessage(message);
                alertDialog.setButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                SocketUtils.turnOnWifi();
                                Toast.makeText(getApplicationContext(), "Wi-Fi Enabled!", Toast.LENGTH_LONG).show();
                                alertDialog.dismiss();
                            }
                        });
                alertDialog.setButton2("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                alertDialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });
    }

    @Override
    public void display(final String msg1, String msg2, int msg3, int msg4) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(mThis,msg1,Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    @Override
    public void onItemClick(View view, int position) {
        ip = arrayIp.get(position);
        txtPCIp.setText("PC : " + ip);
        if (mstreamClient == null) {
            new ConnectTask().execute(ip);
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
                    mBlueToothSenSor.startServicesDiscovery();
                    return;
                }

                for (BluetoothGattService service : services) {

                    if (SensorTagMovementProfile.isCorrectService(service)) {
                        SensorTagMovementProfile move = new SensorTagMovementProfile(mThis, device, service, mBlueToothSenSor);
                        addProfileBluetooth(device, move);
                        move.configureService();
						// [20181127] Initialize buffer and calibrator
                        mSensor = new SensorValue(device.getAddress());
                        mMagnetCalibrator = new MagnetCalibrateCalculate(mThis, device.getAddress());
                        mMagnetCalibrator.getFactor(device.getAddress());
                    }
                }
            }
        });
    }

    @Override
    public void uiCharacteristicForService(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, List<BluetoothGattCharacteristic> chars) {

    }

    @Override
    public void uiCharacteristicsDetails(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, BluetoothGattCharacteristic characteristic) {

    }

    // Mercator coordinate: x phai, y len tren, z di ra
    // Sensor coordinate: x xuong duoi, y phai, z di ra
    double [][] EarthDir = {{1},{1},{1}};

    Date startTime = Calendar.getInstance().getTime();
    int count=0;
    @Override
    public void uiNewValueForCharacteristic(BluetoothGatt gatt, BluetoothDevice device, BluetoothGattService service, final BluetoothGattCharacteristic ch, String strValue, int intValue, byte[] rawValue, String timestamp) {
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
                            count++;
                            if((Calendar.getInstance().getTime().getTime() -startTime.getTime() )>1000){
                                ALog.d("fps","count: " + count);
                                count=0;
                                startTime= Calendar.getInstance().getTime();
                            }

                            //[180404 dungtv] Edit to send raw data
							// [20181127] buffering data and calibrate magnet
                            Point3D accel = mSensor.setAccel(Sensor.MOVEMENT_ACC.convert(ch.getValue()));
                            Point3D gyros = mSensor.setGyros(Sensor.MOVEMENT_GYRO.convert(ch.getValue()));
                            Point3D magnet = mSensor.setMagnet(mMagnetCalibrator.correctMagnetic(Sensor.MOVEMENT_MAG.convert(ch.getValue())));

                            //Log.e("UPDATE PERIOD", gyros.x +" , "+gyros.y+" , "+gyros.z);
                            //SendData(accel.toString()+";"+gyros.toString()+";"+magnet.toString());

                            {
                                // This part only for debug the sensor motion
                                //[2019/06/10 huenguyen] convert rotation matrix to eulers using mercator coordinate
                                ahrs.Update((float) gyros.x * Define.DEG2RAD_COEFF, (float) gyros.y * Define.DEG2RAD_COEFF, (float) gyros.z * Define.DEG2RAD_COEFF,
                                        (float) accel.x, (float) accel.y, (float) accel.z,
                                        (float) magnet.x, (float) magnet.y, (float) magnet.z);


                                double[][] rotMat = ahrs.GetRotationMatrix();
                                rotMat = multipleMatrix(rotMat, Define.T_WEWe);

//                                double[] euler = ahrs.GetEulersAngle();
//                                euler = new  double[]{euler[1], euler[0], euler[2]};
//                                double[][] rotMat = ahrs.EulerAnglesToRotationMatrix(euler);

//                                double[] euler = ahrs.rotationMatrixToEulerAngles(rotMat);
//                                rotMat = ahrs.EulerAnglesToRotationMatrix(euler);

                                SendData(String.format("%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f;%.06f",
                                        accel.x, accel.y, accel.z,
                                        gyros.x, gyros.y, gyros.z,
                                        magnet.x, magnet.y, magnet.z,
                                        rotMat[0][0], rotMat[0][1], rotMat[0][2],
                                        rotMat[1][0], rotMat[1][1], rotMat[1][2],
                                        rotMat[2][0], rotMat[2][1], rotMat[2][2]));
                            }
                        }
                    }
            }
        });
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

    //[20190604 huenguyen] calculate velocity by calculate integration of acceleration from t=0(v0=0)
    double Vx = 0, Vy =0, Vz = 0;
    private Point3D CalcSpeedFields(SensorValue value)
    {
            Point3D acceleration = value.getAccelMPS2();
            Vx += acceleration.x;
            Vy += acceleration.y;
            Vz += acceleration.z;
        return new Point3D(Vx,Vy,Vz);
    }

    private double[][] multipleMatrix(double[][] firstMatrix ,double[][] secondMatrix  ){
        int r1 = firstMatrix.length , c1 = firstMatrix[0].length;
        int r2 = secondMatrix.length, c2 =  secondMatrix[0].length;
        if (c1 != r2 )
            return null;
        double[][] product = new double[r1][c2];
        for(int i = 0; i < r1; i++) {
            for (int j = 0; j < c2; j++) {
                for (int k = 0; k < c1; k++) {
                    product[i][j] += firstMatrix[i][k] * secondMatrix[k][j];
                }
            }
        }
        return  product;
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
    public void uiDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, int queueId) {

    }

    @Override
    public void uiNewRssiAvailable(BluetoothGatt gatt, BluetoothDevice device, int rssi) {

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
    public void onDestroy() {
        super.onDestroy();
        for (BluetoothProfile p : mProfiles) {
            for (GenericBluetoothProfile profile : p.getProfile()) {
                profile.onPause();
            }
        }
        this.mProfiles = null;
    }

    public class ConnectTask extends AsyncTask<String, StreamClient, Boolean> {
        private  ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            progressDialog = new ProgressDialog(mThis);
            progressDialog.setMessage("Processing ...");
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String ip = params[0];
            Boolean res;
            try {
                mstreamClient = new StreamClient(mThis, InetAddress.getByAddress(SocketUtils.asBytes(ip)));
                res = mstreamClient.Connect();
            } catch (Exception ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
                return false;
            }
            return res;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Connected !
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (result) {
                // Hide listView
                layoutSeachIp.setVisibility(View.GONE);
                layoutSendData.setVisibility(View.VISIBLE);
            } else {
                final Toast toast = Toast.makeText(mThis, "Error connect to peer! Please find again", Toast.LENGTH_SHORT);
                toast.show();
            }
            return;
        }
    }

    public class SenderTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            //isComplete = false;
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
           // Log.d(TAG, frameData + "+++++++++++++++++++++++++++++++++++++++++++++ this is frame data=======");
            //mstreamClient.setTimeStamp(timeStamp);
            mstreamClient.sendFrameData();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Sent successfully
            isComplete = true;
            //Log.e(TAG, "Sent .......................................................................... ! ");
        }

    }


}
