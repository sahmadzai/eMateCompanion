package com.emate.ematecompanion;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.UUID;

public class DebugActivity extends AppCompatActivity {

    /**
     * Class variables that are used to store the different values that are selected by the user
     * and some other constant values.
     */
    private float max_speed = 0; // In kph, default 15mph or 24 kph
    private double current_speed = 0.0; // In kph, default 0 mph/kph
    private final int LENGTH_SHORT = 800;
    private final String LOG_TAG = "DebugActivity";
    protected byte[] TBS_Command;
    private TextView connection_status;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connectedDevice; // Added variable to store the connected device
    private BluetoothSocket bluetoothSocket; // Added variable to manage the Bluetooth socket
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // A common UUID for SPP
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private TextView textViewGpsSpeed;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates();
            } else {
                // Permission denied, handle accordingly (e.g., show an error message)
                Log.e(LOG_TAG, "Location Permission denied");
                // Prompt the user for permissions
                Snackbar.make(findViewById(R.id.connection_status), "Please accept Location permissions to use the app's GPS!", Snackbar.LENGTH_SHORT).setDuration(LENGTH_SHORT).show();
            }
        } else if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG, "Permission granted by user, proceeding with data communication.");
                // Permission granted, you can now proceed with sending data over Bluetooth
                sendCommandOverBLTH();
            } else {
                // Permission denied, handle accordingly (e.g., show an error message)
                Log.e(LOG_TAG, "Permission denied");
                // Prompt the user for permissions
                Snackbar.make(findViewById(R.id.connection_status), "Please accept Bluetooth permissions to use this app!", Snackbar.LENGTH_SHORT).setDuration(LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_page);

        /* Setting View(s) variables */
        connection_status = (TextView) findViewById(R.id.connection_status);
        TextView maxSpeedLabel = (TextView) findViewById(R.id.setSpeed);
        TextView currentSpeedLabel = (TextView) findViewById(R.id.speed);
        TextView headlightStatusLabel = (TextView) findViewById(R.id.headlight_status);
        Button headlight_switch = (Button) findViewById(R.id.headlight_switch);

        // Setup and start the seekbar listener
        initSeekBar(maxSpeedLabel);

        // Setup the default values for all fields
        initFields(maxSpeedLabel, currentSpeedLabel, headlightStatusLabel);

        // Setup the toggle button(s)
        initToggleButtons(headlight_switch);

        // Setup the start and stop Bluetooth buttons
        initBluetoothButtons();

        // Get the device address from the intent extras
        String deviceAddress = getIntent().getStringExtra("DEVICE_ADDRESS");
        if (deviceAddress != null) {
            Log.v(LOG_TAG, "Device Address: " + deviceAddress);
            // Now you have the device address, you can use it for further Bluetooth communication
            // For example, you can pass this address to your Bluetooth connection method
            connectToDevice(deviceAddress);
        }

        textViewGpsSpeed = findViewById(R.id.textViewGpsSpeed);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double gpsSpeedMetersPerSecond = location.getSpeed(); // Speed in meters per second
                double gpsSpeedMPH = gpsSpeedMetersPerSecond * 2.237;
//                double gpsSpeedKmph = gpsSpeedMetersPerSecond * 3.6; // Convert to km/h

                textViewGpsSpeed.setText(getString(R.string.gps_value, gpsSpeedMPH));
            }

            // Implement other methods of LocationListener if needed
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener, Looper.getMainLooper());
    }

    private void removeLocationUpdates() {
        locationManager.removeUpdates(locationListener);
    }

    private void initFields(TextView maxSpeedLabel, TextView currentSpeedLabel, TextView headlightStatusLabel) {
        maxSpeedLabel.setText(getString(R.string.setSpeed_value, convertKMPHtoMPH(max_speed)));
        currentSpeedLabel.setText(getString(R.string.speed_value, current_speed));
        Log.v(LOG_TAG, "Setting headlight status");
        headlightStatusLabel.setText(getString(R.string.headlight_value, "OFF"));
    }

    /**
     * Method that sets up the seekbar and starts the listener for it.
     * The listener updates the skill level variable when the user changes the value of the seekbar.
     */
    private void initSeekBar(TextView maxSpeedLabel) {
        // Initialize seekbar
        SeekBar skillSeekBar = findViewById(R.id.setSpeed_slider);
        skillSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int usr_speed = i + 3;
                String text = "Max Speed is now: " + usr_speed;
                Log.v(LOG_TAG, text);
                Snackbar.make(seekBar, text, Snackbar.LENGTH_SHORT).setDuration(LENGTH_SHORT).show();

                // Set the Max Speed Field
                maxSpeedLabel.setText(getString(R.string.setSpeed_value, (float) usr_speed));
                TBS_Command = generateByteCommand("a3", usr_speed);
                sendCommandOverBLTH();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    /**
     * This method sets up the toggle buttons that allow the user to show the solution path, full map
     * of the maze, or the walls of the maze. It also sets up a click listener for each button that
     * logs the event.
     */
    private void initToggleButtons(Button headlight_switch) {
        headlight_switch.setOnClickListener(view -> {
            if (headlight_switch.getText().equals(getString(R.string.headlight_status_ON))) {
                headlight_switch.setText(R.string.headlight_status_OFF);
//                headlightStatusLabel.setText(getString(R.string.headlight_value, "ON"));
                headlight_switch.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray));
                Log.v(LOG_TAG, "Headlights ON");

                TBS_Command = generateByteCommand("a4", 1);
                sendCommandOverBLTH();
            } else {
                headlight_switch.setText(R.string.headlight_status_ON);
//                headlightStatusLabel.setText(getString(R.string.headlight_value, "OFF"));
                headlight_switch.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));
                Log.v(LOG_TAG, "Headlights OFF");

                TBS_Command = generateByteCommand("a4", 0);
                sendCommandOverBLTH();
            }
        });
    }

    /**
     * This method sets up the start and stop Bluetooth buttons and sets up a click listener for each
     * button. The start button will take the user to the BLTConnectActivity.java activity and the
     * stop button will disconnect from the Bluetooth device and stop scanning.
     */
    private void initBluetoothButtons() {
        Button startBluetoothButton = findViewById(R.id.start_blth);
        Button stopBluetoothButton = findViewById(R.id.stop_blth);

        startBluetoothButton.setOnClickListener(view -> {
            Log.v(LOG_TAG, "Starting Bluetooth");
            // Pass the blt_serial object to the BLTConnectActivity.java activity
            Intent intent = new Intent(this, BLTConnectActivity.class);
            startActivity(intent);
        });

        stopBluetoothButton.setOnClickListener(view -> {
            Log.v(LOG_TAG, "Stopping Bluetooth");
            // Close the Bluetooth socket when the activity is destroyed
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket.close();
                    connection_status.setText(getString(R.string.connection_DISCNTD));
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error closing Bluetooth socket: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    // Method to convert a byte array to a hexadecimal string
    private String byteArrayToHexString(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(String.format("%02X ", b));
        }
        return stringBuilder.toString();
    }

    private void connectToDevice(String deviceAddress) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.v(LOG_TAG, "Please accept Bluetooth permissions to use this app!");
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
                        BLUETOOTH_PERMISSION_REQUEST_CODE);
            }
            bluetoothSocket = connectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            Log.v(LOG_TAG, "Connected to device: " + connectedDevice.getName());
            connection_status.setText(getString(R.string.connection_CNTD));
            new ReceiveDataTask().start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to device: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Define a thread to receive data from the ESP32
    private class ReceiveDataTask extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[10];
            try {
                while (true) {
                    if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                        // Read 10 bytes from the Bluetooth input stream
                        int bytesRead = bluetoothSocket.getInputStream().read(buffer);
                        if (bytesRead == 10) {
                            runOnUiThread(() -> interpretReceivedData(buffer));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Method to interpret received data and update views
    private void interpretReceivedData(byte[] data) {
        if (data[0] == (byte) 0xAA && data[9] == (byte) 0xBB) {
            int command = data[1] & 0xFF; // Convert the byte to an unsigned integer
            switch (command) {
                case 0xA1:
                    int speedRPM = ((data[4]&0xFF) << 8) | (data[5] & 0xFF);
                    float speedMPH = convertRPMtoMPH(speedRPM); // Convert RPM to speed in mph
                    int temperatureF = convertCtoF(data[7]);
                    // Update relevant TextViews with received data
                    updateSpeedAndTemperature(speedMPH, temperatureF);
                    break;
                case 0xA2:
                    int odometer = data[4]; // Odometer value based on throttle (unknown value)
                    // Update relevant TextView with received data
                    updateOdometer(odometer);
                    break;
                case 0xA3:
                    int maxSpeedMPH = (int) convertKMPHtoMPH(data[3]); // Max speed in km/h
                    // Update relevant TextViews with received data
                    updateMaxSpeed(maxSpeedMPH);
                    break;
                case 0xA4:
                    byte headlightStatus = data[7]; // 0x00 off, 0x10 on
                    // Update relevant TextViews with received data
                    updateHeadlightStatus(headlightStatus);
                    break;
                default:
                    // Handle unknown command or print raw data
//                    Log.e(LOG_TAG, "Received Unknown Command: " + byteArrayToHexString(data));
                    break;
            }
        }
    }

    // Convert mph to km/h
    private float mphToKmph(float mph) {
        return (float) (mph * 1.60934);
    }

    // Convert km/h to mph
    private float convertKMPHtoMPH(float kmph) {
        return kmph / 1.60934f;
    }
    //Convert motor rpm's to mph
    private float convertRPMtoMPH(int rpm) {
        float wheelRadiusInches = 7.0f; // Radius of the wheel in inches
        float wheelCircumferenceInches = 2.0f * (float) Math.PI * wheelRadiusInches;
        float conversionFactor = 60.0f / 63360.0f; // Convert inches per minute to miles per hour

        return (rpm * wheelCircumferenceInches * conversionFactor);
    }
    // Convert Celsius to Fahrenheit
    private int convertCtoF(int celsius) {
        return (int) (celsius * 1.8 + 32);
    }

    // Update speed and temperature TextViews
    private void updateSpeedAndTemperature(float speed, int temperature) {
        TextView currentSpeedLabel = findViewById(R.id.speed);
        currentSpeedLabel.setText(getString(R.string.speed_value, speed));
        TextView temperatureLabel = findViewById(R.id.temp);
        temperatureLabel.setText(getString(R.string.temp_value, temperature));
    }

    // Update odometer TextView
    private void updateOdometer(int odometer) {
        TextView odometerLabel = findViewById(R.id.odometer_value);
        odometerLabel.setText(getString(R.string.odometer_value, odometer));
    }

    private void updateMaxSpeed(float maxSpeedKMPH) {
        // Update TextViews or UI elements with received max speed and run time data
        TextView maxSpeedLabel = findViewById(R.id.setSpeed);
        maxSpeedLabel.setText(getString(R.string.setSpeed_value, maxSpeedKMPH));
    }

    private void updateHeadlightStatus(byte headlightStatus) {
        // Update TextViews or UI elements with received headlight status data
        // For example:
        TextView headlightStatusLabel = findViewById(R.id.headlight_status);
        String statusText = (headlightStatus == 0x00) ? "Off" : "On";
        headlightStatusLabel.setText(getString(R.string.headlight_value, statusText));
    }

    /**
     * This method sends a command over BLE to the robot.
     */
    private void sendCommandOverBLTH() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.getOutputStream().write(TBS_Command);
                Log.v(LOG_TAG, "Sent command over Bluetooth: " + byteArrayToHexString(TBS_Command));
                TBS_Command = null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error sending command over Bluetooth: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private byte[] generateByteCommand(String command, int value) {
        byte[] byteCommand;
        Log.v(LOG_TAG, "The passed in value is: " + mphToKmph(value));

        if (command.equals("a3")) {
            // Generate byte command for MAX SPEED command aa06061eb4bb
            byte kmph = (byte) (value * 1.609);
            byteCommand = new byte[]{(byte) 0xAA, (byte) 0x06, (byte) 0x06, kmph, (byte) 0x00, (byte) 0xBB};
            byte checksum = calculateChecksum(byteCommand); // Checksum value for speed change
            byteCommand[4] = checksum;
        } else if (command.equals("a4")) {
            // Generate byte command for HEADLIGHT STATUS command
            byte onOffValue = (byte) (value == 1 ? 0x01 : 0x00);
            byte onOffByte = (byte) (value == 1 ? 0xAA : 0xAB);
            Log.v(LOG_TAG, String.valueOf(onOffValue));
            byteCommand = new byte[]{(byte) 0xAA, (byte) 0x07, (byte) 0x06, onOffValue, onOffByte, (byte) 0xBB};
        } else {
            byteCommand = new byte[0]; // Return empty byte array for unknown commands
        }

        return byteCommand;
    }


    private byte calculateChecksum(byte[] byteArray) {
        byte checksum = 0x00;
        for(int i = 0; i < byteArray.length - 2; i++)
            checksum ^= byteArray[i];
        return checksum;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the Bluetooth socket when the activity is destroyed
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                connection_status.setText(getString(R.string.connection_DISCNTD));
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error closing Bluetooth socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}