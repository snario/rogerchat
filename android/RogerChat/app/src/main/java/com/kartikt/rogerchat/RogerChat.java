package com.kartikt.rogerchat;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
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

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.pkmmte.view.CircularImageView;


public class RogerChat extends Activity {



    private Button startButton;
    private Button stopButton;
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private Firebase fb;
    private boolean isRecording =  false;

    GridLayout gl;
    FrameLayout[] people;
    int item;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roger_chat);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Firebase.setAndroidContext(this);
        fb = new Firebase("https://rogerchat.firebaseio.com/channel/bm");
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

        Firebase fb_people = new Firebase("https://rogerchat.firebaseio.com/people");

        /**
         * Step ( 2 )
         *
         *      Update the view with these people.
         */
        gl = (GridLayout)findViewById(R.id.people);

        final Resources res = getResources();
        people = new FrameLayout[9];

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        for (int i=0; i < 9; i++) {
            people[i] = (FrameLayout)inflater.inflate(R.layout.people_button, null);
            people[i].setLayoutParams(new LayoutParams
                    (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            people[i].setPadding(35, 30, 0, 30);
            gl.addView(people[i]);
        }

        // iterate over Firebase people
        fb_people.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int i = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (i < 10) {
                        // give it the proper img src
                        final CircularImageView img = (CircularImageView) people[i++].findViewWithTag("button");
                        Drawable new_bg;
                        try {
                            new_bg = drawableFromUrl(child.child("picture_url").getValue().toString());
                        } catch (IOException e) {
                            new_bg = res.getDrawable(R.drawable.ic_launcher);
                        }
                        img.setImageDrawable(new_bg);


                        Log.d("d", child.child("picture_url").getValue().toString());
                    }
                }
            }

            @Override public void onCancelled(FirebaseError firebaseError) {}
        });

        // assign the listener for touches
//        img.setOnTouchListener(new View.OnTouchListener() {
//            int pos = item;
//
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                if (img.isSelected()) {
//                    img.setSelectorStrokeColor(Color.GREEN);
//
//                    Log.d("d", "red");
//                    // TODO Kartik use the ID of the person.
//                    // index is pos
//                    // array is fb_people (FireBase)
//                    // e.g., fb_people[x].name or wtv
//                } else {
//                    img.setSelectorStrokeColor(Color.RED);
//
//                    Log.d("d", "green");
//                    // TODO Kartik use the ID of the person.
//                    // index is pos
//                    // array is fb_people (FireBase)
//                    // e.g., fb_people[x].name or wtv
//                }
//                return false;
//            }
//        });

        /**
         * Step ( 4 )
         *
         *     Make the record button fucking amazing (aka copy Google)
         *
         */
        final ImageButton mic = (ImageButton) this.findViewById(R.id.mic);
        mic.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                    if(isRecording == true) {
                        mediaRecorder.stop();
                        mediaRecorder.release();
                        mediaRecorder = null;
                        isRecording = false;
                        Log.i("touch", "stopping and saving");
                    }

                    mic.setImageDrawable(res.getDrawable(R.drawable.ic_mic_grey));
                    mic.setBackground(res.getDrawable(R.drawable.round_button));
                    fb.setValue("its stopping!");
                    Log.i("touch", "stopping");
                } else {

                    if(isRecording == false) {
                        if(mediaRecorder == null) {
                            mediaRecorder = new MediaRecorder();
                        }

                        Log.i("touch", "starting to record");
                        resetRecorder();
                        mediaRecorder.start();
                        isRecording = true;
                    }

                    mic.setImageDrawable(res.getDrawable(R.drawable.ic_mic_white));
                    mic.setBackground(res.getDrawable(R.drawable.round_button_red));

                }
                return false;
            }
        });

    }

    public static Drawable drawableFromUrl(String url) throws IOException {
        Bitmap x;

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        InputStream input = connection.getInputStream();

        x = BitmapFactory.decodeStream(input);
        return new BitmapDrawable(x);
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
    protected void onPause() {
        super.onPause();

        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

}
