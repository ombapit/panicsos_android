package com.ombapit.alarmbutton;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import com.ombapit.alarmbutton.utils.RequestHandler;
import com.ombapit.alarmbutton.utils.Utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class LaporanActivity extends AppCompatActivity {

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private Button btnSelect;
    private EditText edPesanKirim;
    private Button btnUpload;
    private ImageView ivImage;
    private String userChoosenTask;
    String mCurrentPhotoPath;
    Uri photoURI;

    public static final String UPLOAD_KEY = "image";
    public static final String TAG = "Laporan";

    private Bitmap bitmap;

    private SQLiteDatabase db;
    private Cursor c;

    String pesan,nama,ktpsim,alamat,hp;
    Boolean sudahUpload = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_laporan);
        db=openOrCreateDatabase("User", Context.MODE_PRIVATE, null);

        ivImage = (ImageView) findViewById(R.id.ivImage);
        btnSelect = (Button) findViewById(R.id.btnSelectPhoto);
        btnSelect.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        edPesanKirim = (EditText) findViewById(R.id.edPesanKirim);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //check jika data belum lengkap
                c = db.rawQuery("SELECT * FROM users",null);
                boolean exists = (c.getCount() > 0);
                if(exists) {
                    c.moveToFirst();
                    pesan = edPesanKirim.getText().toString();
                    nama = c.getString(1);
                    ktpsim = c.getString(2);
                    alamat = c.getString(3);
                    hp = c.getString(4);

                    if (!c.getString(0).equals("") || !c.getString(1).equals("") || c.getString(2).equals("")
                            || c.getString(3).equals("") || c.getString(4).equals("")) {
                        if (!pesan.equals("")) {
                            if (sudahUpload.equals(true)) {
                                uploadImage("gambar");
                            } else {
                                uploadImage("pesan");
                            }
                        } else {
                            Toast.makeText(LaporanActivity.this,"Silahkan isi gambar dan pesan terlebih dahulu",Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LaporanActivity.this,"Silahkan Lengkapi Profil user anda terlebih dahulu",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LaporanActivity.this, ProfileDataActivity.class);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(LaporanActivity.this,"Silahkan Lengkapi Profil user anda terlebih dahulu",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LaporanActivity.this, ProfileDataActivity.class);
                    startActivity(intent);
                }
                c.close();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals("Ambil Photo"))
                        cameraIntent();
                    else if(userChoosenTask.equals("Pilih dari Library"))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = { "Ambil Photo", "Pilih dari Library",
                "Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(LaporanActivity.this);
        builder.setTitle("Tambah Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result=Utility.checkPermission(LaporanActivity.this);

                if (items[item].equals("Ambil Photo")) {
                    userChoosenTask ="Ambil Photo";
                    if(result)
                        cameraIntent();

                } else if (items[item].equals("Pilih dari Library")) {
                    userChoosenTask ="Pilih dari Library";
                    if(result)
                        galleryIntent();

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Pilih File"),SELECT_FILE);
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                photoFile.delete();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d("Photo",ex.toString());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                /*Uri photoURI = FileProvider.getUriForFile(this,
                        "com.ombapit.alarmbutton.fileprovider",
                        photoFile);*/
                photoURI = Uri.fromFile(photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                Log.d("Photo","Simpan Photo ke extra"+photoURI);
            }
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA) {
                onCaptureImageResult();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "AB_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        //mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        mCurrentPhotoPath = image.getAbsolutePath();
        //mCurrentPhotoPath = image;
        return image;
    }
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = photoURI;
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
    private void setPic() {
        // Get the dimensions of the View
        int targetW = ivImage.getWidth();
        int targetH = ivImage.getHeight();
        //Log.d("Photo","Set Pic "+mCurrentPhotoPath);

        Bitmap real_bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);
        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap_view = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        ivImage.setImageBitmap(bitmap_view);
        Log.d("Photo","Tampilkan Foto "+ mCurrentPhotoPath);

        //resize
        Bitmap resized;
        resized = Bitmap.createScaledBitmap(bitmap_view,(int)(real_bitmap.getWidth()*0.4), (int)(real_bitmap.getHeight()*0.4), true);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        bitmap = resized;
    }

    private void onCaptureImageResult() {
        galleryAddPic();
        setPic();
        sudahUpload = true;
    }

    @SuppressWarnings("deprecation")
    private void onSelectFromGalleryResult(Intent data) {

        Bitmap bm=null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ivImage.setImageBitmap(bm);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        bitmap = bm;
        sudahUpload = true;
    }

    public String getStringImage(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }

    private void uploadImage(String mode){
        class UploadImage extends AsyncTask<Bitmap,Void,String> {

            ProgressDialog loading;
            RequestHandler rh = new RequestHandler();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(LaporanActivity.this, "Kirim Pesan", "Mohon tunggu...",true,true);
                loading.setCanceledOnTouchOutside(false);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();

                String msg = "";
                if (s.equals("ok")) {
                    msg = "Pesan sukses terkirim";
                    clearForm();
                } else {
                    msg = getString(R.string.api_error);
                }
                Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
            }

            @Override
            protected String doInBackground(Bitmap... params) {
                HashMap<String,String> data = new HashMap<>();
                if (params.length > 0) {
                    Bitmap bitmap = params[0];
                    String uploadImage = getStringImage(bitmap);
                    data.put(UPLOAD_KEY, uploadImage);
                }
                data.put("pesan", pesan);
                data.put("device_id", Utility.uniqDevice(LaporanActivity.this));

                String uri = "http://"+ getString(R.string.api_url) + "/android_api/kirim_pesan";
                String result = rh.sendPostRequest(uri,data);
                return result;
            }
        }

        UploadImage ui = new UploadImage();
        if (mode.equals("gambar"))
            ui.execute(bitmap);
        else
            ui.execute();
    }

    private void clearForm() {
        ivImage.setImageResource(R.drawable.ic_menu_gallery);
        edPesanKirim.setText("");
        btnSelect.setFocusable(true);
    }
}
