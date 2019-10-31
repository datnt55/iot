package vmio.com.blemultipleconnect.socket;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

/**
 * Created by DatNT on 3/29/2018.
 */

public class SearchIPAsynTask extends AsyncTask<String, Void, String> {

    InterfaceProgress interfaceProgress;
    int MaxProgress;
    /** progress dialog to show user that the backup is processing. */
    private ProgressDialog dialog;

    /** application context. */
    Activity mActivity;

    public SearchIPAsynTask(Activity activity, InterfaceProgress callback) {
        this.mActivity = activity;
        this.interfaceProgress = callback;
        dialog = new ProgressDialog(activity);
    }

    @Override
    protected void onPreExecute() {
        this.dialog.setMessage("Searching ...");
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        this.dialog.show();
        this.interfaceProgress.doProgressPreExecute();
    }

    @Override
    protected String doInBackground(final String... args) {
        return interfaceProgress.doProgressInBackground(args);
    }

    @Override
    protected void onPostExecute(final String result) {
        this.interfaceProgress.doProgressPostExecute(result);
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public interface InterfaceProgress {
        void doProgressPreExecute();
        String doProgressInBackground(final String... args);
        void doProgressPostExecute(String res);
    }
}