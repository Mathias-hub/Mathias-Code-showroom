package master.testgraphview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by martinguul on 10/12/15.
 * Last Edited by Mathias Pinto Bonnesen, 15/10/16
 */
public class SampleData extends Activity {

    private boolean sampler = true;

    private Button bNewSample, bOpenService, bAudio, bAccAudio;
    private TextView tvOrientation;

    private Intent intentAudio, intentAccTimer;

    // TEST SEGMENT
    private static EditText patientName;
    // TEST


    // notification
    NotificationManager nmSampling;
    Notification notifSampling;

    // Preferences
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;
    File direct;

    //mic receiver listener
    private micIntentReceiver micReceiver;
    private IntentFilter micFilter;

    //bat receiver listener
    private battIntentReceiver batReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sampledata);

        bNewSample = (Button) findViewById(R.id.bNewSample);
        bOpenService = (Button) findViewById(R.id.bService);
        bAudio = (Button) findViewById(R.id.bAudio);
        bAccAudio = (Button)findViewById(R.id.bAccAudio);
        tvOrientation = (TextView)findViewById(R.id.tvOrientation);

        intentAudio = new Intent(this, AudioRecorderService.class);
        intentAccTimer = new Intent(this, AccServiceTimer.class);

        // Sets the visibility of button 1 and 2 to zero
        bAudio.setVisibility(View.GONE);
        bOpenService.setVisibility(View.GONE);

        // TODO: 10/12/15 removed to save power
        /*
        // LocalBroadcast for position
        LocalBroadcastManager.getInstance(this).registerReceiver(accReceiver, new IntentFilter("Acc Sample"));
*/

        // LED notification
        nmSampling = ( NotificationManager ) getSystemService( NOTIFICATION_SERVICE );
        notifSampling = new Notification();

        // Preferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false); // initiate default values

        // check for directory and save directory in sharedpreferences
        direct = new File(Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DOWNLOADS), "Data_" + sharedPrefs.getString("PatientName","test"));
        if (!direct.exists()) {
            direct.mkdirs();
        }

        editor = sharedPrefs.edit();
        editor.putString("directory", direct + "");
        editor.apply(); // apply in stead of commit since it runs on an other thread.

        // mic state
        AudioManager au = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        micReceiver = new micIntentReceiver();
        micFilter = new IntentFilter(au.ACTION_HEADSET_PLUG);
        registerReceiver(micReceiver, micFilter);

        // Battery listener
        // TODO: 08/12/15 insert if loop to enable low battery detector
        /*
        batReceiver = new battIntentReceiver();
        IntentFilter battFilter = new IntentFilter(Intent.ACTION_BATTERY_LOW);
        registerReceiver(batReceiver, battFilter);*/

        System.out.println(sharedPrefs.getString("Quest", "NA"));
    }

    private class micIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("state", -2);
            int micState = intent.getIntExtra("microphone",-2);
            switch (state){
                case 0:
                    Toast.makeText(context, "No headset is mounted", Toast.LENGTH_SHORT).show();
                    bAudio.setBackground(getDrawable(R.drawable.button_red));
                    bAccAudio.setBackground(getDrawable(R.drawable.button_red));
                    break;
                case 1:
                    if(micState == 0){
                        Toast.makeText(context, "Headset is mounted but no MIC", Toast.LENGTH_SHORT).show();
                        bAudio.setBackground(getDrawable(R.drawable.button_red));
                        bAccAudio.setBackground(getDrawable(R.drawable.button_red));
                    }else if(micState == 1){
                        Toast.makeText(context, "MIC is mounted", Toast.LENGTH_SHORT).show();
                        bAudio.setBackground(getDrawable(R.drawable.button_green));
                        bAccAudio.setBackground(getDrawable(R.drawable.button_green));}
                    break;
                case -2:
                    Toast.makeText(context, "Wrong", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    // TODO: 10/12/15 not used since saving power
    private class battIntentReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            // Power status
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            float level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            float scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level / scale; // cast
            csv_generator("iFile", "Low Battery: " + batteryPct + " time: " + System.nanoTime());
        }
    }

    // TODO: 10/12/15 not used since saving power
    // Receive broadcast regarding position from AccServiceThread
    private BroadcastReceiver accReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int tmpStatus = intent.getIntExtra("acc_Position", 6);
            if(tmpStatus == 1){
                tvOrientation.setText("Position: Supine");
            } else if(tmpStatus == 2){
                tvOrientation.setText("Position: Prone");
            } else if(tmpStatus == 3){
                tvOrientation.setText("Position: Left Lateral");
            } else if(tmpStatus == 4){
                tvOrientation.setText("Position: Right Lateral");
            } else if(tmpStatus == -2){
                tvOrientation.setText("Position: Upright");
            } else if(tmpStatus == 5){
                tvOrientation.setText("Position: Nothing to Declare");
            } else if(tmpStatus == 6){
                tvOrientation.setText("Position: Can't Find Data");
            }
        }
    };

    public void csv_generator(String sFileName, String input){
        try
        {
            File gpxfile = new File(direct, sFileName);
            FileWriter writer = new FileWriter(gpxfile, true);
            writer.append(input  + ", ");
            writer.flush();
            writer.close();
            updateMediaScanner(gpxfile);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void updateMediaScanner(File file) {
        //Send an Intent to the MediaScanner to scan our file
        //Broadcast the Media Scanner Intent to trigger it
        Uri uri = Uri.fromFile(file);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    }

    public void unregisterReceivers(int state){
        if(state == 1){ // sampling data
            unregisterReceiver(micReceiver);}
        else if(state == 2){ // not sampling data
            registerReceiver(micReceiver, micFilter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(accReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(micReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(batReceiver);
        //stopService(intentAudioStatus); // stop the listener for audio connection
        stopService(intentAudio); // stop sampling audio
        stopService(intentAccTimer);
    }

    // notify that the device samples using LED.
    private void SamplingFlashLight() {
        notifSampling.ledARGB = 0xFFff0000; // choose color for LED
        notifSampling.flags = Notification.FLAG_SHOW_LIGHTS;
        notifSampling.ledOnMS = 100;
        notifSampling.ledOffMS = 100;
        nmSampling.notify(16, notifSampling);
    }

    //------------------------------------------------------------------
    //                  Acc data sampling using service
    //------------------------------------------------------------------
    public void openService(View v) {
        if (sampler) {

            intentAccTimer.putExtra("samplingStatus",1); // number of modalities
            startService(intentAccTimer);
            bOpenService.setText("Stop Recording");
            sampler = false;
            // inactivate the other buttons
            activateButtons(2);

            // unregister non important receivers
            unregisterReceivers(1);

        }
        else{
            // TODO: 10/12/15 removed to save power.
            /*
            // LocalbroadcastManager to stop sampling and unregister listener on acc thread
            Intent intentAccStatus = new Intent("Acc Sampler status");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentAccStatus);
*/
            stopService(intentAccTimer);
            bOpenService.setText("Accelerometer");
            sampler = true;
            // activate all buttons
            activateButtons(5);

            // unregister non important receivers
            unregisterReceivers(2);
        }
    }

    //------------------------------------------------------------------
    //                  Audio sampling
    //------------------------------------------------------------------
    public void audioSample(View v) {
       /* if(sampler && !audioManager.isWiredHeadsetOn()){
            dialogStatus = false;
            while(!dialogStatus){
                DialogInsertHeadset();
             // wait for the response form dialog
            }
        }*/

        if (sampler) {
            intentAudio.putExtra("samplingStatus",1); // number of modalities

            startService(intentAudio); // start the audio sample service
            bAudio.setText("Stop Recording"); // change text on btn
            sampler = false; // update counter

            // inactivate the other buttons
            activateButtons(3);

            // unregister non important receivers
            unregisterReceivers(1);

        } else {
            stopService(intentAudio); // stop the audio sample service
            bAudio.setText("Audio"); // change text on btn
            sampler = true; // update counter
            // activate all buttons
            activateButtons(5);

            // register non important receivers
            unregisterReceivers(2);
        }
    }

    public void DialogInsertHeadset() {
        // create dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.
                Builder(this);
        // set message and title
        alertDialogBuilder.setMessage("Headset is not mounted. This will reduce the quality of the recording");
        alertDialogBuilder.setTitle("Headset Error");

        // Create positive btn
        alertDialogBuilder.setPositiveButton("Continue",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        //dialogStatus = true;
                    }
                });
        // Create negative btn
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        sampler = true; // update counter
                    }
                });
        // build the alertDialog
        alertDialogBuilder.show();
    }


    //------------------------------------------------------------------
    //                  Accelerometer and Audio sampling
    //------------------------------------------------------------------
    public void accAudioSampling(View v) {
        /* if(!audioManager.isWiredHeadsetOn() && sampler) {
            DialogInsertHeadset();
        } else */
        if (sampler) {

            // TEST SEGMENT
            /*
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            String formattedDate = df.format(c.getTime());

            editor.putString("PatientName", formattedDate); // id for patient
            editor.apply();
            !! TEST SEGMENT */

            /*  NOISE SEGMENT, UNCOMMENDED FOR DEVELOPMENT-REASONS
            // SET VOLUME TO MAX and force to use SPEAKERS
            AudioManager am = (AudioManager) getSystemService
                    (Context.AUDIO_SERVICE);
            am.setStreamVolume(AudioManager.STREAM_MUSIC,
                    am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),0);
            am.setSpeakerphoneOn(true);
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);

            // PLAY AUDIO, TO SYNCRONIZE MAS with the PSG
            final MediaPlayer mp = MediaPlayer.create(this, R.raw.test2);
            mp.start();
            */

            intentAudio.putExtra("samplingStatus", 2); // number of modalities, 2 means no notification, is controlled by acc
            startService(intentAudio); // start the audio sample service
            intentAccTimer.putExtra("samplingStatus", 3); // number of modalities, 2 => red LEDs
            startService(intentAccTimer); // start the accelerometer sample service
            bAccAudio.setText("Stop"); // change text on btn
            sampler = false; // update counter
            // inactivate the other buttons
            activateButtons(4);

            // unregister non important receivers
            unregisterReceivers(1);
        } else {
            // LocalbroadcastManager to stop sampling and unregister listener on acc thread
            Intent intentAccStatus = new Intent("Acc Sampler status");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intentAccStatus);

            stopService(intentAudio); // stop the audio sample service
            stopService(intentAccTimer); // stop the accelerometer sample service
            bAccAudio.setText("Start Dataopsamling"); // change text on btn
            sampler = true; // update counter
            // activate all buttons
            activateButtons(5);


            // Go back to previous activity. This was implemented, as a fix
            this.finish();

            // unregister non important receivers
            unregisterReceivers(2);
        }
    }

    public void activateButtons(int input){
        switch(input){
            case 1:
                bAudio.setEnabled(false);
                bOpenService.setEnabled(false);
                bAccAudio.setEnabled(false);
                break;
            case 2:
                // inactivate all buttons
                bAudio.setEnabled(false);
                bAccAudio.setEnabled(false);
                break;
            case 3:
                // inactivate all buttons
                bOpenService.setEnabled(false);
                bAccAudio.setEnabled(false);
                break;
            case 4:
                // activate all buttons
                bAudio.setEnabled(false);
                bOpenService.setEnabled(false);
                break;
            case 5:
                // activate all buttons
                bAudio.setEnabled(true);
                bOpenService.setEnabled(true);
                bAccAudio.setEnabled(true);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_sampledata, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* THIS HAS BEEN REMOVED, FOR USER SIMPLICITY

    @Override

    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;

        switch(item.getItemId()){
            case R.id.action_settings:
                intent = new Intent(this, Settings.class);
                this.startActivity(intent);
                break;
            case R.id.action_add_patient:
                intent = new Intent(this, AddPatient.class);
                this.startActivity(intent);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }
    */


}