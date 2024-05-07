package com.emate.ematecompanion;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

public class BluetoothService extends Service {
    private static final String LOG_TAG = "BluetoothService";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // A common UUID for SPP
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice connectedDevice;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean connectToDevice(String deviceAddress) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                bluetoothSocket = connectedDevice.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
            }
            Log.v(LOG_TAG, "Connected to device: " + connectedDevice.getName());
            // Starting a thread to handle data receiving might be a good idea
            new ReceiveDataTask().start();
            return true;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to device: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                Log.v(LOG_TAG, "Bluetooth socket closed");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error closing Bluetooth socket: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // This thread will handle the data receiving from the connected device
    private class ReceiveDataTask extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[10];
            try {
                while (true) {
                    if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                        int bytesRead = bluetoothSocket.getInputStream().read(buffer);
                        if (bytesRead == 10) {
                            // Broadcasting the received data so any activity can listen and react
                            Intent intent = new Intent("BluetoothData");
                            intent.putExtra("data", buffer);
                            sendBroadcast(intent);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}