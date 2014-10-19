package com.kartikt.rogerchat;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;

import com.firebase.client.Firebase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import at.markushi.ui.CircleButton;

public class RogerChat extends Activity implements View.OnClickListener  {


    private Button startButton;
    private Button stopButton;
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private Firebase fb;

    GridLayout gl;
    FrameLayout[] people;
    int item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roger_chat);

        if(android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Firebase.setAndroidContext(this);
        fb = new Firebase("https://rogerchat.firebaseio.com/channel/bm");

        startButton = (Button) findViewById(R.id.start);
        startButton.setOnClickListener(this);
        startButton.setText("start");

        stopButton = (Button) findViewById(R.id.stop);
        stopButton.setOnClickListener(this);
        stopButton.setEnabled(false);
        stopButton.setText("stop");

        audioFile = new File(Environment.getExternalStorageDirectory(), "ground_control.mp4");


        /**
         * Step ( 1 )
         *
         *      Get all the data from Firebase on people.
         *      Each person is {
         *          name: '<string>',
         *          url:  '<something>.png'
         *      }
         */

        // Get a reference to our people


        /**
         * Step ( 2 )
         *
         *      Update the view with these people.
         */
        gl = (GridLayout)findViewById(R.id.people);

        people = new FrameLayout[9];

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

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



    private void resetRecorder() {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setAudioEncodingBitRate(16);
        mediaRecorder.setAudioSamplingRate(24000);
        mediaRecorder.setOutputFile(audioFile.getAbsolutePath());

        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start:
                mediaRecorder = new MediaRecorder();
                resetRecorder();
                mediaRecorder.start();
                fb.setValue("its starting!");

                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                break;
            case R.id.stop:
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                fb.setValue("its stopping!");

                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                break;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

}
