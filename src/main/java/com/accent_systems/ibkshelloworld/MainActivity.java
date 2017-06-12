package com.accent_systems.ibkshelloworld;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.content.Context;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        {


    TextView prox;
    TextView notif;

    LinearLayout scanBtn;
    LinearLayout notifBtn;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_COARSE_BL = 2;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private static final String TAG = "MainActivity";
    private GoogleApiClient client;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prox = (TextView) findViewById(R.id.proxTV);
        prox.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Khand-Bold.ttf"));
        notif = (TextView) findViewById(R.id.notifTV);
        notif.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Khand-Bold.ttf"));

        SpannableString s = new SpannableString("iBKS Hello World");

        s.setSpan(new com.accent_systems.ibkshelloworld.TypefaceSpan(this, "Khand-Bold.ttf"), 0, s.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(Color.parseColor("#3a3c3e")), 0, s.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        setTitle(s);

        assert getSupportActionBar() != null;
        getSupportActionBar().setLogo( R.mipmap.ibks);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        scanBtn = (LinearLayout) findViewById(R.id.proxBtn);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startProxDemoActivity = new Intent(MainActivity.this, ScanActivity.class);
                startActivity(startProxDemoActivity);
            }
        });

        notifBtn = (LinearLayout) findViewById(R.id.notifBtn);
        notifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startNotifDemoActivity = new Intent(MainActivity.this, MapsActivity.class);
                startNotifDemoActivity.putExtra("callingActivity", 2);
                startActivity(startNotifDemoActivity);

            }
        });


        checkLocBT();
        inicializeBluetooth();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    private BluetoothAdapter mBTAdapter;

    private void inicializeBluetooth() {

        //Check if device does support BT by hardware
        if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            //Toast shows a message on the screen for a LENGTH_SHORT period
            Toast.makeText(this, "BLUETOOTH NOT SUPPORTED!", Toast.LENGTH_SHORT).show();
            finish();
        }

        //Check if device does support BT Low Energy by hardware. Else close the app(finish())!
        if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Toast shows a message on the screen for a LENGTH_SHORT period
            Toast.makeText(this, "BLE NOT SUPPORTED!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            //If BLE is supported, get the BT adapter. Preparing for use!
            mBTAdapter = BluetoothAdapter.getDefaultAdapter();
            //If getting the adapter returns error, close the app with error message!
            if (mBTAdapter == null) {
                Toast.makeText(this, "ERROR GETTING BLUETOOTH ADAPTER!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                //Check if BT is enabled! This method requires BT permissions in the manifest.
                if (!mBTAdapter.isEnabled()) {
                    //If it is not enabled, ask user to enable it with default BT enable dialog! BT enable response will be received in the onActivityResult method.
                    Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBTintent, PERMISSION_REQUEST_COARSE_BL);
                }
            }
        }
    }

    @TargetApi(23)
    private void checkLocBT() {
        //If Android version is M (6.0 API 23) or newer, check if it has Location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //If Location permissions are not granted for the app, ask user for it! Request response will be received in the onRequestPermissionsResult.
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }
    }



    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        //Check if permission request response is from Location
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //User granted permissions. Setup the scan settings
                    Log.d("TAG", "coarse location permission granted");
                } else {
                    //User denied Location permissions. Here you could warn the user that without
                    //Location permissions the app is not able to scan for BLE devices and eventually
                    //Close the app
                    finish();
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Check if the response is from BT
        if (requestCode == PERMISSION_REQUEST_COARSE_BL) {
            // User chose not to enable Bluetooth.
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onResume() {
        super.onResume();
                // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
   public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction0() {
        Thing object = new Thing.Builder()
                .setName("Main Page")
                // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
  public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction0());
    }
}
