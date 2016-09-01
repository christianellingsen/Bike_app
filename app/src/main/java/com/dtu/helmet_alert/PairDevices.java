package com.dtu.helmet_alert;

/**
 * Created by chris on 12-05-2016.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.dtu.helmet_alert.biking.ActivityRecognizedService;
import com.dtu.helmet_alert.biking.BikeRide;
import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PairDevices extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "ConnectToBT";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    private int mHelmetState, mBikeState = UART_PROFILE_DISCONNECTED;
    //private UartService mUARTServiceHelmet, mUARTServiceBike = null;
    private BluetoothDevice mHelmetDevice, mBikeDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    private Button connect_helmet_b, connect_bike_b;
    private ImageView helmetImage,bikeImage;
    private Switch activitySerivceSwitch;

    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;

    String link = MyApplication.TS_ADDRESS;
    HttpClient httpClient = new DefaultHttpClient();
    HttpPost httpPost = new HttpPost(link);
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient mApiClient;

    // TEST STUFF:

    Button test_b;
    OLD_BluetoothService customBTService = new OLD_BluetoothService();
    Firebase ref = new Firebase(MyApplication.firebase_URL);
    Firebase bikeRideRef = ref.child("bikeRides");
    Firebase newBikeRideRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pair_devices_settings);

        test_b = (Button) findViewById(R.id.test_b);

        activitySerivceSwitch = (Switch) findViewById(R.id.activity_service_switch);

        connect_helmet_b = (Button) findViewById(R.id.connect_to_helmet_button);
        connect_bike_b = (Button) findViewById(R.id.connect_to_bike_button);

        helmetImage = (ImageView) findViewById(R.id.connected_to_helemet_img);
        bikeImage = (ImageView) findViewById(R.id.connected_to_bike_img);
        messageListView = (ListView) findViewById(R.id.main_debug_list);

        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);


        updateConnImages();

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        activitySerivceSwitch.setChecked(MyApplication.activityServiceON);

        checkApiClientEnabled();


        // BT
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //service_init();

        test_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick- Test.");
                if (test_b.getText().equals("Start")){
                    MyApplication.ridingBike = true;
                    newBikeRideRef = bikeRideRef.push();
                    MyApplication.bikeRide = new BikeRide();
                    MyApplication.newBikeRideKey = newBikeRideRef.getKey();
                    startService(new Intent(getBaseContext(), OLD_BluetoothService.class));
                    test_b.setText("Stop");
                }
                else if (test_b.getText().equals("Stop")) {
                    MyApplication.ridingBike = false;
                    stopService(new Intent(getBaseContext(), OLD_BluetoothService.class));
                    test_b.setText("Start");
                    MyApplication.bikeRide.setEndTime(System.currentTimeMillis());
                    newBikeRideRef.setValue(MyApplication.bikeRide);
                }
            }
        });

        activitySerivceSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick- switch. State:" + activitySerivceSwitch.isChecked());
                MyApplication.activityServiceON = activitySerivceSwitch.isChecked();
                SharedPreferences prefs = getSharedPreferences("com.dtu.susie_bike_app", Context.MODE_PRIVATE);
                Log.i(TAG, "Saving state to prefs: " +MyApplication.activityServiceON);
                prefs.edit().putBoolean(MyApplication.prefsActivitySerivce, MyApplication.activityServiceON).commit();

                Log.i(TAG, "State in prefs: " + prefs.getBoolean(MyApplication.prefsActivitySerivce, true));

                checkApiClientEnabled();
            }
        });

        // Handler Disconnect & Connect button
        connect_helmet_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick- helmet");
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (connect_helmet_b.getText().equals("Connect")) {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(PairDevices.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mHelmetDevice != null) {
                            //mUARTServiceHelmet.disconnect();
                            //MyApplication.helmetConnect =false;


                        }
                    }
                }
                updateConnImages();
            }
        });

        // Handler Disconnect & Connect button
        connect_bike_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick- bike");
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (connect_bike_b.getText().equals("Connect")) {
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(PairDevices.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mBikeDevice != null) {
                            //mUARTServiceBike.disconnect();
                            //MyApplication.bikeConnect =false;

                        }
                    }
                }
                updateConnImages();
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            /**LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);

            unbindService(mHelmetServiceConnection);
            mUARTServiceHelmet.stopSelf();
            mUARTServiceHelmet = null;

            unbindService(mBikeServiceConnection);
            mUARTServiceBike.stopSelf();
            mUARTServiceBike = null;
             **/

        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }


    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);

                    BluetoothDevice temp_device;

                    temp_device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    if (temp_device.getName().equals("HelmetAlert_H")){
                        mHelmetDevice = temp_device;

                        // Store name nad address of device

                        SharedPreferences prefs = getSharedPreferences("com.dtu.susie_bike_app", Context.MODE_PRIVATE);
                        MyApplication.HELMET_DEVICE_ADDRESS = deviceAddress;
                        MyApplication.HELMET_DEVICE_NAME = temp_device.getName();

                        prefs.edit().putString(MyApplication.prefsHelmetAddress,MyApplication.HELMET_DEVICE_ADDRESS).apply();
                        prefs.edit().putString(MyApplication.prefsHelmetName,MyApplication.HELMET_DEVICE_NAME).apply();
                        prefs.edit().putBoolean(MyApplication.prefsHelmetPaired,true).apply();
                        prefs.edit().commit();

                    }
                    else if (temp_device.getName().equals("HelmetAlert_B")){
                        mBikeDevice = temp_device;

                        // Store name and address of device

                        SharedPreferences prefs = getSharedPreferences("com.dtu.susie_bike_app", Context.MODE_PRIVATE);
                        MyApplication.BIKE_DEVICE_ADDRESS = deviceAddress;
                        MyApplication.BIKE_DEVICE_NAME = temp_device.getName();

                        prefs.edit().putString(MyApplication.prefsBikeAddress,MyApplication.BIKE_DEVICE_ADDRESS).apply();
                        prefs.edit().putString(MyApplication.prefsBikeName,MyApplication.BIKE_DEVICE_NAME).apply();
                        prefs.edit().putBoolean(MyApplication.prefsBikePaired,true).apply();
                        prefs.edit().commit();


                    }

                    updateConnImages();

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mHelmetState == UART_PROFILE_CONNECTED || mBikeState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }


    /**
     *
     * Activity recognition stuff
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.d(TAG,"onConnected called");

        Intent intent = new Intent( this, ActivityRecognizedService.class );
        PendingIntent pendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( mApiClient, 5000, pendingIntent );

        // Firebase log
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String date  = dateFormat.format(new Date());

        Firebase ref = new Firebase(MyApplication.firebase_URL);
        Firebase logRef = ref.child("log").child("ActivityRecognizeStart");
        logRef.setValue(date);
    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.d(TAG, "onConnectionSuspended");


    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG,"onConnectionFailed called");
    }




    public void updateConnImages(){

        /**if (MyApplication.helmetConnect){
            helmetImage.setImageResource(R.drawable.bike_helmet_check);
        }
        if (!MyApplication.helmetConnect){
            helmetImage.setImageResource(R.drawable.bike_helmet_bw);
        }
        if (MyApplication.bikeConnect){
            bikeImage.setImageResource(R.drawable.bike_check);
        }
        if (!MyApplication.bikeConnect){
            bikeImage.setImageResource(R.drawable.bike_check_bw);
        }
        **/
        if (MyApplication.HELMET_DEVICE_ADDRESS.length()>1){
            helmetImage.setImageResource(R.drawable.bike_helmet_check);
        }
        else{
            helmetImage.setImageResource(R.drawable.helmet_bw);
        }
        if (MyApplication.BIKE_DEVICE_ADDRESS.length()>1){
            bikeImage.setImageResource(R.drawable.bike_check);
        }
        else {
            bikeImage.setImageResource(R.drawable.bike_check_bw);
        }
    }

    public void checkApiClientEnabled(){

        if (MyApplication.activityServiceON){
            mApiClient.connect();
            Toast.makeText(this, "Activity tracking ON ", Toast.LENGTH_SHORT).show();
        }
        else {

            if (mApiClient.isConnected()) {
                Intent intent = new Intent(this, ActivityRecognizedService.class);
                PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mApiClient, pendingIntent);
                mApiClient.disconnect();
                Toast.makeText(this, "Activity tracking OFF", Toast.LENGTH_SHORT).show();

                // Firebase log
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String date  = dateFormat.format(new Date());

                Firebase ref = new Firebase(MyApplication.firebase_URL);
                Firebase logRef = ref.child("log").child("ActivityRecognizeEnd");
                logRef.setValue(date);

            }
        }

    }
}
