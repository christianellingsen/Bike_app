package com.dtu.helmet_alert;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.firebase.client.Firebase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    // Bluetooth
    public static String helmetAddress = "";
    public static String bikeAddress = "";
    public static boolean activityServiceON = true;
    public static boolean helmetConnect = false;
    public static boolean bikeConnect = false;

    public static BluetoothDevice helmet_BT_device;
    public static BluetoothDevice bike_BT_device;

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
    public static String prefsActivitySerivce = "activitySerivce";
    public static String prefsHelmetConnected = "helmetConn";
    public static String prefsBikeConnected = "bikeConn";

    // Login
    public static boolean emailExits = false;
    private static User user = new User();


    @Override
    public void onCreate() {
        super.onCreate();

        Firebase.setAndroidContext(this);
        Firebase.getDefaultConfig().setPersistenceEnabled(true);

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference ref = db.getReference("debug");
        ref.setValue("Connected,OK!");

        SharedPreferences prefs = getSharedPreferences("com.dtu.susie_bike_app", Context.MODE_PRIVATE);

        helmetAddress = prefs.getString(prefsHelmetAddress, "");
        bikeAddress = prefs.getString(prefsBikeAddress, "");
        activityServiceON = prefs.getBoolean(prefsActivitySerivce, true);
        helmetConnect = prefs.getBoolean(prefsHelmetConnected,false);
        bikeConnect = prefs.getBoolean(prefsBikeConnected,false);

        Log.d("Stored device", "Helmet address: "+helmetAddress);
        Log.d("Stored device", "Bike address: "+bikeAddress);


    }

    public static void setUser(User user) {
        MyApplication.user = user;
    }
}