package com.example.ivars.cubecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * Created by Ivars on 2017.06.03..
 */
//this class contains all the methods for using bluetooth
public class BTService {
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
    private static final String TAG = "BluetoothUtility";
    private static final UUID mUUID =
            UUID.fromString("cb597ac8-ae6d-4c83-8554-d2918fdfdc0f");
    private static final int STATE_MESSAGE = 1;
    private static final int READ_MESSAGE = 2;
    private static final int WRITE_MESSAGE = 3;
    private static final int ERROR_MESSAGE = 4;
    private final BluetoothAdapter mBluetoothAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState = STATE_NONE;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BTService(Context context, Handler handler) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
    }

    public int getState() {
        return mState;
    }

    private void updateUIState() {
        mHandler.obtainMessage(STATE_MESSAGE, mState, -1).sendToTarget();//what|arg1|arg2
    }

    public void connect(BluetoothDevice BTdevice) {

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        mConnectThread = new ConnectThread(BTdevice);
        mConnectThread.start();

        updateUIState();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public void connected(BluetoothSocket socket, BluetoothDevice device) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        updateUIState();
    }

    private void connectionFailed() {
        Message msg = mHandler.obtainMessage(ERROR_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString("ERROR_MESSAGE", "Connection failed");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        updateUIState();
    }

    public void stop() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        updateUIState();
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            //Get the bluetooth socket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            //keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    bytes = mmInStream.read(buffer);
                    //send the received bytes to the UI
                    mHandler.obtainMessage(READ_MESSAGE, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(WRITE_MESSAGE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothDevice mDevice;
        private BluetoothSocket bluetoothSocket;

        public ConnectThread(BluetoothDevice BTDevice) {
            mDevice = BTDevice;
            BluetoothSocket tmp = null;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = mDevice.createRfcommSocketToServiceRecord(mUUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            bluetoothSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            boolean success = true;
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                bluetoothSocket.connect();
                Log.e("TAG", "Connected 1st try");
            } catch (IOException e) {
                Log.e("TAG", "Could not connect the client socket", e);
                try {
                    //2 connection trys are needed because by default the port get a value -1
                    //which does not work for 4.2 and up android versions
                    //https://stackoverflow.com/questions/18657427/ioexception-read-failed-socket-might-closed-bluetooth-on-android-4-3
                    bluetoothSocket = (BluetoothSocket) mDevice.getClass().getMethod(
                            "createRfcommSocket", new Class[]{int.class}).invoke(mDevice, 1);
                    bluetoothSocket.connect();
                    //bluetoothSocket.close();
                } catch (IOException closeException) {
                    success = false;
                    Log.e("TAG", "Could not close the client socket", e);
                } catch (NoSuchMethodException e1) {
                    success = false;
                    e1.printStackTrace();
                } catch (IllegalAccessException e1) {
                    success = false;
                    e1.printStackTrace();
                } catch (InvocationTargetException e1) {
                    success = false;
                    e1.printStackTrace();
                }
            }
            if (success) {
                mConnectThread = null;
                //start the connected thread
                connected(bluetoothSocket, mDevice);
            } else {
                connectionFailed();
            }

        }

        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}
