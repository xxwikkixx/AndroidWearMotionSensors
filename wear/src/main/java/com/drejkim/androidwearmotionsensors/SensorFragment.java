package com.drejkim.androidwearmotionsensors;

import android.app.Fragment;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SensorFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "SensorFragment";

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    String values;
    Sensor sensorAccelerometer = null;
    Sensor sensorGeoRotationVector = null;
    Sensor mSensor;

    private View mView;
    private TextView mTextTitle;
    private FileWriter input;
    private SensorManager mSensorManager;
    private int lineNumber = 0;
    float[] rotationMatrix = null;
    float[] mAccelerometerValues = null;
    float orientation[] = new float[3];

    private long mShakeTime = 0;
    private long mRotationTime = 0;
    private File directory;
    private File file;
    private BufferedWriter bufferedWriter;

    private String currentTime;

    Calendar calTime = Calendar.getInstance();
    private SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private SimpleDateFormat date = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.US);
    private final String fileName = date.format(calTime.getTime()) + ".csv";

    int count = 0;

    ToggleButton butRecord;

    public static SensorFragment newInstance(int sensorType) {
        SensorFragment f = new SensorFragment();

        // Supply sensorType as an argument
        Bundle args = new Bundle();
        args.putInt("sensorType", sensorType);
        f.setArguments(args);
        return f;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Bundle args = getArguments();
        butRecord = (ToggleButton)mView.findViewById(R.id.butRec);
        butRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prepareSensors();
                } else {
                    destroySensors();
                }
            }
        });
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        directory = new File(Environment.getExternalStorageDirectory().getPath());
        file = new File(directory, "XYZ.csv");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.sensor, container, false);

        mTextTitle = (TextView) mView.findViewById(R.id.text_title);
        //mTextTitle.setText(mSensor.getStringType());
        //record button for data recording
        butRecord = (ToggleButton) mView.findViewById(R.id.butRec);


        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        try{
            if(input!=null){
                input.close();
            }
        } catch(IOException e){
            Log.d(TAG, e.toString());
        }
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        currentTime = time.format(System.currentTimeMillis());
        Log.d(TAG, currentTime);
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometerValues = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            rotationMatrix = new float[16];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrix);
            SensorManager.getOrientation(rotationMatrix, orientation);
        }
        if (mAccelerometerValues != null && rotationMatrix != null) {
            values = currentTime + "," + lineNumber + "," + Float.toString(orientation[0]) + "," + Float.toString(orientation[1]) + ", " + Float.toString(orientation[2]) +","+ Float.toString(mAccelerometerValues[0]) + ", "
                    + Float.toString(mAccelerometerValues[1]) + ", " + Float.toString(mAccelerometerValues[2]) + "\n";
            lineNumber++;
            if(count ==10) {
                writeFile(values);
                count = 0;
            }
            count++;
        }

    }
    
    public void writeFile(String toWrite){
        if(bufferedWriter == null){
            try {
                bufferedWriter = new BufferedWriter(new FileWriter(file, true));
            }catch(java.io.IOException e){
                Log.d(TAG, e.toString());
            }
        }
        //Commenting out because its easier to adb pull XYZ.csv
        //File file = new File(directory, fileName);
        try {
            bufferedWriter.write(toWrite);
        }catch(IOException e){
            Log.d(TAG, e.toString());
        }
        try{
            Thread.sleep(10);
        } catch (InterruptedException e){
            Log.d(TAG, e.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void prepareSensors() {
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGeoRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        //Register listeners.
        mSensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, sensorGeoRotationVector, SensorManager.SENSOR_DELAY_FASTEST);
        butRecord.setText(R.string.stop);
    }
    private void destroySensors(){
        if(mSensorManager!=null){
            mSensorManager.unregisterListener(this);
            butRecord.setText(R.string.start);
        }
    }
}
