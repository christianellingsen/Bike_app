package com.dtu.helmet_alert;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.dtu.helmet_alert.biking.BikeRide;
import com.dtu.helmet_alert.friends.Notification;
import com.firebase.client.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

/**
 * Created by ce on 31-03-2016.
 */
public class MyApplication extends android.app.Application {

    // ThingSpeak parameters
    public static long CHANNEL_ID = 116190;
    public static String WRITE_API_KEY = "1NTJJB4570409VNW";
    public static String READ_API_KEY = "F18H5802GYB9QG6N";
    public static String TS_ADDRESS = "https://api.thingspeak.com/update";

    // Google maps api
    public static String GOOGLE_MAPS_API_KEY = "AIzaSyAADiWBqxg9VRH_wueHUcs-EIQp4Oc6-rU";

    // Firebase
    public static final String firebase_URL = "https://helmet-alert.firebaseio.com/V1/";
    public static String newBikeRideKey = "";
    public static String usersString = "users";
    public static String bikeRidesString = "bikeTrips";
    public static String notificationString = "notifications";
    private static DatabaseReference mDatabase;

    // Bluetooth
    public static String helmetAddress = "";
    public static String bikeAddress = "";
    public static boolean activityServiceON = true;
    public static boolean helmetConnect = false;
    public static boolean bikeConnect = false;
    public static boolean helmetPaired = false;
    public static boolean bikePaired = false;

    public static BluetoothDevice helmet_BT_device;
    public static BluetoothDevice bike_BT_device;

    public static String HELMET_DEVICE_NAME = "";
    public static String HELMET_DEVICE_ADDRESS = "";
    public static String BIKE_DEVICE_NAME = "";
    public static String BIKE_DEVICE_ADDRESS = "";

    public static String STATE_CALIBRATE = "STATE_CALIBRATE";
    public static String STATE_BIKING = "STATE_BIKING";

    public static final UUID HELMET_ALERT_SERVICE_UUID = UUID.fromString("98D41110-4F9A-CFA3-28E0-9F6DEDFC1688");
    public static final UUID HELMET_ALERT_ACC_X_CHAR_UUID = UUID.fromString("98D40001-4F9A-CFA3-28E0-9F6DEDFC1688");
    public static final UUID HELMET_ALERT_ACC_Y_CHAR_UUID = UUID.fromString("98D40002-4F9A-CFA3-28E0-9F6DEDFC1688");
    public static final UUID HELMET_ALERT_ACC_Z_CHAR_UUID = UUID.fromString("98D40003-4F9A-CFA3-28E0-9F6DEDFC1688");
    public static final UUID HELMET_ALERT_ALARM_CHAR_UUID = UUID.fromString("98D40004-4F9A-CFA3-28E0-9F6DEDFC1688");

    // Bike ride object

    public static BikeRide bikeRide;
    public static boolean bleServiceStarted = false;
    public static boolean ridingBike = false;

    //Strings

    //Background service
    public static boolean runUpload = false;
    public static int standStillCounter = 0;

    public static String prefsHelmetAddress = "helmetAddress";
    public static String prefsBikeAddress = "bikeAddress";
    public static String prefsHelmetName = "helmetName";
    public static String prefsBikeName = "bikeName";

    public static String prefsActivitySerivce = "activitySerivce";
    public static String prefsHelmetPaired = "helmetConn";
    public static String prefsBikePaired = "bikeConn";

    // Login
    public static boolean emailExits = false;
    private static User user;
    public static String userName;


    @Override
    public void onCreate() {
        super.onCreate();

        Firebase.setAndroidContext(this);
        Firebase.getDefaultConfig().setPersistenceEnabled(true);

        user = new User();

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference("debug");
        ref.setValue("Connected,OK!");

        SharedPreferences prefs = getSharedPreferences("com.dtu.susie_bike_app", Context.MODE_PRIVATE);


        HELMET_DEVICE_ADDRESS = prefs.getString(prefsHelmetAddress,"");
        HELMET_DEVICE_NAME = prefs.getString(prefsHelmetName,"");
        BIKE_DEVICE_ADDRESS = prefs.getString(prefsBikeAddress,"");
        BIKE_DEVICE_NAME = prefs.getString(prefsBikeName,"");

        activityServiceON = prefs.getBoolean(prefsActivitySerivce, true);
        helmetPaired = prefs.getBoolean(prefsHelmetPaired,false);
        bikePaired = prefs.getBoolean(prefsBikePaired,false);

        userName = prefs.getString("uName","User name");


        Log.d("Stored device", "Helmet address: "+helmetAddress);
        Log.d("Stored device", "Bike address: "+bikeAddress);


    }

    public static void setUser(User user) {
        MyApplication.user = user;
    }

    public static User getUser() {
        return user;
    }
}
