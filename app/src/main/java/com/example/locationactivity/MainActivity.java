package com.example.locationactivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int DEFAULT_UPDATE_INTERVAL = 1;//30
    public static final int FAST_UPDATE_INTERVAL = 1;//5
    public static final int PERMSSIONS_FINE_LOCATION = 99;

//    private HomeViewModel homeViewModel;

    // references to the UI elements
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address;

    Switch sw_locationupdates, sw_gps;

    Button btn_deleteSQL;

    // variable to remember if we are tracking location or not
    boolean updateOn = false;

    //Location request is a config file for all settings related to FusedLocationProviderClient
    LocationRequest locationRequest;

    LocationCallback locationCallBack;

    // Google's API for location services. The majority of the app functions using the class.
    FusedLocationProviderClient fusedLocationProviderClient;

    //SQL
    public ArrayList<Datapoint> datapoints;
    DatabaseHelper myDB;
    int id_test = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        //give each UI variable a value
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);
        btn_deleteSQL = findViewById(R.id.btn_deleteSQL);

        // set all properties of LocationRequest

        locationRequest = new LocationRequest();

        // how often does the default location check occur?
        //setInterval(long) means - set the interval in which you want to get locations
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);
        // how often does the location check occur when set to the most frequent update?
        //setFastestInterval(long) means - if a location is available sooner you can get it (i.e. another app is using the location services).
        locationRequest.setFastestInterval(1000 * FAST_UPDATE_INTERVAL);

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // event that is triggered whenever the update interval is met.
        locationCallBack = new LocationCallback() {

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // save the location
                Location location = locationResult.getLastLocation();
                updateUIValues(location);
            }
        };

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    // most accurate -use GPS
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensors");
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WIFI");
                }
            }
        });

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationupdates.isChecked()) {
                    // turn on location tracking
                    startLocationUpdates();
                } else {
                    // turn off tracking
                    stopLocationUpdates();
                }
            }
        });

        btn_deleteSQL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteAll();
            }
        });

        updateGPS();

        datapoints = new ArrayList<Datapoint>();
        //SQL management
        myDB = new DatabaseHelper(this);
//        datapoints=getAll(datapoints);
        //^SQL management
    }

    private void startLocationUpdates() {
        tv_updates.setText("Location is being tracked");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }

    private void stopLocationUpdates() {
        tv_updates.setText("Location is NOT being tracked");
        tv_lat.setText("Not tracking location");
        tv_lon.setText("Not tracking location");
        tv_speed.setText("Not tracking location");
        tv_address.setText("Not tracking location");
        tv_accuracy.setText("Not tracking location");
        tv_altitude.setText("Not tracking location");
        tv_sensor.setText("Not tracking location");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMSSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }
                else {
                    Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void updateGPS() {
        // get permissions from the user to track GPS
        // get the current location from the fused client
        // update the UI - i.e. set all properties in their associated text view items

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            // user provided the permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // we got permission. Put the values of location. XXX into the UI components
//                    if (location !=null) {
                    updateUIValues(location);
//                    }
                }
            });
        }else{
            // permission not granted yet

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMSSIONS_FINE_LOCATION);
            }
        }

    }

    private void updateUIValues(Location location) {
        //Update SQL v///////////////////////////////////////////////
        id_test = id_test+1;
        String pothole_test = "NA";

        Date today = Calendar.getInstance().getTime();
        long currentTimeInMilli = today.getTime();

        AddData(id_test, currentTimeInMilli, location.getLongitude(),
                location.getLatitude(),pothole_test);
        //Update SQL ^///////////////////////////////////////////////

        // update all of the text view objects with a new location
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        if (location.hasAltitude()){
            tv_altitude.setText(String.valueOf(location.getAltitude()));

        }
        else {
            tv_altitude.setText("Not available");
        }

        if (location.hasSpeed()){
            tv_speed.setText(String.valueOf(location.getSpeed()));

        }
        else {
            tv_speed.setText("Not available");
        }

        Geocoder geocoder = new Geocoder(MainActivity.this);
        try {
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            tv_address.setText(addressList.get(0).getAddressLine(0));
        }catch (Exception e){
            tv_address.setText("Unable to get street address");
        }

    }



    //SQL METHODS
    public void DeleteData(int mId){
        Integer deletedRows = myDB.deleteData(Integer.toString(mId));
        if (deletedRows>0){
            Toast.makeText(this,"Data Deleted", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this, "Data not Deleted", Toast.LENGTH_SHORT).show();
        }
    }

    public void DeleteAll(){
        Integer deletedRows = myDB.deleteAll();
        if (deletedRows>0){
            Toast.makeText(this,"All Data Deleted: " + deletedRows + " rows", Toast.LENGTH_SHORT).show();
        }else {
            Toast.makeText(this, "All Data not Deleted", Toast.LENGTH_SHORT).show();
        }
    }




    //public boolean updateData(String id, String time, String longitude, String latitude, String pothole){
    public void UpdateData(int mId, long mTime, double mLongitude, double mLatitude, String mPothole){
        boolean isUpdate = myDB.updateData(Integer.toString(mId),
                Long.toString(mTime), Double.toString(mLongitude), Double.toString(mLatitude), mPothole);
        if (isUpdate==true){
            Toast.makeText(this,"Data Updated", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(this,"Data not Updated", Toast.LENGTH_SHORT).show();
        }
    }

    public void AddData(int mId, long mTime, Double mLongitude, Double mLatitude, String mPothole){

        boolean isInserted = myDB.insertData(Integer.toString(mId),
                Long.toString(mTime), Double.toString(mLongitude), Double.toString(mLatitude), mPothole);

        if (isInserted==true){
            Toast.makeText(this,"Data Inserted", Toast.LENGTH_SHORT/4).show();
        }else{
            Toast.makeText(this,"Data not Inserted", Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<Datapoint> getAll(ArrayList<Datapoint> patientList){
        Cursor res = myDB.getAllData();
        if (res.getCount()==0){
            //means no data is available
            //error
            Toast.makeText(this,"No Data", Toast.LENGTH_SHORT).show();
            return new ArrayList<Datapoint>();
        }else{

            StringBuffer buffer = new StringBuffer();
            //get all data one by one through res object
            while(res.moveToNext()){
                buffer.append("Id : "+res.getString(0)+"\n");
                buffer.append("Time : " +res.getString(1)+"\n");
                buffer.append("Longitude : "+res.getString(2)+"\n");
                buffer.append("Latitude : "+res.getString(3)+"\n");
                buffer.append("Pothole : "+res.getString(4)+"\n\n");

                int id=Integer.parseInt(res.getString(0));
                long time = Long.parseLong(res.getString(1));
                double longitude =Double.parseDouble(res.getString(2));
                double latitude =Double.parseDouble(res.getString(3));
                String pothole = res.getString(4);

                patientList.add(new Datapoint(id,time,longitude,latitude,pothole));
            }
        }
        return patientList;
    }
    //^SQL METHODS
}