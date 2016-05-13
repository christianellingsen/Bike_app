package com.dtu.susie_app2;

/**
 * Created by chris on 12-05-2016.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainScreen extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;

    private int mHelmetState, mBikeState = UART_PROFILE_DISCONNECTED;
    private UartService mUARTServiceHelmet, mUARTServiceBike = null;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        activitySerivceSwitch = (Switch) findViewById(R.id.activity_service_switch);
        connect_helmet_b = (Button) findViewById(R.id.connect_to_helmet_button);
        connect_bike_b = (Button) findViewById(R.id.connect_to_bike_button);

        helmetImage = (ImageView) findViewById(R.id.connected_to_helemet_img);
        bikeImage = (ImageView) findViewById(R.id.connected_to_bike_img);
        messageListView = (ListView) findViewById(R.id.main_debug_list);

        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);

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

        service_init();

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

                        Intent newIntent = new Intent(MainScreen.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mHelmetDevice != null) {
                            mUARTServiceHelmet.disconnect();
                            MyApplication.helmetConnect =false;
                            updateConnImages();

                        }
                    }
                }
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

                        Intent newIntent = new Intent(MainScreen.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mBikeDevice != null) {
                            mUARTServiceBike.disconnect();

                        }
                    }
                }
            }
        });

    }


    private void uploadValueToTS(String gyro) {
        final int batt = (int) getBatteryCapacity();
        //final int v = value;
        final String g = gyro;

        new AsyncTask() {
            @Override
            protected Object doInBackground(Object... executeParametre) {
                /**StringBuilder sb = new StringBuilder();
                 sb.append("");
                 sb.append(v);
                 String strI = sb.toString();
                 **/

                StringBuilder sb2 = new StringBuilder();
                sb2.append("");
                sb2.append(batt);
                String strI2 = sb2.toString();

                //Post Data
                List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);
                nameValuePair.add(new BasicNameValuePair("api_key", MyApplication.WRITE_API_KEY));
                //nameValuePair.add(new BasicNameValuePair("field1", strI));
                nameValuePair.add(new BasicNameValuePair("field2", strI2));
                nameValuePair.add(new BasicNameValuePair("field3", g));

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

            @Override
            protected void onPostExecute(Object result) {
                Toast.makeText(MainScreen.this, "Done!", Toast.LENGTH_SHORT).show();
                Log.d("Http post", "Posted to field3: " + g + " and field2: " + batt);
            }
        }.execute(10);

    }

    public float getBatteryCapacity() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / (float) scale;
        return batteryPct * 100;
    }


    //UART service connected/disconnected
    private ServiceConnection mHelmetServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mUARTServiceHelmet = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mUARTServiceHelmet= " + mUARTServiceHelmet);
            if (!mUARTServiceHelmet.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
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
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mUARTServiceHelmet.disconnect(mHelmetDevice);
            mUARTServiceBike = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");

                        if (MyApplication.helmetConnect) {

                            connect_helmet_b.setText("Disconnect");
                            //edtMessage.setEnabled(true);
                            //btnSend.setEnabled(true);
                            //((TextView) findViewById(R.id.deviceName)).setText(mHelmetDevice.getName()+ " - ready");
                            listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mHelmetDevice.getName());
                            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                            mHelmetState = UART_PROFILE_CONNECTED;

                        }
                        else if (MyApplication.bikeConnect){

                            connect_bike_b.setText("Disconnect");
                            //edtMessage.setEnabled(true);
                            //btnSend.setEnabled(true);
                            //((TextView) findViewById(R.id.deviceName)).setText(mHelmetDevice.getName()+ " - ready");
                            listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mBikeDevice.getName());
                            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                            mBikeState = UART_PROFILE_CONNECTED;
                        }

                        updateConnImages();
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");

                        connect_helmet_b.setText("Connect");
                        //edtMessage.setEnabled(false);
                        //btnSend.setEnabled(false);
                        //((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        listAdapter.add("[" + currentDateTimeString + "] Disconnected to: " + mHelmetDevice.getName());
                        mHelmetState = UART_PROFILE_DISCONNECTED;
                        mUARTServiceHelmet.close();
                        //setUiState();
                        updateConnImages();
                    }
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mUARTServiceHelmet.enableTXNotification();
            }
            //*********************//
            /**if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

             final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
             runOnUiThread(new Runnable() {
             public void run() {
             try {
             String text = new String(txValue, "UTF-8");
             String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
             uploadValueToTS(text);
             listAdapter.add("["+currentDateTimeString+"] RX: "+text);
             messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

             } catch (Exception e) {
             Log.e(TAG, e.toString());
             }
             }
             });
             }**/
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                final byte [] valueByte = Arrays.copyOfRange(txValue, 1, txValue.length);

                final String text = new String(new byte[] { txValue[0] });

                //Log.d("Speed debug","Recieved: "+ txValue.length);
                //Log.d("Speed debug","Recieved: "+  Arrays.toString(txValue));

                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            if (text.equals("S")) {
                                double speed = ByteBuffer.wrap(valueByte).order(ByteOrder.LITTLE_ENDIAN).getDouble();
                                //System.out.print("Text recieved: " + text + " : " + speed);
                                listAdapter.add("[" + currentDateTimeString + "] RX: " + text + " : " + speed);
                                messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                            } else if (text.equals("H")) {
                                //int orientation = ByteBuffer.wrap(valueByte).order(ByteOrder.LITTLE_ENDIAN).getInt();
                                String text = new String(valueByte, "UTF-8");
                                //System.out.print("Text recieved: " + text + " : " + orientation);
                                checkHelmet(text);
                                listAdapter.add("[" + currentDateTimeString + "] RX: " + text );
                                messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                            }


                            //String text = new String(txValue, "UTF-8");
                            //String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            //uploadValueToTS(text);
                            //Log.d(TAG, "Text recieved: " + text);
                            //checkHelmet(text);
                            //System.out.print("Text recieved: "+ text + " : "+ speed);
                            //listAdapter.add("[" + currentDateTimeString + "] RX: " + text);
                            //messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
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
    public void onStart() {
        super.onStart();
        /**
         // ATTENTION: This was auto-generated to implement the App Indexing API.
         // See https://g.co/AppIndexing/AndroidStudio for more information.
         mApiClient.connect();
         // ATTENTION: This was auto-generated to implement the App Indexing API.
         // See https://g.co/AppIndexing/AndroidStudio for more information.
         Action viewAction = Action.newAction(
         Action.TYPE_VIEW, // TODO: choose an action type.
         "Main_akt Page", // TODO: Define a title for the content shown.
         // TODO: If you have web page content that matches this app activity's content,
         // make sure this auto-generated web page URL is correct.
         // Otherwise, set the URL to null.
         Uri.parse("http://host/path"),
         // TODO: Make sure this auto-generated app deep link URI is correct.
         Uri.parse("android-app://com.dtu.susie_app2/http/host/path")
         );AppIndex.AppIndexApi.start(mApiClient, viewAction);
         **/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

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

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        /**
         // ATTENTION: This was auto-generated to implement the App Indexing API.
         // See https://g.co/AppIndexing/AndroidStudio for more information.
         Action viewAction = Action.newAction(
         Action.TYPE_VIEW, // TODO: choose an action type.
         "Main_akt Page", // TODO: Define a title for the content shown.
         // TODO: If you have web page content that matches this app activity's content,
         // make sure this auto-generated web page URL is correct.
         // Otherwise, set the URL to null.
         Uri.parse("http://host/path"),
         // TODO: Make sure this auto-generated app deep link URI is correct.
         Uri.parse("android-app://com.dtu.susie_app2/http/host/path")
         );
         AppIndex.AppIndexApi.end(mApiClient, viewAction);
         // ATTENTION: This was auto-generated to implement the App Indexing API.
         // See https://g.co/AppIndexing/AndroidStudio for more information.
         mApiClient.disconnect();
         **/
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

                    if (temp_device.getName().equals("Helmet")){
                        mHelmetDevice = temp_device;

                        MyApplication.helmetConnect = true;

                        Log.d(TAG, "... onActivityResultdevice.address==" + mHelmetDevice + "mserviceValue" + mUARTServiceHelmet);
                        //((TextView) findViewById(R.id.deviceName)).setText(mHelmetDevice.getName()+ " - connecting");
                        mUARTServiceHelmet.connect(deviceAddress);


                        // Store address of device

                        SharedPreferences prefs = getSharedPreferences("com.dtu.susie_bike_app", Context.MODE_PRIVATE);
                        MyApplication.helmetAddress = deviceAddress;
                        prefs.edit().putString(MyApplication.prefsHelmetAddress,MyApplication.helmetAddress).commit();

                    }
                    else if (temp_device.getName().equals("Bike")){
                        mBikeDevice = temp_device;

                        MyApplication.bikeConnect = true;

                        Log.d(TAG, "... onActivityResultdevice.address==" + mBikeDevice + "mserviceValue" + mUARTServiceBike);
                        //((TextView) findViewById(R.id.deviceName)).setText(mHelmetDevice.getName()+ " - connecting");
                        mUARTServiceBike.connect(deviceAddress);


                        // Store address of device

                        SharedPreferences prefs = getSharedPreferences("com.dtu.susie_bike_app", Context.MODE_PRIVATE);
                        MyApplication.bikeAddress = deviceAddress;
                        prefs.edit().putString(MyApplication.prefsBikeAddress,MyApplication.bikeAddress).commit();
                    }

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

    public void checkHelmet(String text){

        if (text.equals("UP")){
            helmetImage.setRotation(0);
        }
        else if (text.equals("DOWN")){
            helmetImage.setRotation(-180);
        }
    }

    /**
     *
     * Activity recognition stuff
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Intent intent = new Intent( this, ActivityRecognizedService.class );
        PendingIntent pendingIntent = PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT );
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates( mApiClient, 5000, pendingIntent );
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void updateConnImages(){

        if (MyApplication.helmetConnect){
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
    }

    public void checkApiClientEnabled(){

        if (MyApplication.activityServiceON){
            mApiClient.connect();
            Toast.makeText(this, "Activity tracking ON ", Toast.LENGTH_SHORT).show();
        }
        else {
            mApiClient.disconnect();
            Toast.makeText(this, "Activity tracking OFF", Toast.LENGTH_SHORT).show();
        }

    }
}
