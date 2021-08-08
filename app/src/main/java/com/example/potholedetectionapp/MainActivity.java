package com.example.potholedetectionapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import com.example.potholedetectionapp.libsvm.svm_model;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Handler;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.example.potholedetectionapp.SVM.normalize_features;
import static com.example.potholedetectionapp.SVM.svmPredict;
import static com.example.potholedetectionapp.SVM.svm_load_model;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {

    //LOCATION VARIABLES v//////////////////////////////////////////////////////////////////////////
    public static final int DEFAULT_UPDATE_INTERVAL = 1;//30
    public static final int FAST_UPDATE_INTERVAL = 1;//5
    public static final int PERMSSIONS_FINE_LOCATION = 99;

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
    //LOCATION VARIABLES ^//////////////////////////////////////////////////////////////////////////



    //SQL VARIABLES v///////////////////////////////////////////////////////////////////////////////
    public ArrayList<Datapoint> datapoints;
    DatabaseHelper myDB;
    int id_test = 0;
    //SQL VARIABLES ^///////////////////////////////////////////////////////////////////////////////



    //BLUETOOTH VARIABLES v/////////////////////////////////////////////////////////////////////////
    private final String DEVICE_ADDRESS="00:14:03:06:92:C3";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    Button startButton,clearButton,stopButton;//sendButton
    TextView textView;
    EditText editText;
    boolean deviceConnected=false;
    boolean seeData=false;
    Thread thread;
    byte buffer[];
    int bufferPosition;
    boolean stopThread;
    //BLUETOOTH VARIABLES ^/////////////////////////////////////////////////////////////////////////

    //SVM VARIABLES v///////////////////////////////////////////////////////////////////////////////
    AssetManager am;
    svm_model test_load_model;
    //SVM VARIABLES ^///////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //AssetManager am = getAssets();
        try {
            test_load_model = svm_load_model("svm_model", this);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //LOCATION UI v/////////////////////////////////////////////////////////////////////////////
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

        updateGPS();
        //LOCATION UI ^/////////////////////////////////////////////////////////////////////////////



        //SQL UI v//////////////////////////////////////////////////////////////////////////////////
        btn_deleteSQL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeleteAll();
            }
        });

        datapoints = new ArrayList<Datapoint>();
        myDB = new DatabaseHelper(this);
        //datapoints=getAll(datapoints);
        //SQL UI^///////////////////////////////////////////////////////////////////////////////////



        //BLUETOOTH UI v////////////////////////////////////////////////////////////////////////////
        startButton = (Button) findViewById(R.id.connect);
        //sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.clear);
        stopButton = (Button) findViewById(R.id.stop);
        //editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.bluetooth_text);
        setUiEnabled(false);
        //BLUETOOTH UI ^////////////////////////////////////////////////////////////////////////////
    }

