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
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Base64;
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

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.pkmmte.view.CircularImageView;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class RogerChat extends Activity {

    public static final String PREFS_KEY = "prefs";
    public static final String USER_KEY = "username";

    private Button startButton;
    private Button stopButton;
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private Firebase fb;

    private ArrayList<CircularImageView> selectedPeople = new ArrayList<CircularImageView>();
    private GridLayout gl;
    private FrameLayout[] people;
    private String[] names;
    private Boolean[] online_people;
    private Context c;

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format


    // list that contains whether or not a person is selected right now.
    private Boolean[] hack_people;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_roger_chat);

        c = this;

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        Firebase.setAndroidContext(this);
        fb = new Firebase("https://rogerchat.firebaseio.com/channel/bm");
        audioFile = new File(Environment.getExternalStorageDirectory(), "ground_control.mp4");
        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

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
            Firebase me = new Firebase("https://rogerchat.firebaseio.com/people/" + account + "/online");
            me.setValue(true);
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
        online_people = new Boolean[9];

        final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        for (int i=0; i < 9; i++) {
            names[i] = null;
            hack_people[i] = false;
            online_people[i] = false;
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
                    names[i] = child.getName();
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
                        final int j = i - 1;
                        final Boolean online = (Boolean) child.child("online").getValue();
                        img.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                hack_people[j] = !hack_people[j];
                                if (hack_people[j]) {
                                    img.setBorderColor(Color.rgb(233, 30, 99));//#e91e63
                                } else {
                                    if (!online) {
                                        img.setBorderColor(Color.GRAY);
                                    } else {
                                        img.setBorderColor(Color.GREEN);
                                    }
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


                        try {
                            InputStream inputStream = new FileInputStream(audioFile.getAbsolutePath());//You can get an inputStream using any IO API
                            byte[] bytes;
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            try {
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    output.write(buffer, 0, bytesRead);
//                                    Log.i("writer", buffer.toString());
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            bytes = output.toByteArray();

//                            fb.child("encoded").setValue(Base64.encodeToString(bytes, Base64.NO_WRAP));

                            doFileUpload();
                            sendSoundBroadcast();

                            Log.i("encoded shit", Base64.encodeToString(bytes, Base64.NO_WRAP));
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("exception", e.toString());
                        }

                    }

                    mic.setImageDrawable(res.getDrawable(R.drawable.ic_mic_grey));
                    mic.setBackground(res.getDrawable(R.drawable.round_button));
                    fb.child("status").setValue("its stopping!");
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

                        fb.child("status").setValue("its starting!");
                    }

                    mic.setImageDrawable(res.getDrawable(R.drawable.ic_mic_white));
                    mic.setBackground(res.getDrawable(R.drawable.round_button_red));

                }
                return false;
            }
        });

        /**
         * Step ( 6 )
         *
         *     Have online offline checks.
         */
        fb_people.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String online = dataSnapshot.child("online").getValue().toString();
                int i = Integer.parseInt(dataSnapshot.child("idx").getValue().toString());
                Boolean is_online = Boolean.parseBoolean(online);
                online_people[i] = is_online;
                Log.d("index", dataSnapshot.child("idx").getValue().toString()  );
                String has_msg = dataSnapshot.child("has_message").getValue().toString().toLowerCase();
                if (has_msg == "true") {
                    AudioManager mgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    mgr.setStreamVolume(AudioManager.STREAM_MUSIC, 100, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

                    MediaPlayer player =  MediaPlayer.create(c, Uri.parse("http://50.112.162.251/uploads/audio.mp3"));
                    player.setLooping(false);
                    player.setVolume(100,100);
                    player.start();

                    fb_people.child(names[i]).child("has_message").setValue(false);
                }

                if (!hack_people[i]) {
                    CircularImageView img = (CircularImageView) people[i].findViewWithTag("button");
                    if (is_online) {
                        img.setBorderColor(Color.GREEN);
                    } else {
                        img.setBorderColor(Color.GRAY);
                    }
                }
            }

            @Override public void onChildAdded(DataSnapshot dataSnapshot, String s) {}
            @Override public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override public void onCancelled(FirebaseError firebaseError) {}
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
        Log.d("test", ret.toString());
        return ret;
    }

    /**
     * Sets `people.<name>.has_message` to be true for all people getting a message.
     */
    public void sendSoundBroadcast() {
        ArrayList<String> my_people = getSelectedPeople();
        for (String receiver : my_people) {
            Firebase me = new Firebase("https://rogerchat.firebaseio.com/people/" + receiver + "/has_message");
            me.setValue(true);
        }
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
    protected void onResume () {
        super.onResume();
        final SharedPreferences sharedPref = RogerChat.this.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        String account = sharedPref.getString(USER_KEY, null);
        Firebase me = new Firebase("https://rogerchat.firebaseio.com/people/" +account+"/online");
        me.setValue(true);

    }

    @Override
    protected void onPause() {
        super.onPause();
        final SharedPreferences sharedPref = RogerChat.this.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        String account = sharedPref.getString(USER_KEY, null);
        Firebase me = new Firebase("https://rogerchat.firebaseio.com/people/" +account+"/online");
        me.setValue(false);

        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder.stop();
            mediaRecorder = null;
        }
    }



    private void doFileUpload() {

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        DataInputStream inStream = null;
        String existingFileName = audioFile.getAbsolutePath();
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        String responseFromServer = "";
        String urlString = "http://50.112.162.251/";

        try {

            //------------------ CLIENT REQUEST
            FileInputStream fileInputStream = new FileInputStream(new File(existingFileName));
            // open a URL connection to the Servlet
            URL url = new URL(urlString);
            // Open a HTTP connection to the URL
            conn = (HttpURLConnection) url.openConnection();
            // Allow Inputs
            conn.setDoInput(true);
            // Allow Outputs
            conn.setDoOutput(true);
            // Don't use a cached copy.
            conn.setUseCaches(false);
            // Use a post method.
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + existingFileName + "\"" + lineEnd);
            dos.writeBytes(lineEnd);
            // create a buffer of maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            while (bytesRead > 0) {

                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

            }

            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            // close streams
            Log.e("Debug", "File is written");
            fileInputStream.close();
            dos.flush();
            dos.close();

        } catch (MalformedURLException ex) {
            Log.e("Debug", "error: " + ex.getMessage(), ex);
        } catch (IOException ioe) {
            Log.e("Debug", "error: " + ioe.getMessage(), ioe);
        }

        //------------------ read the SERVER RESPONSE
        try {

            inStream = new DataInputStream(conn.getInputStream());
            String str;

            while ((str = inStream.readLine()) != null) {

                Log.e("Debug", "Server Response " + str);

            }

            inStream.close();

        } catch (IOException ioex) {
            Log.e("Debug", "error: " + ioex.getMessage(), ioex);
        }
    }


    public byte[] readByte(File file) throws IOException{

        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        try {
            byte[] buffer = new byte[4096];
            ous = new ByteArrayOutputStream();
            ios = new FileInputStream(file);
            int read = 0;
            while ( (read = ios.read(buffer)) != -1 ) {
                ous.write(buffer, 0, read);
            }
        } finally {
            try {
                if ( ous != null )
                    ous.close();
            } catch ( IOException e) {
            }

            try {
                if ( ios != null )
                    ios.close();
            } catch ( IOException e) {
            }
        }
        return ous.toByteArray();
    }



