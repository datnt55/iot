package vmio.com.blemultipleconnect.service;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import vmio.com.blemultipleconnect.Utilities.SharePreference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public abstract class OkHttpService {

    public enum Method {
        POST,
        GET
    }

    public abstract void onFailureApi(Call call, Exception e);

    public abstract void onResponseApi(Call call, Response response) throws IOException;

    private ProgressDialog dialog;

    private MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public OkHttpService(Method method, final Context context, String url, Map<String, Object> params, boolean isShowProgress) {
        if (method == Method.POST)
            postOkHttpApi(context, url, params, isShowProgress);
        else if (method == Method.GET)
            getOkHttpApi(context, url, params, isShowProgress);
    }

    public OkHttpService(Method method, final Context context, String url, JSONObject json, boolean isShowProgress) {
        if (method == Method.POST)
            postOkHttpApi(context, url, json, isShowProgress);
    }
    public static final int API_REQUEST_TIMEOUT = 15000; // SECOND
    private void postOkHttpApi(final Context context, final String url, final Map<String, Object> params, final boolean isShowProgress) {
        try {
            if (isShowProgress)
                showLoadingDialog(context);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .dns(new EasyDns())
                    .connectTimeout(API_REQUEST_TIMEOUT, MILLISECONDS)
                    .readTimeout(API_REQUEST_TIMEOUT, MILLISECONDS)
                    .writeTimeout(API_REQUEST_TIMEOUT, MILLISECONDS)
                    .retryOnConnectionFailure(false);

            final OkHttpClient client = builder.build();
            MultipartBody.Builder multipart = new MultipartBody.Builder();
            if (params != null)
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    if (param.getValue() instanceof File)
                        multipart.addFormDataPart(param.getKey(), ((File) param.getValue()).getName(), RequestBody.create(MediaType.parse("image/jpeg"), (File) param.getValue()));
                    else
                        multipart.addFormDataPart(param.getKey(), (String) param.getValue());
                }

            RequestBody requestBody = multipart
                    .setType(MultipartBody.FORM)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer " + new SharePreference(context).getToken())
                    //.addHeader("Device-Type", "android")
                    //.addHeader("version", Constants.version)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (dialog != null) dialog.dismiss();
                    onFailureApi(call, e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (dialog != null) dialog.dismiss();
                    if (response.isSuccessful() || response.code() == 400 || response.code() == 401 || response.code() == 403 || response.code() == 500)
                        onResponseApi(call, response);
//                    else
//                        showErrorTest(context, response.body().string());
                }
            });
        } catch (Exception e) {
            if (dialog != null) dialog.dismiss();
            onFailureApi(null, e);
        }
    }

    private void postOkHttpApi(final Context context, final String url, final JSONObject json, final boolean isShowProcess) {
        try {
            if (isShowProcess)
                showLoadingDialog(context);



            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .dns(new EasyDns())
                    .connectTimeout(API_REQUEST_TIMEOUT, MILLISECONDS)
                    .readTimeout(API_REQUEST_TIMEOUT, MILLISECONDS)
                    .writeTimeout(API_REQUEST_TIMEOUT, MILLISECONDS)
                    .retryOnConnectionFailure(false);

            final OkHttpClient client = builder.build();

            RequestBody body = RequestBody.create(JSON, json.toString());
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Accept", "application/json")
                    //.addHeader("Authorization", "Bearer " + accessToken)
                   // .addHeader("Device-Type", "android")
                   // .addHeader("version", Constants.version)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (dialog != null) dialog.dismiss();
                    onFailureApi(call, e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (dialog != null) dialog.dismiss();

                    String title = url + "\n" + json.toString();
                    if (response.isSuccessful() || response.code() == 400 || response.code() == 401 || response.code() == 403 || response.code() == 500)
                        onResponseApi(call, response);
                    else
                        showErrorTest(context, title + "\n" + response.body().string());
                }
            });

        } catch (Exception e) {
            if (dialog != null) dialog.dismiss();
            onFailureApi(null, e);
        }
    }

    private void getOkHttpApi(final Context context, final String url, final Map<String, Object> params, final boolean isShowProcess) {
        try {
            if (isShowProcess)
                showLoadingDialog(context);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .dns(new EasyDns())
                    .connectTimeout(API_REQUEST_TIMEOUT, MILLISECONDS)
                    .readTimeout(API_REQUEST_TIMEOUT, MILLISECONDS)
                    .writeTimeout(API_REQUEST_TIMEOUT, MILLISECONDS)
                    .retryOnConnectionFailure(false);

            final OkHttpClient client = builder.build();
            HttpUrl.Builder httpBuider = HttpUrl.parse(url).newBuilder();
            if (params != null) {
                for (Map.Entry<String, Object> param : params.entrySet()) {
                    httpBuider.addQueryParameter(param.getKey(), (String) param.getValue());
                }
            }
            Request request = new Request.Builder()
                    .url(httpBuider.build())
                    .get()
                    .addHeader("Accept", "application/json")
                    //.addHeader("Authorization", "Bearer " + accessToken)
                    //.addHeader("Device-Type", "android")
                    //.addHeader("version", Constants.version)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (dialog != null) dialog.dismiss();
                    onFailureApi(call, e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (dialog != null) dialog.dismiss();

                    if (dialog != null) dialog.dismiss();
                    String title = url + "\n";
                    for (Map.Entry<String, Object> param : params.entrySet()) {
                        title += param.getKey();
                        if (param.getValue() instanceof File)
                            title += ":" + ((File) param.getValue()).getAbsolutePath();
                        else
                            title += ":" + (String) param.getValue();
                    }

                    if (response.isSuccessful() || response.code() == 400 || response.code() == 401 || response.code() == 403 || response.code() == 500)
                        onResponseApi(call, response);
                    else
                        showErrorTest(context, title + "\n" + response.body().string());
                }
            });

        } catch (Exception e) {
            if (dialog != null) dialog.dismiss();
            onFailureApi(null, e);
        }
    }

    private void showLoadingDialog(final Context context) {
        dialog = new ProgressDialog(context);
        dialog.setMessage("Đang tải dữ liệu...");
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showErrorTest(final Context context, final String message) {
        new android.os.Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder dialog = new AlertDialog.Builder(context);
                dialog.setTitle("Lỗi server");
                dialog.setMessage(message);
                dialog.setCancelable(false);
                dialog.setNeutralButton("Thoát", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onFailureApi(null, null);
                    }
                });
                dialog.show();
            }
        }, 0);
    }
}
