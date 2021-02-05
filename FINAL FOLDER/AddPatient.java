package master.testgraphview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by martinguul on 02/11/15.
 * Last Edited by Mathias Pinto Bonnesen, 15/10/16
 */
public class AddPatient extends Activity {
    private Spinner spinnerSnoring, spinnerTired, spinnerObserved, spinnerPressure, spinnerBMI, spinnerAge, spinnerNeck, spinnerGender;
    private static EditText PatientAge;
    private static EditText patientName;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;

    private File iFile;
    //private EditText etW, etH;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_patient);

        // preferences
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPrefs.edit();

        // Spinners
        spinnerSnoring = (Spinner) findViewById(R.id.spinnerSnoring);
        spinnerTired = (Spinner) findViewById(R.id.spinnerTired);
        spinnerObserved = (Spinner) findViewById(R.id.spinnerObserved);
        spinnerPressure = (Spinner) findViewById(R.id.spinnerPressure);
        spinnerBMI = (Spinner) findViewById(R.id.spinnerBMI);
        //spinnerAge = (Spinner) findViewById(R.id.spinnerAge);
        spinnerNeck = (Spinner) findViewById(R.id.spinnerNeck);
        spinnerGender = (Spinner) findViewById(R.id.spinnerGender);

        patientName = (EditText) findViewById(R.id.etPatientName);
        patientName.setText(sharedPrefs.getString("etPatientName","NA"));

        PatientAge = (EditText) findViewById(R.id.etPatientAge);
        PatientAge.setText(sharedPrefs.getString("etPatientAge",""));


        // set up spinner array
        ArrayAdapter<CharSequence> STOP_bang_Answer = ArrayAdapter.createFromResource(this,
                R.array.STOP_bang, android.R.layout.simple_spinner_item);

        STOP_bang_Answer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSnoring.setAdapter(STOP_bang_Answer);
        spinnerTired.setAdapter(STOP_bang_Answer);
        spinnerObserved.setAdapter(STOP_bang_Answer);
        spinnerPressure.setAdapter(STOP_bang_Answer);
        //spinnerAge.setAdapter(STOP_bang_Answer);
        spinnerNeck.setAdapter(STOP_bang_Answer);


        // Set up spinner for BMI answers
        ArrayAdapter<CharSequence> BMI_Answer = ArrayAdapter.createFromResource(this,
                R.array.BMI, android.R.layout.simple_spinner_item);

        BMI_Answer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerBMI.setAdapter(BMI_Answer);

        // Set up spinner for Gender answers
        ArrayAdapter<CharSequence> gender_Answer = ArrayAdapter.createFromResource(this,
                R.array.gender, android.R.layout.simple_spinner_item);

        gender_Answer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        spinnerGender.setAdapter(gender_Answer);

        patientName.setVisibility(View.GONE);
        patientName.setVisibility(View.GONE);

        //patientName.setText(sharedPrefs.getString("PatientName","NA"));
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formattedDate = df.format(c.getTime());
        patientName.setText(formattedDate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_add_patient, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String age = PatientAge.getText() + "";

        if(spinnerSnoring.getSelectedItemPosition() != 0
                && spinnerGender.getSelectedItemPosition() != 0 && spinnerBMI.getSelectedItemPosition() != 0 &&
                spinnerNeck.getSelectedItemPosition() != 0 && spinnerPressure.getSelectedItemPosition() != 0
                && spinnerObserved.getSelectedItemPosition() != 0 && spinnerTired.getSelectedItemPosition() != 0
                && age != ""){

            Intent intent = null;
            switch(item.getItemId()){
                case R.id.action_fished:
                    String textInput = patientName.getText() + "";


                    // save patient name to preferences
                    // Store date in variable
                    editor.putString("PatientName", textInput); // id for patient
                    editor.putString("Quest_order: ", "STOP BANG"); // Order of questions
                    editor.putString("Quest",spinnerGender.getSelectedItemPosition()
                            + ", "  + age + ", " +
                            spinnerBMI.getSelectedItemPosition() + ", " +
                            spinnerSnoring.getSelectedItemPosition() + ", " +
                            spinnerTired.getSelectedItemPosition() + ", " +
                            spinnerObserved.getSelectedItemPosition() + ", " +
                            spinnerPressure.getSelectedItemPosition() + ", " +
                            spinnerNeck.getSelectedItemPosition());
                    editor.apply();

                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    String formattedDate = df.format(c.getTime());
                    patientName = (EditText) findViewById(R.id.etPatientName);
                    patientName.setText(formattedDate);

                    // check and define directory
                    File direct = new File(Environment.getExternalStoragePublicDirectory
                            (Environment.DIRECTORY_DOWNLOADS), "Data_" + sharedPrefs.getString("PatientName","test"));

                    if (!direct.exists()) {
                        direct.mkdirs();
                        System.out.println("Checksome");
                    }

                    // START the SampleData.java class
                    intent = new Intent(this, SampleData.class);
                    this.startActivity(intent);
                    break;
            }

        } else{
            DialogMissingAnswer();
        }

        return true;
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

    public void DialogMissingAnswer() {
        // create dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.
                Builder(this);
        // set message and title
        alertDialogBuilder.setMessage("Svar venligst på alle spørgsmålene");
        alertDialogBuilder.setTitle("Fejl i besvarelse");

        // Create positive btn
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                });
        // Create negative btn
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {

                    }
                });

        // build the alertDialog
        alertDialogBuilder.show();
    }

    public void DialogBMI(View view) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.dialog_bmi, null);

        // create dialog
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setView(textEntryView);

        // set message and title
        alertDialogBuilder.setTitle("BMI Udregner");
        alertDialogBuilder.setMessage("Indsæt højde (cm) og vægt (kg), og tryk OK");

        // Create positive btn
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        EditText etW = (EditText) textEntryView.findViewById(R.id.etW);
                        EditText etH = (EditText) textEntryView.findViewById(R.id.etH);

                        // IF statement secures that app does not crash due to lag of inputs
                        if(etH.getText().length() > 0 && etW.getText().length() > 0) {
                            double BMI_val = Integer.valueOf(etW.getText() + "") /
                                    (Integer.valueOf(etH.getText() + "") *
                                            Integer.valueOf(etH.getText() + ""))/10000;

                            // STORE the variables globally for later use
                            SharedPreferences sp = getSharedPreferences("your_prefs", Activity.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putInt("height", Integer.valueOf(etH.getText() + ""));
                            editor.putInt("weight", Integer.valueOf(etW.getText() + ""));
                            editor.commit();


                            if (BMI_val < 1) {
                                spinnerBMI.setSelection(1);
                            }
                        }
                    }
                });
        // Create negative btn
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog,
                                        int which) {

                    }
                });

        // build the alertDialog
        alertDialogBuilder.show();
    }


}
