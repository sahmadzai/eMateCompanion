package com.emate.ematecompanion;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.LinkedList;

public class DashboardActivity extends FragmentActivity implements OnMapReadyCallback {
    private String LOG_TAG = "DASHBOARDACTIVITY";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private static final int PERMISSIONS_REQUEST_LOCATION = 123;
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    private CompassListener compassListener;
    private BluetoothService bluetoothService;
    private boolean isBound = false;
    private SpeedometerGauge speedometer;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setUpMap();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        TextView textViewGpsSpeed = findViewById(R.id.textViewGpsSpeed);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // Update interval in milliseconds

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button debug_btn = findViewById(R.id.debug_mode);
        debug_btn.setOnClickListener(view -> {
            Intent intent = new Intent(this, DebugActivity.class);
            startActivity(intent);
        });

        // Bind to BluetoothService
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Speedometer
        speedometer = findViewById(R.id.speedometer);
        speedometer.setLabelConverter((progress, maxProgress) -> String.valueOf((int) Math.round(progress)));
        speedometer.setMaxSpeed(30);
        speedometer.setMajorTickStep(5);
        speedometer.setMinorTicks(4);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Implement other methods of LocationListener if needed
        locationListener = location -> {
            double gpsSpeedMetersPerSecond = location.getSpeed(); // Speed in meters per second
//                double gpsSpeedMPH = gpsSpeedMetersPerSecond * 2.237;
            double gpsSpeedKmph = gpsSpeedMetersPerSecond * 3.6; // Convert to km/h
            Log.v(LOG_TAG, String.valueOf(gpsSpeedKmph));
            speedometer.setSpeed(gpsSpeedKmph, 1000, 0);
            textViewGpsSpeed.setText(getString(R.string.gps_value, gpsSpeedKmph));
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter("BluetoothData");
        registerReceiver(bluetoothDataReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the BroadcastReceiver
        unregisterReceiver(bluetoothDataReceiver);
    }

    // Service connection to bind with BluetoothService
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    // BroadcastReceiver to receive data from BluetoothService
    private final BroadcastReceiver bluetoothDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] data = intent.getByteArrayExtra("data");
            // TODO: Handle the received data (update UI elements, etc.)
            // For example, if you want to update speedometer, you can do it here.
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Check for location permissions and start location updates
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true); // Display the blue dot for the user's location
            setUpMap();
        }
    }

    private void setUpMap() {
        compassListener = new CompassListener();
        sensorManager.registerListener(compassListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    // Move the camera to the user's location with bearing (angle) and zoom
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(userLocation)
                            .zoom(17)  // Adjust the zoom level as needed
                            .bearing(compassListener.getFusedOrientation())  // Set the angle of the camera using compass data
                            .tilt(30)  // Set the tilt angle for a navigation-like view
                            .build();

                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(compassListener);
        // Unbind from BluetoothService
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }

    private class CompassListener implements SensorEventListener {
        private static final float FILTER_ALPHA = 0.1f;

        private float[] rotationMatrix = new float[9];
        private float[] orientationValues = new float[3];
        private float currentDegree = 0;
        private float[] angularVelocity = new float[3];

        // Complementary filter parameters
        private static final float COMPLEMENTARY_COEFFICIENT = 0.98f;
        private float fusedOrientation = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientationValues);
                float degree = (float) Math.toDegrees(orientationValues[0]);
                currentDegree = currentDegree * (1 - FILTER_ALPHA) + degree * FILTER_ALPHA;
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                angularVelocity[0] = event.values[0];
                angularVelocity[1] = event.values[1];
                angularVelocity[2] = event.values[2];

                // Calculate the change in orientation using gyroscope data
                float deltaOrientation = angularVelocity[2] * (1.0f / 60.0f); // Integration over time step

                // Apply complementary filter
                fusedOrientation = COMPLEMENTARY_COEFFICIENT * (fusedOrientation + deltaOrientation) +
                        (1 - COMPLEMENTARY_COEFFICIENT) * orientationValues[0];
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not needed for this example
        }

        public float getCurrentDegree() {
            return currentDegree;
        }

        public float getFusedOrientation() {
            return fusedOrientation;
        }
    }
}