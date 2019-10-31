package vmio.com.blemultipleconnect.Utilities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by DatNT on 10/18/2017.
 */

public class SdCardUtils {
    public static void saveToSdCard(Activity mActivity){
        final File[] appsDir = ContextCompat.getExternalFilesDirs(mActivity, null);
        if (CommonUtils.externalMemoryAvailable(mActivity)) {
            if (android.os.Build.VERSION.SDK_INT < 21) {
                Toast.makeText(mActivity, "Support android 5.0 and above", Toast.LENGTH_SHORT).show();
                return;
            }
            final ArrayList<File> extRootPaths = new ArrayList<>();
            for (final File f : appsDir)
                extRootPaths.add(f.getParentFile().getParentFile().getParentFile().getParentFile());
            if (CommonUtils.calculateSdcard(extRootPaths.get(1).getAbsolutePath()) < Global.MIN_FREE_STORAGE) {
                Toast.makeText(mActivity, "Can't save log data because external device is full", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                mActivity.startActivityForResult(intent, 42);
            }
        } else {
            Toast.makeText(mActivity, "Can't save log data because there is no external devices", Toast.LENGTH_SHORT).show();
        }
    }

    public static void getSdCardPermission(Intent data, Activity mActivity){
        Uri treeUri = data.getData();
        File desCopyFolder = new File(treeUri.getEncodedPath());
        int takeFlags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        mActivity.getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
    }


}
