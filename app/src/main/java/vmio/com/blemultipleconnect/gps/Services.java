package vmio.com.blemultipleconnect.gps;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.platypii.baseline.GPSSensorStateListener;
import com.platypii.baseline.altimeter.MyAltimeter;
import com.platypii.baseline.bluetooth.BluetoothService;
import com.platypii.baseline.location.LocationService;


/**
 * Start and stop essential services.
 * This class provides essential services intended to persist between activities.
 * This class will also keep services running if logging or audible is enabled.
 */
public class Services {
    private static final String TAG = "Services";

    // Count the number of times an activity has started.
    // This allows us to only stop services once the app is really done.
    private static int startCount = 0;
    private static boolean initialized = false;

    // How long to wait after the last activity shutdown to terminate services
    private final static Handler handler = new Handler();
    private static final int shutdownDelay = 10000;

    // Have we checked for TTS data?
    private static boolean ttsLoaded = false;

    // Services
    public static final BluetoothService bluetooth = new BluetoothService();
    public static final LocationService location = new LocationService(bluetooth);
    public static final MyAltimeter alti = location.alti;

    /**
     * We want preferences to be available as early as possible.
     * Call this in onCreate
     */
    static void create(@NonNull Activity activity) {
        if(!created) {
            Log.i(TAG, "Loading app preferences");
            loadPreferences(activity);
            created = true;
        }
    }
    private static boolean created = false;

    public static void start(@NonNull Activity activity, GPSSensorStateListener listener) {
        startCount++;
        if(!initialized) {
            initialized = true;
            final long startTime = System.currentTimeMillis();
            Log.i(TAG, "Starting services");
            final Context appContext = activity.getApplicationContext();
            handler.removeCallbacks(stopRunnable);

            // Start the various services

            Log.i(TAG, "Starting bluetooth service");
            if(bluetooth.preferenceEnabled) {
                bluetooth.start(activity);
                bluetooth.setGPSChangeListener(listener);
            }
            Log.i(TAG, "Starting location service");
            location.start(activity);
            Log.i(TAG, "Services started in " + (System.currentTimeMillis() - startTime) + " ms");
        } else if(startCount > 2) {
            bluetooth.setGPSChangeListener(listener);
            // Activity lifecycles can overlap
            Log.w(TAG, "Services started more than twice");
        } else {
            bluetooth.setGPSChangeListener(listener);
            Log.v(TAG, "Services already started");
        }
    }


    static void stop() {
        startCount--;
        if(startCount == 0) {
            Log.i(TAG, String.format("All activities have stopped. Services will stop in %.3fs", shutdownDelay * 0.001));
            handler.postDelayed(stopRunnable, shutdownDelay);
        }
    }

    /**
     * A thread that shuts down services after activity has stopped
     */
    private static final Runnable stopRunnable = new Runnable() {
        @Override
        public void run() {
            stopIfIdle();
        }
    };

    /**
     * Stop services IF nothing is using them
     */
    private static synchronized void stopIfIdle() {
        if(initialized && startCount == 0) {
            alti.stop();
            location.stop();
            bluetooth.stop();
            initialized = false;
            handler.removeCallbacks(stopRunnable);
        }
    }

    private static void loadPreferences(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Bluetooth
        bluetooth.preferenceEnabled = prefs.getBoolean("bluetooth_enabled", false);
        bluetooth.preferenceDeviceId = prefs.getString("bluetooth_device_id", null);
        bluetooth.preferenceDeviceName = prefs.getString("bluetooth_device_name", null);

    }

}
