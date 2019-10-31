package com.platypii.baseline.location;

import com.platypii.baseline.Service;
import com.platypii.baseline.measurements.MLocation;
import com.platypii.baseline.util.Numbers;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class LocationProvider implements Service {
    // Duration until location considered stale, in milliseconds
    private static final long LOCATION_TTL = 10000;

    // Listeners
    private final List<MyLocationListener> listeners = new CopyOnWriteArrayList<>();

    // GPS status
    // TODO: Include time from last sample until now if > refreshTime
    public float refreshRate = 0; // Moving average of refresh rate in Hz

    // History
    public MLocation lastLoc; // last location received
    private MLocation prevLoc; // 2nd to last

    /**
     * Give a useful name to the inherited provider
     */
    protected abstract String providerName();

    /**
     * Start location updates
     * @param context The Application context
     */
    @Override
    public abstract void start(@NonNull Context context);

    /**
     * Returns the number of milliseconds since the last fix
     */
    public long lastFixDuration() {
        if(lastLoc != null && lastLoc.millis > 0) {
            final long duration = System.currentTimeMillis() - (lastLoc.millis + TimeOffset.phoneOffsetMillis);
            if(duration < 0) {
                Log.w(providerName(), "Time since last fix should never be negative");
            }
            return duration;
        } else {
            return -1;
        }
    }

    /**
     * Returns whether the last location fix is recent
     */
    public boolean isFresh() {
        return lastLoc != null && lastFixDuration() < LOCATION_TTL;
    }

    /**
     * Add a new listener to be notified of location updates
     */
    public void addListener(MyLocationListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener from location updates
     */
    public void removeListener(MyLocationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Children should call updateLocation() when they have new location information
     */
    void updateLocation(MLocation loc, final String device) {
        // Log.v(providerName(), "MyLocationManager.updateLocation(" + loc + ")");
        if (loc == null){
            for(MyLocationListener listener : listeners) {
                listener.onLostLocation(device);
            }
            return;
        }else {
            for (MyLocationListener listener : listeners) {
                listener.onReceiveLocation(device, loc);
            }
        }

        // Store location
        prevLoc = lastLoc;
        lastLoc = loc;

        // Update gps time offset
        final long clockOffset = System.currentTimeMillis() - lastLoc.millis;
        if (Math.abs(TimeOffset.phoneOffsetMillis - clockOffset) > 1000) {
            if (clockOffset < 0) {
                Log.w(providerName(), "Adjusting clock: phone behind gps by " + (-clockOffset) + "ms");
            } else {
                Log.w(providerName(), "Adjusting clock: phone ahead of gps by " + clockOffset + "ms");
            }
        }
        TimeOffset.phoneOffsetMillis = clockOffset;

        if (prevLoc != null) {
            final long deltaTime = lastLoc.millis - prevLoc.millis; // time since last refresh

            // GPS sample refresh rate
            if (deltaTime > 0) {
                final float newRefreshRate = 1000f / deltaTime; // Refresh rate based on last 2 samples
                if (refreshRate == 0) {
                    refreshRate = newRefreshRate;
                } else {
                    refreshRate += (newRefreshRate - refreshRate) * 0.5f; // Moving average
                }
                if (Double.isNaN(refreshRate)) {
                    Log.e(providerName(), "Refresh rate is NaN, deltaTime = " + deltaTime + " refreshTime = " + newRefreshRate);
                    refreshRate = 0;
                }
            }
        }

        // Notify listeners (using AsyncTask so the manager never blocks!)
        new AsyncTask<MLocation, Void, Void>() {
            @Override
            protected Void doInBackground(MLocation... params) {
                for (MyLocationListener listener : listeners) {
                    listener.onLocationChanged(params[0]);
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void result) {
                for(MyLocationListener listener : listeners) {
                    listener.onLocationChangedPostExecute(device);
                }
            }
        }.execute(lastLoc);
    }

    /**
     * Helper method for getting latest speed in m/s
     * If GPS is not giving us speed natively, fallback to computing from v = dist/time.
     * This should be used for display of the latest groundspeed, but not for logging.
     */
    public double groundSpeed() {
        if(isFresh()) {
            final double lastGroundSpeed = lastLoc.groundSpeed();
            if(Numbers.isReal(lastGroundSpeed)) {
                return lastGroundSpeed;
            } else {
                // Compute ground speed from previous location
                if(prevLoc != null) {
                    final double dist = prevLoc.distanceTo(lastLoc);
                    final double dt = (lastLoc.millis - prevLoc.millis) * 0.001;
                    if(dt > 0) {
                        return dist / dt;
                    }
                }
            }
        }
        return Double.NaN;
    }

    /**
     * Helper method for getting latest speed in m/s
     * If GPS is not giving us speed natively, fallback to computing from v = dist/time.
     * This should be used for display of the latest total speed, but not for logging.
     */
    public double totalSpeed() {
        if(isFresh()) {
            return lastLoc.totalSpeed();
        } else {
            return Double.NaN;
        }
    }

    /**
     * Helper method for getting latest bearing in degrees
     * If GPS is not giving us speed natively, fallback to computing from v = dist/time.
     * This should be used for display of the latest groundspeed, but not for logging.
     */
    public double bearing() {
        if(isFresh()) {
            final double lastBearing = lastLoc.bearing();
            if(Numbers.isReal(lastBearing)) {
                return lastBearing;
            } else {
                // Compute ground speed from previous location
                if(prevLoc != null) {
                    return prevLoc.bearingTo(lastLoc);
                }
            }
        }
        return Double.NaN;
    }

    /**
     * Helper method for getting latest speed in m/s
     * If GPS is not giving us speed natively, fallback to computing from v = dist/time.
     * This should be used for display of the latest total speed, but not for logging.
     */
    public double glideRatio() {
        if(isFresh()) {
            return lastLoc.glideRatio();
        }
        return Double.NaN;
    }

    @Override
    public void stop() {
        if(!listeners.isEmpty()) {
            Log.e(providerName(), "Stopping location service, but listeners are still listening");
        }
    }
}
