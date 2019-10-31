package vmio.com.mioblelib.ble;

/**
 * Created by DatNT on 8/24/2017.
 */

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.util.Log;

import org.joda.time.DateTime;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;

import vmio.com.mioblelib.utilities.ALog;


public class BleDevice implements Serializable  {

    private String TAG = "BLE";
    /* defines (in milliseconds) how often RSSI should be updated */
    private static final int RSSI_UPDATE_TIME_INTERVAL = 1500; // 1.5 seconds

    /* callback object through which we are returning results to the caller */
    private BleWrapperUiCallbacks mUiCallback = null;
    /* define NULL object for UI callbacks */
    private static final BleWrapperUiCallbacks NULL_CALLBACK = new BleWrapperUiCallbacks.Null();

    private Bluetooth mInstance = null;

    private Activity mParent = null;
    private boolean mConnected = false;
    private boolean mIsConnecting = false;
    private boolean mForceDisconnect = false;
    private String mDeviceAddress = "";

    private Bluetooth mBluetooth;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice = null;
    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothGattService mBluetoothSelectedService = null;
    private List<BluetoothGattService> mBluetoothGattServices = null;

    private Handler mTimerHandler = new Handler();
    private boolean mTimerEnabled = false;
    private final int GATT_CONNECT_TIMEOUT_MS = 10000;

    /* creates BleWrapper object, set its parent activity and callback object */
    public BleDevice(Bluetooth bluetooth, String deviceAddress, Activity parent, BleWrapperUiCallbacks callback)
    {
        mBluetooth = bluetooth;
        mBluetoothManager = bluetooth.getManager();
        mBluetoothAdapter = bluetooth.getAdapter();
        mDeviceAddress = deviceAddress;
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
        this.setDependencies(parent, callback);
    }

    public void setDependencies(Activity parent, BleWrapperUiCallbacks callback) {
        this.mParent = parent;
        mUiCallback = callback;
        if(mUiCallback == null) mUiCallback = NULL_CALLBACK;
    }
    public void setCallback(BleWrapperUiCallbacks callback) {
        mUiCallback = callback;
        if(mUiCallback == null) mUiCallback = NULL_CALLBACK;
    }
    private BluetoothGattCallback getCallback() {return mBleCallback; }
    public String getAddress()  { return mDeviceAddress; }
    public BluetoothDevice getDevice()  { return mBluetoothDevice; }
    public BluetoothGatt getGatt()    { return mBluetoothGatt; }
    public BluetoothGattService getCachedService() { return mBluetoothSelectedService; }
    public List<BluetoothGattService> getCachedServices() { return mBluetoothGattServices; }
    public boolean                    isConnected() { return mConnected; }
    public boolean                    isConnecting() { return mIsConnecting; }

    public class ConnectRunable implements Runnable {

        BleDevice mBle = null;
        public ConnectRunable(BleDevice ble) {
            mBle = ble;
        }

        public void run() {
            BluetoothDevice bd = mBle.getDevice();
            if(bd != null)
                bd.connectGatt(mParent, false, mBle.getCallback());
        }
    }

    public void connectInOtherThread()
    {
        ConnectRunable cr = new ConnectRunable(this);
        Thread connectThread = new Thread(cr);
        connectThread.run();
    }

    Handler mConnectTimeoutHandler = null;
    Runnable mConnectTimeoutRunnale = new Runnable() {
        @Override
        public void run() {
            mConnectTimeoutHandler = null;
            if(!mConnected) {
                ALog.d(TAG, "[" + mDeviceAddress + "] GATT connect TIMEOUT");
                mUiCallback.uiDeviceConnectTimeout(mBluetoothGatt, mBluetoothDevice);
                // close();
                // mBluetoothGatt.close(); // Disconnect will raise onConnectionStateChange at callback
            }
        }
    };

