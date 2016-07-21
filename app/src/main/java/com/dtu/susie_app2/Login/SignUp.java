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
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
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
import java.util.Map;

/**
 * Created by chris on 30-04-2016.
 */
public class SignUp extends AppCompatActivity {

    final Firebase ref = new Firebase(MyApplication.firebase_URL);
    EditText mFirstName,mLastName,mEmail,mPassword,mPasswordRepeat;
    Button signIn;

    private View mProgressView;
    private View mSignFormView;
    Handler handler = new Handler();
    boolean doneEmailCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in_akt);

        mFirstName = (EditText) findViewById(R.id.welcome_first_name_et);
        mLastName = (EditText) findViewById(R.id.welcome_last_name_et);
        mEmail = (EditText) findViewById(R.id.welcome_email_et);
        mPassword = (EditText) findViewById(R.id.welcome_password_et);
        mPasswordRepeat = (EditText) findViewById(R.id.welcome_password_rep_et);
        signIn = (Button) findViewById(R.id.signUp_b);

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("email", "Email: " + mEmail.getText().toString());
                doneEmailCheck =false;
                emailExists(mEmail.getText().toString());
                new Thread(resumeWhenReady).start();
            }
        });

        mSignFormView = findViewById(R.id.signUp_form);
        mProgressView = findViewById(R.id.signUp_progress);

        mPasswordRepeat.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.signUp || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

    }

    private void attemptLogin() {

        // Reset errors.
        mEmail.setError(null);
        mPassword.setError(null);

        // Store values at the time of the login attempt.
        final String firstName = mFirstName.getText().toString();
        final String lastName = mLastName.getText().toString();
        final String email = mEmail.getText().toString();
        final String password = mPassword.getText().toString();
        final String passwordRep = mPasswordRepeat.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPassword.setError(getString(R.string.error_invalid_password));
            focusView = mPassword;
            cancel = true;
        }

        if (!isPasswordsSame(password,passwordRep)){
            mPasswordRepeat.setError("Passwords don't match");
            focusView = mPasswordRepeat;
            cancel = true;
        }

        if (MyApplication.emailExits){
            mEmail.setError("Email already exits");
            focusView = mEmail;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmail.setError(getString(R.string.error_field_required));
            focusView = mEmail;
            cancel = true;
        }

        if (!isEmailValid(email)) {
            mEmail.setError(getString(R.string.error_invalid_email));
            focusView = mEmail;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            //createUser(firstName,lastName,email,password,passwordRep);
            showProgress(true);
            ref.createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    System.out.println("Successfully created user account with uid: " + result.get("uid"));

                    ref.authWithPassword(email, password,
                            new Firebase.AuthResultHandler() {

                                @Override
                                public void onAuthenticated(AuthData authData) {
                                    // Authentication just completed successfully :)

                                    User user = new User();
                                    user.setFirstName(firstName);
                                    user.setLastName(lastName);
                                    user.setEmail(email);
                                    user.setU_ID(authData.getUid());
                                    user.setProvider(authData.getProvider());
                                    //user.setStoredTournamentsID(new HashSet<String>());
                                    MyApplication.setUser(user);
                                    ref.child("users").child(authData.getUid()).setValue(user);

                                    SharedPreferences prefs = getSharedPreferences("com.dtu.tournamate_v1", Context.MODE_PRIVATE);
                                    prefs.edit().putString("uID",user.getU_ID()).apply();
                                    prefs.edit().putString("firstName", firstName).apply();
                                    prefs.edit().putString("lastName",lastName).apply();
                                    prefs.edit().putString("email",email).apply();
                                    prefs.edit().putStringSet("tournaments",new HashSet<String>()).apply();
                                    prefs.edit().commit();

                                    Log.d("Sign up", "Sign up success!");
                                    startActivity(new Intent(getBaseContext(), Main_akt.class));

                                }

                                @Override
                                public void onAuthenticationError(FirebaseError error) {
                                    Log.d("Sign up", "Sign up failed:" + error.getMessage());
                                    System.out.println("Sign up failed:" + error.getMessage());
                                }
                            });
                }

                @Override
                public void onError(FirebaseError firebaseError) {
                    // there was an error
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

    private boolean isPasswordsSame(String password, String passwordRep) {
        //TODO: Replace this with your own logic
        return password.equals(passwordRep);
    }

    private void emailExists(final String email){

        MyApplication.emailExits = false;
        Firebase usersRef = ref.child(MyApplication.usersString);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String u_email = (String) child.child("email").getValue();
                    if (u_email.equals(email)) {
                        MyApplication.emailExits = true;
                        Log.d("email", "Email check found existing email");
                    }
                }
                Log.d("Email", "Email check done");
                doneEmailCheck = true;
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }

        });
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

            mSignFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            signIn.setVisibility(show ? View.GONE : View.VISIBLE);
            mSignFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSignFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                    signIn.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mSignFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            signIn.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    public final Runnable resumeWhenReady = new Runnable() {

        @Override
        public void run() {
            if (doneEmailCheck) {
                Log.d("email", "start attemptLogin");
                attemptLogin();
            }
            else {
                Log.d("email", "delay");
                handler.postDelayed(resumeWhenReady, 500);
            }
        }
    };

    public void createUser(final String firstName,final String lastName,final String email,final String password,final String passwordRep ){

        showProgress(true);
        ref.createUser(email, password, new Firebase.ValueResultHandler<Map<String, Object>>() {
            @Override
            public void onSuccess(Map<String, Object> result) {
                System.out.println("Successfully created user account with uid: " + result.get("uid"));

                ref.authWithPassword(email, password,
                        new Firebase.AuthResultHandler() {

                            @Override
                            public void onAuthenticated(AuthData authData) {
                                // Authentication just completed successfully :)
                                //Map<String, String> map = new HashMap<String, String>();
                                //map.put("provider", authData.getProvider());
                                //map.put("firstName", firstName);
                                //map.put("lastName", lastName);
                                //map.put("email", email);
                                //ref.child("users").child(authData.getUid()).setValue(map);

                                User user = new User();
                                user.setFirstName(firstName);
                                user.setLastName(lastName);
                                user.setEmail(email);
                                user.setU_ID(authData.getUid());
                                user.setProvider(authData.getProvider());
                                //user.setStoredTournamentsID(new HashSet<String>());
                                MyApplication.setUser(user);
                                ref.child("users").child(authData.getUid()).setValue(user);

                                SharedPreferences prefs = getSharedPreferences("com.dtu.tournamate_v1", Context.MODE_PRIVATE);
                                prefs.edit().putString("uID",user.getU_ID()).apply();
                                prefs.edit().putString("firstName", firstName).apply();
                                prefs.edit().putString("lastName",lastName).apply();
                                prefs.edit().putString("email",email).apply();
                                prefs.edit().putStringSet("tournaments",new HashSet<String>()).apply();
                                prefs.edit().commit();

                                Log.d("Sign up", "Sign up success!");
                                startActivity(new Intent(getBaseContext(), Main_akt.class));

                            }

                            @Override
                            public void onAuthenticationError(FirebaseError error) {
                                Log.d("Sign up", "Sign up failed:" + error.getMessage());
                            }
                        });
            }

            @Override
            public void onError(FirebaseError firebaseError) {
                // there was an error
            }
        });
    }
}
