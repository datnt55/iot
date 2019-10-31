package vmio.com.blemultipleconnect.widget;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import vmio.com.blemultipleconnect.R;

public class GoProCheckDialog extends Dialog implements android.view.View.OnClickListener {

    public Activity activity;
    public GoProDialogCallback callback;

    public GoProCheckDialog(Activity a) {
        super(a);
        this.activity = a;
    }

    public void setOnCallback(GoProDialogCallback callback){
        this.callback = callback;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_connect_gopro);
        Button btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                dismiss();
                if (callback != null)
                    callback.onStart();
                break;
            default:
                break;
        }
        dismiss();
    }

    public interface GoProDialogCallback{
        void onStart();
    }
}

