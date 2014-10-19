package com.kartikt.rogerchat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.EditText;
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
import java.util.ArrayList;

import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.pkmmte.view.CircularImageView;


public class RogerChat extends Activity {

    public static final String PREFS_KEY = "prefs";
    public static final String USER_KEY = "username";

    private Button startButton;
    private Button stopButton;
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private Firebase fb;
    private boolean isRecording =  false;

    private ArrayList<CircularImageView> selectedPeople = new ArrayList<CircularImageView>();
    private GridLayout gl;
    private FrameLayout[] people;
    private String[] names;

    // list that contains whether or not a person is selected right now.
    private Boolean[] hack_people;

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

        final Firebase fb_people = new Firebase("https://rogerchat.firebaseio.com/people");

        Context context = RogerChat.this;
        final SharedPreferences sharedPref = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        String account = sharedPref.getString(USER_KEY, null);
        if (account == null) {

            AlertDialog.Builder builder= new AlertDialog.Builder(this);
            LayoutInflater inflater= getLayoutInflater();
            final View myView= inflater.inflate(R.layout.get_name, null);
            builder.setTitle("About");
            builder.setMessage("Enter account name:");
            builder.setView(myView);
            final EditText input = (EditText) myView.findViewById(R.id.get_name_edit);
            builder.setCancelable(false)
                   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialogInterface, int i) {
                           SharedPreferences.Editor edit = sharedPref.edit();
                           edit.putString(USER_KEY, input.getText().toString());
                           edit.commit();
                       }
                   });
            AlertDialog alert = builder.create();
            alert.show();
        } else {
            Log.d("username", account);
        }



        /**
         * Step ( 2 )
         *
         *      Update the view with these people.
         */
        gl = (GridLayout)findViewById(R.id.people);

        final Resources res = getResources();
        people = new FrameLayout[9];
        hack_people = new Boolean[9];
        names = new String[9];

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        for (int i=0; i < 9; i++) {
            names[i] = null;
            hack_people[i] = false;
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
                        names[i - 1] = child.child("name").getValue().toString();
                        try {
                            img.setBorderColor(Color.GRAY);
                            new_bg = drawableFromUrl(child.child("picture_url").getValue().toString());
                        } catch (IOException e) {
                            new_bg = res.getDrawable(R.drawable.ic_launcher);
                        }
                        img.setImageDrawable(new_bg);
                        final int j = i - 1;
                        img.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                hack_people[j] = !hack_people[j];
                                if (hack_people[j]) {
                                    img.setBorderColor(Color.rgb(233, 30, 99));//#e91e63
                                } else {
                                    img.setBorderColor(Color.GRAY);
                                }
                            }
                        });


                        Log.d("d", child.child("picture_url").getValue().toString());
                    }
                }
            }

            @Override public void onCancelled(FirebaseError firebaseError) {}
        });


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

    /**
     * Gets the names of those selected.
     */
    public ArrayList<String> getSelectedPeople () {
        ArrayList<String> ret = new ArrayList<String>();
        int i = 0;
        for (Boolean hack : hack_people) {
            if (hack) {
                ret.add(names[i]);
            }
            i++;
        }
        return ret;
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
            Intent i = new Intent(getApplicationContext(), Settings.class);
            startActivityForResult(i, 1);
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
