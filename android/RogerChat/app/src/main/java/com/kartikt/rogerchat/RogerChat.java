package com.kartikt.rogerchat;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridLayout;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import at.markushi.ui.CircleButton;

import java.security.MessageDigest;
import java.util.ArrayList;

public class RogerChat extends Activity {

    GridLayout gl;
    FrameLayout[] people;
    int item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roger_chat);

        /**
         * Step ( 1 )
         *
         *      Get all the data from Firebase on people.
         *      Each person is {
         *          name: '<string>',
         *          url:  '<something>.png'
         *      }
         */
        Firebase.setAndroidContext(this);
        Firebase rootRef = new Firebase("https://rogerchat.firebaseio.com/people");
        // Get a reference to our people


        /**
         * Step ( 2 )
         *
         *      Update the view with these people.
         */
        gl = (GridLayout)findViewById(R.id.people);

        people = new FrameLayout[9];

        LayoutInflater inflater = (LayoutInflater)getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);

        // iterate over Firebase people
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.i("work", "work");
                for (DataSnapshot child : snapshot.getChildren()) {
                    Toast.makeText(RogerChat.this, child.toString(), Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
            }
        });

        for (int i=0; i < 9; i++) {
            people[i] = (FrameLayout)inflater.inflate(R.layout.people_button, null);
            people[i].setLayoutParams(new LayoutParams
                    (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            people[i].setPadding(35, 30, 0, 30);
            gl.addView(people[i]);
        }


        /**
         * Step ( 3 )
         *
         *     Set up event handlers when they click on one of these people.
         *
         */
        for (item = 0; item < 9; item++) {
            CircleButton b = (CircleButton) people[item].findViewWithTag("button");
            b.setOnClickListener(new View.OnClickListener() {
                int pos = item;
                public void onClick(View v) {
                    // TODO Kartik use the ID of the person.
                    // index is pos
                    // array is fb_people (FireBase)
                    // e.g., fb_people[x].name or wtv
                }
            });
        }

        /**
         * Step ( 4 )
         *
         *     Make the record button fucking amazing (aka copy Google)
         *
         */
        final Resources res = getResources();
        final ImageButton mic = (ImageButton) this.findViewById(R.id.mic);
        mic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mic.setImageDrawable(res.getDrawable(R.drawable.ic_mic_grey));
                    mic.setBackground(res.getDrawable(R.drawable.round_button));
                } else {
                    mic.setImageDrawable(res.getDrawable(R.drawable.ic_mic_white));
                    mic.setBackground(res.getDrawable(R.drawable.round_button_red));
                }
                return false;
            }
        });

    }

    public static ArrayList<String> hardCodedData () {
        return new ArrayList<String>();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.roger_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
