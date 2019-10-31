package vmio.com.blemultipleconnect.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vmio.com.blemultipleconnect.R;
import vmio.com.blemultipleconnect.Utilities.CommonUtils;
import vmio.com.blemultipleconnect.Utilities.CopyFileAsynTask;
import vmio.com.blemultipleconnect.Utilities.Define;
import vmio.com.blemultipleconnect.Utilities.Global;
import vmio.com.blemultipleconnect.Utilities.SdCardUtils;
import vmio.com.blemultipleconnect.adapter.FolderNotSyncAdapter;
import vmio.com.blemultipleconnect.model.FileDetail;
import vmio.com.blemultipleconnect.service.BaseService;
import vmio.com.blemultipleconnect.widget.ZipManager;
import vmio.com.mioblelib.model.SensorValue;

import static vmio.com.blemultipleconnect.Utilities.Define.mMioDirectory;
import static vmio.com.blemultipleconnect.Utilities.Define.mMioTempDirectory;
import static vmio.com.blemultipleconnect.Utilities.Define.mMioUploadDirectory;
import static vmio.com.blemultipleconnect.Utilities.Global.sensorValues;

public class FileNotSyncActivity extends AppCompatActivity implements FolderNotSyncAdapter.ItemClickListener {
    private RecyclerView listFolder;
    private FolderNotSyncAdapter adapter;
    private ArrayList<FileDetail> fileDetails = new ArrayList<>();
    private String fileExport;
    private TextView txtNoRecord;
    private Context mContext;
    private File zipFile;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_file_not_sync);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("ログ一覧");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        txtNoRecord = (TextView) findViewById(R.id.txt_empty_record);
        listFolder = (RecyclerView) findViewById(R.id.list_folder_not_sync);
        listFolder.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        listFolder.setLayoutManager(layoutManager);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(listFolder.getContext(), layoutManager.getOrientation());
        listFolder.addItemDecoration(dividerItemDecoration);

        File uploadFile = new File(Define.mMioDirectory);
        File[] subFolder = uploadFile.listFiles();
        if (subFolder == null){
            txtNoRecord.setVisibility(View.VISIBLE);
            listFolder.setVisibility(View.GONE);
        }else{
            txtNoRecord.setVisibility(View.GONE);
            listFolder.setVisibility(View.VISIBLE);
            if (subFolder != null && subFolder.length > 1) {
                Arrays.sort(subFolder, new Comparator<File>() {
                    @Override
                    public int compare(File object1, File object2) {

                        if (object1.lastModified() > object2.lastModified()) {
                            return -1;
                        } else if (object1.lastModified() < object2.lastModified()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
            }
            for(File file : subFolder){
                if (file.isDirectory() && !file.getName().toLowerCase().equals("log")) {
                    if (folderSize(file) > 0 && !file.getName().equals("Saved") && !file.getName().equals("temp"))
                        fileDetails.add(new FileDetail(file.getAbsolutePath(),file.getName(), folderSize(file)));
                }
            }
//            Collections.sort(fileDetails, Collections.<FileDetail>reverseOrder());
            adapter = new FolderNotSyncAdapter(this,fileDetails);
            listFolder.setAdapter(adapter);
            adapter.setOnItemClickListener(this);
        }
    }
    public static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemExportClick(int position) {
       // Toast.makeText(this,"clicked",Toast.LENGTH_SHORT).show();
        fileExport = fileDetails.get(position).getPath();
        SdCardUtils.saveToSdCard(this);
    }

    @Override
    public void onItemShareClick(int position) {
        ArrayList<Uri> files = new ArrayList<Uri>();
        File filesToSend = new File(fileDetails.get(position).getPath());
        for (File file : filesToSend.listFiles()){
            Uri uri = Uri.fromFile(file);
            files.add(uri);

        }
        String subject = "Backup";
        Intent email = new Intent(Intent.ACTION_SEND_MULTIPLE);
        email.putExtra(Intent.EXTRA_SUBJECT, subject);
        email.setType("*/*");
        email.putParcelableArrayListExtra(Intent.EXTRA_STREAM,files);
        startActivity(Intent.createChooser(email, "Send"));
    }

    @Override
    public void onUploadClick(int position) {
        FileDetail f = fileDetails.get(position);
        File file = new File(f.getPath());
        zipFile = new File(mMioTempDirectory,file.getName()+".zip");
        ZipManager zipManager = new ZipManager();
        zipManager.zip(getListFiles(file),zipFile.getAbsolutePath());
        new UploadFileAsynTask(mContext).execute(zipFile.getAbsolutePath());
    }

    private ArrayList<String> getListFiles(File parentDir) {
        ArrayList<String> inFiles = new ArrayList<String>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if(file.getName().endsWith(".csv")){
                    inFiles.add(file.getAbsolutePath());
                }
            }
        }
        return inFiles;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 42:
                if (data == null)
                    return;
                SdCardUtils.getSdCardPermission(data,this);
                new CopyFileAsynTask(this,new File(fileExport)).execute();
                break;
        }

    }

    private class UploadFileAsynTask extends AsyncTask<String, Integer, Integer>{
        private ProgressDialog dialog;
        private Context mThis;

        public UploadFileAsynTask(Context mThis) {
            this.mThis = mThis;
        }

        @Override
        protected Integer doInBackground(String... strings) {
            String selectedFilePath = strings[0];

            int serverResponseCode = 0;
            HttpURLConnection connection;
            DataOutputStream dataOutputStream;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";


            int bytesRead,bytesAvailable,bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File selectedFile = new File(selectedFilePath);
            int totalSize = (int)selectedFile.length();

            String[] parts = selectedFilePath.split("/");
            final String fileName = parts[parts.length-1];

            if (!selectedFile.isFile()){
                return 0;
            }else{
                try{
                    FileInputStream fileInputStream = new FileInputStream(selectedFile);
                    URL url = new URL(Define.URL_CSV_DATA);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);//Allow Inputs
                    connection.setDoOutput(true);//Allow Outputs
                    connection.setUseCaches(false);//Don't use a cached Copy
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    connection.setRequestProperty("uploaded_file",selectedFilePath);

                    //creating new dataoutputstream
                    dataOutputStream = new DataOutputStream(connection.getOutputStream());

                    //writing bytes to data outputstream
                    dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"data\";filename=\""
                            + selectedFilePath + "\"" + lineEnd);

                    dataOutputStream.writeBytes(lineEnd);

                    //returns no. of bytes present in fileInputStream
                    bytesAvailable = fileInputStream.available();
                    //selecting the buffer size as minimum of available bytes or 1 MB
                    bufferSize = Math.min(bytesAvailable,maxBufferSize);
                    //setting the buffer as byte array of size of bufferSize
                    buffer = new byte[bufferSize];

                    //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                    bytesRead = fileInputStream.read(buffer,0,bufferSize);

                    //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                    int progress = 0;

                    while (bytesRead > 0){
                        //write the bytes read from inputstream
                        dataOutputStream.write(buffer,0,bufferSize);
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable,maxBufferSize);
                        bytesRead = fileInputStream.read(buffer,0,bufferSize);
                        progress += bytesRead; // Here progress is total uploaded bytes

                        publishProgress((int)((progress*100)/totalSize)); // sending progress percent to publishProgress
                    }

                    dataOutputStream.writeBytes(lineEnd);
                    dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    serverResponseCode = connection.getResponseCode();
                    String serverResponseMessage = connection.getResponseMessage();

                    Log.e("TAG", "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);

                    //closing the input and output streams
                    fileInputStream.close();
                    dataOutputStream.flush();
                    dataOutputStream.close();



                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext,"File Not Found",Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "URL error!", Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "Cannot Read/Write File!", Toast.LENGTH_SHORT).show();
                }
                return serverResponseCode;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(mThis);
            dialog.setMessage("Uploading...");
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.setProgress(0);
            dialog.setIndeterminate(false);
            dialog.setMax(100);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.show();
        }

        @Override
        protected void onPostExecute(Integer serverResponseCode) {
            super.onPostExecute(serverResponseCode);
            dialog.dismiss();
            //response code of 200 indicates the server status OK
            if(serverResponseCode == 200){
                Toast.makeText(mThis, "サーバーへのアップロードに成功しました。 !",Toast.LENGTH_SHORT).show();
            }else
                Toast.makeText(mThis, "サーバーへのアップロードに失敗しました。 !",Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            dialog.setProgress(values[0]);

        }
    }

}
