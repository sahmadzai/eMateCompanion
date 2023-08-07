package com.emate.ematecompanion;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class BLTConnectActivity extends AppCompatActivity {

    private String LOG_TAG = "BLTConnectActivity";
    private static final int BLUETOOTH_PERMISSION_REQUEST_CODE = 1;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> pairedDevicesList;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Handle the result of the permission request
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BLUETOOTH_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now proceed with sending data over Bluetooth
                Toast.makeText(this, "Permission granted by user, proceeding with data communication.", Toast.LENGTH_SHORT).show();
            } else {
                // Permission denied, handle accordingly (e.g., show an error message)
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blt_connect);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize UI elements
        ListView pairedDevicesListView = findViewById(R.id.paired_devices_list);

        // Check and request Bluetooth permissions if needed
        checkBluetoothPermissions();

        // Get paired devices
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please accept Bluetooth permissions to use this app!", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},
                    BLUETOOTH_PERMISSION_REQUEST_CODE);
        }
        pairedDevicesList = new ArrayList<>(bluetoothAdapter.getBondedDevices());
        ArrayList<String> pairedDevicesNames = new ArrayList<>();
        for (BluetoothDevice device : pairedDevicesList) {
            pairedDevicesNames.add(device.getName());
        }

        // Display paired devices in a ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pairedDevicesNames);
        pairedDevicesListView.setAdapter(adapter);

        // Handle item click to connect to the selected device
        pairedDevicesListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = pairedDevicesList.get(position);
            // Show a confirm connection dialog before connecting which prompts the user to click the device name again to confirm
            Toast.makeText(this, "Confirm you want to connect to: " + selectedDevice.getName() + "by tapping the name again.", Toast.LENGTH_SHORT).show();
            pairedDevicesListView.setOnItemClickListener((parent1, view1, position1, id1) -> {
                Toast.makeText(this, "Connecting to: " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();
                // Connect to the selected device
                connectToDevice(selectedDevice);
            });
        });
    }

    private void checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH},
                    BLUETOOTH_PERMISSION_REQUEST_CODE);
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        Log.v(LOG_TAG, "Connecting to: " + device);
        // Handle the connection logic here
        // You can pass the selected device to DebugActivity and start it
        Intent intent = new Intent(this, DebugActivity.class);
        // Pass the Bluetooth device address to DebugActivity
        intent.putExtra("DEVICE_ADDRESS", device.getAddress());
        startActivity(intent);
    }
}