//    public BufferedReader svm_load_model() throws IOException
//    {
//        AssetManager am = this.getAssets();
//        InputStream is = am.open("testing.txt");
//        //return svm_load_model(new BufferedReader(new FileReader(model_file_name)));
////        InputStream inputStream = getAssets().open("testing.txt");
//        return new svm_load_model(BufferedReader(new InputStreamReader(is)));//contextApp.getAssets()
//    }



    //LOCATION METHODS v////////////////////////////////////////////////////////////////////////////
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
                    if (location !=null) {
                        updateUIValues(location);
                    }
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
    //LOCATION METHODS ^////////////////////////////////////////////////////////////////////////////



    //SQL METHODS///////////////////////////////////////////////////////////////////////////////////
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
    //^SQL METHODS//////////////////////////////////////////////////////////////////////////////////



    //BLUETOOTH METHODSv////////////////////////////////////////////////////////////////////////////
    public void setUiEnabled(boolean bool)
    {
        this.startButton.setEnabled(!bool);
//        sendButton.setEnabled(bool);
        this.stopButton.setEnabled(bool);
        this.textView.setEnabled(bool);
    }

    public boolean BTinit()
    {
        boolean found=false;
        BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth",Toast.LENGTH_SHORT).show();
        }
        if(!bluetoothAdapter.isEnabled())
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices.isEmpty())
        {
            Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();
        }
        else
        {
            for (BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(DEVICE_ADDRESS))
                {
                    device=iterator;
                    found=true;
                    break;
                }
            }
        }
        return found;
    }

    public boolean BTconnect()
    {
        boolean connected=true;
        try {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            connected=false;
        }
        if(connected)
        {
            try {
                outputStream=socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream=socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return connected;
    }

    public void onClickStart(View view) {
        if(BTinit())
        {
            if(BTconnect())
            {
                setUiEnabled(true);
                deviceConnected=true;
                beginListenForData();

                //String string2 = "r";
                //    string2.concat("\n");
                //try {
                // outputStream.write(string2.getBytes());
                //} catch (IOException e) {
                //  e.printStackTrace();
                //}

                textView.append("\nConnection Opened!\n");

            }

        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopThread)
                {
                    try
                    {
                        final int byteCount = inputStream.available();
                        if(byteCount > 9)
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string=new String(rawBytes,"UTF-8");

                            final int dataLength = string.length();
                            if (dataLength == 10)
                            {
                                seeData = true;
                            }
                            final String temp =new String(string.substring(0,5));
                            final String humid =new String(string.substring(5,10));
                            final Float temp3 = Float.parseFloat(temp);
                            final Float humid3 = Float.parseFloat(humid);

                            //byte[] tempNum = Arrays.copyOfRange(rawBytes,0,5 );
                            //byte[] humidNum = Arrays.copyOfRange(rawBytes,5,10 );
                            //final String temp2 =new String(tempNum,"UTF-8");
                            //final String humid2 =new String(humidNum,"UTF-8");

                           //final String temp4 =new String(string.substring(0,1));

                            handler.post(new Runnable() {
                                @RequiresApi(api = Build.VERSION_CODES.N)
                                public void run()
                                {
                                    //textView.append(String.valueOf(dataLength));
                                    //textView.setText("");

                                    //if (seeData) {
                                    //    textView.append(temp2);
                                    //    seeData = false;
                                    //}


                                    //SVM v//////////////////////////////////////////////////////////
                                    //38	445	2733	203	389	3031 Label:1
                                    //2	6	2174	-7	6	2183 Label: 0
                                    //313	-59	2207	302	-198	2276 Label:1
                                    //178	-98	2250	325	16	2126 Label:1
                                    //-78	26	2194	-54	53	2142 Label:0
                                    // Feature[feature_index][feature_value]
                                    double[][] features = new double[1][6];
                                    features[0][0] = 38;
                                    features[0][1] = 445;
                                    features[0][2] = 2733;
                                    features[0][3] = 203;
                                    features[0][4] = 389;
                                    features[0][5] = 3031;

                                    normalize_features(features);
                                    double[] ypred = svmPredict(features, test_load_model);
                                    System.out.println("(Actual:" + 1 + " Prediction:" + ypred[0] + ")");
                                    //SVM ^//////////////////////////////////////////////////////////

                                    textView.setText("");//ADDED TO CLEAR TEXT
                                    textView.append ("Temp = ");
                                    textView.append(String.valueOf(temp3));
                                    textView.append (" Humidity = ");
                                    textView.append(String.valueOf(humid3));
                                    if (humid3>50){
                                        textView.append (" Too High!");
                                    }
                                    textView.append ("\n");
                                    textView.append ("(Actual:" + 1 + " Prediction:" + ypred[0] + ")");
                                }
                            });


                        }
                    }
                    catch (IOException ex)
                    {
                        stopThread = true;
                    }
                }
            }
        });

        thread.start();
    }

    //public void onClickSend(View view) {
    //    String string = editText.getText().toString();
    //    string.concat("\n");
    //      String string2 = ("r");
    //        string.concat("\n");
    //    try {
    //        outputStream.write(string.getBytes());
    //    } catch (IOException e) {
    //        e.printStackTrace();
    //    }
    //    textView.append("\nMonitor Start:"+string+"\n");

    //}

    public void onClickStop(View view) throws IOException {
        stopThread = true;
        outputStream.close();
        inputStream.close();
        socket.close();
        setUiEnabled(false);
        deviceConnected=false;
        textView.append("\nConnection Closed!\n");
    }

    public void onClickClear(View view) {
        textView.setText("");
    }

    //BLUETOOTH METHODS^////////////////////////////////////////////////////////////////////////////
}