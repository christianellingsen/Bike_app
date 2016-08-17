package com.dtu.helmet_alert;

import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.dtu.helmet_alert.friends.FriendsList;
import com.dtu.helmet_alert.friends.Notification;
import com.dtu.helmet_alert.home.Home_frag;
import com.dtu.helmet_alert.login.WelcomeScreen_akt;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    TextView username;
    FrameLayout mainFrame;

    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mainFrame = (FrameLayout) findViewById(R.id.main_frame);

        getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .replace(R.id.main_frame, new Home_frag())
                .commit();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        mDatabase = FirebaseDatabase.getInstance().getReference();


        View header = navigationView.getHeaderView(0);
        username = (TextView) header.findViewById(R.id.drawerTop_userName);
        //username.setText(user.getDisplayName());
        //TODO store user on sign in or login, and get users name from MyApplication object.
        username.setText(MyApplication.userName);
        fetchUser();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_trips) {
            // Handle the camera action
        } else if (id == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.main_frame, new Home_frag())
                    .commit();
        }
        else if (id == R.id.nav_friends) {
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.main_frame, new FriendsList())
                    .commit();
        } else if (id == R.id.nav_calibrate_helmet) {
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.main_frame, new CalibrateHelmet())
                    .commit();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, PairDevices.class));
        } else if (id == R.id.nav_about) {

        } else if (id == R.id.nav_logout) {
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            mAuth.signOut();
            startActivity(new Intent(this, WelcomeScreen_akt.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void notificationListenInit(){

        DatabaseReference notificationsRef = mDatabase.child(MyApplication.usersString).child(MyApplication.getUser().getU_key()).child(MyApplication.notificationString);

        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Notification newNotification = dataSnapshot.getValue(Notification.class);

                Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationCompat.Builder builder2 = new NotificationCompat.Builder(getApplicationContext());
                builder2.setContentText(newNotification.getMessage());
                builder2.setSmallIcon(R.mipmap.ic_launcher);
                builder2.setContentTitle(getString(R.string.app_name));
                builder2.setSound(alarmSound);
                NotificationManagerCompat.from(getApplicationContext()).notify(0, builder2.build());

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        notificationsRef.addChildEventListener(childEventListener);


    }


    public void fetchUser(){
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        mDatabase = FirebaseDatabase.getInstance().getReference();

        if(user!=null) {

            final String uID = user.getUid();
            Log.d("Main fetchUser", "auth uID: " + uID);

            //Query query = mDatabase.child("users").orderByChild("u_ID");

            DatabaseReference userRef;
            userRef = mDatabase.child(MyApplication.usersString).child(uID);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d("MyApplication on start", "Found user! ");
                        User u = dataSnapshot.getValue(User.class);
                        Log.d("MyApplication on start", "Found user! " + u.getFullName());
                            MyApplication.setUser(u);
                            Log.d("On MyApplication start", "Logged in as " + MyApplication.getUser().getFullName());
                            username.setText(u.getFullName());
                            notificationListenInit();
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

}
