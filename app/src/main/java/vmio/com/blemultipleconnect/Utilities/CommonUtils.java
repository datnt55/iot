package vmio.com.blemultipleconnect.Utilities;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Created by hp on 6/21/2017.
 */

public class CommonUtils {

    public static boolean isOnline(Context mContext) {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    // Move file to new location
    public static boolean MoveFile(File currentFile, String targetDir) {
        try {
            File to = new File(targetDir + "/" + currentFile.getName());
            return currentFile.renameTo(to);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //  List all file in directory with extension
    public static File[] GetSortedFileInFolder(String Dir, final String FileExt) {
        File directory = new File(Dir);
        File[] files = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(FileExt);
            }
        });

        Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
            }
        });
        return files;
    }


    public static boolean externalMemoryAvailable(Context context) {
        return ContextCompat.getExternalFilesDirs(context, null).length >= 2;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static List<String> getSdCardPaths(final Context context, final boolean includePrimaryExternalStorage) {
        final File[] externalCacheDirs = ContextCompat.getExternalCacheDirs(context);
        if (externalCacheDirs == null || externalCacheDirs.length == 0)
            return null;
        if (externalCacheDirs.length == 1) {
            if (externalCacheDirs[0] == null)
                return null;
            final String storageState = EnvironmentCompat.getStorageState(externalCacheDirs[0]);
            if (!Environment.MEDIA_MOUNTED.equals(storageState))
                return null;
            if (!includePrimaryExternalStorage && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && Environment.isExternalStorageEmulated())
                return null;
        }
        final List<String> result = new ArrayList<>();
        if (includePrimaryExternalStorage || externalCacheDirs.length == 1)
            result.add(getRootOfInnerSdCardFolder(externalCacheDirs[0]));
        for (int i = 1; i < externalCacheDirs.length; ++i) {
            final File file = externalCacheDirs[i];
            if (file == null)
                continue;
            final String storageState = EnvironmentCompat.getStorageState(file);
            if (Environment.MEDIA_MOUNTED.equals(storageState))
                result.add(getRootOfInnerSdCardFolder(externalCacheDirs[i]));
        }
        if (result.isEmpty())
            return null;
        return result;
    }

    public static String getUUID(Context context) {
        TelephonyManager tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tManager.getDeviceId() == null)
            return randomUUID();
        return tManager.getDeviceId();
    }

    public static String randomUUID() {
        Random r = new Random();
        int range = 8999999;
        int result = r.nextInt(9999999 - range) + range;
        return String.valueOf(result);
    }
    /**
     * Given any file/folder inside an sd card, this will return the path of the sd card
     */
    private static String getRootOfInnerSdCardFolder(File file) {
        if (file == null)
            return null;
        final long totalSpace = file.getTotalSpace();
        while (true) {
            final File parentFile = file.getParentFile();
            if (parentFile == null || parentFile.getTotalSpace() != totalSpace)
                return file.getAbsolutePath();
            file = parentFile;
        }
    }

    public static File getExternalSdCard() {
        File externalStorage = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            File storage = new File("/storage");

            if (storage.exists()) {
                File[] files = storage.listFiles();

                for (File file : files) {
                    if (file.exists()) {
                        try {
                            if (Environment.isExternalStorageRemovable(file)) {
                                externalStorage = file;
                                break;
                            }
                        } catch (Exception e) {
                            Log.e("TAG", e.toString());
                        }
                    }
                }
            }
        } else {
            // do one of many old methods
            // I believe Doomsknight's method is the best option here
        }

        return externalStorage;
    }

    public static long calculateSdcard(String path) {
        StatFs internalStatFs = new StatFs(path);
        long internalTotal;
        long internalFree;

        StatFs externalStatFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        long externalTotal;
        long externalFree;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            internalTotal = (internalStatFs.getBlockCountLong() * internalStatFs.getBlockSizeLong()) / (Global.KILOBYTE * Global.KILOBYTE);
            internalFree = (internalStatFs.getAvailableBlocksLong() * internalStatFs.getBlockSizeLong()) / (Global.KILOBYTE * Global.KILOBYTE);
            externalTotal = (externalStatFs.getBlockCountLong() * externalStatFs.getBlockSizeLong()) / (Global.KILOBYTE * Global.KILOBYTE);
            externalFree = (externalStatFs.getAvailableBlocksLong() * externalStatFs.getBlockSizeLong()) / (Global.KILOBYTE * Global.KILOBYTE);
        } else {
            internalTotal = ((long) internalStatFs.getBlockCountLong() * (long) internalStatFs.getBlockSizeLong()) / (Global.KILOBYTE * Global.KILOBYTE);
            internalFree = ((long) internalStatFs.getAvailableBlocksLong() * (long) internalStatFs.getBlockSizeLong()) / (Global.KILOBYTE * Global.KILOBYTE);
            externalTotal = ((long) externalStatFs.getBlockCountLong() * (long) externalStatFs.getBlockSizeLong()) / (Global.KILOBYTE * Global.KILOBYTE);
            externalFree = ((long) externalStatFs.getAvailableBlocksLong() * (long) externalStatFs.getBlockSizeLong()) / (Global.KILOBYTE * Global.KILOBYTE);
        }

        long total = internalTotal + externalTotal;
        long free = internalFree + externalFree;
        long used = total - free;
        return free;
    }

    public static long getFreeStorageSdCard() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = (long) stat.getBlockSizeLong() * (long) stat.getAvailableBlocksLong();
        long megAvailable = bytesAvailable / (1024 * 1024);
        Log.e("", "Available MB : " + megAvailable);
        return megAvailable;
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    // Convert from dpi to pixel
    public static int convertDpToPx(int dp, Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    public static void dimensionScreen(Activity activity) {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(displaymetrics);
        } else {
            activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        }
        Global.WIDTH_SCREEN = displaymetrics.widthPixels;
        Global.HEIGHT_SCREEN = displaymetrics.heightPixels;
    }

    public static String getPosition(int position) {
        switch (position) {
            case 0:
                return "頭部";
            case 1:
                return "左腕";
            case 2:
                return "右腕";
            case 3:
                return "左脚";
            case 4:
                return "右脚";
            case 5:
                return "胸部";
            case 6:
                return "臀部";
            default:
                return "Undefine";
        }
    }

    public static String getAppVersion(Context context) {
        PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pInfo.versionName;
    }

    public static String getDeviceId(Context context) {
        TelephonyManager tManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tManager.getDeviceId();
    }

    public static double convertGToMPS2(double gravity){
        return SensorManager.STANDARD_GRAVITY * gravity;
    }

    public static double Conv_DegreesToRadians(double degrees)
    {
        return degrees * (Math.PI / 180d);
    }

    public static String timerFormat(long count){
        int hour = (int) (count/1000/60/60);
        int min = (int) (count/1000/60 - hour*60);
        int sec = (int) (count/1000 - min*60 - hour*60*60);

        String txtSec= "",txtMin = "", txtHour = "";

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

        return txtHour+":"+txtMin+":"+txtSec;
    }

    // [20180905    VMio] Adding Debug logging function
    static File logFile = null;
    static BufferedWriter bufWriter = null;
    public static void printLog(String text)
    {
        if(!Define.DEBUG)
            return;
        if(logFile == null)
            logFile = new File(Define.mMioTempDirectory + "/DebugLog.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            if(bufWriter == null) {
                bufWriter = new BufferedWriter(new FileWriter(logFile, true));
                bufWriter.append("========================= START DEBUG ==============================\n");
            }
            String line = String.format("[%s] %s", getTimeStamp(), text);
            bufWriter.append(line);
            bufWriter.newLine();
            bufWriter.flush();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    static public void closeLog()
    {
        try {
            if (bufWriter != null)
                bufWriter.close();
            bufWriter = null;
            logFile = null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    // [20180905    VMio] Get current timestamp in milisec precision
    static public String getTimeStamp()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS");
        return formatter.format(new java.util.Date());
    }

    public static boolean isNetworkConnectionAvailable(Activity activity) {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        NetworkInfo.State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }
}
