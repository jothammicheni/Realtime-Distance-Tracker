package com.example.realtimedistancetracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    private boolean isTracking = false; // Track whether the app is currently tracking
    private double totalDistance = 0; // Variable to store the total distance traveled
    private Location lastLocation; // Variable to store the last location

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private Button btnStart, btnStop, btnReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize location request
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Define location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (lastLocation != null && isTracking) {
                        // Calculate distance from lastLocation to the current location
                        float[] results = new float[1];
                        Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(), location.getLatitude(), location.getLongitude(), results);
                        float distance = results[0];

                        // Apply a threshold to ignore very small movements
                        if (distance > 0.5) { // Ignore changes smaller than 0.5 meters
                            totalDistance += distance; // Update the total distance traveled
                            lastLocation = location; // Update the last location to the current location

                            // Update the TextView with the total distance
                            TextView distanceView = findViewById(R.id.TVdistanceResults);
                            distanceView.setText(String.format("%.2f meters", totalDistance));
                        }
                    } else {
                        // Initialize the lastLocation with the first location received
                        lastLocation = location;
                    }
                }
            }
        };

        // Initialize buttons
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop) ;
        btnReset = findViewById(R.id.btnReset);

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            setUpButtons();
        }
    }

    private void setUpButtons() {
        btnStart.setOnClickListener(v -> {
            if (!isTracking) {
                startLocationUpdates();
                isTracking = true;
                Toast.makeText(MainActivity.this, "Tracking Started", Toast.LENGTH_SHORT).show();
            }
        });

        btnStop.setOnClickListener(v -> {
            if (isTracking) {
                stopLocationUpdates();
                isTracking = false;
                Toast.makeText(MainActivity.this, "Tracking Stopped", Toast.LENGTH_SHORT).show();
            }
        });

        btnReset.setOnClickListener(v -> {
            stopLocationUpdates();
            totalDistance = 0; // Reset the total distance
            lastLocation = null; // Reset the last location
            isTracking = false;
            TextView distanceView = findViewById(R.id.TVdistanceResults);
            distanceView.setText("0 meters"); // Optionally update the TextView to reflect the reset state
            Toast.makeText(MainActivity.this, "Tracking Reset", Toast.LENGTH_SHORT).show();
        });
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    setUpButtons();
                } else {
                    Toast.makeText(MainActivity.this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
}
