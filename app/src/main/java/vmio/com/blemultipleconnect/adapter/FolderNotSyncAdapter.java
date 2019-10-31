package vmio.com.blemultipleconnect.adapter;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.model.FileDetail;

/**
 * Created by DatNT on 7/19/2017.
 */

public class FolderNotSyncAdapter extends RecyclerView.Adapter<FolderNotSyncAdapter.ViewHolder>  {

    private Context mContext;
    private List<FileDetail> mDevices;
    private ItemClickListener listener;

    public FolderNotSyncAdapter(Context context, ArrayList<FileDetail> mDevices) {
        mContext = context;
        this.mDevices = mDevices;
    }

    public void setOnItemClickListener (ItemClickListener listener){
        this.listener = listener;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_folder_not_sync, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.txtPath.setText(mDevices.get(position).getName());
        float kilobyte = ((float)mDevices.get(position).getSize())/1000;
        holder.txtSize.setText(kilobyte+" kb");
        holder.imgOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(mContext, v);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.share_item:
                                if (listener != null)
                                    listener.onItemShareClick(position);
                                return true;
                            case R.id.save_sd_card:
                                if (listener != null)
                                    listener.onItemExportClick(position);
                                return true;
                            case R.id.upload:
                                if (listener != null)
                                    listener.onUploadClick(position);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.inflate(R.menu.popup_menu);
                popup.show();
            }
        });
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
        TextView txtPath;
        TextView txtSize;
        ImageView imgOption;
        public ViewHolder(View itemView) {
            super(itemView);
            txtPath = (TextView) itemView.findViewById(R.id.txt_path);
            txtSize = (TextView) itemView.findViewById(R.id.txt_size);
            imgOption = (ImageView) itemView.findViewById(R.id.img_option);
        }

    }
    public interface ItemClickListener{
        void onItemExportClick(int position);
        void onItemShareClick(int position);
        void onUploadClick(int position);
    }
}