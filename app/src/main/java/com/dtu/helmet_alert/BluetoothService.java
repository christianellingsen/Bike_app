package com.dtu.helmet_alert;

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
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.firebase.client.Firebase;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by chris on 12-05-2016.
 */
public class BluetoothService extends Service {

    private BluetoothAdapter mBluetoothAdapter;

    public static final String TAG = "BluetoothService";

    List<BluetoothDevice> deviceList;
    private ServiceConnection onService = null;
    Map<String, Integer> devRssiValues;
    private static final long SCAN_PERIOD = 50000; //5 seconds
    private Handler mHandler, timerHandler;
    private boolean mScanning;

    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    private int mHelmetState, mBikeState = UART_PROFILE_DISCONNECTED;
    //private UartService mUARTServiceHelmet, mUARTServiceBike = null;
    private HelmetService mHelmetService;
    private BluetoothDevice mHelmetDevice, mBikeDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    Firebase ref = new Firebase(MyApplication.firebase_URL);

    HttpClient httpClient = new DefaultHttpClient();
    HttpPost httpPost = new HttpPost(MyApplication.TS_ADDRESS);


    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();
        timerHandler = new Handler();
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

        // Firebase log
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String date  = dateFormat.format(new Date());

        Firebase ref = new Firebase(MyApplication.firebase_URL);
        Firebase logRef = ref.child("log").child("BluetoothServiceStart");
        logRef.setValue(date);

    }


    //Helmet service connected/disconnected
    private ServiceConnection mHelmetServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mHelmetService = ((HelmetService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mHelmetService= " + mHelmetService);
            if (!mHelmetService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mUARTServiceHelmet.disconnect(mHelmetDevice);
            mHelmetService = null;
        }
    };



    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(HelmetService.ACTION_GATT_CONNECTED)) {
                Log.d(TAG, "HELMET_CONNECT_MSG");


                if (MyApplication.helmetConnect) {
                    mHelmetState = UART_PROFILE_CONNECTED;

                }
                else if (MyApplication.bikeConnect){
                    mBikeState = UART_PROFILE_CONNECTED;
                }
            }

            //*********************//
            if (action.equals(HelmetService.ACTION_GATT_DISCONNECTED)) {
                Log.d(TAG, "HELMET_DISCONNECT_MSG");
                MyApplication.bikeRide.setWoreHelmetCorrect(false);
                if(!MyApplication.helmetConnect && mHelmetDevice!=null) {
                    mHelmetState = UART_PROFILE_DISCONNECTED;
                    mHelmetService.close();
                    Log.d(TAG, "DISCONNECT_HELMET");
                }

                else if(!MyApplication.bikeConnect && mBikeDevice!=null) {
                    mBikeState = UART_PROFILE_DISCONNECTED;
                    mHelmetService.close();
                }

            }


            //*********************//
            if (action.equals(HelmetService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mHelmetService.enableTXNotification();
            }
            //*********************//

            if (action.equals(HelmetService.ACTION_DATA_AVAILABLE)) {

                //final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                //final byte [] valueByte = Arrays.copyOfRange(txValue, 1, txValue.length);

                //final String text = new String(new byte[] { txValue[0] });

                Log.d(TAG,"ACTION_DATA_AVAILABLE");

                /**
                if (text.equals("S")) {
                    double speed = ByteBuffer.wrap(valueByte).order(ByteOrder.LITTLE_ENDIAN).getDouble();
                    MyApplication.bikeRide.getSpeedHistory().add(speed);
                    //Log.d(TAG, "Speed data");


                } else if (text.equals("H")) {
                    String orientation = null;
                    try {
                        orientation = new String(valueByte, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    //checkHelmet(orientation);
                    if (orientation.equals("UP")) {
                        MyApplication.bikeRide.setWoreHelmetCorrect(true);
                    }
                    else {
                        MyApplication.bikeRide.setWoreHelmetCorrect(false);
                    }
                }
                //Log.d(TAG, "New data logged");
                if (!MyApplication.runUpload){
                    new Thread(uploadToCloud).start();
                    MyApplication.runUpload = true;
                }
                 **/

            }
            //*********************//
            if (action.equals(HelmetService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                mHelmetService.disconnect();
            }


        }
    };



    private void service_init() {
        Intent bindIntentHelmet = new Intent(this, HelmetService.class);
        bindService(bindIntentHelmet, mHelmetServiceConnection, Context.BIND_AUTO_CREATE);

        mHelmetService.initialize();

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HelmetService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(HelmetService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(HelmetService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(HelmetService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(HelmetService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStart called");

        if (!mScanning){
            Log.d(TAG,"Start scan");
            scanLeDevice(true);
        }

        //new Thread(uploadToCloud).start();
        //Log.d(TAG, "onStart called");
        //MyApplication.runUpload = true;
        //uploadRunnable = threadPoolExecutor.submit(uploadToCloud);

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
                //Log.d(TAG,"device found");
                if (device.getAddress().equals(MyApplication.helmetAddress)){
                    MyApplication.helmet_BT_device = device;
                    Log.d(TAG,"Adding device: "+ device.getName());
                    mHelmetService.connect(device.getAddress());
                    Log.d(TAG,"Connected to helmet");
                }

                break;
            }
        }

        devRssiValues.put(device.getAddress(), rssi);

        if (!deviceFound) {
            deviceList.add(device);
            //Log.d(TAG, "device added");

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanLeDevice(false);
        Log.d(TAG, "onDestroy  called");

        try {
            mHelmetService.disconnect();

            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);

            unbindService(mHelmetServiceConnection);
            mHelmetService.stopSelf();
            mHelmetService = null;


        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }

        MyApplication.runUpload = false;
        //uploadRunnable.cancel(true);

        // Firebase log
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String date  = dateFormat.format(new Date());

        Firebase ref = new Firebase(MyApplication.firebase_URL);
        Firebase logRef = ref.child("log").child("BluetoothServiceEnd");
        logRef.setValue(date);

    }

    public final Runnable uploadToCloud = new Runnable() {

        @Override
        public void run() {
            Log.d(TAG, "Upload runnable call. mScanning: " + mScanning);

            Log.d(TAG, "runUpload bool: " + MyApplication.runUpload);
            if (MyApplication.runUpload) {
                Log.d(TAG, "Upload runnable call. mScanning: " + mScanning);
                MyApplication.bikeRide.setEndTime(System.currentTimeMillis());
                MyApplication.bikeRide.updateDistance();
                // Save data to firebase
                Firebase newBikeRideRef = ref.child("bikeRides").child(MyApplication.newBikeRideKey);
                newBikeRideRef.setValue(MyApplication.bikeRide);

                // Start new scan
                if(!mScanning) {
                    scanLeDevice(true);
                }

                // Save to ThingSpeak
                uploadValueToTS();

                // Notify if no helmet
                if (!MyApplication.bikeRide.isWoreHelmetCorrect()){
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext());
                    builder.setContentText( "Remember to use your helmet!" );
                    builder.setSmallIcon( R.mipmap.ic_launcher );
                    builder.setContentTitle( getString( R.string.app_name ) );
                    NotificationManagerCompat.from(getBaseContext()).notify(0, builder.build());
                }

                if (MyApplication.ridingBike) {
                    timerHandler.postDelayed(uploadToCloud, 15000);
                }
                else {
                    stopSelf();
                }
            }
            else {
                //timerHandler.postDelayed(uploadToCloud, 15000);
            }
        }
    };

    public float getBatteryCapacity() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float) scale;
        return batteryPct * 100;
    }



    private void uploadValueToTS() {
        final int batt = (int) getBatteryCapacity();
        //final int v = value;
        final double speed = MyApplication.bikeRide.getAverageSpeed();
        final boolean helmetOn = MyApplication.bikeRide.isWoreHelmetCorrect();

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object... executeParametre) {
                StringBuilder sb = new StringBuilder();
                 sb.append("");
                 sb.append(speed);
                 String strI = sb.toString();


                StringBuilder sb2 = new StringBuilder();
                sb2.append("");
                if (helmetOn){
                    sb2.append(1);
                }
                else {
                    sb2.append(0);
                }
                String strI2 = sb2.toString();

                StringBuilder sb3 = new StringBuilder();
                sb3.append("");
                sb3.append(batt);
                String strI3 = sb3.toString();

                //Post Data
                List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);
                nameValuePair.add(new BasicNameValuePair("api_key", MyApplication.WRITE_API_KEY));
                nameValuePair.add(new BasicNameValuePair("field1", strI));
                nameValuePair.add(new BasicNameValuePair("field2", strI2));
                nameValuePair.add(new BasicNameValuePair("field3", strI3));

                //Encoding POST data
                try {
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
                } catch (UnsupportedEncodingException e) {
                    // log exception
                    e.printStackTrace();
                }

                //making POST request.
                try {
                    HttpResponse response = httpClient.execute(httpPost);
                    // write response to log
                    Log.d("Http Post Response:", response.toString());
                } catch (ClientProtocolException e) {
                    // Log exception
                    e.printStackTrace();
                } catch (IOException e) {
                    // Log exception
                    e.printStackTrace();
                }
                return 0;  // <5>
            }

        }.execute();

    }
}



