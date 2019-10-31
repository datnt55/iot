package vmio.com.blemultipleconnect.Utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import vmio.com.blemultipleconnect.model.SensorStore;
import vmio.com.mioblelib.widget.Point3D;


public class SharePreference {

    private Context activity;
    private String ID = "id";
    private String WORKER_NAME = "worker name";
    private String TOKEN = "token";
    private String SENSORS = "sensor";
    private String CALIBRATE_MAGNET = "calibrate magnet";
    private String CAMERA_ATTACHED = "camera attached";
    private String ENABLE_GPS = "enable gps";

    // constructor
    public SharePreference(Context activity) {
        this.activity = activity;
    }

    public void saveSensor(String token) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(SENSORS, token);
        editor.apply();
    }

    public boolean getMustEnableGPS() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        return sp.getBoolean(ENABLE_GPS, true);
    }

    public void saveMustEnableGPS(boolean enable) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(ENABLE_GPS, enable);
        editor.apply();
    }

    public String getSensor() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        return sp.getString(SENSORS, "");
    }


    public void saveConfigToStorage(String name, String device, String description, int status, int position, Point3D accel, Point3D gyro, Point3D magnet) {
        String sensors = getSensor();
        if (sensors.equals("")) {
            JSONArray jsonArray = new JSONArray();
            JSONObject ble = new JSONObject();
            try {
                ble.put("name", name);
                ble.put("address", device);
                ble.put("description", description);
                ble.put("status", status);
                ble.put("position", position);
                JSONObject accelObject = new JSONObject();
                accelObject.put("x", accel.x);
                accelObject.put("y", accel.y);
                accelObject.put("z", accel.z);
                JSONObject gyroObject = new JSONObject();
                gyroObject.put("x", gyro.x);
                gyroObject.put("y", gyro.y);
                gyroObject.put("z", gyro.z);
                JSONObject magnetObject = new JSONObject();
                magnetObject.put("x", magnet.x);
                magnetObject.put("y", magnet.y);
                magnetObject.put("z", magnet.z);
                ble.put("accel", accelObject);
                ble.put("gyro", gyroObject);
                ble.put("magnet", magnetObject);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            jsonArray.put(ble);
            saveSensor(jsonArray.toString());
        } else {
            try {
                ArrayList<SensorStore> storeArrayList = getConfigFromStorage();
                boolean sensorExist = false;
                for (SensorStore sensorStore : storeArrayList)
                    if (sensorStore.getAddress().equals(device)) {
                        sensorStore.setStatus(status);
                        sensorStore.setPosition(position);
                        sensorStore.setAccel(accel);
                        sensorStore.setGyro(gyro);
                        sensorStore.setMagnet(magnet);
                        sensorStore.setDescription(description);
                        sensorExist = true;
                        break;
                    }
                if (!sensorExist) {
                    JSONArray jsonArray = new JSONArray(sensors);
                    JSONObject ble = new JSONObject();
                    ble.put("name", name);
                    ble.put("address", device);
                    ble.put("description", description);
                    ble.put("status", status);
                    ble.put("position", position);
                    JSONObject accelObject = new JSONObject();
                    accelObject.put("x", accel.x);
                    accelObject.put("y", accel.y);
                    accelObject.put("z", accel.z);
                    JSONObject gyroObject = new JSONObject();
                    gyroObject.put("x", gyro.x);
                    gyroObject.put("y", gyro.y);
                    gyroObject.put("z", gyro.z);
                    JSONObject magnetObject = new JSONObject();
                    magnetObject.put("x", magnet.x);
                    magnetObject.put("y", magnet.y);
                    magnetObject.put("z", magnet.z);
                    ble.put("accel", accelObject);
                    ble.put("gyro", gyroObject);
                    ble.put("magnet", magnetObject);
                    jsonArray.put(ble);
                    saveSensor(jsonArray.toString());
                } else {
                    saveJsonToStorage(storeArrayList);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeConfigToStorage(ArrayList<SensorStore> sensorStored, String deviceAddress) {
        Iterator<SensorStore> ble = sensorStored.iterator();
        while (ble.hasNext()) {
            SensorStore d = ble.next();
            if (d.getAddress().equals(deviceAddress)) {
                ble.remove();
                break;
            }
        }
        saveJsonToStorage(sensorStored);
    }

    private void saveJsonToStorage(ArrayList<SensorStore> sensorStored) {
        JSONArray jsonArray = new JSONArray();
        for (SensorStore ble : sensorStored) {
            try {
                JSONObject object = new JSONObject();
                object.put("name", ble.getName());
                object.put("address", ble.getAddress());
                object.put("description", ble.getDescription());
                object.put("status", ble.getStatus());
                object.put("position", ble.getPosition());
                JSONObject accelObject = new JSONObject();
                accelObject.put("x", ble.getAccel().x);
                accelObject.put("y", ble.getAccel().y);
                accelObject.put("z", ble.getAccel().z);
                JSONObject gyroObject = new JSONObject();
                gyroObject.put("x", ble.getGyro().x);
                gyroObject.put("y", ble.getGyro().y);
                gyroObject.put("z", ble.getGyro().z);
                JSONObject magnetObject = new JSONObject();
                magnetObject.put("x", ble.getMagnet().x);
                magnetObject.put("y", ble.getMagnet().y);
                magnetObject.put("z", ble.getMagnet().z);
                object.put("accel", accelObject);
                object.put("gyro", gyroObject);
                object.put("magnet", magnetObject);
                jsonArray.put(object);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        saveSensor(jsonArray.toString());
    }

    public ArrayList<SensorStore> getConfigFromStorage() {
        ArrayList<SensorStore> storeArrayList = new ArrayList<>();
        String sensors = getSensor();
        if (sensors.equals("")) {
            return new ArrayList<>();
        } else {
            try {
                JSONArray jsonArray = new JSONArray(sensors);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject object = jsonArray.getJSONObject(i);
                    String name = object.getString("name");
                    String address = object.getString("address");
                    String description = object.getString("description");
                    int status = object.getInt("status");
                    int position = object.getInt("position");
                    JSONObject accelObject = object.getJSONObject("accel");
                    JSONObject gyroObject = object.getJSONObject("gyro");
                    JSONObject magnetObject = object.getJSONObject("magnet");
                    Point3D accel = new Point3D(accelObject.getDouble("x"), accelObject.getDouble("y"), accelObject.getDouble("z"));
                    Point3D gyro = new Point3D(gyroObject.getDouble("x"), gyroObject.getDouble("y"), gyroObject.getDouble("z"));
                    Point3D magnet = new Point3D(magnetObject.getDouble("x"), magnetObject.getDouble("y"), magnetObject.getDouble("z"));
                    storeArrayList.add(new SensorStore(name, address, description, status, position, accel, gyro, magnet));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return storeArrayList;
        }
    }

    public SensorStore getConfigFromStorageByAddress(String device) {
        String sensors = getSensor();
        if (sensors.equals("")) {
            return null;
        } else {
            try {
                JSONArray jsonArray = new JSONArray(sensors);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject object = jsonArray.getJSONObject(i);
                    String address = object.getString("address");
                    if (address.equals(device)) {
                        String name = object.getString("name");
                        String description = object.getString("description");
                        int status = object.getInt("status");
                        int position = object.getInt("position");
                        JSONObject accelObject = object.getJSONObject("accel");
                        JSONObject gyroObject = object.getJSONObject("gyro");
                        JSONObject magnetObject = object.getJSONObject("magnet");
                        Point3D accel = new Point3D(accelObject.getDouble("x"), accelObject.getDouble("y"), accelObject.getDouble("z"));
                        Point3D gyro = new Point3D(gyroObject.getDouble("x"), gyroObject.getDouble("y"), gyroObject.getDouble("z"));
                        Point3D magnet = new Point3D(magnetObject.getDouble("x"), magnetObject.getDouble("y"), magnetObject.getDouble("z"));
                        return new SensorStore(name, address, description, status, position, accel, gyro, magnet);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    void  putValuesToJsonObject( JSONObject magnetObject, double[][] magnet)
    {
        try {
            magnetObject.put("bx", magnet[0][0]);
            magnetObject.put("by", magnet[0][1]);
            magnetObject.put("bz", magnet[0][2]);

            magnetObject.put("sx", magnet[1][0]);
            magnetObject.put("sy", magnet[1][1]);
            magnetObject.put("sz", magnet[1][2]);

            //[20181221 dungtv] Add Sphere matrix
            magnetObject.put("A20", magnet[2][0]);
            magnetObject.put("A21", magnet[2][1]);
            magnetObject.put("A22", magnet[2][2]);

            magnetObject.put("A30", magnet[3][0]);
            magnetObject.put("A31", magnet[3][1]);
            magnetObject.put("A32", magnet[3][2]);

            magnetObject.put("A40", magnet[4][0]);
            magnetObject.put("A41", magnet[4][1]);
            magnetObject.put("A42", magnet[4][2]);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveCalibrateMagnet(String device, double[][] magnet) {
        JSONArray jsonArrayBLEs = getAllCalibrateMagnet();
        boolean isExist = false;
        // Check if this device magnet factors already stored then replace it's values
        if (jsonArrayBLEs != null) {
            try {
                for (int i = 0; i < jsonArrayBLEs.length(); i++) {
                    JSONObject objectBLE = jsonArrayBLEs.getJSONObject(i);
                    String address = objectBLE.getString("address");
                    if (address != null && address.equals(device)) {
                        isExist = true;
                        JSONObject magnetObject = objectBLE.getJSONObject("magnet");
                        if(magnetObject != null) {
                            // replace current values
                            putValuesToJsonObject(magnetObject, magnet);
                        } else {
                            // [20181123    VMio] Fix some time error data of BLE add new values to current BLE setting
                            magnetObject = new JSONObject();
                            putValuesToJsonObject(magnetObject, magnet);
                            objectBLE.put("magnet", magnetObject);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            jsonArrayBLEs = new JSONArray();
        }

        // if exists already then replaced already else create new BLE item
        if (!isExist) {
            JSONObject objectBLE = new JSONObject();
            try {
                objectBLE.put("address", device);
                JSONObject magnetObject = new JSONObject();
                putValuesToJsonObject(magnetObject, magnet);
                objectBLE.put("magnet", magnetObject);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            jsonArrayBLEs.put(objectBLE);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(CALIBRATE_MAGNET, jsonArrayBLEs.toString());
        editor.apply();
    }

    public double[][] getCalibrateMagnet(String device) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        String calibrate =  sp.getString(CALIBRATE_MAGNET, "");
        if (calibrate == null || calibrate.equals(""))
            return null;
        try {
            JSONArray jsonArray = new JSONArray(calibrate);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String address = object.getString("address");
                if (address.equals(device)) {
                    JSONObject magnetObject = object.getJSONObject("magnet");
                    double bx = magnetObject.getDouble("bx");
                    double by = magnetObject.getDouble("by");
                    double bz = magnetObject.getDouble("bz");

                    double sx = magnetObject.getDouble("sx");
                    double sy = magnetObject.getDouble("sy");
                    double sz = magnetObject.getDouble("sz");

                    double A20 = magnetObject.getDouble("A20");
                    double A21 = magnetObject.getDouble("A21");
                    double A22 = magnetObject.getDouble("A22");

                    double A30 = magnetObject.getDouble("A30");
                    double A31 = magnetObject.getDouble("A31");
                    double A32 = magnetObject.getDouble("A32");

                    double A40 = magnetObject.getDouble("A40");
                    double A41 = magnetObject.getDouble("A41");
                    double A42 = magnetObject.getDouble("A42");

                    double[][] magnet = {{bx,by,bz},{sx,sy,sz},{A20, A21, A22},{A30,A31,A32},{A40,A41,A42}};
                    return magnet;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONArray getAllCalibrateMagnet() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        String calibrate =  sp.getString(CALIBRATE_MAGNET, "");
        if (calibrate == null || calibrate.equals(""))
            return null;
        try {
            JSONArray jsonArray = new JSONArray(calibrate);
            return jsonArray;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getSensorAttachedCamera() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        String address =  sp.getString(CAMERA_ATTACHED, "");
        return address;
    }

    public void saveSensorAttachedCamera(String address) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(CAMERA_ATTACHED, address);
        editor.apply();
    }

    public void clearSensorAttachedCamera() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(CAMERA_ATTACHED, "");
        editor.apply();
    }

    public String getId() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        String address =  sp.getString(ID, "");
        return address;
    }

    public void saveId(String id) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(ID, id);
        editor.apply();
    }

    public String getWorkerName() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        String address =  sp.getString(WORKER_NAME, "");
        return address;
    }

    public void saveWorkerName(String id) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(WORKER_NAME, id);
        editor.apply();
    }

    public void saveToken(String token) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(TOKEN, token);
        editor.apply();
    }

    public String getToken() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        String token =  sp.getString(TOKEN, "");
        return token;
    }
}
