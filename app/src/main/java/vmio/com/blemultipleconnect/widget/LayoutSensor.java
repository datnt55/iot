package vmio.com.blemultipleconnect.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;

/**
 * Created by DatNT on 12/14/2017.
 */

public class LayoutSensor extends LinearLayout {
    private Context mThis;
    private TextView title;
    private TableLayout tableLayout;
    public LayoutSensor(Context context) {
        super(context);
        this.mThis = context;
        initComponents();
    }

    public LayoutSensor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LayoutSensor(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LayoutSensor(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initComponents(){
        setElevation(CommonUtils.convertDpToPx(3, mThis));
        title = new TextView(mThis);
        LinearLayout.LayoutParams paramsTitle = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsTitle.topMargin =  CommonUtils.convertDpToPx(20, mThis);
        paramsTitle.leftMargin =  CommonUtils.convertDpToPx(20, mThis);
        title.setLayoutParams(paramsTitle);
        title.setTextSize(20);
        title.setTextColor(ContextCompat.getColor(mThis, R.color.dark));
        title.setTypeface(Typeface.DEFAULT_BOLD);

        this.setOrientation(LinearLayout.VERTICAL);
        this.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin =  CommonUtils.convertDpToPx(20, mThis);
        this.setLayoutParams(params);

        tableLayout = new TableLayout(mThis);
        LinearLayout.LayoutParams paramsTable = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tableLayout.setLayoutParams(paramsTable);

        View view = new View(mThis);
        LinearLayout.LayoutParams paramsLine = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        paramsLine.leftMargin = CommonUtils.convertDpToPx(20, mThis);
        paramsLine.topMargin = CommonUtils.convertDpToPx(10, mThis);
        paramsLine.bottomMargin = CommonUtils.convertDpToPx(20, mThis);
        view.setLayoutParams(paramsLine);
        view.setBackgroundColor(Color.parseColor("#aaaaaa"));

        tableLayout.removeAllViews();
        this.addView(title);
        this.addView(view);
        this.addView(tableLayout);
    }

    public void setTitle(String title){
        this.title.setText(title);
    }

    public TextView getTitle(){
        return this.title;
    }
    public TableLayout  getTable(){
        return this.tableLayout;
    }

}
