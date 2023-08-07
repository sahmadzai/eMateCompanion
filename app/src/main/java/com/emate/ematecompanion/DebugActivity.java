package com.emate.ematecompanion;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.UUID;

public class DebugActivity extends AppCompatActivity {

    /**
     * Class variables that are used to store the different values that are selected by the user
     * and some other constant values.
     */
    private double conversionFactor = 1.609;
    private int max_speed = 15; // In kph, default 15mph or 24 kph
    private int current_speed = 0; // In kph, default 0 mph/kph
    private final int LENGTH_SHORT = 800;
    private final String LOG_TAG = "DebugActivity";
    protected String TBS_Command;
    private TextView connection_status;
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice connectedDevice; // Added variable to store the connected device
    private BluetoothSocket bluetoothSocket; // Added variable to manage the Bluetooth socket
    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // A common UUID for SPP

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
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
        initToggleButtons(headlight_switch, headlightStatusLabel);

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
    }

    private void initFields(TextView maxSpeedLabel, TextView currentSpeedLabel, TextView headlightStatusLabel) {
        maxSpeedLabel.setText(getString(R.string.setSpeed_value, max_speed));
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
                max_speed = i + 3;
                Log.v(LOG_TAG, "Max Speed is now: " + max_speed);
                String text = "Max Speed is now: " + max_speed;
                Snackbar.make(seekBar, text, Snackbar.LENGTH_SHORT).setDuration(LENGTH_SHORT).show();

                // Set the Max Speed Field
                maxSpeedLabel.setText(getString(R.string.setSpeed_value, max_speed));
                TBS_Command = generateByteCommand("a3", max_speed);
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
    private void initToggleButtons(Button headlight_switch, TextView headlightStatusLabel) {
        headlight_switch.setOnClickListener(view -> {
            if (headlight_switch.getText().equals(getString(R.string.headlight_status_ON))) {
                headlight_switch.setText(R.string.headlight_status_OFF);
                headlightStatusLabel.setText(getString(R.string.headlight_value, "ON"));
                headlight_switch.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_gray));
                Log.v(LOG_TAG, "Headlights ON");

                TBS_Command = generateByteCommand("a4", 1);
                sendCommandOverBLTH();
            } else {
                headlight_switch.setText(R.string.headlight_status_ON);
                headlightStatusLabel.setText(getString(R.string.headlight_value, "OFF"));
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
            TBS_Command = "Connection established with Bluetooth device!";
            sendCommandOverBLTH();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to device: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * This method sends a command over BLE to the robot.
     */
    private void sendCommandOverBLTH() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.getOutputStream().write(TBS_Command.getBytes());
                Log.v(LOG_TAG, "Sent command over Bluetooth: " + TBS_Command);
                TBS_Command = null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error sending command over Bluetooth: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private String generateByteCommand(String command, int value) {
        String hexValue = Integer.toHexString(value);
        String byteCommand = "";

        if (command.equals("a3")) {
            // Generate byte command for MAX SPEED command
            // Byte 1: a3, Byte 2: 00, Byte 3: 00, Byte 4: value
            byteCommand = "a30000" + hexValue;
        } else if (command.equals("a4")) {
            // Generate byte command for HEADLIGHT STATUS command
            // Byte 1: a4, Byte 2: ac, Byte 3: 00, Byte 4: 00, Byte 5: value
            byteCommand = "a4ac0000" + hexValue;
        }

        return byteCommand;
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