package com.example.ivars.cubecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BlueToothConnect extends AppCompatActivity {

    private static final int STATE_MESSAGE = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    ArrayList<BTdevice> BTdevices = new ArrayList<BTdevice>();
    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth_connect);

        initBT();
    }

    private void initBT() {
        //The BluetoothAdapter is required for any and all Bluetooth activity
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //check if BT enabled, if not ask to enable
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            showPairedDevices();
        }
    }

    public void showPairedDevices() {
        //get all already paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BTdevices.add(new BTdevice(deviceName, deviceHardwareAddress));
            }
        }

        //Toast.makeText(this, "Number of paired devices: "+BTdevices.size(),Toast.LENGTH_SHORT).show();

        ListView listView = (ListView) findViewById(R.id.list);
        ArrayAdapter<BTdevice> bTdeviceArrayAdapter = new ArrayAdapter<BTdevice>(this, R.layout.simple_list_item, BTdevices);
        listView.setAdapter(bTdeviceArrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BTdevice clickedDevice = BTdevices.get(position);
                //Toast.makeText(MainActivity.this, clickedDevice.getDeviceHardewareAddress()+"\n"+
                //clickedDevice.getDeviceName(), Toast.LENGTH_SHORT).show();

                //BluetoothDevice mDevice = mBluetoothAdapter.getRemoteDevice(clickedDevice.getDeviceHardewareAddress());

                Intent BTControlIntent = new Intent(BlueToothConnect.this, BTControlActivity.class);
                BTControlIntent.putExtra("deviceAddress", clickedDevice.getDeviceHardewareAddress());
                startActivity(BTControlIntent);

                //mBTService = new BTService(MainActivity.this, mHandler);
                //mBTService.connect(mDevice);
            }
        });
    }

    private void connectToBtDevice(BluetoothDevice device) {
        String TAG = "error";
        BluetoothSocket bluetoothSocket = null;
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            UUID mUUID = UUID.fromString("cb597ac8-ae6d-4c83-8554-d2918fdfdc0f");
            bluetoothSocket = device.createRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            Log.e("TAG", "Socket's create() method failed", e);
        }

        try {
            bluetoothSocket.connect();
            Log.e("TAG", "Connected 1st try");
        } catch (IOException e) {
            Log.e("TAG", "Could not connect the client socket", e);
            try {
                bluetoothSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                bluetoothSocket.connect();

                //bluetoothSocket.close();
            } catch (IOException closeException) {
                Log.e("TAG", "Could not close the client socket", e);
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
            }
        }

//        Toast.makeText(this, "connection succesfull", Toast.LENGTH_SHORT).show();
//
        //test sending a byty
        OutputStream outputStream = null;
        try {
            outputStream = bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        byte[] bytes = {4};
        try {
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForIncommingConnection() {
        //this could be used for a different project but is not needed now
        //When you want to connect two devices, one must act as a server by holding an open BluetoothServerSocket
        BluetoothServerSocket serverSocket = null;
        try {
            //first argument is arbitrary
            //second is UUiD which chould be unique, in this case creating with a random UUID generator
            UUID mUUID = UUID.fromString("cb597ac8-ae6d-4c83-8554-d2918fdfdc0f");
            serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("myName", mUUID);
        } catch (IOException e) {
            Log.e("Get socket: ", "error", e);
        }

        BluetoothSocket socket = null;
        //// Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e("Accept socket: ", "error", e);
            }

            if (socket != null) {
                Toast.makeText(this, "Connection established", Toast.LENGTH_SHORT).show();
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    Log.e("Close socket: ", "error", e);
                }
                break;
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //check if BT was allowed to enable
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "BT enable succesfull", Toast.LENGTH_SHORT).show();
                    showPairedDevices();
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "BT enable failed", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
