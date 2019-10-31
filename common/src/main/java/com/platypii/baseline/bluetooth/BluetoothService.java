package com.platypii.baseline.bluetooth;

import com.platypii.baseline.GPSSensorStateListener;
import com.platypii.baseline.Service;
import com.platypii.baseline.common.R;
import com.platypii.baseline.events.BluetoothEvent;
import com.platypii.baseline.util.Exceptions;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import org.greenrobot.eventbus.EventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Class to manage a bluetooth GPS receiver.
 * Note: instantiating this class will not automatically start bluetooth. Call startAsync to connect.
 */
public class BluetoothService implements Service {
    private static final String TAG = "Bluetooth";

    private static final int ENABLE_BLUETOOTH_CODE = 13;

    // Android shared preferences for bluetooth
    public boolean preferenceEnabled = true;
    public String preferenceDeviceId = null;
    public String preferenceDeviceName = null;

    // Bluetooth finite state machine
    public static final int BT_STOPPED = 0;
    public static final int BT_CONNECTING = 1;
    public static final int BT_CONNECTED = 2;
    public static final int BT_DISCONNECTED = 3;
    public static final int BT_STOPPING = 4;

    private static final String[] BT_STATES = {"BT_STOPPED", "BT_CONNECTING", "BT_CONNECTED", "BT_DISCONNECTED", "BT_STOPPING"};

    // Bluetooth state
    private int bluetoothState = BT_STOPPED;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothRunnable bluetoothRunnable;
    private Thread bluetoothThread;

    // Bluetooth device battery level
    public float powerLevel = Float.NaN;
    public boolean charging = false;
    private GPSSensorStateListener gpsSensorStateListener;

    final List<GpsStatus.NmeaListener> listeners = new ArrayList<>();

    @Override
    public void start(@NonNull Context context) {
        if(!(context instanceof Activity)) {
            Exceptions.report(new Exception("Bluetooth context must be an activity"));
            return;
        }
        final Activity activity = (Activity) context;
        if (bluetoothState == BT_STOPPED) {
            setState(BluetoothService.BT_CONNECTING);
            // Start bluetooth thread
            if(bluetoothRunnable != null) {
                Log.e(TAG, "Bluetooth listener thread already started");
            }
            startAsync(activity);
        } else {
           // Exceptions.report(new Exception("Bluetooth already started: " + BT_STATES[bluetoothState]));
            Log.e(TAG, "Bluetooth already started");
        }
    }

    private void startAsync(final Activity activity) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter = getAdapter(activity);
                bluetoothRunnable = new BluetoothRunnable(BluetoothService.this, bluetoothAdapter,gpsSensorStateListener);
                bluetoothThread = new Thread(bluetoothRunnable);
                bluetoothThread.start();
            }
        });
    }

    /**
     * Start the bluetooth service, and connect to gps receiver if selected
     * @return true iff bluetooth service started successfully
     */
    private BluetoothAdapter getAdapter(Activity activity) {
        // TODO: Make sure this doesn't take too long
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
            // Device not supported
            Log.e(TAG, "Bluetooth not supported");
        } else if(!bluetoothAdapter.isEnabled()) {
            // Turn on bluetooth
            // TODO: Handle result?
            final Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBluetoothIntent, ENABLE_BLUETOOTH_CODE);
        }
        return bluetoothAdapter;
    }

    public Set<BluetoothDevice> getDevices() {
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            return bluetoothAdapter.getBondedDevices();
        } else {
            Log.w(TAG, "Tried to get devices, but bluetooth is not enabled");
            return null;
        }
    }

    public int getState() {
        return bluetoothState;
    }

    void setState(int state) {
        if(bluetoothState == BT_STOPPING && state == BT_CONNECTING) {
            Log.e(TAG, "Invalid bluetooth state transition: " + BT_STATES[bluetoothState] + " -> " + BT_STATES[state]);
        }
        Log.d(TAG, "Bluetooth state: " + BT_STATES[bluetoothState] + " -> " + BT_STATES[state]);
        bluetoothState = state;
        EventBus.getDefault().post(new BluetoothEvent(bluetoothState));
    }

    /**
     * Return a human-readable string for the bluetooth state
     */
    public String getStatusMessage(Context context) {
        if(bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            if(preferenceDeviceId == null) {
                return context.getString(R.string.bluetooth_status_not_selected);
            } else {
                switch (bluetoothState) {
                    case BT_STOPPED:
                        return context.getString(R.string.bluetooth_status_stopped);
                    case BT_CONNECTING:
                        return context.getString(R.string.bluetooth_status_connecting);
                    case BT_CONNECTED:
                        return context.getString(R.string.bluetooth_status_connected);
                    case BT_DISCONNECTED:
                        return context.getString(R.string.bluetooth_status_disconnected);
                    case BT_STOPPING:
                        return context.getString(R.string.bluetooth_status_stopping);
                    default:
                        return "";
                }
            }
        } else {
            // Hardware disabled
            return context.getString(R.string.bluetooth_status_disabled);
        }
    }

    @Override
    public synchronized void stop() {
        if(bluetoothState != BT_STOPPED) {
            Log.i(TAG, "Stopping bluetooth service");
            setState(BluetoothService.BT_STOPPING);
            // Stop thread
            if (bluetoothRunnable != null) {
                bluetoothRunnable.stop();
                try {
                    bluetoothThread.join(1000);

                    // Thread is dead, clean up
                    bluetoothRunnable = null;
                    bluetoothThread = null;
                    if(bluetoothState != BluetoothService.BT_STOPPED) {
                        Log.e(TAG, "Unexpected bluetooth state: state should be STOPPED when thread has stopped");
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "Bluetooth thread interrupted while waiting for it to die", e);
                }
                Log.i(TAG, "Bluetooth service stopped");
            } else {
                Log.e(TAG, "Cannot stop bluetooth: runnable is null: " + BT_STATES[bluetoothState]);
                // Set state to stopped since it prevents getting stuck in state STOPPING
                setState(BluetoothService.BT_STOPPED);
            }
        }
    }

    public synchronized void restart(Activity activity) {
        Log.i(TAG, "Restarting bluetooth service");
        stop();
        if(bluetoothState != BT_STOPPED) {
            Log.e(TAG, "Error restarting bluetooth: not stopped: " + BT_STATES[bluetoothState]);
        }
        start(activity);
    }

    public void addNmeaListener(GpsStatus.NmeaListener nmeaListener) {
        listeners.add(nmeaListener);
    }
    public void removeNmeaListener(GpsStatus.NmeaListener nmeaListener) {
        listeners.remove(nmeaListener);
    }
    public void setGPSChangeListener(GPSSensorStateListener listener) {
        this.gpsSensorStateListener = listener;
        if (bluetoothRunnable != null)
            bluetoothRunnable.setGPSChangeListener(listener);
    }

}
