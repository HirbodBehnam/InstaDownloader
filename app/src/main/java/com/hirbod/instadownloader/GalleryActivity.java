package com.hirbod.instadownloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class GalleryActivity extends Activity {
    File[] listOfFiles;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        setTitle("Downloaded Files");
        getActionBar().setDisplayHomeAsUpEnabled(true);
        //First time check
        {
            final SharedPreferences preferences = getSharedPreferences("MainSharedPreferences", 0);
            if (preferences.getBoolean("FirstGallery", true))
                new AlertDialog.Builder(this)
                        .setTitle("Notice")
                        .setMessage("I strongly recommend you to use your phone gallery instead of this built-in gallery. Just look for InstaDownloader album.\nAll downloaded files are at \"SDCard/InstaDownloader\"")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                preferences.edit().putBoolean("FirstGallery", false).apply();
                            }
                        }).show();
        }
        if(Build.VERSION.SDK_INT>=24){
            try{
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        //Read files
        File folder = new File(Environment.getExternalStorageDirectory() + "/InstaDownloader");
        listOfFiles = folder.listFiles();

        Arrays.sort(listOfFiles, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Long.valueOf(o2.lastModified()).compareTo(o1.lastModified());
            }
        });

        List<HashMap<String, String>> aList = new ArrayList<>();

        for (File listOfFile : listOfFiles) {
            HashMap<String, String> hm = new HashMap<>();
            hm.put("listview_title", listOfFile.getName());
            if (listOfFile.getAbsolutePath().endsWith(".jpg"))
                hm.put("listview_image", listOfFile.getAbsolutePath());
            else
                hm.put("listview_image", Integer.toString(R.drawable.video_camera));
            aList.add(hm);
        }

        String[] from = {"listview_image", "listview_title"};
        int[] to = {R.id.listview_image, R.id.listview_item_title};

        SimpleAdapter simpleAdapter = new SimpleAdapter(getBaseContext(), aList, R.layout.files_list, from, to);
        ListView androidListView = findViewById(R.id.MainListView);
        androidListView.setAdapter(simpleAdapter);

        androidListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String file = listOfFiles[position].getAbsolutePath();
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                if(file.endsWith(".jpg")){
                    intent.setDataAndType(Uri.fromFile(listOfFiles[position]), "image/*");
                    startActivity(intent);
                }else if(file.endsWith(".mp4")){
                    intent.setDataAndType(Uri.fromFile(listOfFiles[position]), "video/*");
                    startActivity(intent);
                }else
                    Toast.makeText(GalleryActivity.this,"Unsupported file!",Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        onBackPressed();
        return true;
    }
}
