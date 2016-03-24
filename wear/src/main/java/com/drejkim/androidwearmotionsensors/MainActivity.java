package com.drejkim.androidwearmotionsensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {

    //Button start;
    //Button stop;

    private static final String TAG = "MainActivity";

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    String values;
    Sensor sensorAccelerometer = null;
    Sensor sensorGeoRotationVector = null;
    Sensor mSensor;
    ArrayList<String> allValues;

    private View mView;
    private TextView mTextTitle;
    private FileWriter input;
    private SensorManager mSensorManager;
    private int lineNumber = 0;
    float[] rotationMatrix = null;
    float[] mAccelerometerValues = null;
    float orientation[] = new float[3];

    private File directory;
    private File file;
    private BufferedWriter bufferedWriter;

    private String currentTime;

    Calendar calTime = Calendar.getInstance();
    private SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private SimpleDateFormat date = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.US);
    private final String fileName = date.format(calTime.getTime()) + ".csv";

    ToggleButton butRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        directory = new File(Environment.getExternalStorageDirectory().getPath());
        file = new File(directory, fileName);
        butRecord = (ToggleButton) this.findViewById(R.id.butRec);
        butRecord.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    prepareSensors();
                } else {
                    Thread thread = new Thread() {
                        public void run() {
                            try {
                                wait(50000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    thread.start();
                    destroySensors();
                    TextView textView = (TextView) findViewById(R.id.text_values);
                    textView.setText(R.string.writing);
                    ListIterator<String> it = allValues.listIterator();
                    int num = 0;
                    while(it.hasNext()){
                        if(num%10==0)
                            writeFile(it.next());
                        num++;
                    }
                    textView.setText(R.string.done_writing);
                }
            }
        });
    }

    /**
     * Method called when the sensors change
     * @param event
     * Data is organized in the following way:
     * Time: Time in which the data was collected
     * Line Number: data point being collected. Used for sorting after the fact
     * Azimuth: Degrees from north
     * Pitch
     * Roll
     * Acceleration in the X direction
     * Acceleration in the Y direction
     * Acceleration in the Z direction
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        if (allValues == null) {
            allValues = new ArrayList<>();
        }
        currentTime = time.format(System.currentTimeMillis());
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
            values = currentTime + ","
                    + lineNumber + ","
                    + Float.toString(orientation[0]) + ","
                    + Float.toString(orientation[1]) + ", "
                    + Float.toString(orientation[2]) + ","
                    + Float.toString(mAccelerometerValues[0]) + ", "
                    + Float.toString(mAccelerometerValues[1]) + ", "
                    + Float.toString(mAccelerometerValues[2]) + "\n";
            TextView timeTextView = (TextView) findViewById(R.id.text_time);
            timeTextView.setText(currentTime);
            lineNumber++;
            allValues.add(values);
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
        try {
            bufferedWriter.write(toWrite);
        }catch(IOException e){
            Log.d(TAG, e.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Method for preparing the sensors
     * Sensor delay is set to SENSOR_DELAY_UI. This is optimal for the watch, considering collection
     * happens on the main thread. Any faster causes the application to hang, and data is not recorded
     * properly
     *
     * SENSOR_DELAY_UI sets the sampling rate to 60000 microseconds (0.06 seconds)
     */
    private void prepareSensors() {
        mSensorManager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGeoRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        //Register listeners.
        mSensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, sensorGeoRotationVector, SensorManager.SENSOR_DELAY_UI);
        butRecord.setText(R.string.stop);
    }

    private void destroySensors(){
        if(mSensorManager!=null){
            mSensorManager.unregisterListener(this);
            butRecord.setText(R.string.start);
        }
    }
}