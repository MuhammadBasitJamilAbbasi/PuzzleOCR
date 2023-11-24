package com.upwork.ocr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button selectimage;
    TextView result,sorted;

    int REQUEST_READ_EXTERNAL_STORAGE=103;
    int REQUEST_WRITE_EXTERNAL_STORAGE=102;
   ArrayList<String> temp=new ArrayList<>();
FloatingActionButton floatingActionButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        selectimage=findViewById(R.id.selectimage);
        sorted=findViewById(R.id.sortedresult);
        result=findViewById(R.id.result);
       floatingActionButton=findViewById(R.id.export);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportToCSV(temp);

            }
        });

        selectimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                floatingActionButton.setVisibility(View.GONE);
                if (Build.VERSION.SDK_INT > 32) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 121);
                    } else {

                        selectImageFromGallery();
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
                    } else if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
                    } else {

                        selectImageFromGallery();
                    }


                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 12 && resultCode == RESULT_OK && data != null) {
           Uri selectedImageUri = data.getData();
            Bitmap selectedImageBitmap = null;

            try {

                selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);

                extractText(selectedImageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }



        }
    }


    public void extractText(Bitmap selectedbitmap) {
        temp.clear();
        ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.setTitle("Extracting....");
        progressDialog.show();
        TextRecognizer textRecognizer = new TextRecognizer.Builder(MainActivity.this).build();
        Bitmap bitmap = selectedbitmap;
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<TextBlock> sparseArray = textRecognizer.detect(frame);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < sparseArray.size(); i++) {
            TextBlock textBlock = sparseArray.valueAt(i);
          //  Log.i("Result",textBlock.getValue().toString());
            String str = textBlock.getValue();
            stringBuilder.append(str);
        }





        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
                result.setText(""+stringBuilder);

        String[] words =result.getText().toString().split("\\s+");

        result.setText("");
        for (int i = 0; i < words.length; i++) {
            if(words[i].length()>1)
            {
                temp.add(words[i]);
            }

        }
        Collections.sort(temp);

        for (int i = 0; i <temp.size(); i++) {

              result.append(temp.get(i)+"\n");


        }

                floatingActionButton.setVisibility(View.VISIBLE);


            }
        }, 4000);


    }

    public void exportToCSV(ArrayList<String> exportedtext) {
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd_HH_mm_ss", Locale.ENGLISH); // Replace colons with underscores in the file name
        String formattedDate = dateFormat.format(currentDate);
        String fileName = formattedDate+"_pocr" + ".csv";
        File exportfile;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
           exportfile= new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) + "/" + getResources().getString(R.string.app_name));
        }
        else
        {
            exportfile = new File(Environment.getExternalStorageDirectory(), "DCIM/" + getResources().getString(R.string.app_name));

        }

        if (!exportfile.exists() && !exportfile.mkdirs()) {
            Log.e("export", "Failed to create directory");
            Toast.makeText(MainActivity.this, "Failed to create directory", Toast.LENGTH_LONG).show();
            return;
        }

        File randomCSVFile = new File(exportfile, fileName);

        try {
            FileWriter fw = new FileWriter(randomCSVFile);
            fw.append("Crossword Puzzle Sorted Results\n");
            // Write the header row
            for (int i = 0; i < exportedtext.size(); i++) {
                String rowData = exportedtext.get(i) + "\n";
                fw.append(rowData);
            }

            fw.flush();
            fw.close();
            Log.e("export", exportfile.getPath());
            shareCSVFile(randomCSVFile);

            Toast.makeText(MainActivity.this, "Exported", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("export", e.getMessage());
            Toast.makeText(MainActivity.this, "Error exporting: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareCSVFile(File csvFile) {
        Uri csvUri =FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".fileprovider", csvFile);


        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "CSV File");
        shareIntent.putExtra(Intent.EXTRA_STREAM, csvUri);

        // Grant read permissions to other apps
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share CSV File"));
    }


    public void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 12);
    }


}