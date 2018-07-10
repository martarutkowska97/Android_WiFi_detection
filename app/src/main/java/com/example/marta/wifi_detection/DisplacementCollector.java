package com.example.marta.wifi_detection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Class used to collect the data from accelerometer and interpret them as displacement
 * in three dimensions.
 */
public class DisplacementCollector implements SensorEventListener {

    private float[] last_values = null;
    private float[] velocity = null;
    private float[] position = null;
    private long last_timestamp = 0;
    private float[] displacement = new float[3];
    /**
     * Constant used to convert a value into a proper one.
     */
    private static final float NS2S = 1.0f / 1000000000.0f;


    /**
     * Constructor creating the instance of DisplacementCollector and initializing proper variables
     * @param context Context of the Activity in which it is used.
     */
    DisplacementCollector(Context context){
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 1000000);
        sm.registerListener(this,sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 1000000);
    }

    /**
     * Called when sensor values have changed.
     * @param event SensorEvent: the SensorEvent.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(last_values != null){
            float dt = (event.timestamp - last_timestamp) * NS2S;

            for(int index = 0; index < 3;++index){
                velocity[index] += (event.values[index] + last_values[index])/2 * dt;
                position[index] += velocity[index] * dt;
            }
        }
        else{
            last_values = new float[3];
            velocity = new float[3];
            position = new float[3];
            velocity[0] = velocity[1] = velocity[2] = 0f;
            position[0] = position[1] = position[2] = 0f;
        }
        System.arraycopy(event.values, 0, last_values, 0, 3);
        last_timestamp = event.timestamp;
    }

    /**
     * Called when the accuracy of a sensor has changed.
     * @param sensor The ID of the sensor being monitored.
     * @param i The new accuracy of this sensor.
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Copies the values of position into a displacement array and returns it.
     * @return Array of displacement values.
     */
    public float[] getDisplacement(){
        System.arraycopy(position, 0, displacement, 0, 3);
        resetMeasurements();
        return displacement;
    }

    /**
     * Resets values in velocity and position arrays by setting them as zero.
     */
    private void resetMeasurements(){
        velocity[0] = velocity[1] = velocity[2] = 0f;
        position[0] = position[1] = position[2] = 0f;
    }

}