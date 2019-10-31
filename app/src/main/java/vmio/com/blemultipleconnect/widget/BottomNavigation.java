package vmio.com.blemultipleconnect.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.SingleClick;

/**
 * TODO: document your custom view class.
 */
public class BottomNavigation extends LinearLayout implements SingleClick.OnClickListener {
    private Context mContext;
    LinearLayout layoutRecord;
    private BottomNaviCallback callback;
    private SingleClick singleClick;
    public BottomNavigation(Context context) {
        super(context);
        this.mContext = context;
        initLayout();
    }

    public void setCallback(BottomNaviCallback callback){
        this.callback = callback;
    }

    public BottomNavigation(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initLayout();
    }

    public BottomNavigation(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContext = context;
        initLayout();
    }

    private void initLayout() {
        singleClick = new SingleClick(this);
        LayoutInflater mInflater = LayoutInflater.from(mContext);
        View view = mInflater.inflate(R.layout.layout_bottom_navigation, this, true);
        layoutRecord = view.findViewById(R.id.layout_record);
        singleClick.setView(layoutRecord);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.layout_record:
                if (callback != null)
                    callback.onStartRecord();
                break;
        }

    }

    public interface BottomNaviCallback{
        void onStartRecord();
    }
}
