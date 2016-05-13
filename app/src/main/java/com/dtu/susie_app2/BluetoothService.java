package com.dtu.susie_app2;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

//import com.firebase.client.Firebase;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by chris on 12-05-2016.
 */
public class BluetoothService extends Service {

    private BluetoothAdapter mBluetoothAdapter;

    public static final String TAG = "BluetoothService";

    List<BluetoothDevice> deviceList;
    private ServiceConnection onService = null;
    Map<String, Integer> devRssiValues;
    private static final long SCAN_PERIOD = 10000; //10 seconds
    private Handler mHandler;
    private boolean mScanning;

    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    private int mHelmetState, mBikeState = UART_PROFILE_DISCONNECTED;
    private UartService mUARTServiceHelmet, mUARTServiceBike = null;
    private BluetoothDevice mHelmetDevice, mBikeDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    //Firebase ref = new Firebase(MyApplication.firebase_URL);

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
        deviceList = new ArrayList<BluetoothDevice>();
        devRssiValues = new HashMap<String, Integer>();
        Log.d(TAG, "onCreate called");

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        // BT
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

        mScanning = false;

        service_init();

    }


    //UART service connected/disconnected
    private ServiceConnection mHelmetServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mUARTServiceHelmet = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mUARTServiceHelmet= " + mUARTServiceHelmet);
            if (!mUARTServiceHelmet.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mUARTServiceHelmet.disconnect(mHelmetDevice);
            mUARTServiceHelmet = null;
        }
    };

    //UART service connected/disconnected
    private ServiceConnection mBikeServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mUARTServiceBike = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mUARTServiceBike= " + mUARTServiceBike);
            if (!mUARTServiceBike.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mUARTServiceHelmet.disconnect(mHelmetDevice);
            mUARTServiceBike = null;
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_CONNECT_MSG");

                        if (MyApplication.helmetConnect) {
                            mHelmetState = UART_PROFILE_CONNECTED;

                        }
                        else if (MyApplication.bikeConnect){
                            mBikeState = UART_PROFILE_CONNECTED;
                        }
                    }
                };
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                new Runnable() {
                    public void run() {
                        if(!MyApplication.helmetConnect && mHelmetDevice!=null) {
                            mHelmetState = UART_PROFILE_DISCONNECTED;
                            mUARTServiceHelmet.close();
                        }

                        else if(!MyApplication.bikeConnect && mBikeDevice!=null) {
                            mBikeState = UART_PROFILE_DISCONNECTED;
                            mUARTServiceBike.close();
                        }
                    }
                };
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mUARTServiceHelmet.enableTXNotification();
            }
            //*********************//

            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                final byte [] valueByte = Arrays.copyOfRange(txValue, 1, txValue.length);

                final String text = new String(new byte[] { txValue[0] });

                Log.d(TAG,"UART data available");

                new Runnable() {
                    public void run() {
                        try {
                            if (text.equals("S")) {
                                double speed = ByteBuffer.wrap(valueByte).order(ByteOrder.LITTLE_ENDIAN).getDouble();
                                MyApplication.bikeRide.setLastSpeed(speed);

                            } else if (text.equals("H")) {
                                String orientation = new String(valueByte, "UTF-8");
                                //checkHelmet(orientation);
                                if (orientation.equals("UP")) {
                                    MyApplication.bikeRide.setWoreHelmetCorrect(true);
                                }
                                else {
                                    MyApplication.bikeRide.setWoreHelmetCorrect(false);
                                }
                            }

                            //Firebase newBikeRideRef = ref.child("bikeRides").child(MyApplication.newBikeRideKey);
                            //newBikeRideRef.setValue(MyApplication.bikeRide);


                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                };
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                mUARTServiceHelmet.disconnect();
            }


        }
    };



    private void service_init() {
        Intent bindIntentHelmet = new Intent(this, UartService.class);
        bindService(bindIntentHelmet, mHelmetServiceConnection, Context.BIND_AUTO_CREATE);

        Intent bindIntentBike = new Intent(this, UartService.class);
        bindService(bindIntentBike, mBikeServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStart called");

        if (!mScanning){
            Log.d(TAG,"Start scan");
            scanLeDevice(true);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);


                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {

                            // TEST!! Only scan for "Helmet devices
                            //if (device.getName().equals("Helmet")) {
                            //Log.d(TAG,"device added");
                            addDevice(device, rssi);
                            //}

                }
            };



    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;
        //Log.d(TAG,"addDevice called");
        for (BluetoothDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                Log.d(TAG,"device found");
                if (device.getAddress().equals(MyApplication.helmetAddress)){
                    MyApplication.helmet_BT_device = device;
                    mUARTServiceHelmet.connect(device.getAddress());
                    Log.d(TAG,"Connected to helmet");
                }

                else if (device.getAddress().equals(MyApplication.bikeAddress)){
                    MyApplication.bike_BT_device = device;
                    mUARTServiceBike.connect(device.getAddress());
                    Log.d(TAG,"Connected to bike");
                }



                break;
            }
        }

        devRssiValues.put(device.getAddress(), rssi);

        if (!deviceFound) {
            deviceList.add(device);
            Log.d(TAG, "device added");

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanLeDevice(false);

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mHelmetServiceConnection);
        mUARTServiceHelmet.stopSelf();
        mUARTServiceHelmet = null;

        unbindService(mBikeServiceConnection);
        mUARTServiceBike.stopSelf();
        mUARTServiceBike = null;
    }
}


