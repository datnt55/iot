package vmio.com.blemultipleconnect.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.model.ButtonDevice;
import vmio.com.blemultipleconnect.model.ConfigBleDeviceInfo;

import static vmio.com.blemultipleconnect.Utilities.Define.STATUS_NOT_CONNECT;

/**
 * Created by DatNT on 7/19/2017.
 */

public class ConfigBleAdapter extends RecyclerView.Adapter<ConfigBleAdapter.ViewHolder> {
    private Context mContext;
    private List<ConfigBleDeviceInfo> mDevices;
    private ItemClickListener listener;
    public ArrayList<ButtonDevice> arrayDeviceConnected = new ArrayList<>();

    public ConfigBleAdapter(Context context, ArrayList<ConfigBleDeviceInfo> mDevices) {
        mContext = context;
        this.mDevices = mDevices;
    }

    public void setOnItemClickListener(ItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.config_ble_item, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        String name = mDevices.get(position).getBluetoothDevice().getName();
        if (name == null) {
            name = new String("Unknown device");
        }
        holder.txtName.setText(name);
        holder.txtAddress.setText(mDevices.get(position).getBluetoothDevice().getAddress());
        holder.imgSignal.setVisibility(View.GONE);

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mDevices.get(position).getBluetoothDevice().getName().contains("GNS 2000")) {
                    if (listener != null)
                        listener.onItemClick(position);
                }
            }
        });
        holder.txtDescription.setTextColor(ContextCompat.getColor(mContext, R.color.red));
        holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.red));
        if (!mDevices.get(position).getBluetoothDevice().getName().contains("GNS 2000")) {
            if (mDevices.get(position).getStatus() > Define.STATUS_CALIBRATE)
                holder.btnFastCalibrate.setVisibility(View.VISIBLE);
            else
                holder.btnFastCalibrate.setVisibility(View.GONE);
            if (mDevices.get(position).getStatus() != STATUS_NOT_CONNECT)
                holder.imgCamera.setVisibility(View.VISIBLE);
            else
                holder.imgCamera.setVisibility(View.INVISIBLE);
            SharePreference preference = new SharePreference(mContext);
            if (preference.getSensorAttachedCamera().equals(mDevices.get(position).getBluetoothDevice().getAddress()))
                holder.imgCamera.setImageResource(R.drawable.ic_videocam_black_24dp);
            else
                holder.imgCamera.setImageResource(R.drawable.ic_camera_disable);
        } else {
            holder.btnFastCalibrate.setVisibility(View.GONE);
            holder.imgCamera.setVisibility(View.INVISIBLE);
        }
        SharePreference preference = new SharePreference(mContext);
        switch (mDevices.get(position).getStatus()) {
            case STATUS_NOT_CONNECT:
                holder.btnStatus.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
                holder.btnStatus.setColorFilter(ContextCompat.getColor(mContext, R.color.gray), android.graphics.PorterDuff.Mode.SRC_ATOP);
                holder.btnConfig.setVisibility(View.INVISIBLE);
                holder.txtDescription.setVisibility(View.INVISIBLE);
                holder.txtPosition.setVisibility(View.INVISIBLE);
                break;
            case Define.STATUS_CONNECTED:
                holder.btnStatus.setImageResource(R.drawable.ic_fiber_manual_record_black_24dp);
                holder.btnStatus.setColorFilter(ContextCompat.getColor(mContext, R.color.green), android.graphics.PorterDuff.Mode.SRC_ATOP);
                holder.txtDescription.setText("メモ記入：未");
                if (preference.getSensorAttachedCamera().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                    holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.green));
                    holder.txtPosition.setText(CommonUtils.getPosition(0));
                }else {
                    holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.red));
                    holder.txtPosition.setText("加速度センサー較正：未");
                }
                if (!mDevices.get(position).getBluetoothDevice().getName().contains("GNS 2000")) {
                    holder.btnConfig.setVisibility(View.VISIBLE);
                    holder.txtDescription.setVisibility(View.VISIBLE);
                    holder.txtPosition.setVisibility(View.VISIBLE);
                } else {
                    holder.btnConfig.setVisibility(View.INVISIBLE);
                    holder.txtDescription.setVisibility(View.INVISIBLE);
                    holder.txtPosition.setVisibility(View.INVISIBLE);
                }
                break;
            case Define.STATUS_CALIBRATE:
                holder.btnStatus.setImageResource(R.drawable.calibrate);
                holder.btnStatus.setColorFilter(ContextCompat.getColor(mContext, R.color.green), android.graphics.PorterDuff.Mode.SRC_ATOP);
                holder.btnConfig.setVisibility(View.VISIBLE);
                holder.txtDescription.setVisibility(View.VISIBLE);
                holder.txtPosition.setVisibility(View.VISIBLE);
                holder.txtDescription.setText("メモ記入：未");
                if (preference.getSensorAttachedCamera().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                    holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.green));
                    holder.txtPosition.setText(CommonUtils.getPosition(0));
                }else {
                    holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.red));
                    holder.txtPosition.setText("加速度センサー較正：未");
                }
                break;
            case Define.STATUS_CALIBRATE_MAGNET:
                holder.btnStatus.setImageResource(R.drawable.calibrate);
                holder.btnStatus.setColorFilter(ContextCompat.getColor(mContext, R.color.green), android.graphics.PorterDuff.Mode.SRC_ATOP);
                holder.btnConfig.setVisibility(View.VISIBLE);
                holder.txtDescription.setVisibility(View.VISIBLE);
                holder.txtPosition.setVisibility(View.VISIBLE);
                holder.txtDescription.setText("メモ記入：未");
                if (preference.getSensorAttachedCamera().equals(mDevices.get(position).getBluetoothDevice().getAddress())) {
                    holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.green));
                    holder.txtPosition.setText(CommonUtils.getPosition(0));
                }else {
                    holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.red));
                    holder.txtPosition.setText("加速度センサー較正：未");
                }
                break;
            case Define.STATUS_MEMO:
                holder.btnStatus.setImageResource(R.drawable.account_settings_variant);
                holder.btnStatus.setColorFilter(ContextCompat.getColor(mContext, R.color.green), android.graphics.PorterDuff.Mode.SRC_ATOP);
                holder.btnConfig.setVisibility(View.VISIBLE);
                break;
            case Define.STATUS_SAVE:
                holder.btnStatus.setImageResource(R.drawable.ic_check_black_24dp);
                holder.btnStatus.setColorFilter(ContextCompat.getColor(mContext, R.color.green), android.graphics.PorterDuff.Mode.SRC_ATOP);
                holder.btnConfig.setVisibility(View.VISIBLE);
                holder.txtDescription.setVisibility(View.VISIBLE);
                holder.txtPosition.setVisibility(View.VISIBLE);
                holder.txtDescription.setTextColor(ContextCompat.getColor(mContext, R.color.green));
                holder.txtPosition.setTextColor(ContextCompat.getColor(mContext, R.color.green));
                holder.txtDescription.setText(mDevices.get(position).getDescription());
                holder.txtPosition.setText(CommonUtils.getPosition(mDevices.get(position).getPosition()));
                break;
        }

        holder.btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogUtils.showMessageDialog(mContext, "お知らせ", mDevices.get(position).getBluetoothDevice().getAddress() + "のセンサーデバイスを外しても宜しいですか？", new DialogUtils.YesNoListener() {
                    @Override
                    public void onYes() {
                        if (listener != null)
                            listener.onConfigClick(position);
                    }
                });
            }
        });
        switch (mDevices.get(position).getKey()) {
            case 0x1:
                holder.imgSignal.setVisibility(View.VISIBLE);
                break;
            case 0x2:
                holder.imgSignal.setVisibility(View.VISIBLE);
                break;
            case 0x3:
                holder.imgSignal.setVisibility(View.VISIBLE);
                break;
            case 0x4:
                holder.imgSignal.setVisibility(View.GONE);
                break;
            case 0x5:
                holder.imgSignal.setVisibility(View.VISIBLE);
                break;
            case 0x6:
                holder.imgSignal.setVisibility(View.VISIBLE);
                break;
            case 0x7:
                holder.imgSignal.setVisibility(View.VISIBLE);
                break;
            default:
                holder.imgSignal.setVisibility(View.GONE);
                break;
        }
        holder.imgCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mDevices.get(position).isCameraAttached())
                    return;
