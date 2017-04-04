package com.jmk.guiam;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {


    private GoogleMap mMap;
    private LocationManager locationManager;
    TimerTask timerTask;
    Timer timer;
    Toolbar toolbar;
    Menu menu;
    Circle circle,point;
    ImageButton imageButton;
    double latitud, longitude = 0;
    LinearLayout linearDB;
    ImageButton botonCancel;
    AutoCompleteTextView autoCompView;
    String lastProvider = "";
    float lastAccuracy = 100000;
    int contadorProvider = 0;
    ArrayAccuracy gps, network, passive;

    private static final String LOG_TAG = "Main2Activity";
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private AutoCompleteTextView mAutocompleteTextView;
    private GoogleApiClient mGoogleApiClient;
    private PlaceArrayAdapter mPlaceArrayAdapter;
    private static final LatLngBounds BOUNDS_MOUNTAIN_VIEW = new LatLngBounds(
            new LatLng(37.398160, -122.180831), new LatLng(37.430610, -121.972090));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkSharedPreferences();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        setOnThreadLocation();
        imageButton = (ImageButton) findViewById(R.id.miUbicacionButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng center = new LatLng(latitud, longitude);
                CameraUpdate location = CameraUpdateFactory.newLatLngZoom(center, 15);
                mMap.animateCamera(location, 1500, null);
            }
        });
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();
        mAutocompleteTextView = (AutoCompleteTextView) findViewById(R.id
                .autoCompleteTextView);
        mAutocompleteTextView.setOnItemClickListener(mAutocompleteClickListener);
        mPlaceArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1,
                BOUNDS_MOUNTAIN_VIEW, null);
        mAutocompleteTextView.setAdapter(mPlaceArrayAdapter);

        linearDB = (LinearLayout) findViewById(R.id.linearDB);
        botonCancel = (ImageButton) findViewById(R.id.botonCancel);
        botonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                linearDB.setVisibility(LinearLayout.INVISIBLE);
                menu.setGroupVisible(R.id.grupo1,true);
                mAutocompleteTextView.setText("");
            }
        });

        gps = new ArrayAccuracy("gps");
        network = new ArrayAccuracy("network");
        passive = new ArrayAccuracy("passive");


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){
            case R.id.buscarRuta:
                break;
            case R.id.buscarCalle:
                menu.setGroupVisible(R.id.grupo1,false);
                linearDB.setVisibility(LinearLayout.VISIBLE);

                /*Intent intent = new Intent(this, Main2Activity.class);
                startActivity(intent);*/
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    private void checkSharedPreferences(){

        SharedPreferences preferences = getSharedPreferences("lastLocation", Context.MODE_PRIVATE);

        double latitud = preferences.getFloat("latitud",0);
        double longitude = preferences.getFloat("longitude",0);

        if(latitud != 0 && longitude != 0){

            this.latitud = latitud;
            this.longitude = longitude;

        }
    }

    private void saveSharedPreferences(){

        SharedPreferences preferences = getSharedPreferences("lastLocation", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat("latitud", (float) latitud);
        editor.putFloat("longitude", (float) longitude);
        editor.commit();
    }

    public void mostrar(View view){

        Animation animation = AnimationUtils.loadAnimation(this,R.anim.aparecer);
        animation.setFillAfter(true);
        view.startAnimation(animation);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

            mMap = googleMap;
            googleMap.getUiSettings().setMapToolbarEnabled(false);
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            if(latitud != 0 && longitude != 0){

                LatLng center = new LatLng(latitud, longitude);
                CameraUpdate location = CameraUpdateFactory.newLatLngZoom(center, 15);
                mMap.animateCamera(location, 1500, null);
            }

            inicializarUbicacion();

        }

    private void inicializarUbicacion() {

        LatLng center = new LatLng(0, 0);
        int radius = 40;
        CircleOptions circleOptions = new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeColor(Color.argb(200, 33, 150, 243))
                .strokeWidth(1)
                .fillColor(Color.argb(32, 33, 150, 243));
        circle = mMap.addCircle(circleOptions);


        int radius2 = 3;
        CircleOptions pointOptions = new CircleOptions()
                .center(center)
                .radius(radius2)
                .strokeWidth(0)
                .fillColor(Color.argb(200, 33, 150, 243))
                .clickable(true);
        point = mMap.addCircle(pointOptions);
    }

    private void actualizarUbicacion(){
        LatLng center = new LatLng(latitud, longitude);
        int radius = (int) lastAccuracy;

        circle.setCenter(center);
        circle.setRadius(radius);
        point.setCenter(center);
    }
    
    private void setMark(String texto, double latitud, double longitud, boolean moverCamara) {
        // Add a marker in Sydney and move the camera
        LatLng posicion = new LatLng(latitud, longitud);
        mMap.addMarker(new MarkerOptions().position(posicion).title(texto));
        if (moverCamara) mMap.moveCamera(CameraUpdateFactory.newLatLng(posicion));
    }

    //Start - Implementacion metodos LocationListener
    @Override
    public void onLocationChanged(Location location) {

        Log.v("Aplicacion","Provider:" + location.getProvider()
                + ", Accuracy: " + location.getAccuracy()
                +", LatLong: " + location.getLatitude() + " - " + location.getLongitude());

        getBestProvider(location);
        actualizarUbicacion();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
    //End - Implementacion metodos LocationListener

    @Override
    protected void onDestroy() {

        timer.cancel();
        timer.purge();
        saveSharedPreferences();
        super.onDestroy();
    }

    private void setOnThreadLocation(){
        timer = new Timer();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, this);

        timerTask = new TimerTask() {
            @Override
            public void run() {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) MainActivity.this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, (LocationListener) MainActivity.this);
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, (LocationListener) MainActivity.this);
            }
        };

        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    private void getBestProvider(Location location){

        double latitud = location.getLatitude();
        double longitude = location.getLongitude();
        float accuracy = location.getAccuracy();
        String provider = location.getProvider();
        this.lastProvider = provider;

        switch (location.getProvider()){
            case "gps":
                gps.insertarAccuracy(accuracy);
                Log.v("Aplicacion","Gps: " + gps.getPromedio());
                break;
            case "network":
                network.insertarAccuracy(accuracy);
                Log.v("Aplicacion","Network: " + network.getPromedio());
                break;
            case "passive":
                passive.insertarAccuracy(accuracy);
                Log.v("Aplicacion","Passive: " + passive.getPromedio());
                break;
        }

        ArrayAccuracy mejor = ArrayAccuracy.getMejor(gps,network,passive);

        if(mejor.getName().equals((provider))){

            this.latitud = latitud;
            this.longitude = longitude;
            this.lastAccuracy = mejor.getPromedio();
            contadorProvider = 0;
        }else{
            contadorProvider++;
            if(contadorProvider>20){

                contadorProvider = 0;
                mejor.inicializarArray();
            }
        }
    }


    @Override
    public void onConnected(Bundle bundle) {
        mPlaceArrayAdapter.setGoogleApiClient(mGoogleApiClient);
        Log.i(LOG_TAG, "Google Places API connected.");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG_TAG, "Google Places API connection failed with error code: "
                + connectionResult.getErrorCode());

        Toast.makeText(this,
                "Google Places API connection failed with error code:" +
                        connectionResult.getErrorCode(),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mPlaceArrayAdapter.setGoogleApiClient(null);
        Log.e(LOG_TAG, "Google Places API connection suspended.");
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final PlaceArrayAdapter.PlaceAutocomplete item = mPlaceArrayAdapter.getItem(position);
            final String placeId = String.valueOf(item.placeId);
            Log.i(LOG_TAG, "Selected: " + item.description);
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            Log.i(LOG_TAG, "Fetching details for ID: " + item.placeId);


        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.e(LOG_TAG, "Place query did not complete. Error: " +
                        places.getStatus().toString());
                return;
            }
            // Selecting the first object buffer.
            final Place place = places.get(0);
            CharSequence attributions = places.getAttributions();

           /* mNameTextView.setText(Html.fromHtml(place.getName() + ""));
            mAddressTextView.setText(Html.fromHtml(place.getAddress() + ""));
            mIdTextView.setText(Html.fromHtml(place.getId() + ""));
            mPhoneTextView.setText(Html.fromHtml(place.getPhoneNumber() + ""));
            mWebTextView.setText(place.getWebsiteUri() + "");*/
            LatLng latLng = place.getLatLng();

            Log.v("Aplicacion",latLng.latitude + " - " + latLng.longitude);

            String nombre = String.valueOf(Html.fromHtml((String) place.getName()));

            setMark(nombre,latLng.latitude, latLng.longitude,true);


            if (attributions != null) {
                //mAttTextView.setText(Html.fromHtml(attributions.toString()));
            }
        }
    };
}
