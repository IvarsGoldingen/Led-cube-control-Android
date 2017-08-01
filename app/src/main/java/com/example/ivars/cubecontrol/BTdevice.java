package com.example.ivars.cubecontrol;

/**
 * Created by Ivars on 2017.06.01..
 */

public class BTdevice {
    private String mDeviceName;
    private String mDeviceHardewareAddress;

    public BTdevice(String deviceName, String deviceHardewareAddress) {
        mDeviceName = deviceName;
        mDeviceHardewareAddress = deviceHardewareAddress;
    }

    public String getDeviceName() {
        return mDeviceName;
    }

    public String getDeviceHardewareAddress() {
        return mDeviceHardewareAddress;
    }

    @Override
    public String toString() {
        return mDeviceName + "\n" + mDeviceHardewareAddress;
    }
}