    /* connect to the device with specified address */
    public boolean connect() {
        // check actual state from Android. If connected then just fire connected event
        if(isGattInConnectedState()) {
            ALog.d(TAG, "[" + mBluetoothGatt.getDevice().getAddress() + "] Connect in Already Connected state");
            // mBleCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);
            return true;
        }
        // check if we need to connect from scratch or just reconnect to previous device
        if(mBluetoothGatt != null && mBluetoothGatt.getDevice().getAddress().equals(mDeviceAddress)) {
            // just reconnect
            ALog.d(TAG, "[" + mBluetoothGatt.getDevice().getAddress() + "] Re-connect GATT");
            mIsConnecting = true;
            return mBluetoothGatt.connect();
        }
        else {
            // connect from address
            // get BluetoothDevice object for specified address
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
            if (mBluetoothDevice == null) {
                // we got wrong address - that device is not available!
                ALog.d(TAG, "[" + mDeviceAddress + "] connect with not exists device address");
                return false;
            }
            ALog.d(TAG, "[" + mDeviceAddress + "] GATT connecting...");
            mIsConnecting = true;
            // mBluetoothAdapter.cancelDiscovery();
            // connect with remote device
            mBluetoothGatt = mBluetoothDevice.connectGatt(mParent, false, mBleCallback);
            // clear services cached in Android
            clearSupportedServices();
            // clear services cached in BLE device
            refreshDeviceCache(mBluetoothGatt);

            // remove previous timeout handler if already added
            if(mConnectTimeoutHandler != null)mConnectTimeoutHandler.removeCallbacks(mConnectTimeoutRunnale);
            mConnectTimeoutHandler = new Handler();
            mConnectTimeoutHandler.postDelayed(mConnectTimeoutRunnale, GATT_CONNECT_TIMEOUT_MS);
        }
        return true;
    }

    /* disconnect the device. It is still possible to reconnect to it later with this Gatt client */
    public void disconnect() {
        if(mBluetoothGatt != null) mBluetoothGatt.disconnect();

      //  mUiCallback.uiDeviceDisconnected(mBluetoothGatt, mBluetoothDevice);
    }

    /* close GATT client completely */
    public void close() {
        mForceDisconnect = true;
        stopMonitoringRssiValue();
        clearSupportedServices();
        if(mBluetoothGatt != null)  {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        }
        mBluetoothGatt = null;
    }

    /* request new RSSi value for the connection*/
    public void readPeriodicalyRssiValue(final boolean repeat) {
        mTimerEnabled = repeat;
        // check if we should stop checking RSSI value
        if(mConnected == false || mBluetoothGatt == null || mTimerEnabled == false) {
            mTimerEnabled = false;
            return;
        }

        mTimerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mBluetoothGatt == null ||
                        mBluetoothAdapter == null ||
                        mConnected == false)
                {
                    mTimerEnabled = false;
                    return;
                }

