package vmio.com.mioblelib.ble;

/**
 * Created by DatNT on 8/24/2017.
 */

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

public class Bluetooth {

    /* callback object through which we are returning results to the caller */
    private BleWrapperUiCallbacks mUiCallback = null;
    /* define NULL object for UI callbacks */
    private static final BleWrapperUiCallbacks NULL_CALLBACK = new BleWrapperUiCallbacks.Null();

    private static Bluetooth mInstance = null;

    private Activity mParent = null;

    private static BluetoothManager mBluetoothManager = null;
    private static BluetoothAdapter mBluetoothAdapter = null;

    /* creates BleWrapper object, set its parent activity and callback object */
    public Bluetooth(Activity parent, BleWrapperUiCallbacks callback)
    {
        this.setDependencies(parent, callback);
    }

    public static Bluetooth getInstance(Activity parent, BleWrapperUiCallbacks callback)
    {
        if(mInstance == null)
        {
            mInstance = new Bluetooth(parent, callback);
        }
        else {
            mInstance.setDependencies(parent, callback);
        }
        return mInstance;
    }

    public void setDependencies(Activity parent, BleWrapperUiCallbacks callback) {

        this.mParent = parent;
        mUiCallback = callback;
        if(mUiCallback == null) mUiCallback = NULL_CALLBACK;
    }

    public BluetoothManager getManager() { return mBluetoothManager; }
    public BluetoothAdapter getAdapter() { return mBluetoothAdapter; }

    /* run test and check if this device has BT and BLE hardware available */
    public boolean checkBleHardwareAvailable() {
        // First check general Bluetooth Hardware:
        // get BluetoothManager...
        final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
        if(manager == null) return false;
        // .. and then get vmio.com.blemultipleconnect.adapter from manager
        final BluetoothAdapter adapter = manager.getAdapter();
        if(adapter == null) return false;

        // and then check if BT LE is also available
        boolean hasBle = mParent.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        return hasBle;
    }

    /* before any action check if BT is turned ON and enabled for us
     * call this in onResume to be always sure that BT is ON when Your
     * application is put into the foreground */
    public boolean isBtEnabled() {
        final BluetoothManager manager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
        if(manager == null) return false;

        final BluetoothAdapter adapter = manager.getAdapter();
        if(adapter == null) return false;

        return adapter.isEnabled();
    }

    /* start scanning for BT LE devices around */
    public void startScanning() {
        mBluetoothAdapter.startLeScan(mDeviceFoundCallback);
    }

    /* stops current scanning */
    public void stopScanning() {
        mBluetoothAdapter.stopLeScan(mDeviceFoundCallback);
    }

    /* initialize BLE and get BT Manager & Adapter */
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mParent.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        if(mBluetoothAdapter == null) mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    public boolean isInitialized() {
        return mBluetoothManager != null && mBluetoothAdapter != null;
    }

    /* defines callback for scanning results */
    private BluetoothAdapter.LeScanCallback mDeviceFoundCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            Log.d("BLE", String.format("device Name %s, device Address %s", device.getName(), device.getAddress()));
            mUiCallback.uiDeviceFound(device, rssi, scanRecord);
        }
    };
}