//                if (mDevices.get(position).getPosition() == 0)
//                    return;
                for (ConfigBleDeviceInfo ble : mDevices)
                    ble.setCameraAttached(false);
                mDevices.get(position).setCameraAttached(true);
                SharePreference preference = new SharePreference(mContext);
                preference.saveSensorAttachedCamera(mDevices.get(position).getBluetoothDevice().getAddress());
                notifyDataSetChanged();
            }
        });
        holder.btnFastCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null)
                    listener.onFastCalibrate(position);
            }
        });
    }

    public ArrayList<ButtonDevice> getArrayDeviceConnected() {
        return arrayDeviceConnected;
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

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtName;
        ImageView btnStatus;
        TextView txtAddress;
        ImageView imgSignal;
        TextView txtDescription;
        TextView txtPosition;
        ImageView btnConfig;
        RelativeLayout root;
        ImageView imgCamera;
        Button btnFastCalibrate;

        public ViewHolder(View itemView) {
            super(itemView);
            root = (RelativeLayout) itemView.findViewById(R.id.root);
            txtName = (TextView) itemView.findViewById(R.id.txt_name);
            btnStatus = (ImageView) itemView.findViewById(R.id.btn_status);
            btnConfig = (ImageView) itemView.findViewById(R.id.btn_config);
            txtAddress = (TextView) itemView.findViewById(R.id.txt_address);
            imgSignal = (ImageView) itemView.findViewById(R.id.img_key);
            txtDescription = (TextView) itemView.findViewById(R.id.txt_description);
            txtPosition = (TextView) itemView.findViewById(R.id.txt_position);
            imgCamera = (ImageView) itemView.findViewById(R.id.img_camera);
            btnFastCalibrate = itemView.findViewById(R.id.btn_fast_calibrate);
        }
    }

    public interface ItemClickListener {
        void onItemClick(int position);
        void onConfigClick(int position);
        void onFastCalibrate(int position);
    }
}