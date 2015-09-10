package com.drejkim.androidwearmotionsensors;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SensorFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "SensorFragment";

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    private View mView;
    private TextView mTextTitle;
    private String values, values2;
    private TextView mTextValues;
    private FileWriter input;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mSensorType;

    float[] rotationMatrix = null;
    float[] mAccelerometerValues = null;
    float orientation[] = new float[3];

    private long mShakeTime = 0;
    private long mRotationTime = 0;
    private File directory;

    private String currentTime;
    private String fileName;
    Calendar calTime = Calendar.getInstance();
    private SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
    private SimpleDateFormat date = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");



    Button butRecord;

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
        if(args != null) {
            mSensorType = args.getInt("sensorType");
        }

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(mSensorType);
        fileName = date.format(calTime.getTime()) + ".csv";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.sensor, container, false);

        mTextTitle = (TextView) mView.findViewById(R.id.text_title);
        mTextTitle.setText(mSensor.getStringType());
        mTextValues = (TextView) mView.findViewById(R.id.text_values);

        //record button for data recording
        butRecord = (Button) mView.findViewById(R.id.butRec);

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
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }
        currentTime = time.format(calTime.getTime());


        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mAccelerometerValues = event.values;
        if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            rotationMatrix = new float[16];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotationMatrix);
            SensorManager.getOrientation(rotationMatrix, orientation);
        }
        if(mAccelerometerValues!=null && rotationMatrix!= null) {
            values = currentTime + "," + Float.toString(mAccelerometerValues[0]) + ", " + Float.toString(mAccelerometerValues[1]) + ", " + Float.toString(mAccelerometerValues[2]) + "," +
                    Float.toString(orientation[0]) + "," + Float.toString(orientation[1]) + ", " + Float.toString(orientation[2]) + "\n";
            writeFile(values);
            mTextValues.setText(
                    currentTime + Float.toString(event.values[0]) + ", " + "y = " + Float.toString(event.values[1]) + ", " + "z = " + Float.toString(event.values[2]) + "\n"
            );
        }
    }

    public void writeFile(String toWrite){
        //String time, String azimuth, String pitch, String roll, String xData, String yData, String zData
        if(directory==null){
            directory = new File("/sdcard/");
        }
        File file = new File(directory, fileName);
        String line = "t4est";
        //time + ", " + azimuth + ", " + pitch + ", " + roll + ", " + xData + ", " + yData + ", " + zData;

        try{
            if(input==null){
                input = new FileWriter(file, true);
            }
            input.write(toWrite);
        }catch (IOException e){
            Log.d(TAG, e.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;
            //acclerometer
            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            float gForce = FloatMath.sqrt(gX*gX + gY*gY + gZ*gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color
            if(gForce > SHAKE_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
            }
            else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }

    private void detectRotation(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mRotationTime) > ROTATION_WAIT_TIME_MS) {
            mRotationTime = now;

            // Change background color if rate of rotation around any
            // axis and in any direction exceeds threshold;
            // otherwise, reset the color
            //gyroscope
            if(Math.abs(event.values[0]) > ROTATION_THRESHOLD ||
               Math.abs(event.values[1]) > ROTATION_THRESHOLD ||
               Math.abs(event.values[2]) > ROTATION_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
            }
            else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }
}
