package com.dtu.helmet_alert.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import com.dtu.helmet_alert.MyApplication;
import com.dtu.helmet_alert.R;
import com.dtu.helmet_alert.SampleGattAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceServiceBT extends Service {
    private final static String TAG = DeviceServiceBT.class.getSimpleName();

    public static final String EXTRAS_STATE = "STATE";

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public final static String NEW_ACC_DATA =
            "com.example.bluetooth.le.NEW_ACC_DATA";
    public final static String ACC_X_DATA_VALUE =
            "com.example.bluetooth.le.ACC_X_DATA_VALUE";
    public final static String ACC_Y_DATA_VALUE =
            "com.example.bluetooth.le.ACC_Y_DATA_VALUE";
    public final static String ACC_Z_DATA_VALUE =
            "com.example.bluetooth.le.ACC_Z_DATA_VALUE";


    private String mDeviceName;
    private String mDeviceAddress;

    private BluetoothService mBluetoothLeService;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    public boolean mConnected = false;


    private BluetoothGattCharacteristic helmetAccXCharacteristic,helmetAccYCharacteristic,helmetAccZCharacteristic,alarmCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private static final int STATE_CALIBRATE = 0;
    private static final int STATE_BIKING = 1;
    private  int CURRENT_STATE = 0;

    private Handler mHandler;
    private Thread readAccThread, reconnectThread;
    private boolean stopThread0, stopThread1 = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand called");
        mDeviceName=(String) intent.getExtras().get(EXTRAS_DEVICE_NAME);
        mDeviceAddress=(String) intent.getExtras().get(EXTRAS_DEVICE_ADDRESS);
        if (((String)intent.getExtras().get(EXTRAS_STATE)).equals(MyApplication.STATE_CALIBRATE)){
            CURRENT_STATE=STATE_CALIBRATE;
        }
        else if (((String)intent.getExtras().get(EXTRAS_STATE)).equals(MyApplication.STATE_BIKING)){
            CURRENT_STATE=STATE_BIKING;
        }

        Log.d(TAG,"Intent found with data: "+ mDeviceName + " , " + mDeviceAddress);
        return flags;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG,"onCreate called");
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        mHandler = new Handler();

        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        readAccThread = new Thread(readHelmetAccChar);
        reconnectThread = new Thread(retryConnectBT);

        //mBluetoothLeService.connect(mDeviceAddress);
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG,"onServiceConnected called");
            mBluetoothLeService = ((BluetoothService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.d(TAG, "Unable to initialize Bluetooth");
                reconnectThread.start();

            }
            // Automatically connects to the device upon successful start-up initialization.
            Log.d(TAG,"onServiceConnected .connect called on address: "+mDeviceAddress);
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService.disconnect();
            mBluetoothLeService = null;
        }
    };

    private void broadcastUpdate(final String action, final String axis,
                                 final double acc_value) {
        final Intent intent = new Intent(action);
        intent.putExtra(axis, acc_value);
        sendBroadcast(intent);
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.d(TAG,"ACTION_GATT_CONNECTED");
                mConnected = true;
                if (mDeviceName.equals(MyApplication.HELMET_DEVICE_NAME)){
                    MyApplication.helmetConnect=true;
                }
                else if (mDeviceName.equals(MyApplication.BIKE_DEVICE_NAME)){
                    MyApplication.bikeConnect=true;
                }

            } else if (BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.d(TAG,"ACTION_GATT_DISCONNECTED");
                mConnected = false;
                if (mDeviceName.equals(MyApplication.HELMET_DEVICE_NAME)){
                    MyApplication.helmetConnect=false;
                }
                else if (mDeviceName.equals(MyApplication.BIKE_DEVICE_NAME)){
                    MyApplication.bikeConnect=false;
                }
                reconnectThread.start();


            } else if (BluetoothService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());

                Log.d(TAG,"ACTION_GATT_SERVICES_DISCOVERED");
                MyApplication.helmetConnect=true;
                // loop through gatt services to find helmet acc char
                for (BluetoothGattService gattService : mBluetoothLeService.getSupportedGattServices()){
                    //Log.d(TAG,"service uuid: "+ gattService.getUuid().toString());
                    if (gattService.getUuid().toString().equals(MyApplication.HELMET_ALERT_SERVICE_UUID.toString())){
                        Log.d(TAG,"Found Helmet alert service");
                        List<BluetoothGattCharacteristic> gattCharacteristics =
                                gattService.getCharacteristics();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                            if (gattCharacteristic.getUuid().toString().equals(MyApplication.HELMET_ALERT_ACC_X_CHAR_UUID.toString())){
                                helmetAccXCharacteristic = gattCharacteristic;
                                Log.d(TAG,"Helmet acc_x char found");
                                //start runnable to read acc data while connected

                            }
                            else if (gattCharacteristic.getUuid().toString().equals(MyApplication.HELMET_ALERT_ACC_Y_CHAR_UUID.toString())){
                                helmetAccYCharacteristic = gattCharacteristic;
                                Log.d(TAG,"Helmet acc_y char found");
                                //start runnable to read acc data while connected

                            }
                            else if (gattCharacteristic.getUuid().toString().equals(MyApplication.HELMET_ALERT_ACC_Z_CHAR_UUID.toString())){
                                helmetAccZCharacteristic = gattCharacteristic;
                                Log.d(TAG,"Helmet acc_z char found");
                                //start runnable to read acc data while connected

                            }
                            else if (gattCharacteristic.getUuid().toString().equals(MyApplication.HELMET_ALERT_ALARM_CHAR_UUID.toString())){
                                alarmCharacteristic = gattCharacteristic;
                                Log.d(TAG,"Helmet alarm char found");
                                //start runnable to read acc data while connected

                            }
                        }
                    }
                }
                readAccThread.start();

            } else if (BluetoothService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothService.EXTRA_DATA));

                String value = intent.getStringExtra(BluetoothService.EXTRA_DATA);

                //Log.d(TAG,"ACTION_DATA_AVAILABLE! Acc data from char returned: "+value);

                if (intent.getStringExtra(BluetoothService.EXTRA_DATA_AXIS).equals(BluetoothService.X_AXIS)){
                    int acc_x = Integer.parseInt(value);

                    // +- 4G / Resolution (10bits) -> 8/1024
                    double scale = 0.0078;
                    double acc_x_g = scale*acc_x;

                    //offset
                    acc_x_g=acc_x_g*0.85;

                    //Log.d(TAG,"Acc data x in G unit: "+acc_x_g);
                    broadcastUpdate(NEW_ACC_DATA,ACC_X_DATA_VALUE,acc_x_g);
                }
                else if (intent.getStringExtra(BluetoothService.EXTRA_DATA_AXIS).equals(BluetoothService.Y_AXIS)){
                    int acc_y = Integer.parseInt(value);

                    // +- 4G / Resolution (10bits) -> 8/1024
                    double scale = 0.0078;
                    double acc_y_g = scale*acc_y;

                    //offset
                    //acc_y_g=acc_y_g*0.85;

                    //Log.d(TAG,"Acc data y in G unit: "+acc_y_g);
                    broadcastUpdate(NEW_ACC_DATA,ACC_X_DATA_VALUE,acc_y_g);
                }
                else if (intent.getStringExtra(BluetoothService.EXTRA_DATA_AXIS).equals(BluetoothService.Z_AXIS)){
                    int acc_z = Integer.parseInt(value);

                    // +- 4G / Resolution (10bits) -> 8/1024
                    double scale = 0.0078;
                    double acc_z_g = scale*acc_z;

                    //offset
                    //acc_z_g=acc_z_g*0.85;

                    Log.d(TAG,"Acc data z in G unit: "+acc_z_g);
                    broadcastUpdate(NEW_ACC_DATA,ACC_Z_DATA_VALUE,acc_z_g);


                }






            }
        }
    };


    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };


    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService.disconnect();
        mBluetoothLeService = null;
        reconnectThread.interrupt();
        readAccThread.interrupt();
        stopThread0=true;
        stopThread1 = true;
        Log.d(TAG,"onDestroy. Threads: "+reconnectThread.isInterrupted()+readAccThread.isInterrupted());
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public final Runnable readHelmetAccChar = new Runnable() {

        @Override
        public void run() {
            //Log.d(TAG, "Read acc data runnable call.");

            if (mConnected && mBluetoothLeService!=null && !stopThread0) {

                if (CURRENT_STATE==STATE_CALIBRATE) {
                    mBluetoothLeService.readCharacteristic(helmetAccXCharacteristic);
                }
                else if (CURRENT_STATE==STATE_BIKING){
                    mBluetoothLeService.readCharacteristic(helmetAccZCharacteristic);
                }
                mHandler.postDelayed(readHelmetAccChar, 1000);
            }
            else {
                stopThread0=true;
                stopSelf();
            }
        }

    };

    public final Runnable retryConnectBT = new Runnable() {

        @Override
        public void run() {
            //Log.d(TAG, "Read acc data runnable call.");

            if (!MyApplication.helmetConnect && mBluetoothLeService!=null && !stopThread1) {
                Log.d(TAG,"helmet not connected. Trying again");

                // Automatically connects to the device upon successful start-up initialization.
                Log.d(TAG,"Init done. Connecting to address: "+mDeviceAddress);
                mBluetoothLeService.connect(mDeviceAddress);


                mHandler.postDelayed(retryConnectBT, 30000);
            }
            else {
                stopSelf();
                stopThread1=true;
            }
        }

    };


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        //mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
