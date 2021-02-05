package master.testgraphview;

/**
 * Created by martinguul on 10/12/15.
 * Last Edited by Mathias Pinto Bonnesen, 15/10/16
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends Activity implements SensorEventListener  {

    private LineGraphSeries<DataPoint> series_x;
    private LineGraphSeries<DataPoint> series_y;
    private LineGraphSeries<DataPoint> series_z;
    private TextView tvSampleNr;
    private Viewport viewport;
    private int lastX = 0;
    private int lastY = 0;
    private int lastZ = 0;


    private boolean sample = false;
    private boolean sampler = true;
    private boolean audioSampler = true;
    private int sample_number = 1;
    // Accelerometer
    private SensorManager senSensorManager;
    private Sensor senAccelerometer, senOrientation;
    private Button bNewSample, bOpenService, bAudio, bAccAudio;
    private TextView tvOrientation;
    private GraphView graph;
    private DataPoint[] data_x, data_y, data_z;

    private Intent intentAcc, intentAudio, intentAudioStatus, intentAccTimer;

    private int measure  = 100;
    private int counter = 0;
    private long lastUpdate = 0;
    private float last_x, last_y, last_z;
    private float x = 0;
    private float y = 0;
    private float z = 0;

    private int max_val = 10;
    private int min_val = -10;

    // Sampling
    private FileWriter writer;
    File direct;
    private float sampleTimex, sampleTimey, sampleTimez;
    private boolean dialogStatus = true;

    private ImageView imgView;

    // Position
    private int broadcastCounter = 0;
    private boolean init = false; // boolean to indicate if one winow is sampled
    int statusPosition  = 9000;
    private int w = 5; // window length
    private float[][] acc_data = new float[w][3]; // init data array
    private float[] tmp_array = new float[3]; // tmp data array for received acc data

    // Audio
    private AudioManager audioManager;

    // notification
    NotificationManager nmSampling;
    Notification notifSampling;

    // Preferences
    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    //Sample rerun due to error check
    private int sampleCounter = 1;

    //mic receiver listener
    private micIntentReceiver micReceiver;
    private IntentFilter micFilter;

    //bat receiver listener
    private battIntentReceiver batReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bNewSample = (Button) findViewById(R.id.bNewSample);
        bOpenService = (Button) findViewById(R.id.bService);
        bAudio = (Button) findViewById(R.id.bAudio);
        bAccAudio = (Button)findViewById(R.id.bAccAudio);
        tvOrientation = (TextView)findViewById(R.id.tvOrientation);


        tvSampleNr = (TextView) findViewById(R.id.tvSampleNr);
        tvSampleNr.setText("Sample Nr.: " + sample_number);
        // we get graph view instance
        GraphView graph = (GraphView) findViewById(R.id.graph1);
        // data
        series_x = new LineGraphSeries<DataPoint>();
        series_y = new LineGraphSeries<DataPoint>();
        series_z = new LineGraphSeries<DataPoint>();

        graph.addSeries(series_x);
        graph.addSeries(series_y);
        graph.addSeries(series_z);

        // customize a little bit viewport
        viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(-10);
        viewport.setMaxY(10);
        viewport.setScrollable(true);
        viewport.setScalable(true);

        // Accelerometer
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);

        intentAcc = new Intent(this, AccServiceThread.class);
        // TODO: 08/12/15 intent changed to test for audio class update  
        intentAudio = new Intent(this, AudioRecorderService.class);
       // intentAudioStatus = new Intent(this,WiredInReceiver.class);
        intentAccTimer = new Intent(this, AccServiceTimer.class);


        Log.i("fs", senAccelerometer.getMinDelay() + ""); // output the min break between samples in micro seconds.

        // LocalBroadcast for position
        LocalBroadcastManager.getInstance(this).registerReceiver(accReceiver, new IntentFilter("Acc Sample"));

        // Audio
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

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

        // Memory information
        Log.i("Memory",  ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() + "");

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
    }

    private class micIntentReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("state", -2);
            int micState = intent.getIntExtra("microphone",-2);
            switch (state){
                case 0:
                    Toast.makeText(context, "No headset is mounted", Toast.LENGTH_SHORT).show();
                    bAudio.setBackground(context.getResources().getDrawable(R.drawable.button_red));
                    bAccAudio.setBackground(context.getResources().getDrawable(R.drawable.button_red));
                    break;
                case 1:
                    if(micState == 0){
                        Toast.makeText(context, "Headset is mounted but no MIC", Toast.LENGTH_SHORT).show();
                        bAudio.setBackground(context.getResources().getDrawable(R.drawable.button_red));
                        bAccAudio.setBackground(context.getResources().getDrawable(R.drawable.button_red));
                    }else if(micState == 1){
                    Toast.makeText(context, "MIC is mounted", Toast.LENGTH_SHORT).show();
                        bAudio.setBackground(context.getResources().getDrawable(R.drawable.button_green));
                        bAccAudio.setBackground(context.getResources().getDrawable(R.drawable.button_green));}
                    break;
                case -2:
                    Toast.makeText(context, "Wrong", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

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
        stopService(intentAcc); // stop sampling with accelerometer
        stopService(intentAccTimer);
    }

    public void sampling(View view){
        if (!sample){
            sample = true;
            sampleAcc();

            // defining directory for samples
            direct = new File(Environment.getExternalStoragePublicDirectory
                    (Environment.DIRECTORY_DOWNLOADS), "Data_" + sample_number);
            // set text on btn
            bNewSample.setText("Stop Recording");
            tvSampleNr.setText("Sample Nr.: " + sample_number);

            // inactivate the other buttons
            activateButtons(1);

            SamplingFlashLight();
        }
        else{
            sample = false;
            sampleAcc();

            // increase counter for number of sample epochs
            sample_number++;

            // set text on btn
            bNewSample.setText("UI Accelerometer");
            // cancel LED notification
            nmSampling.cancel(16);

            // activate buttons
            activateButtons(5);
        }
    }

    // notify that the device samples using LED.
    private void SamplingFlashLight() {
        notifSampling.ledARGB = 0xFFff0000; // choose color for LED
        notifSampling.flags = Notification.FLAG_SHOW_LIGHTS;
        notifSampling.ledOnMS = 100;
        notifSampling.ledOffMS = 100;
        nmSampling.notify(16, notifSampling);
    }

    protected void sampleAcc(){
        new Thread(new Runnable() {

            @Override
            public void run() {
                // we add 100 new entries
                while (sample) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            addEntry();

                        }
                    });
                    // sleep to slow down the add of entries
                    try {
                        Thread.sleep(100); // the delay time in milliseconds
                    } catch (InterruptedException e) {
                        // manage error
                    }
                }
            }
        }).start();
    }

    // add random data to graph
    private void addEntry() {
        // here, we choose to display max 10 points on the viewport and we scroll to end
        series_x.appendData(new DataPoint(lastX++, x), true, 100);
        series_x.setColor(Color.BLUE);
        series_y.appendData(new DataPoint(lastY++, y), true, 100);
        series_y.setColor(Color.RED);
        series_z.appendData(new DataPoint(lastZ++, z), true, 100);
        series_z.setColor(Color.GREEN);

        csv_generator("x_coor.txt", x + "");
        csv_generator("timex.txt", sampleTimex + "");
        csv_generator("y_coor.txt", y + "");
        csv_generator("timey.txt", sampleTimey + "");
        csv_generator("z_coor.txt", z + "");
        csv_generator("timez.txt", sampleTimez + "");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            x = sensorEvent.values[0];
            sampleTimex = System.nanoTime();
            y = sensorEvent.values[1];
            sampleTimey = System.nanoTime();
            z = sensorEvent.values[2];
            sampleTimez = System.nanoTime(); // get time stamp

            // set the max and min value at view port
            if(max_val < x | max_val < y | max_val < z){
                max_val = (int) Math.max(x,(int) Math.max(y,z));
                viewport.setMaxY(max_val*1.3);
            }

            if(min_val < Math.abs(x) | min_val < Math.abs(y) | min_val < Math.abs(z)){
                min_val = (int) Math.min(x,(int) Math.min(y,z));
                viewport.setMinY(-10);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void csv_generator(String sFileName, String input){
        try
        {
            if (!direct.exists()) {
                direct.mkdirs();
            }
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

    //------------------------------------------------------------------
    //                  Acc data sampling using service
    //------------------------------------------------------------------
    public void openService(View v) {
        if (sampler) {
            intentAcc.putExtra("samplingStatus",1); // number of modalities
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

        if (sampler && dialogStatus) {
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
                        dialogStatus = true;
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
            intentAudio.putExtra("samplingStatus", 2); // number of modalities, 2 means no notification, is controlled by acc
            startService(intentAudio); // start the audio sample service
            intentAccTimer.putExtra("samplingStatus", 2); // number of modalities, 2 => red LEDs
            startService(intentAccTimer); // start the accelerometer sample service
            bAccAudio.setText("Stop Recording"); // change text on btn
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
            bAccAudio.setText("Accelerometer & Audio"); // change text on btn
            sampler = true; // update counter
            // activate all buttons
            activateButtons(5);

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
                bNewSample.setEnabled(false);
                break;
            case 3:
                // inactivate all buttons
                bOpenService.setEnabled(false);
                bAccAudio.setEnabled(false);
                bNewSample.setEnabled(false);
                break;
            case 4:
                // activate all buttons
                bAudio.setEnabled(false);
                bOpenService.setEnabled(false);
                bNewSample.setEnabled(false);
                break;
            case 5:
                // activate all buttons
                bAudio.setEnabled(true);
                bOpenService.setEnabled(true);
                bAccAudio.setEnabled(true);
                bNewSample.setEnabled(true);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


// REMOVE THE MENU ITEMS not Useable by the users
/*
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