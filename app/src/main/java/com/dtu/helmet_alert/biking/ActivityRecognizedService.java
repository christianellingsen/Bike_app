package com.dtu.helmet_alert.biking;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.dtu.helmet_alert.MyApplication;
import com.firebase.client.Firebase;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by chris on 11-05-2016.
 */
public class ActivityRecognizedService extends IntentService {

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }
    public ActivityRecognizedService(String name) {
        super(name);
    }

    private String TAG = "ActivityRecognize";

    Firebase ref = new Firebase(MyApplication.firebase_URL);
    Firebase bikeRideRef = ref.child("bikeRides");
    Firebase newBikeRideRef;

    //Firebase Google update
    DatabaseReference mDatabase;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate called");

        // Firebase log
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String date  = dateFormat.format(new Date());

        Firebase ref = new Firebase(MyApplication.firebase_URL);
        Firebase logRef = ref.child("log").child("ActivityRecognizeCalled");
        logRef.setValue(date);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        Log.d("ActivityRecog", "ActivityService called");


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities( result.getProbableActivities() );
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.e( "ActivityRecogition", "In Vehicle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    if( activity.getConfidence() >= 75 ) {
                        Log.d( "ActivityRecogition", "On Bicycle: " + activity.getConfidence() );
                        /**NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Are you bicycling?" );
                        builder.setSmallIcon(R.mipmap.ic_launcher);
                        builder.setContentTitle(getString(R.string.app_name));
                        NotificationManagerCompat.from(this).notify(0, builder.build());
                         **/


                        MyApplication.standStillCounter = 0;
                        MyApplication.ridingBike = true;

                        if (!MyApplication.bleServiceStarted) {

                            startService(new Intent(getBaseContext(), MonitorBikeRide.class));


                            MyApplication.bleServiceStarted = true;


                            // Firebase log
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                            String date  = dateFormat.format(new Date());

                            Firebase ref = new Firebase(MyApplication.firebase_URL);
                            Firebase logRef = ref.child("log").child("BikeTripStart");
                            logRef.setValue(date);
                        }

                    }


                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.e( "ActivityRecogition", "On Foot: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.e("ActivityRecogition", "Running: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.e( "ActivityRecogition", "Still: " + activity.getConfidence() );
                    if( activity.getConfidence() >= 75 ) {
                        /**NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Are you still?" );
                        builder.setSmallIcon( R.mipmap.ic_launcher );
                        builder.setContentTitle( getString( R.string.app_name ) );
                        NotificationManagerCompat.from(this).notify(0, builder.build());
                        **/
                        if (MyApplication.standStillCounter<10) {
                            Log.d("ActivityRecog","standStillCounter: "+MyApplication.standStillCounter);
                            MyApplication.standStillCounter++;
                        }

                    if (MyApplication.standStillCounter>10){
                        Log.d("ActivityRec", "Stand still counter > 10");
                        stopService(new Intent(getBaseContext(), MonitorBikeRide.class));


                        /**NotificationCompat.Builder builder2 = new NotificationCompat.Builder(this);
                         builder2.setContentText( "Done biking?" );
                         builder2.setSmallIcon(R.mipmap.ic_launcher);
                         builder2.setContentTitle(getString(R.string.app_name));
                         NotificationManagerCompat.from(this).notify(0, builder2.build());
                        **/

                        MyApplication.ridingBike = false;

                        // Firebase log
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                        String date  = dateFormat.format(new Date());

                        Firebase ref = new Firebase(MyApplication.firebase_URL);
                        Firebase logRef = ref.child("log").child("BikeTripEnd");
                        logRef.setValue(date);

                    }

                    }
                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.e( "ActivityRecogition", "Tilting: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.WALKING: {
                    Log.e( "ActivityRecogition", "Walking: " + activity.getConfidence() );
                    if( activity.getConfidence() >= 75 ) {


                    }
                    break;
                }
                case DetectedActivity.UNKNOWN: {
                    Log.e("ActivityRecogition", "Unknown: " + activity.getConfidence());
                    break;
                }
            }
        }
    }
}