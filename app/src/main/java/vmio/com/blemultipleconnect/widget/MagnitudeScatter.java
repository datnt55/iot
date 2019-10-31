package vmio.com.blemultipleconnect.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import vmio.com.blemultipleconnect.R;

/**
 * Created by DatNT on 1/31/2018.
 */

public class MagnitudeScatter extends View {
    private Context mThis;
    private Paint borderPaint;
    private Paint scatterPaint;
    public MagnitudeScatter(@NonNull Context context) {
        super(context);
        this.mThis = context;
        initComponents();
    }

    public MagnitudeScatter(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mThis = context;
        initComponents();
    }

    public MagnitudeScatter(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mThis = context;
        initComponents();
    }

    public MagnitudeScatter(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mThis = context;
        initComponents();
    }

    public void initComponents(){
        borderPaint = new Paint();
        borderPaint.setColor(ContextCompat.getColor(mThis, R.color.colorPrimaryDark));
        borderPaint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        int x = getWidth();
        int y = getHeight();
        int radius;
        radius = 100;
        borderPaint.setStyle(Paint.Style.STROKE);
        //canvas.drawPaint(borderPaint);
        canvas.drawCircle(x / 2, y / 2, radius, borderPaint);
    }
}
