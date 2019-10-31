package vmio.com.blemultipleconnect.thread;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Created by DatNT on 4/16/2018.
 */

public class GoProService {
    private Activity context;
    private ConnectGoProListener listener;

    public GoProService(Activity context, ConnectGoProListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void Record() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("http://10.5.5.9/gp/gpControl/command/mode?p=0").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
//                context.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show();
//                    }
//                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String result = response.message().toString();
                    joinDevice();
                }
            }
        });


    }

    public void joinDevice() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("http://10.5.5.9/gp/gpControl/command/shutter?p=1").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//                context.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(context, "failed to record camera", Toast.LENGTH_SHORT).show();
//                    }
//                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String result = response.message().toString();
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Start record", Toast.LENGTH_SHORT).show();
                        }
                    });
                    if (listener != null)
                        listener.onConnected();
                }
            }
        });
    }

    public void changeToMobileNetwork()
    {
        Log.d("Mio","Requesting CELLULAR network connectivity...");
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR) //NetworkCapabilities.TRANSPORT_WIFI
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

        // Request Mobile data network
        connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback()
        {
            /**
             * Called when the framework connects and has declared a new network ready for use.
             * This callback may be called more than once if the Network that is
             * satisfying the request changes.
             *
             * This method will be called on non-UI thread, so beware not to use any UI updates directly.
             *
             * @param network The Network that the satisfying the request.
             */
            @Override
            public void onAvailable(final Network network)
            {
                Log.d("Mio","Got available network: " + network.toString());
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                if(android.os.Build.VERSION.SDK_INT < 23) {
                    cm.setProcessDefaultNetwork(network);
                } else {
                    cm.bindProcessToNetwork(network);
                }
                // Test the connection over mobile network
                Log.d("Mio","Do request test page from remote http server...");
                try {
                    final HttpURLConnection connection = (HttpURLConnection) network.openConnection(new URL("http://www.google.com"));
                    connection.connect();
                    Log.d("Mio", "Response Code=" + connection.getResponseCode());
                    Log.d("Mio", "Response Message=" + connection.getResponseMessage());
                    //Toast.makeText(context, "Connect to google success",Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Log.e("Mio", e.toString());
                } finally {
                    cm.unregisterNetworkCallback(this);
                }
            }
        });
    }

    public void changeToWifiNetwork(final boolean start)
    {
        Log.d("Mio","Requesting Wifi network connectivity...");
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);

        final NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI) //NetworkCapabilities.TRANSPORT_WIFI
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

        // Request Mobile data network
        connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback()
        {
            /**
             * Called when the framework connects and has declared a new network ready for use.
             * This callback may be called more than once if the Network that is
             * satisfying the request changes.
             *
             * This method will be called on non-UI thread, so beware not to use any UI updates directly.
             *
             * @param network The Network that the satisfying the request.
             */
            @Override
            public void onAvailable(final Network network)
            {
                Log.d("Mio","Got available network: " + network.toString());
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                if(android.os.Build.VERSION.SDK_INT < 23) {
                    cm.setProcessDefaultNetwork(network);
                } else {
                    cm.bindProcessToNetwork(network);
                }
                if (start)
                    Record();
                else
                    stopRecord();
            }
        });
    }
    public void stopRecord() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("http://10.5.5.9/gp/gpControl/command/shutter?p=0").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
//                context.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(context, "failed to record camera", Toast.LENGTH_SHORT).show();
//                    }
//                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String result = response.message().toString();
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Stop record", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }


    public interface ConnectGoProListener {
        void onConnected();
    }
}
