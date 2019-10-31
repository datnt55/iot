package vmio.com.blemultipleconnect.model;

import java.util.ArrayList;

import vmio.com.mioblelib.service.GenericBluetoothProfile;


/**
 * Created by DatNT on 9/12/2017.
 */

public class BluetoothProfile {
    private String address;
    private ArrayList<GenericBluetoothProfile> profile;

    public BluetoothProfile(String address, ArrayList<GenericBluetoothProfile> profile) {
        this.address = address;
        this.profile = profile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public ArrayList<GenericBluetoothProfile> getProfile() {
        return profile;
    }

    public void setProfile(ArrayList<GenericBluetoothProfile> profile) {
        this.profile = profile;
    }
}
