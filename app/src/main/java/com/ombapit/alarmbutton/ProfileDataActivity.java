package com.ombapit.alarmbutton;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.ombapit.alarmbutton.utils.RequestHandler;
import com.ombapit.alarmbutton.utils.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;

public class ProfileDataActivity extends AppCompatActivity {

    private TextView info;
    private LoginButton loginButton;
    private CallbackManager callbackManager;

    EditText edNama,edKtpsim, edAlamat, edHp;

    Button btnSimpan;
    private SQLiteDatabase db;
    private Cursor c;

    String nama,ktpsim,alamat,hp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_profile_data);
        db=openOrCreateDatabase("User", Context.MODE_PRIVATE, null);

        //set form
        edNama = (EditText) findViewById(R.id.etNama);
        edKtpsim = (EditText) findViewById(R.id.etKtp);
        edAlamat = (EditText) findViewById(R.id.etAlamat);
        edHp = (EditText) findViewById(R.id.etHP);

        //set form jika sudah ada data
        c = db.rawQuery("SELECT * FROM users", null);
        boolean exists = (c.getCount() > 0);
        if(exists) {
            c.moveToFirst();
            edNama.setText(c.getString(1));
            edKtpsim.setText(c.getString(2));
            edAlamat.setText(c.getString(3));
            edHp.setText(c.getString(4));
        }
        c.close();

        info = (TextView)findViewById(R.id.fb_info);
        loginButton = (LoginButton)findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList("public_profile",
                "user_location"));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken accessToken = loginResult.getAccessToken();

                // Facebook Email address
                GraphRequest request = GraphRequest.newMeRequest(
                        accessToken,
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(
                                    JSONObject object,
                                    GraphResponse response) {
                                Log.v("LoginActivity Response ", response.toString());

                                try {
                                    String Name = object.getString("name");
                                    String Alamat = object.getJSONObject("location").getString("name");

                                    //set form
                                    edNama.setText(Name);
                                    edAlamat.setText(Alamat);

                                    //Toast.makeText(getApplicationContext(), "Alamat " + Alamat, Toast.LENGTH_LONG).show();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,location");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                info.setText("Login attempt canceled.");
            }

            @Override
            public void onError(FacebookException e) {
                info.setText("Login attempt failed.");
            }
        });

        //listener button simpan
        btnSimpan = (Button) findViewById(R.id.btSimpanProfile);
        btnSimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                insertIntoDB();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    protected void insertIntoDB(){
        nama = edNama.getText().toString().trim();
        ktpsim = edKtpsim.getText().toString().trim();
        alamat = edAlamat.getText().toString().trim();
        hp = edHp.getText().toString().trim();
        if(nama.equals("") || ktpsim.equals("") || alamat.equals("") || hp.equals("")){
            Toast.makeText(getApplicationContext(),"Mohon diisi semua field yang tersedia", Toast.LENGTH_LONG).show();
            return;
        }

        //check data sudah ada atau belum
        c = db.rawQuery("SELECT * FROM users", null);
        boolean exists = (c.getCount() > 0);
        //jika belum ada,insert
        if(!exists) {
            saveprofile("insert","");
        } else {
            c.moveToFirst();
            String f_id= c.getString(0);
            saveprofile("update",f_id);
        }
        c.close();
    }

    private void saveprofile(final String aksi, final String f_id){
        class SaveProfile extends AsyncTask<String,Void,String> {

            ProgressDialog loading;
            RequestHandler rh = new RequestHandler();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(ProfileDataActivity.this, "User Profil", "Data sedang disimpan, Mohon tunggu ...",true,true);
                loading.setCanceledOnTouchOutside(false);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();

                //jika aksi gagal,balikkan ke semula
                Log.d("result_url",s);
                if (s.equals("ok")){
                    //masukkan ke sqlite
                    if (aksi == "insert") {
                        String query = "INSERT INTO users (nama,ktpsim,alamat,hp) VALUES('" + nama + "', '" + ktpsim + "', '" + alamat + "', '" + hp + "');";
                        db.execSQL(query);
                    } else if (aksi == "update") {
                        String query = "UPDATE users set nama = '" + nama + "'," +
                        "       ktpsim = '" + ktpsim + "'," +
                        "       alamat = '" + alamat + "'," +
                        "       hp = '" + hp + "'" +
                        "       where id =" + f_id + ";";
                        db.execSQL(query);
                    }

                    Toast.makeText(getApplicationContext(),"Data telah tersimpan",Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(),getString(R.string.api_error),Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected String doInBackground(String... params) {
                final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);

                HashMap<String,String> data = new HashMap<>();
                data.put("f_id", params[1]);
                data.put("device_id", Utility.uniqDevice(ProfileDataActivity.this));
                data.put("nama", nama);
                data.put("ktpsim", ktpsim);
                data.put("alamat", alamat);
                data.put("hp", hp);

                String uri = "http://"+ getString(R.string.api_url) + "/android_api/save_profile";
                String result = rh.sendPostRequest(uri,data);

                return result;
            }
        }

        SaveProfile ui = new SaveProfile();
        ui.execute(aksi,f_id);
    }
}
