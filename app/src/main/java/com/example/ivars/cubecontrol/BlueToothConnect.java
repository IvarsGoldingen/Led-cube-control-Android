package com.example.ivars.cubecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class BlueToothConnect extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    ArrayList<BTdevice> BTDevices = new ArrayList<BTdevice>();
    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth_connect);

        initBT();
    }

    private void initBT() {
        //The BluetoothAdapter is required for any and all Bluetooth activities
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
                BTDevices.add(new BTdevice(deviceName, deviceHardwareAddress));
            }
        }

        ListView listView = (ListView) findViewById(R.id.list);
        ArrayAdapter<BTdevice> bTDeviceArrayAdapter = new ArrayAdapter<BTdevice>(this, R.layout.simple_list_item, BTDevices);
        listView.setAdapter(bTDeviceArrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BTdevice clickedDevice = BTDevices.get(position);
                Intent BTControlIntent = new Intent(BlueToothConnect.this, BTControlActivity.class);
                BTControlIntent.putExtra("deviceAddress", clickedDevice.getDeviceHardewareAddress());
                //start the control activity by sending it the BT device address
                startActivity(BTControlIntent);
            }
        });
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
