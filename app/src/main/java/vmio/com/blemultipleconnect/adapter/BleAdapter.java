package vmio.com.blemultipleconnect.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.platypii.baseline.Service;
import com.platypii.baseline.location.MyLocationListener;
import com.platypii.baseline.measurements.MLocation;

import java.util.ArrayList;
import java.util.List;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.gps.Services;
import vmio.com.blemultipleconnect.listener.ItemTouchHelperAdapter;
import vmio.com.blemultipleconnect.listener.SimpleItemTouchHelperCallback;
import vmio.com.blemultipleconnect.model.BleDeviceInfo;
import vmio.com.blemultipleconnect.model.ButtonDevice;
import vmio.com.blemultipleconnect.model.SensorStore;

/**
 * Created by DatNT on 7/19/2017.
 */

public class BleAdapter extends RecyclerView.Adapter<BleAdapter.ViewHolder> implements ItemTouchHelperAdapter, MyLocationListener {

    private Context mContext;
    private List<BleDeviceInfo> mDevices;
    private ItemClickListener listener;
    public ArrayList<ButtonDevice> arrayDeviceConnected = new ArrayList<>();
    private ArrayList<SensorStore> sensorStored = new ArrayList<>();
    private SimpleItemTouchHelperCallback callBack;
    private SharePreference preference;

    public BleAdapter(Context context, ArrayList<BleDeviceInfo> mDevices) {
        mContext = context;
        this.mDevices = mDevices;
        preference = new SharePreference(mContext);
        sensorStored = preference.getConfigFromStorage();
        Services.bluetooth.preferenceEnabled = true;
        Services.start((Activity) mContext, null);
        Services.location.addListener(this);
    }

