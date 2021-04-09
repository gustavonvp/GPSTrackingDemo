package com.br.gpstrackingdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.Manifest;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int DEFAULT_UPDATE_INTERVAL = 30;
    public static final int FAST_UPDATE_INTERVAL = 5;
    public static final int PERMISSIONS_FINE_LOCATION = 99;

//    String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;

    // references to the UI elements

    private TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_wayPointCounts;

    Button btn_newWaipoint, btn_showWayPointList, btn_showMap;

    Switch sw_locationupdates, sw_gps;


    //variable to remember if we are tracking location or not.
    boolean updateOn = false;

    // current location
     Location currentLocation;

    //list of saved locations
     List<Location> savedLocations;

    // Google 's API for location service
    FusedLocationProviderClient fusedLocationProviderClient;

    // Location request is a config file for all settings related to FusedLocationProviderClient
    LocationRequest locationRequest;

    //  Location callback is a config data, and return the last location with data and hora last change.
    LocationCallback locationCallBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // give each UI variable a value

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
        btn_newWaipoint = findViewById(R.id.btn_newWaiPoint);
        btn_showWayPointList = findViewById(R.id.btn_showWayPointList);
        tv_wayPointCounts = findViewById(R.id.tv_countOfCrumbs);
        btn_showMap = findViewById(R.id.btn_showMap);


        /*//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);*/

        //set all properties of LocationRequest

        locationRequest = new LocationRequest();

        /*locationRequest.setInterval(30000);
        locationRequest.setFastestInterval(5000);*/

        //how often does the default location check occur?
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);

        //how often does the location check occur when set to the most frequent update?
        locationRequest.setFastestInterval(1000 * FAST_UPDATE_INTERVAL);

        // how often does the verification of energy are set  /default is 10 meters of range/
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // when the device location has changed or can no longer be determined.
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                updateUIValues(locationResult.getLastLocation());
            }
        };

        btn_newWaipoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the gps location


                // add the neew location to the global list
                MyApplication myApplication = (MyApplication)getApplicationContext();
                savedLocations = myApplication.getMyLocations();
                savedLocations.add(currentLocation);
            }
        });

        btn_showWayPointList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent i = new Intent(MainActivity.this, ShowSavedLocationsList.class);
                startActivity(i);

            }
        });

        btn_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(i);

            }
        });

        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationupdates.isChecked()) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });


        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    //most acurate - use GPS
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    String text = "Using GPS sensors";
                    tv_sensor.setText(text);

                } else {

                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    String text = "Using Towers + WIFi";
                    tv_sensor.setText(text);
                }
            }
        });


        updateGPS();

    }   // end onCreate method

    private void stopLocationUpdates() {

        tv_updates.setText("Location is NOT being tracked");
        tv_lat.setText("Not tracking location");
        tv_lon.setText("Not tracking location");
        tv_speed.setText("Not tracking location");
        tv_address.setText("Not tracking location");
        tv_accuracy.setText("Not tracking location");
        tv_altitude.setText("Not tracking location");
        tv_sensor.setText("Not tracking location");
        Toast.makeText(this, "done", Toast.LENGTH_SHORT).show();
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        String text = "Location is being tracked";
        tv_updates.setText(text);
        Toast.makeText(this, "tracking again", Toast.LENGTH_SHORT).show();
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//               != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//               != PackageManager.PERMISSION_GRANTED) {
//
//
////            // TODO: Consider calling
////            //    ActivityCompat#requestPermissions
////            // here to request the missing permissions, and then overriding
////            public void onRequestPermissionsResult(int requestCode, String[] permissions,
////                                         int[] grantResults)
//////            // to handle the case where the user grants the permission. See the documentation
//////            // for ActivityCompat#requestPermissions for more details.
////                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
////                   Manifest.permission.ACCESS_COARSE_LOCATION},
////                     PERMISSIONS_FINE_LOCATION);
////                        return;
//        }

        updateGPS();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION } , PERMISSIONS_FINE_LOCATION);

        switch (requestCode) {
            case PERMISSIONS_FINE_LOCATION:

                   if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                       updateGPS();
                   } else {
                       Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
                       finish();
                   }
        }

    }

    private void updateGPS() {
        // get permissions from the user to track GPS
        // get the current location from the fused client
        // update the UI - i.e set all properties in their associated text view items.

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            // user provided the permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // we got permission. Put the values of location. XXX into the  UI components.
                    updateUIValues(location);
                    currentLocation = location;
                }
            });
        }else {
            //permissions not granted yet.

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_FINE_LOCATION);

            }
        }

    }

    private void updateUIValues(Location location) {
        if (location != null) {

            // update all of the text view objects with a new location.
            tv_lat.setText(String.valueOf(location.getLatitude()));
            tv_lon.setText(String.valueOf(location.getLongitude()));
            tv_accuracy.setText(String.valueOf(location.getAccuracy()));

            if (location.hasAltitude()) {
                tv_altitude.setText(String.valueOf(location.getAltitude()));
            } else {
                String text = "Not available- altitude";
                tv_altitude.setText(text);
            }

            if (location.hasSpeed()) {
                tv_speed.setText(String.valueOf(location.getSpeed()));
            } else {
                String text = "Not available- speed";
                tv_speed.setText(text);
            }

            Geocoder geocoder = new Geocoder(MainActivity.this);

            try {
                List<Address> addressList = geocoder.getFromLocation(location.getLatitude(),
                        location.getLongitude(), 1);
                tv_address.setText(addressList.get(0).getAddressLine(0));
            } catch (IOException e) {
                String text = "geocode didnt work";
                tv_address.setText(text);
                e.printStackTrace();
            }


        } else {
            Toast.makeText(this, "the gps doesnt work", Toast.LENGTH_SHORT).show();
        }

        MyApplication myApplication = (MyApplication)getApplicationContext();
        savedLocations = myApplication.getMyLocations();

        // show the number of waypoints saved.
        tv_wayPointCounts.setText(Integer.toString(savedLocations.size()));

    }



}


