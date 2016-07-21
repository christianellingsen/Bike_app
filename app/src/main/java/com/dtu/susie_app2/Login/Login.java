package com.dtu.susie_app2.Login;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dtu.susie_app2.Main_akt;
import com.dtu.susie_app2.MyApplication;
import com.dtu.susie_app2.R;
import com.dtu.susie_app2.User;
import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.HashSet;

/**
 * A login screen that offers login via email/password.
 */
public class Login extends AppCompatActivity  {

    final Firebase ref = new Firebase(MyApplication.firebase_URL);

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private boolean doneFetchingUser =false;
    private Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_akt);

        handler = new Handler();

        // Set up the login form.
        mEmailView = (EditText) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }


    private void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }
        if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            new Thread(startWhenReady).start();
            ref.authWithPassword(email, password,
                    new Firebase.AuthResultHandler() {

                        @Override
                        public void onAuthenticated(AuthData authData) {
                            // Authentication just completed successfully :)
                            /**Map<String, String> map = new HashMap<String, String>();
                             map.put("provider", authData.getProvider());
                             if (authData.getProviderData().containsKey("displayName")) {
                             map.put("displayName", authData.getProviderData().get("displayName").toString());
                             }

                             ref.child("users").child(authData.getUid()).setValue(map);
                             **/
                            Log.d("Login","Login success");
                            saveLocalUserData(authData.getUid());
                        }

                        @Override
                        public void onAuthenticationError(FirebaseError error) {
                            //startActivity(new Intent(getBaseContext(),Login.class));
                            showProgress(false);
                            Snackbar snackbar = Snackbar
                                    .make(mLoginFormView, "Failed to log in", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    });
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public void saveLocalUserData(final String uid){

        MyApplication.setUser(null);

        Firebase usersRef = ref.child(MyApplication.usersString);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String child_uid = (String) child.getKey();
                    if (uid.equals(child_uid)) {
                        User user;
                        user = child.getValue(User.class);
                        user.setU_ID(child_uid.trim());
                        Log.d("Firebase", "User found: " + user.getFirstName());
                        MyApplication.setUser(user);

                        SharedPreferences prefs = getSharedPreferences("com.dtu.tournamate_v1", Context.MODE_PRIVATE);
                        prefs.edit().putString("uID",user.getU_ID()).apply();
                        prefs.edit().putString("firstName", user.getFirstName()).apply();
                        prefs.edit().putString("lastName", user.getLastName()).apply();
                        prefs.edit().putString("email", user.getEmail()).apply();
                        prefs.edit().putStringSet("tournaments", new HashSet<String>(user.getStoredTournamentsID())).apply();
                        prefs.edit().commit();

                    }
                    doneFetchingUser = true;
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }

        });

    }


    public final Runnable startWhenReady = new Runnable() {

        @Override
        public void run() {

            if (doneFetchingUser==true) {
                startActivity(new Intent(getBaseContext(), Main_akt.class));
            }
            else {
                handler.postDelayed(startWhenReady, 100);
            }
        }
    };

}

