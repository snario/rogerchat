package com.kartikt.rogerchat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.pkmmte.view.CircularImageView;

import java.io.IOException;

/**
 * Created by lihorne on 2014-10-19.
 */
public class Settings extends Activity
{

    public static final String PREFS_KEY = "prefs";
    public static final String USER_KEY = "username";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        final SharedPreferences sharedPref = this.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        String account = sharedPref.getString(USER_KEY, null);
        Firebase me = new Firebase("https://rogerchat.firebaseio.com/people/" +account+"/online");



    }

}