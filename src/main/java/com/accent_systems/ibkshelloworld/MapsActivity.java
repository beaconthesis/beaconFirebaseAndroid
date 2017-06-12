package com.accent_systems.ibkshelloworld;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import android.os.Bundle;


/**
 * An activity that displays a map showing the place at the device's current location.
 */
public class MapsActivity<S, U> extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    private CameraPosition mCameraPosition;



    // The entry point to Google Play services, used by the Places API and Fused Location Provider.
    private GoogleApiClient mGoogleApiClient;

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(41.3851, 2.1734);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    // Used for selecting the current place.
    private final int mMaxEntries = 5;
    private String[] mLikelyPlaceNames = new String[mMaxEntries];
    private String[] mLikelyPlaceAddresses = new String[mMaxEntries];
    private String[] mLikelyPlaceAttributions = new String[mMaxEntries];
    private LatLng[] mLikelyPlaceLatLngs = new LatLng[mMaxEntries];

    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    //private ArrayList <String> beaconString;
    private ArrayList <String> scanString = new ArrayList <String>();
    private ArrayList <String> nameList = new ArrayList <String>();
    private ArrayList <Integer> RssiList = new ArrayList<>();
    private ArrayList <String> addressList = new ArrayList <String>();
    private ArrayList <beacon> beaconString;
    private beacon addBeacon = new beacon();
    private String address;


    public class beacon{
        public String name;
        public String address;
        public int Rssi;
        public double latitude;
        public double longitude;
    }
    private int callingActivity;

    private class coords{
        private double longitude;
        private double latitude;
        private String longitude1;
        private String latitude1;
    }
    private coords coordinates;
    private LatLng retrievedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps);

        // Build the Play services client for use by the Fused Location Provider and the Places API.
        // Use the addApi() method to request the Google Places API and the Fused Location Provider.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    /**
     * Builds the map when the Google Play services client is successfully connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Handles failure to connect to the Google Play services client.
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // Refer to the reference doc for ConnectionResult to see what error codes might
        // be returned in onConnectionFailed.
        Log.d(TAG, "Play services connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    /**
     * Handles suspension of the connection to the Google Play services client.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Play services connection suspended");
    }

    /**
     * Sets up the options menu.
     * @param menu The options menu.
     * @return Boolean.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.current_place_menu, menu);
        return true;
    }

    /**
     * Handles a click on the menu option to get a place.
     @param item The menu item to handle.
     @return Boolean.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place)
        {
            showCurrentPlace();
        }
        return true;
    }

    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        LatLng coord= new LatLng( addBeacon.latitude,addBeacon.longitude);
        map.addMarker( new MarkerOptions()
                       .position( new LatLng( 10,10 ) )
                        .title( "Hello World" ));
        // Use a custom info window adapter to handle multiple lines of text in the
        // info window contents.
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            // Return null here, so that getInfoContents() is called next.
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate the layouts for the info window, title and snippet.
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_contents,
                        (FrameLayout)findViewById(R.id.map), false);

                TextView title = ((TextView) infoWindow.findViewById(R.id.title));
                title.setText(marker.getTitle());

                TextView snippet = ((TextView) infoWindow.findViewById(R.id.snippet));
                snippet.setText(marker.getSnippet());

                return infoWindow;
            }
        });

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

     // Gets the current location of the device, and positions the map's camera.

    private void getDeviceLocation() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = database.getReference();

        Intent intent = getIntent();
        callingActivity = intent.getIntExtra(  "callingActivity",0 );
        //If the map has been called from ScanActivity
        if (callingActivity == 1) {

            // Get the best and most recent location of the device
            if (mLocationPermissionGranted) {
                mLastKnownLocation = LocationServices.FusedLocationApi
                        .getLastLocation( mGoogleApiClient );

                scanString = intent.getStringArrayListExtra( "beaconList" );
                nameList = intent.getStringArrayListExtra( "nameList" );
                addressList = intent.getStringArrayListExtra( "addressList" );
                RssiList = intent.getIntegerArrayListExtra( "RssiList" );

                for ( int i=0; i < scanString.size(); i++) {
                    addBeacon.name = nameList.get( i );
                    addBeacon.address = addressList.get( i );
                    addBeacon.Rssi = RssiList.get( i );
                    addBeacon.latitude = mLastKnownLocation.getLatitude();
                    addBeacon.longitude = mLastKnownLocation.getLongitude();
                    databaseReference.child( "foo" ).child(addBeacon.address).setValue( addBeacon);
                }
            }
        }
        //If the maps has been called from MainActivity -> FindMybeacon button
        else {
           //databaseReference = FirebaseDatabase.getInstance().getReference().child( "foo" );
           databaseReference = FirebaseDatabase.getInstance().getReference().child( "foo" );
            databaseReference.keepSynced( true );
            scanString.add("11");

            //databaseReference.addValueEventListener( new ValueEventListener() {
            //databaseReference.addChildEventListener( new ChildEventListener() {


            ValueEventListener beaconListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                 scanString.add("add beacon retrieved");
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            databaseReference.child( "foo" ).push().setValue( scanString );
            databaseReference.addValueEventListener( beaconListener );

            /*ChildEventListener childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                    //Log.d("Child Event,longitude", dataSnapshot.getKey());
                    //address = dataSnapshot.child( addBeacon.address ).toString();

                    scanString.add("add beacon retrieved");
                }
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                    //Log.d("Child Event,longitude","onChildChanged:"+ dataSnapshot.getKey());
                    //addBeacon = dataSnapshot.child( addBeacon.address ).getValue( beacon.class );
                }
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {                }
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            };
            databaseReference.addChildEventListener(childEventListener);
            databaseReference.child( "foo" ).push().setValue( scanString );

                 }

            /*@Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s){
                        String child = (String) dataSnapshot.getValue();
                     scanString.add(  "2");
                    if (dataSnapshot.hasChild( "foo" )) {
                        //coordinates.latitude1 = dataSnapshot.child( "latitude" ).getValue().toString();
                        //coordinates.longitude1 = dataSnapshot.child( "longitude" ).getValue().toString();
                        scanString.add(  "y");
                    } else {
                        scanString.add(  "n");
                    }
                    //mLastKnownLocation.setLatitude( coordinates.latitude );
                    //mLastKnownLocation.setLongitude( post.longitude );
                }
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            } );

            databaseReference.child( "foo" ).push().setValue( scanString );*/
        }

        // Set the map's camera position to the current location of the device.
        if (mCameraPosition != null) {
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCameraPosition));
        } else if (mLastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        } else {
            Log.d(TAG, "Current location is null. Using defaults.");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }


    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    /**
     * Prompts the user to select the current place from a list of likely places, and shows the
     * current place on the map - provided the user has granted location permission.
     */
    private void showCurrentPlace() {
        if (mMap == null) {
            return;
        }

        // Add a default marker, because the user hasn't selected a place.
        if (mLocationPermissionGranted) {
            // Get the likely places - that is, the businesses and other points of interest that
            // are the best match for the device's current location.
            @SuppressWarnings("MissingPermission")
            PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                    .getCurrentPlace(mGoogleApiClient, null);
            result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                @Override
                public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {
                    int i = 0;
                    mLikelyPlaceNames = new String[mMaxEntries];
                    mLikelyPlaceAddresses = new String[mMaxEntries];
                    mLikelyPlaceAttributions = new String[mMaxEntries];
                    mLikelyPlaceLatLngs = new LatLng[mMaxEntries];
                    for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                        // Build a list of likely places to show the user. Max 5.
                        mLikelyPlaceNames[i] = (String) placeLikelihood.getPlace().getName();
                        mLikelyPlaceAddresses[i] = (String) placeLikelihood.getPlace().getAddress();
                        mLikelyPlaceAttributions[i] = (String) placeLikelihood.getPlace()
                                .getAttributions();
                        mLikelyPlaceLatLngs[i] = placeLikelihood.getPlace().getLatLng();

                        i++;
                        if (i > (mMaxEntries - 1)) {
                            break;
                        }
                    }
                    // Release the place likelihood buffer, to avoid memory leaks.
                    likelyPlaces.release();

                    // Show a dialog offering the user the list of likely places, and add a
                    // marker at the selected place.
                    openPlacesDialog();
                }
            });
        } else mMap.addMarker( new MarkerOptions()
                .title( getString( R.string.default_info_title ) )
                .position( mDefaultLocation ) );
        // .snippet( getString( R.string.default_info_snippet ) );
    }

    /**
     * Displays a form allowing the user to select a place from a list of likely places.
     */
    private void openPlacesDialog() {
        // Ask the user to choose the place where they are now.
        DialogInterface.OnClickListener listener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The "which" argument contains the position of the selected item.
                        LatLng markerLatLng = mLikelyPlaceLatLngs[which];
                        String markerSnippet = mLikelyPlaceAddresses[which];
                        if (mLikelyPlaceAttributions[which] != null) {
                            markerSnippet = markerSnippet + "\n" + mLikelyPlaceAttributions[which];
                        }
                        // Add a marker for the selected place, with an info window
                        // showing information about that place.
                        mMap.addMarker(new MarkerOptions()
                                .title(mLikelyPlaceNames[which])
                                .position(markerLatLng)
                                .snippet(markerSnippet));

                        // Position the map's camera at the location of the marker.
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLatLng,
                                DEFAULT_ZOOM));
                    }
                };

        // Display the dialog.
        AlertDialog dialog = new AlertDialog.Builder(this)
                // .setTitle(R.string.pick_place)
                .setItems(mLikelyPlaceNames, listener)
                .show();
    }

    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }

        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mLocationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            mMap.setMyLocationEnabled(false);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }
    }
}



