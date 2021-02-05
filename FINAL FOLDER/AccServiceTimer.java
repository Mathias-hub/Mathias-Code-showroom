package master.testgraphview;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by martinguul on 08/10/15.
 * Last Edited by Mathias Pinto Bonnesen, 15/10/16
 */
public class AccServiceTimer extends Service {

    private static int acc_T;
    private static int package_size;
    private String xBuf, yBuf, zBuf, tBuf; // string to collect sample time, and buffers for writer
    private boolean sample = true; // boolean to control sampling on/off
    private  boolean sensorActive = false;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private boolean sampleStarted = false;
    int stopsamp = 0;        // To control if the accelerometer measurements gets written into the
                             // text-file.
    private Context context;

    private int counter = 1; // counter to build packets
    private File direct; // defining directory for samples

    // TODO: 08/12/15 changed to be called in the function to avoid memory drift
    //private float[] accData; // array for acc samples

    private Thread t_sample, t_write; // initiate thread for sampling

    private File file, xFile, yFile, zFile, tFile, pFile, iFile, eFile;

    // TODO: 09/12/15 position has been removed to lower power consumption
    /*
    // Position
    private int broadcastCounter = 0;
    private boolean init = false; // boolean to indicate if one window is sampled
    int statusPosition  = 9000;
    private int w = 5; // window length
    private float[][] acc_data = new float[w][3]; // init data array
    */

    // notification
    private NotificationManager nmSampling;
    private Notification notifSampling;
    private int samplingType;
    private boolean activatedLED = false;
    private boolean bfailSampling = true;

    //Preferences
    private SharedPreferences sharedPrefs;

    //batterystatus
    float batteryPct;

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        samplingType = intent.getIntExtra("samplingStatus",1); // passed via intent used to start service
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Preferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        context = this;
        //accData = new float[3]; // array for acc sampling.

        // check and define directory

