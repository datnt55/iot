package vmio.com.mioblelib.ble;

import android.os.Handler;

import java.util.ArrayList;

import vmio.com.mioblelib.utilities.ALog;

/**
 * Created by hp on 10/23/2017.
 */

public class BleWaitingQueue {

    final String TAG = "WQU";

    public enum WaitAction {
        NONE,
        WAIT_FOR_SCAN,
        WAIT_FOR_CONNECT,
        WAIT_FOR_DISCOVER,
        WAIT_FOR_DECONFIG
    }

    public interface BleNextActionCallbacks {
        void nextScan(BleDevice ble);
        void nextDiscover(BleDevice ble);
        void nextConnect(BleDevice ble);
        void nextDeconfigure(BleDevice ble);
        void finished();
    }

    public class BleWaitItem {
        BleDevice ble;
        WaitAction action;
        int timeOut;
        public BleWaitItem(BleDevice _ble, WaitAction _action, int _timeOut) {
            ble = _ble;
            action = _action;
            timeOut = _timeOut;
        }
    }

    public static ArrayList<BleWaitItem> mWaitingBLEs = new ArrayList<>(); // list of waiting for connect BLE devices since BLE have to be connect sequentially
    BleNextActionCallbacks mCallBack = null;

    // process is in progress
    boolean isProcessing = false;
    boolean getIsProcessing() { return isProcessing; }
    BleWaitItem bleQueueItem = null;

    public BleWaitingQueue(BleNextActionCallbacks callbacks)
    {
        mCallBack = callbacks;
    }

    synchronized public void clear()
    {
        mWaitingBLEs.clear();
    }

    synchronized public void addProcess(BleDevice ble, WaitAction act, int timeout) {
        ALog.d(TAG, "[" + ble.getAddress() + "]" + " --> Add process: " + act.toString());
        BleWaitItem bleItem = new BleWaitItem(ble, act, timeout);
        mWaitingBLEs.add(bleItem);
    }

    synchronized public boolean processNext() {
        // other process is doing
        if(isProcessing) {
            ALog.d(TAG, "Failed: processNext called when other task in-progress");
            return false;
        }

        // post finish handler
        if(mWaitingBLEs.size() <= 0) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ALog.d(TAG, "[processNext]: Queue Empty. Nothing process");
                    mCallBack.finished();
                    isProcessing = false;
                }
            }, 10);
            isProcessing = false;
            return false;
        }

        // post next process handler
        bleQueueItem = mWaitingBLEs.remove(0);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ALog.d(TAG, "[" + bleQueueItem.ble.getAddress() + "]" + " --> Next process: " + bleQueueItem.action.toString());
                if(mCallBack != null) {
                    switch (bleQueueItem.action) {
                        case WAIT_FOR_SCAN:
                            mCallBack.nextScan(bleQueueItem.ble);
                            break;
                        case WAIT_FOR_DISCOVER:
                            mCallBack.nextDiscover(bleQueueItem.ble);
                            break;
                        case WAIT_FOR_CONNECT:
                            mCallBack.nextConnect(bleQueueItem.ble);
                            break;
                        case WAIT_FOR_DECONFIG:
                            mCallBack.nextDeconfigure(bleQueueItem.ble);
                            break;
                        case NONE:
                            break;
                    }
                }
                isProcessing = false;
                // if task finished before timeout then cancel current timeout task
                cancelTimeoutTask();
            }
        }, 10);
        isProcessing = true;

        // post timeout handler
        if(mHandlerProcessNextSensor != null) {
            mHandlerProcessNextSensor = new Handler();
            mHandlerProcessNextSensor.postDelayed(mRunnableProcessNextSensor, bleQueueItem.timeOut);
        }
        return true;
    }

    // cancel current timeout task
    private void cancelTimeoutTask()
    {
        if(mHandlerProcessNextSensor != null) {
            mHandlerProcessNextSensor.removeCallbacks(mRunnableProcessNextSensor);
            mHandlerProcessNextSensor = null;
        }
    }

    private WaitAction waitAction = WaitAction.NONE;
    private Handler mHandlerProcessNextSensor = null;
    private Runnable mRunnableProcessNextSensor = new Runnable() {
        @Override
        public void run() {
            // if current task already finished
            if(!isProcessing)
                return;
            isProcessing = false;
            ALog.d(TAG, "[" + bleQueueItem.ble.getAddress() + "]" + " --> TIMEOUT: " + bleQueueItem.action.toString() + "[ms]. Process Next Handler");
            processNext();
        }
    };

}