//    public void PCMtoFile() throws IOException {
//        byte[] header = new byte[44];
//        byte[] data = readByte(audioFile);
//
//        OutputStream os = new FileOutputStream("/sdcard/wav.wav");
//
//        byte[] byteData = null;
//        long totalDataLen = data.length + 36;
//        int srate = 8000;
//        int channel = AudioFormat.CHANNEL_IN_STEREO;
//        int format = AudioFormat.ENCODING_PCM_16BIT;
//        long bitrate = 8000 * AudioFormat.CHANNEL_IN_STEREO * AudioFormat.ENCODING_PCM_16BIT;
////        long bitrate = srate * channel * format;
//
//        header[0] = 'R';
//        header[1] = 'I';
//        header[2] = 'F';
//        header[3] = 'F';
//        header[4] = (byte) (totalDataLen & 0xff);
//        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
//        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
//        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
//        header[8] = 'W';
//        header[9] = 'A';
//        header[10] = 'V';
//        header[11] = 'E';
//        header[12] = 'f';
//        header[13] = 'm';
//        header[14] = 't';
//        header[15] = ' ';
//        header[16] = (byte) format;
//        header[17] = 0;
//        header[18] = 0;
//        header[19] = 0;
//        header[20] = 1;
//        header[21] = 0;
//        header[22] = (byte) channel;
//        header[23] = 0;
//        header[24] = (byte) (srate & 0xff);
//        header[25] = (byte) ((srate >> 8) & 0xff);
//        header[26] = (byte) ((srate >> 16) & 0xff);
//        header[27] = (byte) ((srate >> 24) & 0xff);
//        header[28] = (byte) ((bitrate / 8) & 0xff);
//        header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
//        header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
//        header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
//        header[32] = (byte) ((channel * format) / 8);
//        header[33] = 0;
//        header[34] = 16;
//        header[35] = 0;
//        header[36] = 'd';
//        header[37] = 'a';
//        header[38] = 't';
//        header[39] = 'a';
//        header[40] = (byte) (data.length  & 0xff);
//        header[41] = (byte) ((data.length >> 8) & 0xff);
//        header[42] = (byte) ((data.length >> 16) & 0xff);
//        header[43] = (byte) ((data.length >> 24) & 0xff);
//
//        Log.i("Work", "PLZ PLZP PLZ");
//        os.write(header, 0, 44);
//        os.write(data);
//        os.close();
//    }



    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte

        String filePath = "/sdcard/ground_control.mp4";
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            System.out.println("Short wirting to file" + sData.toString());
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
