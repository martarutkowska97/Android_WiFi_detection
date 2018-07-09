package com.example.marta.wifi_detection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class DisplacementCollector implements SensorEventListener {

    private float[] last_values = null;
    private float[] velocity = null;
    private float[] position = null;
    private long last_timestamp = 0;
    private float[] displacement = new float[3];
    private static final float NS2S = 1.0f / 1000000000.0f;


    DisplacementCollector(Context context){
        SensorManager sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 1000000);

        boolean accelerometer;

        accelerometer = sm.registerListener(this,sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), 1000000);

        if(accelerometer)
        {
            System.out.println("dupka");
        }
        else{
            System.out.println("nie ma");
        }
    }

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

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public float[] getDisplacement(){
        System.arraycopy(position, 0, displacement, 0, 3);
        resetMeasurements();
        return displacement;
    }

    private void resetMeasurements(){
        velocity[0] = velocity[1] = velocity[2] = 0f;
        position[0] = position[1] = position[2] = 0f;
    }

}