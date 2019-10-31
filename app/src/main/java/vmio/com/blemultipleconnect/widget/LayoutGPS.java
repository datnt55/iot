package vmio.com.blemultipleconnect.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.platypii.baseline.util.Numbers;

import java.util.Locale;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;

/**
 * Created by DatNT on 12/14/2017.
 */

public class LayoutGPS extends LinearLayout{
    private Context mThis;
    private TextView title;
    private TextView txtLat, txtLon, txtAccuracy;
    private ImageView imgGNS;
    public LayoutGPS(Context context) {
        super(context);
        this.mThis = context;
        initComponent();
    }

    public LayoutGPS(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LayoutGPS(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LayoutGPS(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initComponent(){
        setPadding(0,CommonUtils.convertDpToPx(10, mThis),0,0 );
        setElevation(CommonUtils.convertDpToPx(3, mThis));
        LinearLayout layoutTitle = new LinearLayout(mThis);
        layoutTitle.setOrientation(LinearLayout.HORIZONTAL);
        title = new TextView(mThis);
        title.setId(1);

        layoutTitle.addView(title);
        LinearLayout.LayoutParams paramsTitle = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsTitle.gravity = Gravity.BOTTOM;
        paramsTitle.leftMargin = CommonUtils.convertDpToPx(20, mThis);
        title.setLayoutParams(paramsTitle);
        title.setTextSize(20);
        title.setTextColor(ContextCompat.getColor(mThis, R.color.dark));
        title.setTypeface(Typeface.DEFAULT_BOLD);

        imgGNS = new ImageView(mThis);
        imgGNS.setAdjustViewBounds(true);
        layoutTitle.addView(imgGNS);
        LinearLayout.LayoutParams paramsGNS = new LinearLayout.LayoutParams(CommonUtils.convertDpToPx(70,mThis), ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsGNS.topMargin = CommonUtils.convertDpToPx(20, mThis);
        paramsGNS.leftMargin = CommonUtils.convertDpToPx(10, mThis);
        imgGNS.setLayoutParams(paramsGNS);

        View view = new View(mThis);
        LinearLayout.LayoutParams paramsLine = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        paramsLine.leftMargin = CommonUtils.convertDpToPx(20, mThis);
        paramsLine.topMargin = CommonUtils.convertDpToPx(10, mThis);
        paramsLine.bottomMargin = CommonUtils.convertDpToPx(20, mThis);
        view.setLayoutParams(paramsLine);
        view.setBackgroundColor(Color.parseColor("#aaaaaa"));


        setOrientation(LinearLayout.VERTICAL);
        setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = CommonUtils.convertDpToPx(20, mThis);
        setLayoutParams(params);
        addView(layoutTitle);
        addView(view);
    }

    public void setLabel(String label){
        title.setText(label);
    }
    public void setLabelColor(int color){
        title.setTextColor(color);
    }

    public void createValueView(){
        txtLat = new TextView(mThis);
        LinearLayout.LayoutParams paramsLat = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsLat.leftMargin = CommonUtils.convertDpToPx(20, mThis);
        txtLat.setLayoutParams(paramsLat);
        txtLat.setTextSize(14);

        txtLon = new TextView(mThis);
        LinearLayout.LayoutParams paramsLon = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsLon.leftMargin = CommonUtils.convertDpToPx(20, mThis);
        txtLon.setLayoutParams(paramsLon);
        txtLon.setTextSize(14);

        txtAccuracy = new TextView(mThis);
        LinearLayout.LayoutParams paramsAccuracy = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        paramsAccuracy.leftMargin = CommonUtils.convertDpToPx(20, mThis);
        paramsAccuracy.bottomMargin = CommonUtils.convertDpToPx(20, mThis);
        txtAccuracy.setLayoutParams(paramsAccuracy);
        txtAccuracy.setTextSize(14);

        txtLat.setText(mThis.getString(R.string.latitude)+"Unknown");
        txtLon.setText(mThis.getString(R.string.longitude)+"Unknown");
        txtAccuracy.setText(mThis.getString(R.string.accuracy)+"Unknown");
        addView(txtLat);
        addView(txtLon);
        addView(txtAccuracy);
    }

    public boolean isConnected(){
        return txtLat != null;
    }

    public void setLatitude(double latitude){
        if (Numbers.isReal(latitude))
            txtLat.setText(String.format(Locale.getDefault(), "緯度: %.6f", latitude));
        else
            txtLat.setText(mThis.getString(R.string.latitude)+"Unknown");
    }

    public void setLongitude(double longitude){
        if (Numbers.isReal(longitude))
            txtLon.setText(String.format(Locale.getDefault(), "経度: %.6f", longitude));
        else
            txtLon.setText(mThis.getString(R.string.longitude)+"Unknown");
    }

    public void setAccuracy(double accuracy){
        if (Numbers.isReal(accuracy)) {
            String strAccuracy = "";
            if (accuracy <= 5)
                strAccuracy = "GPS精度: <font color='green'>"+ String.format(Locale.getDefault(), "%.6f", accuracy) +" m</font>";
            else if (accuracy > 5 && accuracy <= 10)
                strAccuracy = "GPS精度: <font color='#aad809'>"+ String.format(Locale.getDefault(), "%.6f", accuracy) +" m</font>";
            else if (accuracy > 10 && accuracy <= 20)
                strAccuracy = "GPS精度: <font color='#fba500'>"+ String.format(Locale.getDefault(), "%.6f", accuracy) +" m</font>";
            else
                strAccuracy = "GPS精度: <font color='red'>"+ String.format(Locale.getDefault(), "%.6f", accuracy) +" m</font>";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                txtAccuracy.setText(Html.fromHtml(strAccuracy, Html.FROM_HTML_MODE_COMPACT));
            } else {
                txtAccuracy.setText(Html.fromHtml(strAccuracy));
            }
        }else
            txtAccuracy.setText("GPS精度: Unknown");
    }

    public boolean isLockGPS(){
        if (txtLat.getText().equals((mThis.getString(R.string.latitude)+"Unknown")))
            return false;
        return true;
    }

    public void setIconGNS(int drawable){
        imgGNS.setImageResource(drawable);
    }
}
