package vmio.com.mioblelib.utilities;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Using this ALog instead of class Log
 */
public class ALog {

    private enum ALogMode {
        DEBUG,
        ERROR,
        INFORMATION,
        WARN,
        VERBOSE,
        // TODO: add more
    }

    public static void d(String tag, String msg) {
        _Log(tag, msg, ALogMode.DEBUG);
    }

    public static void e(String tag, String msg) {
        _Log(tag, msg, ALogMode.ERROR);
    }

    public static void i(String tag, String msg) {
        _Log(tag, msg, ALogMode.INFORMATION);
    }

    public static void v(String tag, String msg) {
        _Log(tag, msg, ALogMode.VERBOSE);
    }

    public static void w(String tag, String msg) {
        _Log(tag, msg, ALogMode.WARN);
    }

    private static BufferedWriter buf = null;
    private static String lock = "LockedLog";

    private static void _Log(String tag, String msg, ALogMode mode)
    {
        if (mode == ALogMode.DEBUG) Log.d(tag, msg);
        else if (mode == ALogMode.ERROR) Log.e(tag, msg);
        else if (mode == ALogMode.INFORMATION) Log.i(tag, msg);
        else if (mode == ALogMode.WARN) Log.w(tag, msg);
        else if (mode == ALogMode.VERBOSE) Log.v(tag, msg);

        //	do not log to file when Release
        //if(!Constant.isForDebug)
        //return;

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String dateLog = df.format(c.getTime());

        SimpleDateFormat df1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss a");
        String timeStamp = df1.format(c.getTime());

        File dir = new File(Define.mMioDirectory + "/Log");
        if(!dir.exists()){
            dir.mkdir();
        }

        File logFile = new File(dir, dateLog + ".log");

        // Synchonized access to Log buffer then write log
        synchronized (lock) {
            if (!logFile.exists()) {
                try {
                    if (buf != null) {
                        buf.flush();
                        buf.close();
                        buf = null;
                    }
                    logFile.createNewFile();
                } catch (IOException e) {
                    ALog.e("ALog", "ERROR = " + e.getMessage());
                }
            }

            try {
                if (buf == null) {
                    buf = new BufferedWriter(new FileWriter(logFile, true));
                }

                if (buf != null) {
                    buf.append("[" + timeStamp + "]" + " [" + tag + "] " + msg);
                    buf.newLine();
                    buf.flush();
                }
            } catch (IOException e) {
                Log.e("ALOG", "ERROR = " + e.getMessage());
            }
        }
    }
}
