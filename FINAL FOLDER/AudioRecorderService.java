package master.testgraphview;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by martinguul on 01/12/15.
 * inspiration: http://stackoverflow.com/questions/8499042/android-audiorecord-example/13487250#13487250
 * Last Edited by Mathias Pinto Bonnesen, 15/10/16
 */
public class AudioRecorderService extends Service {

    private int RECORDER_SAMPLERATE; // static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private int BufferElements2Rec;
    private static final int BytesPerElement = 2;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private String audioDirectString;
    private SharedPreferences sharedPrefs;

    // file system
    private File iFile;

    @Override
    public void onCreate() {
        super.onCreate();

        //Preferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        //init values from preferences
        RECORDER_SAMPLERATE = Integer.valueOf(sharedPrefs.getString("AudioFs", "16000")); // [Hz]
        BufferElements2Rec = Integer.valueOf(sharedPrefs.getString("AudioPackage", "1024"));

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);


        // String defining directory
        audioDirectString = Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DOWNLOADS) + "/Data_" +
                sharedPrefs.getString("PatientName","test");

        startRecording();

        // file system
        //iFile = new File(audioDirectString, "iFile.txt");


        // batteryStatus(1);
    }

    @Override
    public void onDestroy() {
        stopRecording();

        //batteryStatus(2);
        //updateMediaScanner(iFile);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
        // AudioRecord(int audioSource, int sampleRateInHz, int channelConfig,
        // int audioFormat, int bufferSizeInBytes)

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread"); //Thread(runnable, thread name);
        //csv_info(iFile, "Fs aud: " + RECORDER_SAMPLERATE);
        //csv_info(iFile, "Package aud: " + BufferElements2Rec);

        recordingThread.start();
    }

    //convert short to byte
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
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(audioDirectString + "/audio.pcm");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short writing to file" + sData.toString());
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

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;

            // update media scanner
            File tmp = new File(audioDirectString + "/audio.pcm");
            Uri uri = Uri.fromFile(tmp);
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
        }
    }

    public void batteryStatus(int status){
        // Power status
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        float level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        float scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level / scale; // cast

        switch (status){
            case 1:
                //csv_info(iFile, "startBatteryStatus: " + batteryPct);
                //csv_info(iFile, "Fs aud: " + RECORDER_SAMPLERATE);
                break;
            case 2:
                //csv_info(iFile, "endBatteryStatus: " + batteryPct);
                break;
        }

    }

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

    public void updateMediaScanner(File file) {
        //Send an Intent to the MediaScanner to scan our file
        //Broadcast the Media Scanner Intent to trigger it
        Uri uri = Uri.fromFile(file);
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    }
}