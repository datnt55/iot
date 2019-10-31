package vmio.com.blemultipleconnect.thread;

import static vmio.com.blemultipleconnect.Utilities.Define.SAVE_CSV_INTERVAL;
import static vmio.com.blemultipleconnect.Utilities.Define.SCANNING_SENSOR_INTERVAL;

/**
 * Created by DatNT on 12/14/2017.
 */

public class SaveCSVThread implements Runnable {
    private SaveDataToCSVCallback  callback;
    private boolean stopFlag;
    private Thread mThread;
    public SaveCSVThread(SaveDataToCSVCallback callback) {
        this.callback = callback;
        this. stopFlag = false;
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
                if (callback !=null)
                    callback.onSaveToCSV();
                Thread.sleep(SAVE_CSV_INTERVAL);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        synchronized (Global.startThreadScanBle) {
//            Global.startThreadScanBle = false;
//        }
    }

    public void stop(){
        this.stopFlag = true;
    }


    public interface SaveDataToCSVCallback{
        void onSaveToCSV();
    }
}
