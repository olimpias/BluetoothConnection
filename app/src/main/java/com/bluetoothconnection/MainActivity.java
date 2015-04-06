package com.bluetoothconnection;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;



public class MainActivity extends ActionBarActivity {
    BluetoothAdapter bluetoothAdapter;
    Button findBlueToothButton;
    Button ledOffButton;
    Button ledOnButton;
    TextView textView;
    BluetoothDevice mmDevice;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    volatile boolean isBluetoothFound = false;
    List<BluetoothGattService> gattServices;
    public static final int BLUETOOTH_ACTIVITY = 1;
    final int RECIEVE_MESSAGE = 1;
    private BluetoothLeService mBluetoothLeService;
    private StringBuilder sb = new StringBuilder();
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLUETOOTH IS NOT SUPPORTED", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        textView = (TextView) findViewById(R.id.textView);
        findBlueToothButton = (Button) findViewById(R.id.findBlueToothButton);
        ledOffButton = (Button) findViewById(R.id.ledOffButton);
        ledOnButton = (Button) findViewById(R.id.ledOnButton);
        textView.setText(R.string.connection_off);
        findBlueToothButton.setOnClickListener(BlueToothFindListener);
        ledOffButton.setOnClickListener(OnOffLedListener);
        ledOnButton.setOnClickListener(OnOffLedListener);

    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mmDevice);
            if(mBluetoothLeService == null) finish();
            textView.setText(R.string.connection_on);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private View.OnClickListener BlueToothFindListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent requestBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                requestBluetooth.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
                startActivityForResult(requestBluetooth, BLUETOOTH_ACTIVITY);
            } else {
                InitiateConnection();
            }
        }
    };
    private View.OnClickListener OnOffLedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String data = "o";
            if (v.getId() == ledOffButton.getId()) {
                data = "c";
            }
            if(mWriteCharacteristic == null){
                gattServices  = mBluetoothLeService.getSupportedGattServices();
                SetCharacters(gattServices);
            }

            try {
                mWriteCharacteristic.setValue(URLEncoder.encode(data,"utf-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.e("ERROR","conversion");
            }
            mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
        }
    };

    public void InitiateConnection () {
        beginConnection();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void SetCharacters(List<BluetoothGattService> gattServices){

        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> characteristics =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                characteristics.add(gattCharacteristic);
            }
            mGattCharacteristics.add(characteristics);
        }
        int counter = 0;
        Log.e("Size is ", mGattCharacteristics.size()+"");
        for(int i = 0 ;i<mGattCharacteristics.size() ;i++){
            ArrayList<BluetoothGattCharacteristic> subArray = mGattCharacteristics.get(i);
            Log.e("Size is ", subArray.size()+"");

            for(int j = 0 ;j<subArray.size() ;j ++){
                BluetoothGattCharacteristic characteristic = subArray.get(j);
                final int charaProp = characteristic.getProperties();
                Log.e("Write character",charaProp+"");

                if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) |
                        (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                    mWriteCharacteristic = characteristic;
                    // popup an dialog to write something.
                    counter++;
                    Log.e("Write character","NOT NULL");
                }
            }
        }
        Log.e("Write character type",""+counter);

    }


    void beginConnection() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("Cem")) {
                    mmDevice = device;
                    Log.d("ArduinoBT", "findBT found device named " + mmDevice.getName());
                    Log.d("ArduinoBT", "device address is " + mmDevice.getAddress());
                    break;
                }
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BLUETOOTH_ACTIVITY) {
            if (resultCode == RESULT_OK) {
               InitiateConnection();
            }
        }

    }

}