package com.dtu.helmet_alert.biking;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.dtu.helmet_alert.MyApplication;
import com.dtu.helmet_alert.R;
import com.dtu.helmet_alert.bluetooth.BluetoothService;
import com.dtu.helmet_alert.bluetooth.DeviceServiceBT;
import com.dtu.helmet_alert.friends.Friend;
import com.dtu.helmet_alert.friends.Notification;
import com.firebase.client.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chris on 17-08-2016.
 */
public class MonitorBikeRide extends Service {

    private final static String TAG = MonitorBikeRide.class.getSimpleName();

    private DatabaseReference mDatabase;
    BikeRide bikeRide;
    DatabaseReference bikeRideRef;

    private double acc_z = 0.0;
    private int updateCounter = 0;
    private int updateCounterMax = 30;
    private boolean helmet_violationFlag0 = false;
    private boolean helmet_violationFlag1 = false;
    private boolean firstRun = true;

    private Handler mHandler;
    private Thread uploadThread;
    private boolean threadStarted = false;
    private boolean stopThread = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand called");

        return flags;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate called");

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Broadcast reciever
        registerReceiver(accDataUpdateReceiver, accDataUpdateIntentFilter());

        mHandler = new Handler();
        uploadThread = new Thread(uploadFirebase);

        bikeRide = new BikeRide();

        bikeRideRef = mDatabase.child(MyApplication.bikeRidesString).push();
        bikeRide.setBikeRide_ID(bikeRideRef.getKey());
        bikeRide.setUserID(MyApplication.getUser().getU_key());

        final Intent intent = new Intent(getBaseContext(), DeviceServiceBT.class);
        intent.putExtra(DeviceServiceBT.EXTRAS_DEVICE_NAME, MyApplication.HELMET_DEVICE_NAME);
        intent.putExtra(DeviceServiceBT.EXTRAS_DEVICE_ADDRESS, MyApplication.HELMET_DEVICE_ADDRESS);
        intent.putExtra(DeviceServiceBT.EXTRAS_STATE,MyApplication.STATE_BIKING);
        startService(intent);

        //uploadThread.start();

    }


    public void uploadToFirebase(double value)
    {

        Log.d(TAG,"upload. Value: "+value);

        if (value<3.12 && helmet_violationFlag0==false && !firstRun){
            //Helmet upside down
            bikeRide.setWoreHelmetCorrect(false);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String date  = dateFormat.format(new Date());

            bikeRide.getViolationTimeStamp().add(date);
            bikeRide.getViolationType().add("Helmet not worn correctly");
            bikeRideRef.setValue(bikeRide);
            makeAlarm();
            helmet_violationFlag0 = true;

        }
        else if (!MyApplication.helmetConnect && helmet_violationFlag1==false){
            bikeRide.setWoreHelmetCorrect(false);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String date  = dateFormat.format(new Date());

            bikeRide.getViolationTimeStamp().add(date);
            bikeRide.getViolationType().add("Helmet not in range");
            bikeRideRef.setValue(bikeRide);
            makeAlarm();
            helmet_violationFlag1=true;
        }
        else {
            bikeRide.setWoreHelmetCorrect(true);
        }

        if (updateCounter==updateCounterMax) {
            bikeRideRef.setValue(bikeRide);
            updateCounter = 0;

            helmet_violationFlag0=false;
            helmet_violationFlag1=false;
            firstRun = false;
        }
        updateCounter++;

    }

    private static IntentFilter accDataUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DeviceServiceBT.NEW_ACC_DATA);
        intentFilter.addAction(BluetoothService.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }

    private final BroadcastReceiver accDataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DeviceServiceBT.NEW_ACC_DATA.equals(action)) {

                //update acc_z value
                acc_z = intent.getDoubleExtra(DeviceServiceBT.ACC_Z_DATA_VALUE,0);
                //uploadToFirebase(acc_z);

            }
            else if(BluetoothService.ACTION_GATT_DISCONNECTED.equals(action)){
                Log.d(TAG,"BT device not connected");
                MyApplication.helmetConnect=false;
            }
            if (!threadStarted && !uploadThread.isAlive()){
                uploadThread.start();
                threadStarted=true;
            }

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy  called");

        //Notify friend if violations:
        if (bikeRide.getViolationType().size()>0){
            for (Friend f : MyApplication.getUser().getStoredFriends()){
                Log.d(TAG, "Friend: "+f.getFullName());
                String f_key = f.getU_key();

                DatabaseReference nofificationRef;
                nofificationRef = mDatabase.child(MyApplication.usersString).child(f_key).child(MyApplication.notificationString).push();

                Notification n = new Notification();

                n.setFromU_ID(MyApplication.getUser().getU_key());
                n.setToU_ID(f_key);
                n.setMessage(MyApplication.getUser().getFirstName()+" just completed a bike ride with "+
                                bikeRide.getViolationType().size()+ " violations. Remind your friend to use the helmet");
                nofificationRef.setValue(n);
            }
        }

        DatabaseReference userBikeRideIDRef;
        userBikeRideIDRef = mDatabase.child(MyApplication.usersString).child(MyApplication.getUser().getU_key()).child("storedTripsID").push();
        userBikeRideIDRef.setValue(bikeRide.getBikeRide_ID());

        uploadThread.interrupt();
        stopThread=true;
        unregisterReceiver(accDataUpdateReceiver);
        Log.d(TAG, "Thread stopped:" + uploadThread.isInterrupted());
        try {
            stopService(new Intent(getBaseContext(), DeviceServiceBT.class));
            stopSelf();

        } catch (Exception ignore) {
            Log.d(TAG, ignore.toString());
        }

        Log.d(TAG,"test after onDestroy");

        bikeRide.setEndTime(System.currentTimeMillis());
        bikeRideRef.setValue(bikeRide);


        // Firebase log
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String date  = dateFormat.format(new Date());

        Firebase ref = new Firebase(MyApplication.firebase_URL);
        Firebase logRef = ref.child("log").child("BluetoothServiceEnd");
        logRef.setValue(date);

    }

    public void makeAlarm(){
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder2 = new NotificationCompat.Builder(this);
         builder2.setContentText( "Remember to use your helmet!" );
         builder2.setSmallIcon(R.mipmap.ic_launcher);
         builder2.setContentTitle(getString(R.string.app_name));
         builder2.setSound(alarmSound);
        builder2.setVibrate(new long[] { 1000, 1000, 1000, 1000 });
         NotificationManagerCompat.from(this).notify(0, builder2.build());

    }

    public final Runnable uploadFirebase = new Runnable() {

        @Override
        public void run() {
            //Log.d(TAG, "Read acc data runnable call.");

                if (!stopThread) {
                    uploadToFirebase(acc_z);
                    mHandler.postDelayed(uploadFirebase, 1000);
                }
                else {

                }
            }



    };

}
