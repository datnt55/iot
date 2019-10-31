package vmio.com.blemultipleconnect.thread;

import static vmio.com.blemultipleconnect.Utilities.Define.CHECK_ACTIVE_SENSOR_INTERVAL;
import static vmio.com.blemultipleconnect.Utilities.Define.SCANNING_SENSOR_INTERVAL;

/**
 * Created by DatNT on 12/14/2017.
 */

public class CheckBleActiveThread implements Runnable {
    private CheckBleActiveCallback callback;
    private boolean stopFlag;
    private Thread mThread;

    public CheckBleActiveThread(CheckBleActiveCallback callback) {
        this.callback = callback;
        this.stopFlag = false;
        mThread = new Thread(this);
    }

    public synchronized void start() {
        if (!mThread.isAlive())
            mThread.start();
    }

    @Override
    public void run() {
        try {
            while (!stopFlag) {
                if (callback != null)
                    callback.onBleCheckActive();
                Thread.sleep(CHECK_ACTIVE_SENSOR_INTERVAL);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        synchronized (Global.startThreadScanBle) {
//            Global.startThreadScanBle = false;
//        }
    }

    public void stop() {
        this.stopFlag = true;
    }


    public interface CheckBleActiveCallback {
        void onBleCheckActive();
    }
}
