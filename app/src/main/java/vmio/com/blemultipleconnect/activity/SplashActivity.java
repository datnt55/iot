package vmio.com.blemultipleconnect.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;
import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.ALog;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.DialogUtils;
import vmio.com.blemultipleconnect.Utilities.FileUtil;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.PermissionUtils;
import vmio.com.blemultipleconnect.Utilities.SharePreference;
import vmio.com.blemultipleconnect.service.BaseService;
import vmio.com.blemultipleconnect.service.OkHttpService;
import vmio.com.mioblelib.ble.Bluetooth;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static vmio.com.blemultipleconnect.activity.SettingActivity.mBlueToothSenSor;

public class SplashActivity extends AppCompatActivity {
    private String mMioDirectory = Environment.getExternalStoragePublicDirectory(Define.mMioDirectory/*"Mio"*/).getAbsolutePath();
    private String mMioUploadDirectory = Environment.getExternalStoragePublicDirectory(Define.mMioUploadDirectory/*"Mio/uploads"*/).getAbsolutePath();
    private Activity mContext;
    private static final int REQ_ENABLE_BT = 0;
    private static boolean mFirstTimeEnterSplash = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PermissionUtils.checkAndRequestPermissions(this)) {
            initComponents();
        }

    }

    private void initComponents() {
        Global.ApplicationVersion = CommonUtils.getAppVersion(this);
        Global.DeviceId = CommonUtils.getDeviceId(this);

        // Create Folders
        FileUtil.createNecessaryFolder();
        FileUtil.checkDialogFreeStorage(this);

        // Log start app time
        if (mFirstTimeEnterSplash) {
            mFirstTimeEnterSplash = false;
            ALog.d("APP", "================================= START APP =================================");
            try {
                PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
                int version = pInfo.versionCode;
                String versionName = pInfo.versionName;
                ALog.d("APP", "Version Name " + versionName + " - Version Code " + version);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        setContentView(R.layout.activity_splash);
        mContext = this;
        CommonUtils.dimensionScreen(this);
        boolean stg = Define.HOST.contains("stg");
        if (stg)
            findViewById(R.id.txt_version).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.txt_version).setVisibility(View.GONE);
        if (Bluetooth.getInstance(this, null).isBtEnabled()) {
            if (CommonUtils.isOnline(this)) {
                new MoveDataToUploadFolder().execute();
            } else
                getWorkerId();
        } else {
            // Request BT adapter to be turned on
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQ_ENABLE_BT);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionUtils.REQUEST_ID_MULTIPLE_PERMISSIONS) {
            if ((grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED))
                initComponents();
        }
    }

    public class MoveDataToUploadFolder extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            File path = new File(mMioDirectory);
            if (!path.exists()) {
                path.mkdirs();
            }
            // uploads folder. Not sent yet csv files stored here
            File pathUpload = new File(mMioUploadDirectory);
            if (!pathUpload.exists()) {
                pathUpload.mkdirs();
            }
            File[] files = CommonUtils.GetSortedFileInFolder(mMioDirectory, ".csv");
            for (int i = 0; i < files.length; i++) {
                if (files[i].exists())
                    CommonUtils.MoveFile(files[i], mMioUploadDirectory);
            }
            File[] filesLog = CommonUtils.GetSortedFileInFolder(mMioDirectory, ".txt");
            for (int i = 0; i < filesLog.length; i++) {
                if (filesLog[i].exists())
                    CommonUtils.MoveFile(filesLog[i], mMioUploadDirectory);
            }
            File[] filesUpload = CommonUtils.GetSortedFileInFolder(mMioUploadDirectory, ".txt");
            for (int i = 0; i < filesUpload.length; i++) {
                if (filesUpload[i].exists())
                    sendLogFileToServer(filesUpload[i].getAbsolutePath());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    getWorkerId();
                } else {
                    ActivityCompat.requestPermissions(mContext, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 12);
                }
            } else
                getWorkerId();
        }
    }

    // public static final String URL_CSV = "http://iot.demo.miosys.vn/upload.php";
    // Send file to server and delete file if success
    private void sendLogFileToServer(final String filePath) {
        final File sendFile = new File(filePath);
        RequestParams params;
        params = new RequestParams();
        try {
            params.put("data", sendFile);//new FileInputStream(sendFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //params.setForceMultipartEntityContentType(true);
        BaseService.getHttpClient().post(Define.URL_CSV, params, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {

            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                // called when response HTTP status is "200 OK"
                Log.i("JSON CSV", new String(responseBody));
                try {
                    final JSONObject json = new JSONObject(new String(responseBody));
                    final String status = json.getString("status");
                    if (status.equals("success")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "Uploaded: " + sendFile.getName(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        sendFile.delete();

                    } else {
                        final String message = json.getString("message");
                        final boolean fileExisted = message.toLowerCase().contains("already exist");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, fileExisted ? "Already uploaded: " + sendFile.getName() : "Upload failed: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                        if (fileExisted) {
                            File sendFile = new File(filePath);
                            sendFile.delete();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
            }

            @Override
            public void onRetry(int retryNo) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_ENABLE_BT:
                initComponents();
                break;
        }
    }

    private void getWorkerId() {
//        Map<String, Object> params = new HashMap<>();
//        params.put("uuid", CommonUtils.getUUID(this));
//        params.put("app_token", "6c17d2af3d615c155d90408a8d281fe0");
//        new OkHttpService(OkHttpService.Method.POST, this, Define.URL_GET_WORKER_ID, params, false) {
//            @Override
//            public void onFailureApi(Call call, Exception e) {
//                Log.e("Error",e.getMessage());
//            }
//
//            @Override
//            public void onResponseApi(Call call, Response response) throws IOException {
//                String result = response.body().string();
//                try {
//                    JSONObject json = new JSONObject(result);
//                    JSONObject data = json.getJSONObject("data");
//                    String name = data.getString("name");
//                    String username = data.getString("username");
//                    new SharePreference(mContext).saveId(username);
//                    new SharePreference(mContext).saveWorkerName(name);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
//                                startActivity(intent);
//                                finish();
//                            }
//                        }, 1000);
//                    }
//                });
//            }
//        };
        if (new SharePreference(this).getToken().equals("")){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 1000);
        }else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, 1000);
        }

    }
}
