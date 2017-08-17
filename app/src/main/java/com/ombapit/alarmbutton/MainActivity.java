package com.ombapit.alarmbutton;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.location.LocationRequest;
import com.ombapit.alarmbutton.utils.GlobalVar;
import com.ombapit.alarmbutton.utils.LocationManagerInterface;
import com.ombapit.alarmbutton.utils.RequestHandler;
import com.ombapit.alarmbutton.utils.SmartLocationManager;
import com.ombapit.alarmbutton.utils.Utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        LocationManagerInterface {

    private String[] arraySpinner;
    public static final String TAG = MainActivity.class.getSimpleName();
    private String latitude, longitude = "";

    SmartLocationManager mLocationManager;
    TextView mLocalTV, mLocationProviderTV, mlocationTimeTV;

    private SQLiteDatabase db;
    private Cursor c;

    String pesan,nama,ktpsim,alamat,hp,jenis_laporan;
    String state = "Off";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        createDatabase();

        mLocationManager = new SmartLocationManager(getApplicationContext(), this, this, SmartLocationManager.ALL_PROVIDERS, LocationRequest.PRIORITY_HIGH_ACCURACY, 10 * 1000, 1 * 1000, SmartLocationManager.LOCATION_PROVIDER_RESTRICTION_NONE); // init location manager
        /*mLocalTV = (TextView) findViewById(R.id.locationDisplayTV);
        mLocationProviderTV = (TextView) findViewById(R.id.locationProviderTV);
        mlocationTimeTV = (TextView) findViewById(R.id.locationTimeFetchedTV);*/

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        /*fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                //startActivity(intent);
            }
        });*/
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        final Spinner s = (Spinner) findViewById(R.id.cbType);

        final Button btalarm = (Button) findViewById(R.id.bt_alarm);
        btalarm.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //check jika data belum lengkap
                c = db.rawQuery("SELECT * FROM users",null);
                boolean exists = (c.getCount() > 0);
                if(exists){
                    c.moveToFirst();

                    jenis_laporan = s.getSelectedItem().toString();
                    nama = c.getString(1);
                    ktpsim = c.getString(2);
                    alamat = c.getString(3);
                    hp = c.getString(4);
                    if (!c.getString(0).equals("") || !c.getString(1).equals("") || c.getString(2).equals("")
                            || c.getString(3).equals("") || c.getString(4).equals("")) {
                        // Is the toggle on?
                        Log.d(TAG,"Jenis Laporan:" + jenis_laporan);

                        if (state == "On") {
                            Log.d("off", btalarm.getText().toString());
                            //matikan alarm
                            hitalarm("mati");
                        } else {
                            //getlatlon
                            if (latitude != null) {
                                //Toast.makeText(MainActivity.this, "Latlon" + latitude + "," + longitude, Toast.LENGTH_SHORT).show();
                                //hidupkan alarm
                                hitalarm("hidup");
                            } else {
                                Toast.makeText(MainActivity.this, "Aplikasi tidak mendapatkan posisi anda, mohon hidupkan terlebih dahulu GPS anda", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Log.d("on", btalarm.getText().toString());
                        }
                    } else {
                        Toast.makeText(MainActivity.this,"Silahkan Lengkapi Profil user anda terlebih dahulu",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, ProfileDataActivity.class);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(MainActivity.this,"Silahkan Lengkapi Profil user anda terlebih dahulu",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, ProfileDataActivity.class);
                    startActivity(intent);
                }
                c.close();
            }
        });

        arraySpinner = new String[] {
                "DARURAT", "KEBAKARAN", "RAMPOK", "RANMOR", "TERSESAT"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, arraySpinner) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                v.setMinimumHeight((int) (40*getContext().getResources().getDisplayMetrics().density));
                v.setBackgroundColor(Color.rgb(222, 222, 222));


                return v;
            }
        };
        s.setAdapter(adapter);

        //printHashKey();

        //ambil ke server jika kosong
        GlobalVar gv = GlobalVar.getInstance();
        final String smsc  = gv.getSMSCenter();
        if (smsc.equals("")) {
            getsmscenter();
            Log.d("get sms center",smsc);
        } else {
            Log.d("get sms center","smsc sudah dapat");
        }
    }

    private void getsmscenter(){
        class SmsCenter extends AsyncTask<String,Void,String> {

            ProgressDialog loading;
            RequestHandler rh = new RequestHandler();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this, "Set SMS Center", "Mohon tunggu...",true,true);
                loading.setCanceledOnTouchOutside(false);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();

            }

            @Override
            protected String doInBackground(String... params) {
                String uri = "http://"+ getString(R.string.api_url) + "/android_api/getsmscenter";
                String result = rh.sendGetRequest(uri);

                GlobalVar gv = GlobalVar.getInstance();
                //Set name and email in global/application context
                gv.setSMSCenter(result);

                return result;
            }
        }

        SmsCenter ui = new SmsCenter();
        ui.execute();
    }

    private void hitalarm(final String aksi){
        class HitAlarm extends AsyncTask<String,Void,String> {

            ProgressDialog loading;
            RequestHandler rh = new RequestHandler();
            Button btalarm = (Button) findViewById(R.id.bt_alarm);

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                String text_aksi = "";
                if (aksi.equals("hidup")){
                    text_aksi = "Hidupkan";
                } else {
                    text_aksi = "Matikan";
                }
                loading = ProgressDialog.show(MainActivity.this, text_aksi+ " Alarm", "Mohon tunggu...",true,true);
                loading.setCanceledOnTouchOutside(false);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();

                String kondisi = "";
                //jika aksi gagal,balikkan ke semula
                Log.d("url",s);
                if (s.equals("ok")){
                    if (aksi.equals("hidup")) {
                        btalarm.setText(R.string.on);
                        btalarm.setBackgroundResource(R.drawable.roundedbutton_click);
                        state = "On";
                        kondisi = "dinyalakan";
                    } else {
                        btalarm.setText(R.string.off);
                        btalarm.setBackgroundResource(R.drawable.roundedbutton);
                        state = "Off";
                        kondisi = "dimatikan";
                    }
                    Toast.makeText(getApplicationContext(),"Alarm telah "+ kondisi,Toast.LENGTH_LONG).show();
                } else {
                    if (aksi.equals("hidup")) {
                        btalarm.setText(R.string.off);
                        btalarm.setBackgroundResource(R.drawable.roundedbutton);
                        state = "Off";
                    } else {
                        btalarm.setText(R.string.on);
                        btalarm.setBackgroundResource(R.drawable.roundedbutton_click);
                        state = "On";
                    }
                    Toast.makeText(getApplicationContext(),"Server Error, silahkan coba kembali ...",Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected String doInBackground(String... params) {
                final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

                if (params[0].equals("hidup")) {
                    //kirim sms
                    String sms_pesan = "Ada "+ jenis_laporan + " yang dilaporkan oleh "+ nama +", silahkan periksa dashboard Panic SOS OKU";
                    SmsManager manager = SmsManager.getDefault();

                    GlobalVar gv = GlobalVar.getInstance();
                    final String smsc  = gv.getSMSCenter();
                    //matikansms manager.sendTextMessage(smsc,null,sms_pesan,null,null);
                    Log.d("kirim smsc",smsc);
                }

                HashMap<String,String> data = new HashMap<>();
                data.put("tombol_status", params[0]);
                data.put("jenis_laporan", jenis_laporan);
                data.put("device_id", Utility.uniqDevice(MainActivity.this));
                data.put("lat", latitude);
                data.put("lon", longitude);

                String uri = "http://"+ getString(R.string.api_url) + "/android_api/hit_alarm";
                String result = rh.sendPostRequest(uri,data);

                return result;
            }
        }

        HitAlarm ui = new HitAlarm();
        ui.execute(aksi);
    }

    public void printHashKey(){
        // Add code to print out the key hash
        Log.d("KeyHash:","Mulai key");
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.ombapit.alarmbutton",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_laporan) {
            Intent intent = new Intent(this, LaporanActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_manage) {
            Intent intent = new Intent(this, ProfileDataActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_website) {
            Intent intent = new Intent(this, WebsiteActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void onStart() {
        super.onStart();
        mLocationManager.startLocationFetching();
    }

    protected void onStop() {
        super.onStop();
        mLocationManager.abortLocationFetching();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.pauseLocationFetching();
    }

    @Override
    public void locationFetched(Location mLocal, Location oldLocation, String time, String locationProvider) {
        //Toast.makeText(getApplication(), "Lat : " + mLocal.getLatitude() + " Lng : " + mLocal.getLongitude(), Toast.LENGTH_LONG).show();
        /*mLocalTV.setText("Lat : " + mLocal.getLatitude() + " Lng : " + mLocal.getLongitude());
        mLocationProviderTV.setText(locationProvider);
        mlocationTimeTV.setText(time);*/
        latitude = String.valueOf(mLocal.getLatitude());
        longitude = String.valueOf(mLocal.getLongitude());
    }

    protected void createDatabase(){
        db=openOrCreateDatabase("User", Context.MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "nama VARCHAR,ktpsim VARCHAR, alamat VARCHAR, hp VARCHAR);");
    }
}
