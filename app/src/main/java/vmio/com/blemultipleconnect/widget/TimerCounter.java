package vmio.com.blemultipleconnect.widget;

import android.widget.TextView;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import vmio.com.blemultipleconnect.R;

/**
 * Created by DatNT on 12/14/2017.
 */

public class TimerCounter {
    private Timer T;
    private long count = 0;
    private TimerCounterListener listener;
    public TimerCounter(TimerCounterListener listener) {
        T=new Timer();
        this.listener = listener;
        startCountUpTimer();
    }

    private void startCountUpTimer (){
        count = 0;
        T.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int hour = (int) (count/1000/60/60);
                int min = (int) (count/1000/60 - hour*60);
                int sec = (int) (count/1000 - min*60 - hour*60*60);
                int mili = (int) (count - sec*1000 - min*60*1000 - hour*60*60*1000);

                String txtMili = "", txtSec= "",txtMin = "", txtHour = "";
                if (mili < 100)
                    txtMili = "0"+mili;
                else
                    txtMili = mili+"";
                if (sec < 10)
                    txtSec = "0"+sec;
                else
                    txtSec = ""+sec;
                if (min < 10)
                    txtMin = "0"+ min;
                else
                    txtMin = ""+ min;
                if (hour< 10)
                    txtHour = "0"+ hour;
                else
                    txtHour = ""+ hour;

                if (listener != null)
                    listener.onTimerCount(txtHour+":"+txtMin+":"+txtSec+"."+txtMili);
                count++;
            }
        }, 1, 1);
    }

    public void cancel(){
        this.T.cancel();
    }
    public interface TimerCounterListener{
        void onTimerCount(String tick);
    }
}
