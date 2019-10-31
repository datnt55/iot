package vmio.com.blemultipleconnect.activity;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.model.BleBody;
import vmio.com.blemultipleconnect.model.SensorStore;

import static vmio.com.blemultipleconnect.activity.MainActivity.deviceInfoExists;

public class SensorPositionSettingActivity extends AppCompatActivity {

    private Button btnPos0, btnPos1, btnPos2, btnPos3, btnPos4, btnPos5, btnPos6, btnClose;
    private ArrayList<BleBody> bleBodyArrayList = new ArrayList<>();
    private SharePreference preference = new SharePreference(this);
    private ArrayList<SensorStore> sensorStored = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_position_setting);
        initView();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideNavigationBar();
    }

    private void initView() {
        btnPos0 = (Button) findViewById(R.id.btn_pos_0);
        btnPos1 = (Button) findViewById(R.id.btn_pos_1);
        btnPos2 = (Button) findViewById(R.id.btn_pos_2);
        btnPos3 = (Button) findViewById(R.id.btn_pos_3);
        btnPos4 = (Button) findViewById(R.id.btn_pos_4);
        btnPos5 = (Button) findViewById(R.id.btn_pos_5);
        btnPos6 = (Button) findViewById(R.id.btn_pos_6);
        btnClose = (Button) findViewById(R.id.btn_close);
        refreshBodyDialog();

        int width = Global.WIDTH_SCREEN;
        int height = Global.HEIGHT_SCREEN;
        int round = CommonUtils.convertDpToPx(20, this);

        RelativeLayout.LayoutParams params0 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, this), CommonUtils.convertDpToPx(40, this));
        params0.setMargins((width / 2) - round, (height * 12 / 100) - round, 0, 0);
        btnPos0.setLayoutParams(params0);

        RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, this), CommonUtils.convertDpToPx(40, this));
        params1.setMargins((width * 71 / 100) - round, (height * 56 / 100) - round, 0, 0);
        btnPos1.setLayoutParams(params1);

        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, this), CommonUtils.convertDpToPx(40, this));
        params2.setMargins((width * 29 / 100) - round, (height * 56 / 100) - round, 0, 0);
        btnPos2.setLayoutParams(params2);

        RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, this), CommonUtils.convertDpToPx(40, this));
        params3.setMargins((width * 69 / 100) - round, (height * 90 / 100) - round, 0, 0);
        btnPos3.setLayoutParams(params3);

        RelativeLayout.LayoutParams params4 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, this), CommonUtils.convertDpToPx(40, this));
        params4.setMargins((width * 31 / 100) - round, (height * 90 / 100) - round, 0, 0);
        btnPos4.setLayoutParams(params4);

        RelativeLayout.LayoutParams params5 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, this), CommonUtils.convertDpToPx(40, this));
        params5.setMargins((width / 2) - round, (height * 42 / 100) - round, 0, 0);
        btnPos5.setLayoutParams(params5);

        RelativeLayout.LayoutParams params6 = new RelativeLayout.LayoutParams(CommonUtils.convertDpToPx(40, this), CommonUtils.convertDpToPx(40, this));
        params6.setMargins((width / 2) - round, (height * 58 / 100) - round, 0, 0);
        btnPos6.setLayoutParams(params6);

        btnPos0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listSensorDialog(0);
            }
        });
        btnPos1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listSensorDialog(1);
            }
        });
        btnPos2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listSensorDialog(2);
            }
        });
        btnPos3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listSensorDialog(3);
            }
        });
        btnPos4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listSensorDialog(4);
            }
        });
        btnPos5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listSensorDialog(5);
            }
        });
        btnPos6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listSensorDialog(6);
            }
        });
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void refreshBodyDialog() {
        btnPos0.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
        btnPos1.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
        btnPos2.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
        btnPos3.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
        btnPos4.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
        btnPos5.setBackgroundResource(R.drawable.ic_adjust_black_24dp);
        btnPos6.setBackgroundResource(R.drawable.ic_adjust_black_24dp);

        btnPos0.setText("");
        btnPos1.setText("");
        btnPos2.setText("");
        btnPos3.setText("");
        btnPos4.setText("");
        btnPos5.setText("");
        btnPos6.setText("");

        bleBodyArrayList = new ArrayList<>();
        sensorStored = preference.getConfigFromStorage();
        for (SensorStore store : sensorStored) {
            if (deviceInfoExists(store.getAddress())) {
                if (store.getPosition() >= 0) {
                    switch (store.getPosition()) {
                        case 0:
                            btnPos0.setBackgroundResource(R.drawable.sensortag2);
                            btnPos0.setText(store.getDescription());
                            bleBodyArrayList.add(new BleBody(btnPos0, store.getAddress()));
                            break;
                        case 1:
                            btnPos1.setBackgroundResource(R.drawable.sensortag2);
                            btnPos1.setText(store.getDescription());
                            bleBodyArrayList.add(new BleBody(btnPos1, store.getAddress()));
                            break;
                        case 2:
                            btnPos2.setBackgroundResource(R.drawable.sensortag2);
                            btnPos2.setText(store.getDescription());
                            bleBodyArrayList.add(new BleBody(btnPos2, store.getAddress()));
                            break;
                        case 3:
                            btnPos3.setBackgroundResource(R.drawable.sensortag2);
                            btnPos3.setText(store.getDescription());
                            bleBodyArrayList.add(new BleBody(btnPos3, store.getAddress()));
                            break;
                        case 4:
                            btnPos4.setBackgroundResource(R.drawable.sensortag2);
                            btnPos4.setText(store.getDescription());
                            bleBodyArrayList.add(new BleBody(btnPos4, store.getAddress()));
                            break;
                        case 5:
                            btnPos5.setBackgroundResource(R.drawable.sensortag2);
                            btnPos5.setText(store.getDescription());
                            bleBodyArrayList.add(new BleBody(btnPos5, store.getAddress()));
                            break;
                        case 6:
                            btnPos6.setBackgroundResource(R.drawable.sensortag2);
                            btnPos6.setText(store.getDescription());
                            bleBodyArrayList.add(new BleBody(btnPos6, store.getAddress()));
                            break;
                    }
                }
            }
        }
    }

    private void listSensorDialog(final int position) {
        boolean checkPosition = false;
        sensorStored = preference.getConfigFromStorage();
        final ArrayList<SensorStore> listSensor = new ArrayList<>();
        final ArrayList<String> strings = new ArrayList<>();
        for (SensorStore store : sensorStored) {
            if (deviceInfoExists(store.getAddress())) {
                listSensor.add(store);
                if (store.getPosition() < 0) {
                    if (store.getDescription() != null) {
                        strings.add("Put " + store.getDescription() + " to " + CommonUtils.getPosition(position));
                    } else {
                        strings.add("Put " + store.getAddress() + " to " + CommonUtils.getPosition(position));
                    }
                } else {
                    if (store.getDescription() != null) {
                        strings.add("Switch " + store.getDescription() + " (" + CommonUtils.getPosition(store.getPosition()) + ") " + " to " + CommonUtils.getPosition(position));
                    } else {
                        strings.add("Switch " + store.getAddress() + " (" + CommonUtils.getPosition(store.getPosition()) + ") " + " to " + CommonUtils.getPosition(position));
                    }
                }
                if (position == store.getPosition()) {
                    checkPosition = true;
                }
            }
        }
        if (listSensor.size() < 1) {
            Toast.makeText(this, "Please keep connecting at least one sensor tag", Toast.LENGTH_LONG).show();
            return;
        }
        if (checkPosition) {
            strings.add("Remove Sensor");
        }
        String[] stockArr = new String[strings.size()];
        stockArr = strings.toArray(stockArr);
        final String[] finalStockArr = stockArr;
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Select action")
                .setItems(stockArr, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selection = finalStockArr[which];
                        if (selection.equals("Remove Sensor")) {
                            for (SensorStore store : listSensor) {
                                if (store.getPosition() == position) {
                                    preference.saveConfigToStorage(store.getName(), store.getAddress(), store.getDescription(), Define.STATUS_CALIBRATE, -1, store.getAccel(), store.getGyro(), store.getMagnet());
                                }
                            }
                        } else {
                            preference.saveConfigToStorage(listSensor.get(which).getName(), listSensor.get(which).getAddress(), listSensor.get(which).getDescription(), Define.STATUS_SAVE, position, listSensor.get(which).getAccel(), listSensor.get(which).getGyro(), listSensor.get(which).getMagnet());
                        }
                        refreshBodyDialog();
                        dialog.dismiss();
                    }
                });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        alert.show();
    }

    private void hideNavigationBar() {
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(uiOptions);
        }
    }
}
