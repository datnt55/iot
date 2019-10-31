package vmio.com.blemultipleconnect.Utilities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.UriPermission;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DatNT on 10/18/2017.
 */

public class CopyFileAsynTask extends AsyncTask<Void, Integer, Void> {
    private Activity mActivity;
    private ProgressDialog dialog;
    private File fileSource;
    public CopyFileAsynTask(Activity mActivity, File fileSource) {
        this.mActivity = mActivity;
        this.fileSource = fileSource;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(mActivity);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setIndeterminate(false);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMax(100);
        dialog.setMessage("Exporting...");
        dialog.setProgress(0);
        dialog.show();
    }

    @Override
    protected Void doInBackground(Void... params) {
       // fileSource = Environment.getExternalStoragePublicDirectory(Define.mMioUploadSaveDirectory);//"Mio/done");
        final File[] appsDir = ContextCompat.getExternalFilesDirs(mActivity, null);
        final ArrayList<File> extRootPaths = new ArrayList<>();
        for (final File f : appsDir)
            extRootPaths.add(f.getParentFile().getParentFile().getParentFile().getParentFile());

        // Create folder Mio-Ai-Logger
        DocumentFile dir = getDocumentFileIfAllowedToWrite(extRootPaths.get(1), mActivity);
        File folderUpload = new File(extRootPaths.get(1), Define.sdCardSaveDirectory);
        if (!folderUpload.exists()) {
            dir.createDirectory(Define.sdCardSaveDirectory);
        }

        // Create folder Saved
        DocumentFile dirSave = getDocumentFileIfAllowedToWrite(folderUpload, mActivity);
        File folderSave = new File(folderUpload, "Saved");
        if (!folderSave.exists()) {
            dirSave.createDirectory("Saved");
        }

        // Create folder session
        DocumentFile dirSession = getDocumentFileIfAllowedToWrite(folderSave, mActivity);
        File folderSession = new File(folderSave, fileSource.getName());
        if (!folderSession.exists()) {
            dirSession.createDirectory(fileSource.getName());
        }
        boolean canWrite = dirSession.canWrite();
        if (fileSource.exists()) {
            File[] files = fileSource.listFiles();

            int index = 0;
            for (File file : files) {
                publishProgress(index * 100 / files.length);
                copy(file, folderSession.getAbsolutePath(), mActivity);
                index++;
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Toast.makeText(mActivity, "copied successfully!", Toast.LENGTH_SHORT).show();
        dialog.dismiss();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        dialog.setProgress(values[0]);
    }
    public static DocumentFile getDocumentFileIfAllowedToWrite(File file, Context con) {

        List<UriPermission> permissionUris = con.getContentResolver().getPersistedUriPermissions();

        for (UriPermission permissionUri : permissionUris) {
            Uri treeUri = permissionUri.getUri();
            DocumentFile rootDocFile = DocumentFile.fromTreeUri(con, treeUri);
            String rootDocFilePath = FileUtil.getFullPathFromTreeUri(treeUri, con);

            if (file.getAbsolutePath().startsWith(rootDocFilePath)) {

                ArrayList<String> pathInRootDocParts = new ArrayList<String>();
                while (!rootDocFilePath.equals(file.getAbsolutePath())) {
                    pathInRootDocParts.add(file.getName());
                    file = file.getParentFile();
                }

                DocumentFile docFile = null;

                if (pathInRootDocParts.size() == 0) {
                    docFile = DocumentFile.fromTreeUri(con, rootDocFile.getUri());
                } else {
                    for (int i = pathInRootDocParts.size() - 1; i >= 0; i--) {
                        if (docFile == null) {
                            docFile = rootDocFile.findFile(pathInRootDocParts.get(i));
                        } else {
                            docFile = docFile.findFile(pathInRootDocParts.get(i));
                        }
                    }
                }
                if (docFile != null && docFile.canWrite()) {
                    return docFile;
                } else {
                    return null;
                }

            }
        }
        return null;
    }

    public boolean copy(File copy, String directory, Context con) {
        FileInputStream inStream = null;
        OutputStream outStream = null;
        DocumentFile dir = getDocumentFileIfAllowedToWrite(new File(directory), con);
        String mime = mime(copy.toURI().toString());
        DocumentFile copy1 = dir.createFile(mime, copy.getName());
        try {
            inStream = new FileInputStream(copy);
            outStream = con.getContentResolver().openOutputStream(copy1.getUri());
            byte[] buffer = new byte[16384];
            int bytesRead;
            while ((bytesRead = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                copy.delete();
                inStream.close();
                outStream.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public static String mime(String URI) {
        String type = "";
        String extention = MimeTypeMap.getFileExtensionFromUrl(URI);
        if (extention != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extention);
        }
        return type;
    }
}
