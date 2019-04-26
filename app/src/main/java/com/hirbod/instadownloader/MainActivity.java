package com.hirbod.instadownloader;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.json.*;

import static android.util.Patterns.WEB_URL;

public class MainActivity extends Activity {
    static String WebSource = "";
    static String LinkToFile = "";
    String textBox = "";
    String FileNameSave = "";
    static ProgressDialog mProgressDialog;
    static AlertDialog.Builder mAlertDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //ETC
        findViewById(R.id.Help).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Help_Dialog();
            }
        });
        findViewById(R.id.about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String vn = "";
                String vc = "";
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    vn = pInfo.versionName;
                    vc = Integer.toString(pInfo.versionCode);
                }catch (Throwable e){
                    e.printStackTrace();
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("About")
                        .setMessage("Developed by Hirbod Behnam\nWith Android Studio & Java\n" + "Version " + vn + " Build Version: " + vc)
                        .setPositiveButton("OK",null)
                        .setNegativeButton("Source Code", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HirbodBehnam/InstaDownloader")));
                            }
                        })
                        .show();
            }
        });
        findViewById(R.id.DownloadBTN).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DownloadFile();
            }
        });

        findViewById(R.id.PasteBTN).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard.hasPrimaryClip()) {
                        ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                        ((EditText) findViewById(R.id.ShareURLEditText)).setText(item.getText());
                    }
            }
        });

        //Runtime permission
        if(Build.VERSION.SDK_INT >= 23){
            if (checkSelfPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    Toast.makeText(this, "The writing permission is needed to download and save photos.", Toast.LENGTH_LONG).show();
                else
                    requestPermissions(
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
            }
        }

        //Make Folder
        if(!(new File(Environment.getExternalStorageDirectory() + "/InstaDownloader").exists())) {
            boolean b = new File(Environment.getExternalStorageDirectory() + "/InstaDownloader").mkdir();
            if(!b){
                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Cannot create \"InstaDownloader\" directory in external memory.\nThis may cause problems later.")
                        .setPositiveButton("OK", null)
                        .setIcon(R.drawable.ic_warning_24dp)
                        .show();

            }
        }
        //Test permission
        boolean b = new File(Environment.getExternalStorageDirectory() + "/InstaDownloader1").mkdir();
        boolean b1 = new File(Environment.getExternalStorageDirectory() + "/InstaDownloader1").delete();
        if(!b || !b1) {
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("We have some difficulties writing in external memory.\nCheck your permissions. If storage permission is off you need to turn that on.")
                    .setPositiveButton("Close", null)
                    .setIcon(R.drawable.ic_warning_24dp)
                    .show();

        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Downloading, Please Wait...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setCancelable(false);
        mAlertDialog = new AlertDialog.Builder(this);
        //Check share
        {
            // Get intent, action and MIME type
            Intent intent = getIntent();
            String action = intent.getAction();
            String type = intent.getType();

            if (Intent.ACTION_SEND.equals(action) && type != null) {
                if ("text/plain".equals(type)) {
                    handleSendText(intent); // Handle text being sent
                }
            }
        }
    }
    void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            ((EditText) findViewById(R.id.ShareURLEditText)).setText(sharedText);
            findViewById(R.id.DownloadBTN).performClick();
        }
    }
    private void DownloadFile() {
        //Values
        if(!isOnline()){
            mAlertDialog
                .setMessage("Cannot connect to internet.")
                .setTitle("Error!")
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_warning_24dp)
                .show();
            return;
        }
        WebSource ="";
        mProgressDialog.show();
        textBox = ((EditText) findViewById(R.id.ShareURLEditText)).getText().toString().toLowerCase();

        //Give error if text box is empty or not valid
        if (textBox.matches("")) {
            Toast.makeText(this, "You did not enter a URL", Toast.LENGTH_SHORT).show();
            mProgressDialog.dismiss();
            return;
        }
        if (!WEB_URL.matcher(textBox).matches()) {
            Toast.makeText(this, "URL not valid", Toast.LENGTH_SHORT).show();
            mProgressDialog.dismiss();
            return;
        }
        //
        LinkToFile = ((EditText) findViewById(R.id.ShareURLEditText)).getText().toString();
        if(!(LinkToFile.startsWith("https://") || LinkToFile.startsWith("http://")))
            LinkToFile = "http://" + LinkToFile;
        DownloadPage task = new DownloadPage();
        task.execute();
        //See DownloadFile2
    }
    private void DownloadFile2(){
        if(WebSource.contains("<script type=\"text/javascript\">window._sharedData = ")){
            String WebSource1 = WebSource;
            WebSource1 = WebSource1.substring(WebSource1.indexOf("<script type=\"text/javascript\">window._sharedData = "));
            WebSource1 = WebSource1.substring(WebSource1.indexOf("=") + 1);
            WebSource1 = WebSource1.substring(WebSource1.indexOf("=") + 1);
            WebSource1 = WebSource1.substring(0,WebSource1.indexOf("</script>") - 1);
            if(WebSource1.contains("\"edge_sidecar_to_children\"")){//multi picture post
                try{
                    List<String> Download_List = new ArrayList<>();
                    JSONObject obj = new JSONObject(WebSource1);
                    JSONArray jsonArray;
                    String str = obj.getString("entry_data");
                    obj = new JSONObject(str);
                    jsonArray = obj.getJSONArray("PostPage");
                    str = jsonArray.toString();
                    str = str.substring(1);
                    str = str.substring(0,str.length()-1);
                    obj = new JSONObject(str);
                    obj = new JSONObject(obj.getString("graphql"));
                    obj = new JSONObject(obj.getString("shortcode_media"));
                    obj = new JSONObject(obj.getString("edge_sidecar_to_children"));
                    jsonArray = obj.getJSONArray("edges");
                    for(int i = 0;i<jsonArray.length();i++){
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        jsonObject = new JSONObject(jsonObject.getString("node"));
                        if(jsonObject.getBoolean("is_video")){
                            Download_List.add(jsonObject.getString("video_url"));
                        }else{
                            Download_List.add(jsonObject.getString("display_url"));
                        }
                    }
                    DownloadList dl = new DownloadList();
                    dl.download_list = Download_List;
                    dl.execute();
                }catch (Throwable e){
                    e.printStackTrace();
                    mProgressDialog.dismiss();
                    mAlertDialog
                            .setMessage("There was a problem downloading the multi post.")
                            .setTitle("Error")
                            .setPositiveButton("OK",null)
                            .setIcon(R.drawable.ic_warning_24dp)
                            .show();
                }
            }else {//single post
                if(WebSource.contains("<meta property=\"og:video\" content=")){//Video
                    Toast.makeText(this, "Downloading Video Started...", Toast.LENGTH_SHORT).show();
                    LinkToFile = WebSource.substring(WebSource.indexOf("<meta property=\"og:video\" content=\"")).split("\"")[3];
                    //File Name
                    String[] Title = textBox.split("/");
                    FileNameSave = Environment.getExternalStorageDirectory().toString() + "/InstaDownloader/" + GetNameFromURL(Title) + ".mp4";
                    DownloadFile task = new DownloadFile();
                    task.execute();
                }else if(WebSource.contains("<meta property=\"og:image\" content=")){//Picture
                    Toast.makeText(this, "Downloading Photo Started...", Toast.LENGTH_SHORT).show();
                    LinkToFile = WebSource.substring(WebSource.indexOf("<meta property=\"og:image\" content=\"")).split("\"")[3];
                    //File Name
                    if(!(textBox.endsWith("/"))){textBox += "/";}
                    String[] Title = textBox.split("/");
                    FileNameSave = Environment.getExternalStorageDirectory().toString() + "/InstaDownloader/" + GetNameFromURL(Title) + ".jpg";
                    DownloadFile task = new DownloadFile();
                    task.execute();
                }else{//Bullshit
                    mProgressDialog.dismiss();
                    mAlertDialog
                            .setMessage("Internet connection is OK, but this page doesn't look like valid Instagram page.")
                            .setTitle("Not Valid!")
                            .setPositiveButton("OK",null)
                            .setIcon(R.drawable.ic_warning_24dp)
                            .show();
                }
            }
        }else{
            mProgressDialog.dismiss();
            mAlertDialog
                    .setMessage("Internet connection is OK, but this page doesn't look like valid Instagram page.")
                    .setTitle("Not Valid!")
                    .setPositiveButton("OK",null)
                    .setIcon(R.drawable.ic_warning_24dp)
                    .show();
        }
    }

    private class DownloadList extends AsyncTask<Void,Void,Void>{
        List<String> download_list;
        List<String> fileNames;
        int i = 0;
        @Override
        protected Void doInBackground(Void... params) {
            fileNames = new ArrayList<>();
            try{
                String[] Title = textBox.split("/");
                for(i = 0;i<download_list.size();i++){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"Downloading post "+(i+1)+" from " + download_list.size() +" posts." ,Toast.LENGTH_SHORT).show();
                        }
                    });
                    //FileName save
                    fileNames.add(Environment.getExternalStorageDirectory().toString() + "/InstaDownloader/" + GetNameFromURL(Title) + i + "." + GetFormatFromURL(download_list.get(i)));
                    //Download
                    int count;
                    URL url = new URL(download_list.get(i));
                    URLConnection connection = url.openConnection();
                    connection.connect();
                    // download the file
                    InputStream input = new BufferedInputStream(url.openStream(), 8192);
                    // Output stream
                    OutputStream output = new FileOutputStream(fileNames.get(i));
                    byte[] data = new byte[1024];
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();
                }
            }catch (Throwable e){
                e.printStackTrace();
            }
            for(int i =0;i<fileNames.size();i++){
                if(FileNameSave.endsWith(".jpg")){
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeFile(FileNameSave, options);
                    OutputStream fOut = null;
                    File file = new File(FileNameSave);
                    try {
                        fOut = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                    try {
                        assert fOut != null;
                        fOut.flush();
                        fOut.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.TITLE, "A picture");
                    values.put(MediaStore.Images.Media.DESCRIPTION, "Downloaded with Instadownloader Hirbod Behnam");
                    values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis ());
                    values.put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode());
                    values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.getName().toLowerCase(Locale.US));
                    values.put("_data", file.getAbsolutePath());

                    ContentResolver cr = getContentResolver();
                    cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                }else{
                    File file = new File(FileNameSave);
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Every thing is done
            ((EditText) findViewById(R.id.ShareURLEditText)).setText("");
            Toast.makeText(MainActivity.this, "Download Done", Toast.LENGTH_SHORT).show();
            mProgressDialog.dismiss();
        }
    }
    private class DownloadPage extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... urls) {
            try {
                // Build and set timeout values for the request.
                URLConnection connection = (new URL(LinkToFile)).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                // Read and store the result line by line then return the entire string.
                InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder html = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; ) {
                    html.append(line);
                }
                in.close();
                WebSource = html.toString();
            }catch (Throwable e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            DownloadFile2();
        }
    }
    private class DownloadFile extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... urls) {
            int count;
            try {
                URL url = new URL(LinkToFile);
                URLConnection connection = url.openConnection();
                connection.connect();


                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                OutputStream output = new FileOutputStream(FileNameSave);

                byte[] data = new byte[1024];

                while ((count = input.read(data)) != -1) {

                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            DONE();
        }
    }
    private void DONE(){
        if(FileNameSave.endsWith(".jpg")){
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(FileNameSave, options);
            OutputStream fOut = null;
            File file = new File(FileNameSave);
            try {
                fOut = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            try {
                assert fOut != null;
                fOut.flush();
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "A picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "Test");
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis ());
            values.put(MediaStore.Images.ImageColumns.BUCKET_ID, file.toString().toLowerCase(Locale.US).hashCode());
            values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, file.getName().toLowerCase(Locale.US));
            values.put("_data", file.getAbsolutePath());

            ContentResolver cr = getContentResolver();
            cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }else{
            File file = new File(FileNameSave);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        }
        //Show result
        ((EditText) findViewById(R.id.ShareURLEditText)).setText("");
        Toast.makeText(this, "Download Done", Toast.LENGTH_SHORT).show();
        mProgressDialog.dismiss();
    }
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
    private void Help_Dialog() {
        new AlertDialog.Builder(this)
                .setTitle("Help")
                .setMessage("On Instagram go to a post and select three dots on top right of a post. Then click on \"Share Link...\" and choose InstaDownloader from list.\nOr\nOpen a post and tap on three dots and choose \"Copy Share URL\".Then, paste the URL here.")
                .setIcon(R.drawable.ic_help_24dp)
                .setPositiveButton("OK", null)
                .setNegativeButton("Learn More",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Uri uri = Uri.parse("http://ccm.net/faq/41542-how-to-copy-the-url-of-an-instagram-photo");
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                    }
                })
                .show();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == 1) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent i = getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getBaseContext().getPackageName());
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            } else {

                new AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Cannot create \"InstaDownloader\" directory in external memory.\nThis may cause problems later.")
                        .setPositiveButton("Close App", null)
                        .setIcon(R.drawable.ic_warning_24dp)
                        .show();
            }
        }
    }
    private static String GetNameFromURL(String[] split){
        for(int i = 0;i<split.length;i++)
            if(split[i].equals("p") && (++i) > split.length)
                return split[i+1];
        //If not found we are going to create a random name
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz12345678790";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for(int i = 0;i<10;i++)
            sb.append(upper.charAt(rnd.nextInt(upper.length())));
        return sb.toString();
    }
    private static String GetFormatFromURL(String url){
        return url.substring(url.lastIndexOf('/') + 1).split("\\?")[0].split("#")[0];
    }

}