    public void setOnItemClickListener(ItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_ble, viewGroup, false);
        return new ViewHolder(view);
    }

    public void updateSensorStorage(ArrayList<SensorStore> sensorStored) {
        this.sensorStored = sensorStored;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        String bleName = "";
        if (mDevices.get(position).getBluetoothDevice().getName() == null) {
            for (SensorStore sensorStore : sensorStored)
                if (sensorStore.getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                    bleName = sensorStore.getName();
                    holder.txtName.setText(sensorStore.getName());
                }
        } else {
            bleName = mDevices.get(position).getBluetoothDevice().getName();
            holder.txtName.setText(bleName);
        }
        holder.imgKey.setVisibility(View.GONE);
        if (bleName.contains("GNS 2000")) {
            holder.imgSignal.setVisibility(View.GONE);
            holder.imgBattery.setVisibility(View.GONE);
            holder.imgCamera.setVisibility(View.GONE);
            holder.btnWarning.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
            if (mDevices.get(position).isConnected()) {
                holder.btnWarning.setColorFilter(ContextCompat.getColor(mContext, R.color.green), android.graphics.PorterDuff.Mode.SRC_ATOP);
                holder.imgGps.setVisibility(View.VISIBLE);
                if (mDevices.get(position).isHaveGPS())
                    holder.imgGps.setImageResource(R.drawable.ic_location);
                else
                    holder.imgGps.setImageResource(R.drawable.ic_no_location);
            } else {
                holder.btnWarning.setColorFilter(ContextCompat.getColor(mContext, R.color.gray), android.graphics.PorterDuff.Mode.SRC_ATOP);
                holder.imgGps.setVisibility(View.GONE);
            }
        } else {
            holder.imgGps.setVisibility(View.GONE);
            if (mDevices.get(position).isConnected()) {
                if (mDevices.get(position).getBluetoothDevice().getAddress().equals(preference.getSensorAttachedCamera())) {
                    holder.txtPosition.setVisibility(View.VISIBLE);
                    holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.green));
                    holder.txtPosition.setText(CommonUtils.getPosition(0));
                    holder.imgCamera.setVisibility(View.VISIBLE);
                } else
                    holder.imgCamera.setVisibility(View.GONE);
                holder.imgBattery.setVisibility(View.VISIBLE);
                holder.imgSignal.setVisibility(View.VISIBLE);
                if (mDevices.get(position).getRssi() > -50)
                    holder.imgSignal.setImageResource(R.drawable.signal_strong);
                else if (mDevices.get(position).getRssi() <= -50 && mDevices.get(position).getRssi() > -100)
                    holder.imgSignal.setImageResource(R.drawable.signal_normal);
                else if (mDevices.get(position).getRssi() <= -100 && mDevices.get(position).getRssi() > -200)
                    holder.imgSignal.setImageResource(R.drawable.signal_weak);
                else
                    holder.imgSignal.setImageResource(R.drawable.signal_loss);
                for (SensorStore sensorStore : sensorStored)
                    if (!sensorStore.getAddress().equals(preference.getSensorAttachedCamera())) {
                        if (sensorStore.getAddress().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                            if (sensorStore.getPosition() != -1) {
                                holder.btnWarning.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
                                holder.btnWarning.setColorFilter(ContextCompat.getColor(mContext, R.color.green), PorterDuff.Mode.SRC_ATOP);
                                holder.btnWarning.setOnClickListener(null);
                                holder.txtPosition.setVisibility(View.VISIBLE);
                                if (sensorStore.getDescription() != null) {
                                    holder.txtPosition.setText(CommonUtils.getPosition(sensorStore.getPosition()) + " (" + sensorStore.getDescription() + ") ");
                                } else {
                                    holder.txtPosition.setText(CommonUtils.getPosition(sensorStore.getPosition()));
                                }
                            } else {
                                holder.txtPosition.setVisibility(View.VISIBLE);
                                holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.red));
                                if (sensorStore.getStatus() == Define.STATUS_CONNECTED)
                                    holder.txtPosition.setText("加速度センサー較正：未");
                                else if (sensorStore.getStatus() == Define.STATUS_CALIBRATE)
                                    holder.txtPosition.setText("地磁気センサー較正：未");
                                else if (sensorStore.getStatus() == Define.STATUS_CALIBRATE_MAGNET)
                                    holder.txtPosition.setText("メモ記入：未");
                                else if (sensorStore.getStatus() == Define.STATUS_MEMO)
                                    holder.txtPosition.setText("装着位置設定：未");
                                holder.btnWarning.setImageResource(R.drawable.ic_warning_black_24dp);
                                holder.btnWarning.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (listener != null)
                                            listener.onItemWarningClick(holder.btnWarning, position);
                                    }
                                });
                            }
                        }
                    }
                if (mDevices.get(position).getBattery() == 0) {
                    holder.txtBattery.setVisibility(View.INVISIBLE);
                    holder.imgBattery.setImageResource(R.drawable.ic_very_weak_battery);
                } else {
                    holder.txtBattery.setVisibility(View.VISIBLE);
                    holder.txtBattery.setText("電池残量: " + mDevices.get(position).getBattery() + "%");
                    int battery = mDevices.get(position).getBattery();
                    if (battery >= 85)
                        holder.imgBattery.setImageResource(R.drawable.ic_strong_battery);
                    else if (battery < 85 && battery >= 75)
                        holder.imgBattery.setImageResource(R.drawable.ic_normal_battery);
                    else if (battery < 75)
                        holder.imgBattery.setImageResource(R.drawable.ic_weak_battery);
                }


                switch (mDevices.get(position).getKey()) {
                    case 0x1:
                        holder.imgKey.setVisibility(View.VISIBLE);
                        break;
                    case 0x2:
                        holder.imgKey.setVisibility(View.VISIBLE);
                        break;
                    case 0x3:
                        holder.imgKey.setVisibility(View.VISIBLE);
                        break;
                    case 0x4:
                        holder.imgKey.setVisibility(View.GONE);
                        break;
                    case 0x5:
                        holder.imgKey.setVisibility(View.VISIBLE);
                        break;
                    case 0x6:
                        holder.imgKey.setVisibility(View.VISIBLE);
                        break;
                    case 0x7:
                        holder.imgKey.setVisibility(View.VISIBLE);
                        break;
                    default:
                        holder.imgKey.setVisibility(View.GONE);
                        break;
                }
            } else {
                holder.imgSignal.setVisibility(View.INVISIBLE);
                holder.imgKey.setVisibility(View.INVISIBLE);
                holder.txtBattery.setVisibility(View.INVISIBLE);
                holder.txtPosition.setVisibility(View.INVISIBLE);
                holder.imgBattery.setVisibility(View.INVISIBLE);
                holder.imgCamera.setVisibility(View.INVISIBLE);
                holder.btnWarning.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
                holder.btnWarning.setColorFilter(ContextCompat.getColor(mContext, R.color.gray), android.graphics.PorterDuff.Mode.SRC_ATOP);
                holder.btnWarning.setOnClickListener(null);
            }
        }
        holder.txtAddress.setText(mDevices.get(position).getBluetoothDevice().getAddress());
    }

    @Override
    public int getItemCount() {
        if (mDevices == null) return 0;
        else return mDevices.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {

    }

    @Override
    public void onItemDismiss(int position) {
        if (mDevices.get(position).getBluetoothDevice().getName() != null)
            if (!mDevices.get(position).getBluetoothDevice().getName().contains("GNS 2000"))
                if (listener != null)
                    listener.onSwipeItem(position);
    }

    public void setItemTouchCallBack(SimpleItemTouchHelperCallback callBack) {
        this.callBack = callBack;
    }

    @Override
    public void onLocationChanged(@NonNull MLocation loc) {
        for (int i = 0; i < mDevices.size(); i++) {
            BleDeviceInfo ble = mDevices.get(i);
            if (ble.getBluetoothDevice().getName() != null) {
                if (ble.getBluetoothDevice().getName().contains("GNS 2000")) {
                    if (!ble.isHaveGPS()) {
                        ble.setHaveGPS(true);
                        final int finalI = i;
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notifyItemChanged(finalI);
                            }
                        });

                    }
                }
            }
        }
    }

    @Override
    public void onLocationChangedPostExecute(String device) {

    }

    @Override
    public void onLostLocation(String device) {
        for (int i = 0; i < mDevices.size(); i++) {
            BleDeviceInfo ble = mDevices.get(i);
            if (ble.getBluetoothDevice().getName() != null) {
                if (ble.getBluetoothDevice().getName().contains("GNS 2000")) {
                    if (ble.isHaveGPS()) {
                        ble.setHaveGPS(false);
                        final int finalI = i;
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notifyItemChanged(finalI);
                            }
                        });

                    }
                }
            }
        }
    }

    @Override
    public void onReceiveLocation(String device, MLocation location) {
        for (int i = 0; i < mDevices.size(); i++) {
            BleDeviceInfo ble = mDevices.get(i);
            if (ble.getBluetoothDevice().getName() != null) {
                if (ble.getBluetoothDevice().getName().contains("GNS 2000")) {
                    if (!ble.isHaveGPS()) {
                        ble.setHaveGPS(true);
                        final int finalI = i;
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notifyItemChanged(finalI);
                            }
                        });

                    }
                }
            }
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        ImageView btnWarning;
        ImageView imgSignal;
        TextView txtAddress;
        ImageView imgKey;
        TextView txtBattery;
        TextView txtPosition;
        ImageView imgBattery;
        ImageView imgCamera;
        ImageView imgGps;

        public ViewHolder(View itemView) {
            super(itemView);
            txtName = (TextView) itemView.findViewById(R.id.txt_name);
            btnWarning = (ImageView) itemView.findViewById(R.id.btn_warning);
            txtAddress = (TextView) itemView.findViewById(R.id.txt_address);
            imgKey = (ImageView) itemView.findViewById(R.id.img_key);
            txtBattery = (TextView) itemView.findViewById(R.id.txt_battery);
            txtPosition = (TextView) itemView.findViewById(R.id.txt_position);
            imgSignal = (ImageView) itemView.findViewById(R.id.img_signal);
            imgCamera = (ImageView) itemView.findViewById(R.id.img_camera);
            imgBattery = (ImageView) itemView.findViewById(R.id.img_battery_status);
            imgGps = (ImageView) itemView.findViewById(R.id.img_gps);
        }
    }

    public interface ItemClickListener {
        void onItemWarningClick(ImageView btnWarning, int position);

        void onSwipeItem(int position);
    }
}