                // request RSSI value
                mBluetoothGatt.readRemoteRssi();
                // add call it once more in the future
                readPeriodicalyRssiValue(mTimerEnabled);
            }
        }, RSSI_UPDATE_TIME_INTERVAL);
    }

    /* starts monitoring RSSI value */
    public void startMonitoringRssiValue() {
        readPeriodicalyRssiValue(true);
    }

    /* stops monitoring of RSSI value */
    public void stopMonitoringRssiValue() {
        readPeriodicalyRssiValue(false);
    }

    public int getServicesSize() {
        if(mBluetoothGatt != null) return mBluetoothGatt.getServices().size();
        else
            return  0;
    }

    /* request to discover all services available on the remote devices
     * results are delivered through callback object */
    public void startServicesDiscovery() {
        if(mBluetoothGatt != null) {mBluetoothGatt.discoverServices();}
    }

    /* gets services and calls UI callback to handle them
     * before calling getServices() make sure service discovery is finished! */
    public void getSupportedServices() {
        clearSupportedServices();
        // keep reference to all services in local array:
        if(mBluetoothGatt != null) mBluetoothGattServices = mBluetoothGatt.getServices();

        mUiCallback.uiAvailableServices(mBluetoothGatt, mBluetoothDevice, mBluetoothGattServices);
    }

    /* get all characteristic for particular service and pass them to the UI callback */
    public void getCharacteristicsForService(final BluetoothGattService service) {
        if(service == null) return;
        List<BluetoothGattCharacteristic> chars = null;

        chars = service.getCharacteristics();
        mUiCallback.uiCharacteristicForService(mBluetoothGatt, mBluetoothDevice, service, chars);
        // keep reference to the last selected service
        mBluetoothSelectedService = service;
    }

    /* request to fetch newest value stored on the remote device for particular characteristic */
    public void requestCharacteristicValue(BluetoothGattCharacteristic ch) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;

        mBluetoothGatt.readCharacteristic(ch);
        // new value available will be notified in Callback Object
    }

    /* get characteristic's value (and parse it for some types of characteristics)
     * before calling this You should always update the value by calling requestCharacteristicValue() */
    public void getCharacteristicValue(BluetoothGattCharacteristic ch) {
        /*
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) return;
        byte[] rawValue = ch.getValue();
        String strValue = null;
        int intValue = 0;
        // lets read and do real parsing of some characteristic to get meaningful value from it
        UUID uuid = ch.getUuid();
        if(uuid.equals(BleDefinedUUIDs.Characteristic.HEART_RATE_MEASUREMENT)) { // heart rate
        	// follow https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        	// first check format used by the device - it is specified in bit 0 and tells us if we should ask for index 1 (and uint8) or index 2 (and uint16)
        	int index = ((rawValue[0] & 0x01) == 1) ? 2 : 1;
        	// also we need to define format
        	int format = (index == 1) ? BluetoothGattCharacteristic.FORMAT_UINT8 : BluetoothGattCharacteristic.FORMAT_UINT16;
        	// now we have everything, get the value
        	intValue = ch.getIntValue(format, index);
        	strValue = intValue + " bpm"; // it is always in bpm units
        }
        else if (uuid.equals(BleDefinedUUIDs.Characteristic.HEART_RATE_MEASUREMENT) || // manufacturer name string
        		 uuid.equals(BleDefinedUUIDs.Characteristic.MODEL_NUMBER_STRING) || // model number string)
        		 uuid.equals(BleDefinedUUIDs.Characteristic.FIRMWARE_REVISION_STRING)) // firmware revision string
        {
        	// follow https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.manufacturer_name_string.xml etc.
        	// string value are usually simple utf8s string at index 0
        	strValue = ch.getStringValue(0);
        }
        else if(uuid.equals(BleDefinedUUIDs.Characteristic.APPEARANCE)) { // appearance
        	// follow: https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.gap.appearance.xml
        	intValue  = ((int)rawValue[1]) << 8;
        	intValue += rawValue[0];
        	strValue = BleNamesResolver.resolveAppearance(intValue);
        }
        else if(uuid.equals(BleDefinedUUIDs.Characteristic.BODY_SENSOR_LOCATION)) { // body sensor location
        	// follow: https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.body_sensor_location.xml
        	intValue = rawValue[0];
        	strValue = BleNamesResolver.resolveHeartRateSensorLocation(intValue);
        }
        else if(uuid.equals(BleDefinedUUIDs.Characteristic.BATTERY_LEVEL)) { // battery level
        	// follow: https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.battery_level.xml
        	intValue = rawValue[0];
        	strValue = "" + intValue + "% battery level";
        }
        else {
        	// not known type of characteristic, so we need to handle this in "general" way
        	// get first four bytes and transform it to integer
        	intValue = 0;
        	if(rawValue.length > 0) intValue = (int)rawValue[0];
        	if(rawValue.length > 1) intValue = intValue + ((int)rawValue[1] << 8);
        	if(rawValue.length > 2) intValue = intValue + ((int)rawValue[2] << 8);
        	if(rawValue.length > 3) intValue = intValue + ((int)rawValue[3] << 8);
            if (rawValue.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(rawValue.length);
                for(byte byteChar : rawValue) {
                    stringBuilder.append(String.format("%c", byteChar));
                }
                strValue = stringBuilder.toString();
            }
        }
        */
        String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(new Date());
        mUiCallback.uiNewValueForCharacteristic(mBluetoothGatt,
                mBluetoothDevice,
                mBluetoothSelectedService,
                ch,
                null,
                0,
                null,
                timestamp);
    }

    /* reads and return what what FORMAT is indicated by characteristic's properties
     * seems that value makes no sense in most cases */
    public int getValueFormat(BluetoothGattCharacteristic ch) {
        int properties = ch.getProperties();

        if((BluetoothGattCharacteristic.FORMAT_FLOAT & properties) != 0) return BluetoothGattCharacteristic.FORMAT_FLOAT;
        if((BluetoothGattCharacteristic.FORMAT_SFLOAT & properties) != 0) return BluetoothGattCharacteristic.FORMAT_SFLOAT;
        if((BluetoothGattCharacteristic.FORMAT_SINT16 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_SINT16;
        if((BluetoothGattCharacteristic.FORMAT_SINT32 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_SINT32;
        if((BluetoothGattCharacteristic.FORMAT_SINT8 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_SINT8;
        if((BluetoothGattCharacteristic.FORMAT_UINT16 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_UINT16;
        if((BluetoothGattCharacteristic.FORMAT_UINT32 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_UINT32;
        if((BluetoothGattCharacteristic.FORMAT_UINT8 & properties) != 0) return BluetoothGattCharacteristic.FORMAT_UINT8;

        return 0;
    }

    /* set new value for particular characteristic */
    public void writeDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite)
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) return;

        // first set it locally....
        ch.setValue(dataToWrite);
        // ... and then "commit" changes to the peripheral
        mBluetoothGatt.writeCharacteristic(ch);
    }

    /* enables/disables notification for characteristic */
    public void setNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled)
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;

        boolean success = mBluetoothGatt.setCharacteristicNotification(ch, enabled);
        if(!success) {
            Log.e(TAG, "Seting proper notification status for characteristic failed!");
        }

        // This is also sometimes required (e.g. for heart rate monitors) to enable notifications/indications
        // see: https://developer.bluetooth.org/gatt/descriptors/Pages/DescriptorViewer.aspx?u=org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
        BluetoothGattDescriptor descriptor = ch.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if(descriptor != null) {
            byte[] val = enabled ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            descriptor.setValue(val);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    // Query actual connection state in Android, maybe difference from app mConnected
    private boolean isGattInConnectedState()
    {
        if(mBluetoothGatt == null)
            return false;
        int connectionState = mBluetoothManager.getConnectionState(mBluetoothGatt.getDevice(), BluetoothProfile.GATT);
        return (connectionState == BluetoothProfile.STATE_CONNECTED);
    }

    private void clearSupportedServices()
    {
        if(mBluetoothGattServices != null && mBluetoothGattServices.size() > 0) mBluetoothGattServices.clear();
    }

    /**
     * Call to private Android method 'refresh'
     * This method does actually clear the cache from a bluetooth device. But the problem is that we don't have access to it. But in java we have reflection, so we can access this method.
     * http://stackoverflow.com/questions/22596951/how-to-programmatically-force-bluetooth-low-energy-service-discovery-on-android
     */
    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                Log.e(TAG, this.mDeviceAddress + ": Force sensor to refresh caches");
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    /* callbacks called for any action on particular Ble Device */
    private final BluetoothGattCallback mBleCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    ALog.d(TAG, "[" + gatt.getDevice().getAddress() + "] onConnectionStateChange[STATE_CONNECTED]");
                    mConnected = true;
                    mIsConnecting = false;
                    mForceDisconnect = false;
                    clearTxQueue();
                    mUiCallback.uiDeviceConnected(mBluetoothGatt, mBluetoothDevice);

                    // now we can start talking with the device, e.g.
                    mBluetoothGatt.readRemoteRssi();
                    // in our case we would also like automatically to call for services discovery
                    // startServicesDiscovery();

                    // and we also want to get RSSI value to be updated periodically
                    startMonitoringRssiValue();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    ALog.d(TAG, "[" + gatt.getDevice().getAddress() + "] onConnectionStateChange[STATE_DISCONNECTED]");
                    mConnected = false;
                    mIsConnecting = false;
                    if(mForceDisconnect) {
                        ALog.d(TAG, "[" + gatt.getDevice().getAddress() + "] BLE is forced closing");
                    } else {
                        mUiCallback.uiDeviceDisconnected(mBluetoothGatt, mBluetoothDevice);
                    }
                } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                    ALog.d(TAG, "[" + gatt.getDevice().getAddress() + "] onConnectionStateChange[STATE_CONNECTING]");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                    mIsConnecting = false;
                    ALog.d(TAG, "[" + gatt.getDevice().getAddress() + "] onConnectionStateChange[STATE_DISCONNECTING]");
                }
            } else {
                if(mForceDisconnect) {
                    ALog.d(TAG, "[" + gatt.getDevice().getAddress() + "][mForceDisconnect] onConnectionStateChange[NOT GATT_SUCCESS]: Status: " + status + ". New status: " + newState);
                } else {
                    ALog.d(TAG, "[" + gatt.getDevice().getAddress() + "] onConnectionStateChange[NOT GATT_SUCCESS]: Status: " + status + ". New status: " + newState);
                    mConnected = false;
                    if (mIsConnecting) {
                        mUiCallback.uiDeviceConnectTimeout(mBluetoothGatt, mBluetoothDevice);
                    } else {
                        //close();
                        mUiCallback.uiDeviceForceDisconnected(mBluetoothGatt, mBluetoothDevice);
                    }
                }
            }
        }

        @Override
        public synchronized void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                clearTxQueue();
                // now, when services discovery is finished, we can call getServices() for Gatt
                ALog.d(TAG, "[" + mDeviceAddress + "][onServicesDiscovered]" + getTxQueueItemProcessingDesc());
                getSupportedServices();
            }
        }

        @Override
        public synchronized void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                // ALog.d(TAG, "[" + gatt.getDevice().getAddress() + "]" + "RSSI = " + rssi);
                // we got new value of RSSI of the connection, pass it to the UI
                // ALog.d(TAG, "[" + mDeviceAddress + "][onReadRemoteRssi] <-- " + getTxQueueItemProcessingDesc());
                mUiCallback.uiNewRssiAvailable(mBluetoothGatt, mBluetoothDevice, rssi);
            }
        };

        @Override
        public synchronized void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            // ALog.d(TAG, "[" + mDeviceAddress + "][onCharacteristicChanged] <-- " + getTxQueueItemProcessingDesc());
            // characteristic's value was updated due to enabled notification, lets get this value
            // the value itself will be reported to the UI inside getCharacteristicValue
            getCharacteristicValue(characteristic);
            // also, notify UI that notification are enabled for particular characteristic
            mUiCallback.uiGotNotification(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic);
        }

        // we got response regarding our request to fetch characteristic value
        @Override
        public synchronized void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status)
        {
            ALog.d(TAG, "[" + mDeviceAddress + "][onCharacteristicRead] <-- " + getTxQueueItemProcessingDesc());
            // Ready for next transmission
            processTxQueue();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Succeeded, so we can get the value
                getCharacteristicValue(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            int queueId = txQueueItemProcessing.Id; // call before processTxQueue
            ALog.d(TAG, "[" + mDeviceAddress + "][onCharacteristicWrite] <-- " + getTxQueueItemProcessingDesc());
            String deviceName = gatt.getDevice().getName();
            String serviceName = BleNamesResolver.resolveServiceName(characteristic.getService().getUuid().toString().toLowerCase(Locale.getDefault()));
            String charName = BleNamesResolver.resolveCharacteristicName(characteristic.getUuid().toString().toLowerCase(Locale.getDefault()));
            String description = "Device: " + deviceName + " Service: " + serviceName + " Characteristic: " + charName;

            // Ready for next transmission
            processTxQueue();

            // we got response regarding our request to write new value to the characteristic
            // let see if it failed or not
            if(status == BluetoothGatt.GATT_SUCCESS) {
                mUiCallback.uiSuccessfulWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description, queueId);
            }
            else {
                mUiCallback.uiFailedWrite(mBluetoothGatt, mBluetoothDevice, mBluetoothSelectedService, characteristic, description + " STATUS = " + status, queueId);
            }
        };

        /**
         * Callback indicating the result of a descriptor write operation.
         *
         * @param gatt GATT client invoked {@link BluetoothGatt#writeDescriptor}
         * @param descriptor Descriptor that was written to the associated
         *                   remote device.
         * @param status The result of the write operation
         *               {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            int queueId = txQueueItemProcessing.Id;;
            ALog.d(TAG, "[" + mDeviceAddress + "][onDescriptorWrite] <-- " + getTxQueueItemProcessingDesc());

            // Ready for next transmission
            processTxQueue();
            mUiCallback.uiDescriptorWrite(gatt, descriptor, status, queueId);
        }
    };

    /* An enqueueable write operation - notification subscription or characteristic write */
    private class TxQueueItem
    {
        int Id; // unique number identified the request
        BluetoothGattCharacteristic characteristic;
        byte[] dataToWrite; // Only used for characteristic write
        boolean enabled; // Only used for characteristic notification subscription
        public TxQueueItemType type;
        DateTime timestamp; // Add to queue time
        String Desciption = "";  // For debug
    }

    /**
     * The queue of pending transmissions
     */
    private Queue<TxQueueItem> txQueue = new LinkedList<TxQueueItem>();
    private TxQueueItem txQueueItemProcessing;  // current processing item
    private boolean txQueueProcessing = false;
    private int mIncreasementNumberAsId = 0;

    private enum TxQueueItemType {
        SubscribeCharacteristic,
        ReadCharacteristic,
        WriteCharacteristic
    }

    synchronized private String getTxQueueItemProcessingDesc()
    {
        return txQueueItemProcessing == null ? "No Wait" :
                (txQueueItemProcessing.Desciption == null || txQueueItemProcessing.Desciption.isEmpty()) ? "No Desc" :
                        txQueueItemProcessing.Desciption;
    }

    /* queues enables/disables notification for characteristic */
    public int queueSetNotificationForCharacteristic(BluetoothGattCharacteristic ch, boolean enabled, String desc)
    {
        // Add to queue because shitty Android GATT stuff is only synchronous
        TxQueueItem txQueueItem = new TxQueueItem();
        txQueueItem.characteristic = ch;
        txQueueItem.enabled = enabled;
        txQueueItem.type = TxQueueItemType.SubscribeCharacteristic;
        txQueueItem.Desciption = desc;
        return addToTxQueue(txQueueItem);
    }

    /* queues enables/disables notification for characteristic */
    public int queueWriteDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite, String desc)
    {
        // Add to queue because shitty Android GATT stuff is only synchronous
        TxQueueItem txQueueItem = new TxQueueItem();
        txQueueItem.characteristic = ch;
        txQueueItem.dataToWrite = dataToWrite;
        txQueueItem.type = TxQueueItemType.WriteCharacteristic;
        txQueueItem.Desciption = desc;
        return addToTxQueue(txQueueItem);
    }

    /* request to fetch newest value stored on the remote device for particular characteristic */
    public int queueRequestCharacteristicValue(BluetoothGattCharacteristic ch, String desc) {

        // Add to queue because shitty Android GATT stuff is only synchronous
        TxQueueItem txQueueItem = new TxQueueItem();
        txQueueItem.characteristic = ch;
        txQueueItem.type = TxQueueItemType.ReadCharacteristic;
        txQueueItem.Desciption = desc;
        return addToTxQueue(txQueueItem);
    }

    /**
     * Add a transaction item to transaction queue
     * @param txQueueItem
     */
    synchronized private int addToTxQueue(TxQueueItem txQueueItem) {

        txQueueItem.Id = mIncreasementNumberAsId ++;
        txQueueItem.timestamp = DateTime.now();
        // Description is Id + Class name
        txQueueItem.Desciption = String.format("[%06d][%s]", txQueueItem.Id, txQueueItem.Desciption);
        txQueue.add(txQueueItem);

        // If there is no other transmission processing, go do this one!
        if (!txQueueProcessing) {
            processTxQueue();
        } else {
            ALog.d(TAG, "[" + mDeviceAddress + "][addToTxQueue][PENDING] --> " + txQueueItem.Desciption);
        }
        return txQueueItem.Id;
    }

    /**
     * Call when a transaction has been completed.
     * Will process next transaction if queued
     */
    synchronized private boolean processTxQueue()
    {
        if (txQueue.size() <= 0)  {
            txQueueItemProcessing = null;
            txQueueProcessing = false;
            txQueueItemProcessing = null;   // Nothing wait for response
            return true;
        }

        txQueueProcessing = true;
        TxQueueItem txQueueItem = txQueue.remove();
        switch (txQueueItem.type) {
            case WriteCharacteristic:
                writeDataToCharacteristic(txQueueItem.characteristic, txQueueItem.dataToWrite);
                ALog.d(TAG, "[" + mDeviceAddress + "][writeDataToCharacteristic] " + txQueueItem.timestamp + " --> " + txQueueItem.Desciption);
                break;
            case SubscribeCharacteristic:
                setNotificationForCharacteristic(txQueueItem.characteristic, txQueueItem.enabled);
                ALog.d(TAG, "[" + mDeviceAddress + "][SubscribeCharacteristic] " + txQueueItem.timestamp + " --> " + txQueueItem.Desciption);
                break;
            case ReadCharacteristic:
                ALog.d(TAG, "[" + mDeviceAddress + "][ReadCharacteristic] " + txQueueItem.timestamp + " --> " + txQueueItem.Desciption);
                requestCharacteristicValue(txQueueItem.characteristic);
                break;
        }
        txQueueItemProcessing = txQueueItem;
        return false;
    }

    synchronized private void clearTxQueue()
    {
        if(txQueue == null)
            return;
        txQueue.clear();
        txQueueProcessing = false;
    }
}