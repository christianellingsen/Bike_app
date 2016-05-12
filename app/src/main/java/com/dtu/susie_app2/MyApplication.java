package com.dtu.susie_app2;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by ce on 31-03-2016.
 */
public class MyApplication extends android.app.Application {

    // ThingSpeak parameters
    public static long CHANNEL_ID = 104531;
    public static String WRITE_API_KEY = "Z1TWGBJK5QJ0YA0H";
    public static String READ_API_KEY = "F18H5802GYB9QG6N";
    public static String TS_ADDRESS = "https://api.thingspeak.com/update";

    // Google maps api
    public static String GOOGLE_MAPS_API_KEY = "AIzaSyAADiWBqxg9VRH_wueHUcs-EIQp4Oc6-rU";

    // Bluetooth

    public static String helmetAddress = "";
    public static String bikeAddress = "";
    public static boolean activityServiceON = true;
    public static boolean helmetConnect = false;
    public static boolean bikeConnect = false;

    public static BluetoothDevice helmet_BT_device;
    public static BluetoothDevice bike_BT_device;


    //Strings

    public static String prefsHelmetAddress = "helmetAddress";
    public static String prefsBikeAddress = "bikeAddress";
    public static String prefsActivitySerivce = "activitySerivce";
    public static String prefsHelmetConnected = "helmetConn";
    public static String prefsBikeConnected = "bikeConn";

    //TODO
    // Get address from shared prefs
    // Setup helmet and bike. Scan and connect. Store addresses.
    // On bike activity detected scan for bike and helmet, and start log/notify helmet missing etc.



    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences prefs = getSharedPreferences("com.dtu.susie_bike_app", Context.MODE_PRIVATE);

        helmetAddress = prefs.getString(prefsHelmetAddress, "");
        bikeAddress = prefs.getString(prefsBikeAddress, "");
        activityServiceON = prefs.getBoolean(prefsActivitySerivce, true);
        helmetConnect = prefs.getBoolean(prefsHelmetConnected,false);
        bikeConnect = prefs.getBoolean(prefsBikeConnected,false);

        Log.d("Stored device", "Helmet address: "+helmetAddress);


    }
}
