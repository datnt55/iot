package com.platypii.baseline.location;

import com.platypii.baseline.measurements.MLocation;
import android.support.annotation.NonNull;

/**
 * Used by MyLocationManager to notify activities of updated location
 */
public interface MyLocationListener {

    /**
     * Process the new location on a background thread
     */
    void onLocationChanged(@NonNull MLocation loc);

    /**
     * Process the new location on the UI thread
     * PostExecute doesn't get a parameter, because UI threads should just pull the latest data
     */
    void onLocationChangedPostExecute(String device);

    void onLostLocation(String device);

    void onReceiveLocation(String device, MLocation location);
}
