package vmio.com.blemultipleconnect.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;

import vmio.com.blemultipleconnect.Utilities.SharePreference;

public class ServiceManager {
    private static final String TAG = "ServiceManager";
    public static ServiceManager mPingManager;
    private Context mContext;
    private boolean connected;
    private PingService mService;
    private OnInitializedCallback mCallback;
    private ServiceConnection mServiceConnection;
    class CustomServiceConnection implements ServiceConnection {
        final ServiceManager mBubbleManager;

        CustomServiceConnection(ServiceManager serviceManager) {
            this.mBubbleManager = serviceManager;
        }

        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = ((PingService.CustomBinder) iBinder).getService();
            connected = true;
            if (mCallback != null) {
                mCallback.OnInitialized();
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            connected = false;
        }
    }

    public static class PingManagerInstance {
        private ServiceManager mManager;

        public PingManagerInstance(Context context) {
            this.mManager = ServiceManager.getManager(context);
        }

        public PingManagerInstance addCallBack(OnInitializedCallback mCallback) {
            this.mManager.mCallback = mCallback;
            return this;
        }

        public ServiceManager getmManager() {
            return this.mManager;
        }
    }

    private static ServiceManager getManager(Context context) {
        if (mPingManager == null) {
            mPingManager = new ServiceManager(context);
        }
        return mPingManager;
    }

    public ServiceManager(Context context) {
        this.mContext = context;
        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceConnection = new CustomServiceConnection(this);
    }

    public void ping(){
        mService.sendDataCSVToServer(new SharePreference(mContext).getId());
    }

    public void startService() {
//        if (connected) {
//            stopService();
//        }
//        mContext.startService(new Intent(mContext, PingService.class));

        mContext.bindService(new Intent(mContext, PingService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    public void stopService() {
       // connected = false;
        this.mContext.unbindService(this.mServiceConnection);
        //this.mContext.stopService(new Intent(this.mContext, PingService.class));
    }

    public interface OnInitializedCallback {
        void OnInitialized();
    }

}