        direct = new File(Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DOWNLOADS), "Data_"
                + sharedPrefs.getString("PatientName","test"));

        if (direct.exists()) {

        } else {
            direct.mkdirs();

        }

        // LED notification
        nmSampling = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifSampling = new Notification();

        // sampling settings
        acc_T = 1000000000/Integer.valueOf(sharedPrefs.getString("AccFs", "NA")); //[ns]
        package_size = Integer.valueOf(sharedPrefs.getString("AccPackage", "50")); //[samples]

        // File directories/paths
        xFile = new File(direct, "xFile.txt");
        yFile = new File(direct, "yFile.txt");
        zFile = new File(direct, "zFile.txt");
        tFile = new File(direct, "tFile.txt");
        pFile = new File(direct, "pFile.txt");
        iFile = new File(direct, "iFile.txt");
        eFile = new File(direct, "eFile.txt");


        SharedPreferences sp = getSharedPreferences("your_prefs", Activity.MODE_PRIVATE);
        int height = sp.getInt("height", -1);
        int weight = sp.getInt("weight", -1);
        csv_info(iFile,"hight: " + height + " weight: " + weight);


        // This is where the iFile and eFile is created
        csv_info(iFile, "Delta t [ms]: " + acc_T);
        csv_info(iFile, "Start Time [ns]: " + System.nanoTime());
        csv_info(iFile, "Package Size [samples]: " + package_size);
        // csv_info(iFile, "Audio Bit Rate [bits/sample]: " + sharedPrefs.getString("AudioBitRate", "NA"));
        // csv_info(iFile, "Audio fs [Hz]: " + sharedPrefs.getString("AudioFs", "NA"));
        // csv_info(iFile, "Stop Bang Answers: " + sharedPrefs.getString("Quest", "NA"));
        csv_info(iFile, "Stop Bang Answers: " + sharedPrefs.getString("Quest", "NA"));
        csv_info(eFile, "init string, ");

        // Get battery status from start
        batteryStatus(1);

        
        // start thread for sampling
        t_sample=new Thread(new AccSampler(), "Accelerometer Sample Thread"); // sample on a new thread.
        t_sample.start();
    }

    @Override
    public void onDestroy() { // when service is destroyed
        sample = false; // stop sampling

        // cancel LED notification
        nmSampling.cancel(17);

        // Get end battery status
        batteryStatus(2);

        // turn off sample thread
        t_sample.interrupt();
        // update mediascanners
        updateMediaScanner(xFile);
        updateMediaScanner(yFile);
        updateMediaScanner(zFile);
        updateMediaScanner(tFile);
        updateMediaScanner(pFile);
        updateMediaScanner(iFile);
        updateMediaScanner(eFile);
        super.onDestroy();
    } // WORKS!



    public void csv_info(File sFileName, String input) {
        try {
            FileWriter writer = new FileWriter(sFileName, true);
            writer.append(input + ", " + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void batteryStatus(int status){
        // Power status
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        float level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        float scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        batteryPct = level / scale; // cast

        switch (status){
            case 1:
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String formattedDate = df.format(c.getTime());
                csv_info(iFile, "Date and time: " + formattedDate );
                csv_info(iFile, "startBatteryStatus: " + batteryPct);
                stopsamp = 0; // Write down the accelerometer data.
                break;
            case 2:
                Calendar r = Calendar.getInstance();
                SimpleDateFormat dr = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String formattedDateEnd = dr.format(r.getTime());
                csv_info(iFile, "Date and time: " + formattedDateEnd );
                csv_info(iFile, "endBatteryStatus: " + batteryPct);
                stopsamp = 1; // Stop recording the accelerometer data.
                break;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    public void updateMediaScanner(File file) {
        //Send an Intent to the MediaScanner to scan our file
        //Broadcast the Media Scanner Intent to trigger it
        Uri uri = Uri.fromFile(file);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    } // WORKS!

    //---------------------------------------------------------------------------------------------
    // class for acc sampling, runs on a separate thread.
    //---------------------------------------------------------------------------------------------
    public class AccSampler implements SensorEventListener, Runnable {
        long tStamp, tStampUpdate; // currentTime, nextSampleTime
        double timeDif; // max diff time
        boolean firstSample = true;
        boolean sampleType = false; // normal sample ie game delay;

        @Override
        public void run() {
            // settings for accelerometer.
            senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if(sharedPrefs.getBoolean("AccPerformance", false) == false){ // normal performance
                senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_GAME, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
            } else if(sharedPrefs.getBoolean("AccPerformance", false) == true){ // high performance, may reduce power!!
                senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                sampleType = false;
            }

            timeDif = acc_T * 0.5; //50 % of difference in the sample period in [ns] is accepted
            System.out.println(sharedPrefs.getBoolean("AccPerformance", false) + "");
            //-------------------------------------------------------------------------------------
            // Check that sampling will not begin before the sensor is activated to avoid 0 as first
            // measure. Boolean is activated onSensorChange detection.
            //-------------------------------------------------------------------------------------
            while(!sensorActive){
                try {
                    Thread.sleep(1);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // TODO: 10/12/15 removed to save power
            /*
            // LocalBroadcast for position
            LocalBroadcastManager.getInstance(context).registerReceiver(accSample, new IntentFilter("Acc Sampler status"));
            */
            
            //-------------------------------------------------------------------------------------
            // Start sampling until push button again on UI.
            //-------------------------------------------------------------------------------------
            while(sample=true){ // no ideal solution!!
                //
            }
            if(!sample) { // sample updates by localbroadcast receiver.
                senSensorManager.unregisterListener(this); // unregister listener to reduce power consumption
                // cancel LED notification
                nmSampling.cancel(17);

            }
        }

        // TODO: 10/12/15 removed to save power 
        /*
        // Receive broadcast regarding stop sampling from main activity
        private BroadcastReceiver accSample = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sample = false; // stop sampling and unregister acc listener on sampling thread.
            }
        };*/
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(sampleType){ // Sampling method depends on the settings. This is for game delay

                Sensor mySensor = event.sensor; // create sensor.
                sensorActive = true; // boolean to hold thread in the beginning until sensor is activated  to avoid initial zeros
                if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    //System.out.println(event.timestamp);
                    //System.out.println(tStampUpdate);
                    //System.out.println(tStampUpdate + timeDif);
                    //System.out.println(firstSample);


                    if(event.timestamp >= tStampUpdate && event.timestamp  <= (tStampUpdate + timeDif)|| firstSample) { // Only sample if matches Fs or first sample
                        float[] accData = event.values; // acquire data x, y, and z from sensor. // TODO: 08/12/15 check if it is okay that timestamp and evnt dat a not are together?!
                        addEntry(accData);
                        System.out.println("I Made it !");

                        if (counter < package_size){ // save samples in buffers, less than package_size since else includes the last sample
                            if(counter > 1) { // check for first sample
                                xBuf = xBuf + event.values[0] + ", ";
                                yBuf = yBuf + event.values[1] + ", ";
                                zBuf = zBuf + event.values[2] + ", ";
                                tBuf = tBuf + event.timestamp  + ", ";
                            }else{ // first sample
                                xBuf = event.values[0] + ", ";
                                yBuf = event.values[1] + ", ";
                                zBuf = event.values[2] + ", ";
                                tBuf = event.timestamp  + ", ";
                            }
                            tStampUpdate = event.timestamp + acc_T; // The time for next sample [ns]
                            System.out.println(counter);
                            counter++; // update counter
                        }else{
                            counter = 1;
                            csv_generator(1, xBuf + event.values[0]);
                            csv_generator(2, yBuf + event.values[1]);
                            csv_generator(3, zBuf + event.values[2]);
                            csv_generator(4, tBuf + event.timestamp);
                            xBuf = yBuf = zBuf = tBuf = null;
                        }
                        // TODO: 09/12/15 position has been removed to lower power consumption
                        // Compute position
                        //position();
                        //System.out.println("Data sampled");
                        //tStampUpdate = event.timestamp + acc_T; // The time for next sample [ns]
                        firstSample = false; // boolean for first sample to access if statement.
                    }else if(event.timestamp  > tStampUpdate + acc_T * 2) {
                        // TODO: 08/12/15 check what happens!
                        Log.i("Sample Error", "Did not sample");
                        csv_generator(6, "Sample Error at [ns]: " + event.timestamp); // log error to file.
                        // TODO: 10/12/15 removed to save power
                        nmSampling.cancel(17); // cancel notification to stop LED
                }
                    // update to new ideal time
                    //tStampUpdate = event.timestamp + acc_T;
                    // cancel LED notification
                    if(bfailSampling){
                        nmSampling.cancel(17);
                        samplingType = 2;
                        bfailSampling = false;
                        SamplingFlashLight();}
                }
            } else{ // Sampling defined from settings. Maks sampling!
                if (counter < package_size){ // save samples in buffers, less than package_size since else includes the last sample
                    if(counter > 1) { // check for first sample
                        xBuf = xBuf + event.values[0] + ", ";
                        yBuf = yBuf + event.values[1] + ", ";
                        zBuf = zBuf + event.values[2] + ", ";
                        tBuf = tBuf + event.timestamp  + ", ";

                    }else{ // first sample
                        xBuf = event.values[0] + ", ";
                        yBuf = event.values[1] + ", ";
                        zBuf = event.values[2] + ", ";
                        tBuf = event.timestamp  + ", ";
                    }
                    counter++; // update counter
                }else{
                    // IF-LOOP makes sure to only sample while we want to be doing just that.
                    if( stopsamp == 0) {
                        counter = 1;
                        csv_generator(1, xBuf + event.values[0]);
                        csv_generator(2, yBuf + event.values[1]);
                        csv_generator(3, zBuf + event.values[2]);
                        csv_generator(4, tBuf + event.timestamp);
                        xBuf = yBuf = zBuf = tBuf = null;

                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        private void addEntry(float[] accData) {
            if (counter < package_size){ // save samples in buffers, less than package_size since else includes the last sample
                if(counter > 1) { // check for first sample
                    xBuf = xBuf + accData[0] + ", ";
                    yBuf = yBuf + accData[1] + ", ";
                    zBuf = zBuf + accData[2] + ", ";
                    tBuf = tBuf + tStamp + ", ";

                }else{ // first sample
                    xBuf = accData[0] + ", ";
                    yBuf = accData[1] + ", ";
                    zBuf = accData[2] + ", ";
                    tBuf = tStamp + ", ";
                }
                counter++; // update counter
            }else{
                counter = 1;
                csv_generator(1, xBuf + accData[0]);
                csv_generator(2, yBuf + accData[1]);
                csv_generator(3, zBuf + accData[2]);
                csv_generator(4, tBuf + tStamp);
                xBuf = yBuf = zBuf = tBuf = null;

            }
            sampleStarted = true;
        }

        public void csv_generator(int sFileName, String input) {
            try {
                switch(sFileName){
                    case 1:
                        file = xFile;
                        // start LED notification sampling. Only activated one time.
                        //if(!activatedLED){
                        //    SamplingFlashLight();}
                        break;
                    case 2:
                        file = yFile;
                        break;
                    case 3:
                        file = zFile;
                        break;
                    case 4:
                        file = tFile;
                        break;
                    case 5:
                        file = pFile;
                        break;
                    case 6:
                        file = eFile;
                        break;
                    default:
                }
                FileWriter writer = new FileWriter(file, true);
                writer.append(input + ", ");
                writer.flush();
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //---------------------------------------------------------------------------------------------
        //                  Compute position
        //---------------------------------------------------------------------------------------------
        // TODO: 09/12/15 position has been removed to lower power consumption
        /*
        private void position(float[] accData) {

            // Extract data from acc.
            for (int i = 0; i < 3; i++) {
                acc_data[broadcastCounter][i] = accData[i];
            }

            // moving average window to low pass filter
            if (init || broadcastCounter == w - 1) {
                float x_sum = 0;
                float y_sum = 0;
                float z_sum = 0;
                init = true;
                // sum the values within the window
                for (int ii = 0; ii < w; ii++) {
                    x_sum = x_sum + acc_data[ii][0];
                    y_sum = y_sum + acc_data[ii][1];
                    z_sum = z_sum + acc_data[ii][2];
                }

                // Average within window
                x_sum = x_sum / w;
                y_sum = y_sum / w;
                z_sum = z_sum / w;

                // Classify position using classification tree
                if (Math.abs(x_sum) > Math.abs(y_sum) & Math.abs(x_sum) > Math.abs(z_sum)) {
                    if (x_sum < 0) {
                        statusPosition = 3; // left lateral
                    } else {
                        statusPosition = 4; // right lateral
                    }
                } else if (Math.abs(y_sum) > Math.abs(x_sum) & Math.abs(y_sum) > Math.abs(z_sum)) {
                    statusPosition = -2; // upright
                } else if (Math.abs(z_sum) > Math.abs(x_sum) & Math.abs(z_sum) > Math.abs(y_sum)) {
                    if (z_sum > 0) {
                        statusPosition = 1; // supine
                    } else {
                        statusPosition = 2; // prone
                    }
                }
                else {
                    statusPosition = 5; // Nothing to declare
                }

                // broadcastManager
                Intent intentPosition = new Intent("Acc Sample");
                // including statusPosition to intent as Extras.
                intentPosition.putExtra("acc_Position", statusPosition);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intentPosition);
                // write position to csv file
                csv_generator(5, statusPosition + "");
            }

            // update counter
            if (broadcastCounter < w - 1) {
                broadcastCounter++;

            } else {
                broadcastCounter = 0;
            }
        }
        */
    }

    // notify that the device samples using LED.
    private void SamplingFlashLight() {
        if(samplingType == 1){
            notifSampling.ledARGB = 0x009900; // green
        }else if(samplingType == 2){
            notifSampling.ledARGB = 0xCC0000; // red
        }else if(samplingType == 3){
            notifSampling.ledARGB = 0xFFFF00; // yellow
        }

        notifSampling.flags = Notification.FLAG_SHOW_LIGHTS;
        notifSampling.ledOnMS = 200;
        notifSampling.ledOffMS = 2000;
        nmSampling.notify(17, notifSampling);
        activatedLED = true;
    }
}