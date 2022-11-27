package es.unex.asee.frojomar.asee_ses.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import es.unex.asee.frojomar.asee_ses.activities.appointments.AppointmentsActivity;
import es.unex.asee.frojomar.asee_ses.activities.centers.CentersActivity;
import es.unex.asee.frojomar.asee_ses.R;
import es.unex.asee.frojomar.asee_ses.activities.navigationdrawer.NavDrawerActivity;
import es.unex.asee.frojomar.asee_ses.model.Person;
import es.unex.asee.frojomar.asee_ses.repository.networking.NetworkUtils;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends NavDrawerActivity {

    /**
     * Regex to validate a NIF and NIE.
     */
    private static final String nifRegex = "[0-9]{8}[TRWAGMYFPDXBNJZSQVHLCKE]$";
    private static final String nieRegex = "[XYZ][0-9]{7}[TRWAGMYFPDXBNJZSQVHLCKE]$";

    public static final String MyPREFERENCES = "MyPrefs" ;
    private static final String TAG = "LoginActivity";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mDniView;
    private View mProgressView;
    private View mLoginFormView;

    private static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //It's not necessary, because we have linked a layout to the activity in the NavDrawerActivity.class
        //setContentView(R.layout.activity_login);

        LoginActivity.mContext= getApplicationContext();

        SharedPreferences preferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        if(preferences.getString("login", "no").equals("yes")){
            goToAppointmentsActivity();
        }
        else {
            // Set up the login form.
            mDniView = (EditText) findViewById(R.id.dni);
            mDniView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                        attemptLogin();
                        return true;
                    }
                    return false;
                }
            });

            Button mDniSignInButton = (Button) findViewById(R.id.dni_sign_in_button);
            mDniSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLogin();
                }
            });

            Button mOmitSigninButton = (Button) findViewById(R.id.omit_sign_in);
            mOmitSigninButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    goToCentersActivity();
                }
            });

            mLoginFormView = findViewById(R.id.login_form);
            mProgressView = findViewById(R.id.login_progress);

        }

    }

    /**
     * This method, it's necessary in all the Activities that extends NavDrawerActivity, to link the
     * layout for this activity with the layouts for NavDrawerActivity class.
     */
    @Override
    protected void linkSpecificLayoutforActivity(){

        // We link the layout of NavDrawer activities with the layout of this specific activity
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.content_activities);
        LinearLayout childLayout = (LinearLayout) View.inflate(this, R.layout.activity_login, null);
        mainLayout.addView(childLayout);
    }



    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mDniView.setError(null);

        // Store values at the time of the login attempt.
        String dni = mDniView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid dni.
        if (TextUtils.isEmpty(dni)) {
            mDniView.setError(getString(R.string.error_field_required));
            focusView = mDniView;
            cancel = true;
        } else if (!isDniValid(dni)) {
            mDniView.setError(getString(R.string.error_invalid_dni));
            focusView = mDniView;
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
            mAuthTask = new UserLoginTask(dni);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isDniValid(String dni) {
        return dni.matches(nifRegex) || dni.matches(nieRegex);
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


    public void goToAppointmentsActivity(){
        Intent intent = new Intent(this, AppointmentsActivity.class);
        startActivity(intent);
    }

    public void goToCentersActivity(){
        Intent intent = new Intent(this, CentersActivity.class);
        startActivity(intent);
    }

    public void closeSession(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(preferences.getString("login", "no").equals("yes")){
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
            editor.remove("login");
            editor.remove("dni");
            editor.remove("name");
            Log.i(LoginActivity.TAG, "Closing session");
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private static final String BASE_URL=NetworkUtils.BASE_URL;
        private static final String RESOURCE_PATH_PERSONS="persons";

        private final String mDni;
        private String personName;

        UserLoginTask(String dni) {
            mDni = dni;
            personName="";
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            URL queryURL;
            JSONObject result;

            //Load upcoming movies
            queryURL = NetworkUtils.buildURL(BASE_URL,
                    new String[]{RESOURCE_PATH_PERSONS, mDni});
            result = NetworkUtils.getJSONResponse(queryURL);

            Log.i(LoginActivity.TAG, "Cargando información de la persona de " + queryURL.toString());

            if (result == null){
                Log.e(LoginActivity.TAG, "Resultado obtenido. Ninguna persona devuelta");
                return false;
            }
            else {
                Log.i(LoginActivity.TAG, "Resultado obtenido de "+queryURL.toString());
                Person p= jsonToObject(result);
                if(p.getDni().equals(mDni)){
                    return true;
                }
                else{
                    Log.e(LoginActivity.TAG, "El DNI de la persona devuelta no coincide con el buscado");
                    return false;
                }
            }
        }

        private Person jsonToObject(JSONObject responseObject) {
            Log.i(LoginActivity.TAG, "Convirtiendo el resultado a objeto");
            Person person = new Person();
            try {
                // Extract value of "" key -- a List
                Log.i(LoginActivity.TAG,responseObject.toString());
                ObjectMapper mapper = new ObjectMapper();
                person = mapper.readValue(responseObject.toString(), Person.class);
                personName=person.getName();
                Log.i(LoginActivity.TAG, "Mapeada la persona "+personName);

            } catch (JsonParseException e) {
                Log.e(LoginActivity.TAG, "Error por parseando el JSON");
                e.printStackTrace();
            } catch (JsonMappingException e) {
                Log.e(LoginActivity.TAG, "Error por Mapeando el JSON");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(LoginActivity.TAG, "Error por IOException");
                e.printStackTrace();
            }
            return person;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                SharedPreferences preferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("login", "yes");
                editor.putString("dni",mDni);
                editor.putString("name",personName);
                editor.commit();

                goToAppointmentsActivity();
            } else {
                mDniView.setError(getString(R.string.error_incorrect_dni));
                mDniView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

    }

}

