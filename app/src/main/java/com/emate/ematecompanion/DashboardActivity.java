package com.emate.ematecompanion;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // Update interval in milliseconds

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

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
                            .zoom(16)  // Adjust the zoom level as needed
                            .bearing(compassListener.getCurrentDegree())  // Set the angle of the camera using compass data
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
    }

    private static class CompassListener implements SensorEventListener {
        private static final float FILTER_ALPHA = 1.0f;

        private final float[] rotationMatrix = new float[9];
        private final float[] orientationValues = new float[3];
        private float currentDegree = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.getOrientation(rotationMatrix, orientationValues);
                float degree = (float) Math.toDegrees(orientationValues[0]);
                currentDegree = currentDegree * (1 - FILTER_ALPHA) + degree * FILTER_ALPHA;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not needed for this example
        }

        public float getCurrentDegree() {
            return currentDegree;
        }
    }
}