package vmio.com.blemultipleconnect.Utilities;

/**
 * Created by DatNT on 6/22/2017.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;

import static vmio.com.blemultipleconnect.Utilities.Define.mMioDirectory;
import static vmio.com.blemultipleconnect.Utilities.Define.mMioTempDirectory;
import static vmio.com.blemultipleconnect.Utilities.Define.mMioUploadSaveDirectory;

@SuppressLint("NewApi")
public final class FileUtil {

    static String TAG="TAG";
    private static final String PRIMARY_VOLUME_NAME = "primary";



    public static boolean isKitkat() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT;
    }
    public static boolean isAndroid5() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }


    @NonNull
    public static String getSdCardPath() {
        String sdCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

        try {
            sdCardDirectory = new File(sdCardDirectory).getCanonicalPath();
        }
        catch (IOException ioe) {
            Log.e(TAG, "Could not get SD directory", ioe);
        }
        return sdCardDirectory;
    }


    public static ArrayList<String> getExtSdCardPaths(Context con) {
        ArrayList<String> paths = new ArrayList<String>();
        File[] files = ContextCompat.getExternalFilesDirs(con, "external");
        File firstFile = files[0];
        for (File file : files) {
            if (file != null && !file.equals(firstFile)) {
                int index = file.getAbsolutePath().lastIndexOf("/Android/data");
                if (index < 0) {
                    Log.w("", "Unexpected external file dir: " + file.getAbsolutePath());
                }
                else {
                    String path = file.getAbsolutePath().substring(0, index);
                    try {
                        path = new File(path).getCanonicalPath();
                    }
                    catch (IOException e) {
                        // Keep non-canonical path.
                    }
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    @Nullable
    public static String getFullPathFromTreeUri(@Nullable final Uri treeUri, Context con) {
        if (treeUri == null) {
            return null;
        }
        String volumePath = FileUtil.getVolumePath(FileUtil.getVolumeIdFromTreeUri(treeUri),con);
        if (volumePath == null) {
            return File.separator;
        }
        if (volumePath.endsWith(File.separator)) {
            volumePath = volumePath.substring(0, volumePath.length() - 1);
        }

        String documentPath = FileUtil.getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator)) {
            documentPath = documentPath.substring(0, documentPath.length() - 1);
        }

        if (documentPath.length() > 0) {
            if (documentPath.startsWith(File.separator)) {
                return volumePath + documentPath;
            }
            else {
                return volumePath + File.separator + documentPath;
            }
        }
        else {
            return volumePath;
        }
    }

    // Create new log file named as current timestamp
    public static void createNecessaryFolder() {
        File path = new File(mMioDirectory);
        if (!path.exists()) {
            path.mkdirs();
        }

        path = new File(mMioTempDirectory);
        if (!path.exists()) {
            path.mkdirs();
        }
    }

    public static android.app.AlertDialog checkDialogFreeStorage(Context parent) {
        if (CommonUtils.getFreeStorageSdCard() < Global.MIN_FREE_STORAGE) {
            android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(parent);
            alertDialogBuilder.setTitle("Caution");
            alertDialogBuilder.setMessage("Free space of internal storage is not enough. Please ensure at least " + Global.MIN_FREE_STORAGE + " MB.")
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
            android.app.AlertDialog alert = alertDialogBuilder.create();
            alert.show();
            return alert;
        }
        return null;
    }

    private static String getVolumePath(final String volumeId, Context con) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }

        try {
            StorageManager mStorageManager =
                    (StorageManager) con.getSystemService(Context.STORAGE_SERVICE);

            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");

            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME.equals(volumeId)) {
                    return (String) getPath.invoke(storageVolumeElement);
                }

                // other volumes?
                if (uuid != null) {
                    if (uuid.equals(volumeId)) {
                        return (String) getPath.invoke(storageVolumeElement);
                    }
                }
            }

            // not found.
            return null;
        }
        catch (Exception ex) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");

        if (split.length > 0) {
            return split[0];
        }
        else {
            return null;
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) {
            return split[1];
        }
        else {
            return File.separator;
        }
    }

    // Create new csv file named as current timestamp
    public static File createUploadDirectory(Context mContext) {
        // root data folder
        File path = new File(mMioDirectory);
        if (!path.exists()) {
            path.mkdirs();
            MediaScannerConnection.scanFile(mContext, new String[] {path.toString()}, null, null);
        }
        path = new File(mMioUploadSaveDirectory);
        if (!path.exists()) {
            path.mkdirs();
            MediaScannerConnection.scanFile(mContext, new String[] {path.toString()}, null, null);
        }
        DateTime dateTime = new DateTime();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        // uploads folder. Not sent yet csv files stored here
        File uploadFolder = new File(mMioDirectory+"/"+dtf.print(dateTime)+"_"+new SharePreference(mContext).getId());
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
            MediaScannerConnection.scanFile(mContext, new String[] {uploadFolder.toString()}, null, null);
        }
        FileUtil.checkDialogFreeStorage(mContext);
        return  uploadFolder;
    }

    public static File createUploadDoneDirectory(Context mContext) {
        // root data folder
        File path = new File(mMioUploadSaveDirectory);
        if (!path.exists()) {
            path.mkdirs();
            MediaScannerConnection.scanFile(mContext, new String[] {path.toString()}, null, null);
        }
        DateTime dateTime = new DateTime();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyyMMdd_HHmmssSSS");
        File uploadDoneFolder = new File(mMioUploadSaveDirectory +"/"+dtf.print(dateTime));
        if (!uploadDoneFolder.exists()) {
            uploadDoneFolder.mkdirs();
        }
        FileUtil.checkDialogFreeStorage(mContext);
        return  uploadDoneFolder;
    }

}