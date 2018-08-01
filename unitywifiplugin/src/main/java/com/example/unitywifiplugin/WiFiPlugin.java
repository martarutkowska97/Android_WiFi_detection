package com.example.unitywifiplugin;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.unity3d.player.UnityPlayer;

/**
 * @author marta
 * Android Java Plugin. It is useful for collecting the data about nearby routers: its signal strenght
 * and MAC address. Plugin is strictly directed to special Unity Android application (which holds the
 * map of the office, and takes the coordinates of a measurement place with all the wifi data collected.
 * After collecting all the data we need, everything can be saved to JSON and txt file in JSON format
 * in order to further processing.
 */


/**
 * This class holds whole plugin functionality
 */
public class WiFiPlugin {

    /**
     * File name of the file which is used to store data about the file counter
     */
    public static final String FILE_COUNTER_EXTENSION = "fileCounter.bin";
    private static WifiManager wifiManager;
    /**
     * Broadcast receiver used to receive WiFi data by scanning nearby WiFis
     */
    private static WifiScanReceiver wifiScanReceiver;
    private static int fileCounter;
    /**
     * Arraylist of the objects containing information about WiFis available nearby a specific coordinate.
     * An effect of collecting one measurement.
     */
    private static ArrayList<JSONObject> dataCollected;
    /**
     * Contect of the application, taken from the Unity.
     */
    static final Context context = UnityPlayer.currentActivity;
    private static boolean isScannerReady = true;

    private static float current_x_coord = -1;
    private static float current_y_coord = -1;

    private static float rotation_x = -1;
    private static float rotation_y = -1;
    private static float rotation_z = -1;


    private static SensorManager sensorManager;
    private static Sensor rotationVector;

    /**
     * This method initializes needed class fields.
     */
    public static void initialize() {
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = WifiScanReceiver.getInstance();
        registerListener();
        fileCounter = readFileCounterFromFile();
        dataCollected = new ArrayList<>();
        checkAllPermissions();

        sensorManager=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        rotationVector=sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        SensorEventListener rotationVectorListener= new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                rotation_x=sensorEvent.values[0];
                rotation_y=sensorEvent.values[1];
                rotation_z=sensorEvent.values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        sensorManager.registerListener(rotationVectorListener, rotationVector, 100);
    }


    /**
     * Checks if all required permissions are granted
     * if they are not application sends a request to the user
     */
    private static void checkAllPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 0);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 0);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
    }

    /**
     * Outward method used in Unity to take the measurement by starting scanning.
     * @param x_coord x coordinate on the office map
     * @param y_coord y coordinate in the office map
     */
    public static void takeMeasurement(final float x_coord, final float y_coord) {
        current_x_coord = x_coord;
        current_y_coord = y_coord;
        wifiManager.startScan();
        isScannerReady = false;
    }

    public static void removeLastMeasurement(){
        if(dataCollected.size()!=0){
            dataCollected.remove(dataCollected.size()-1);
        }
    }


    /**
     * Registers the broadcast receiver in the application
     */
    public static void registerListener() {
        context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    /**
     * Unregisters the broadcast receiver from the application
     */
    public static void unregisterListener() {
        context.unregisterReceiver(wifiScanReceiver);
    }

    /**
     * Method used outwardly in order to finish one stage of collecting data and save it to the JSON and txt files
     * in JSON format. In Unity it is attached to the SAVE button.
     */
    public static void finishAndSave() {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < dataCollected.size(); i++) {
            arr.put(dataCollected.get(i));
        }

        File directory = new File(Environment.getExternalStorageDirectory()+"/JSON");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss", Locale.GERMANY);
        String format = s.format(new Date());

        try (FileWriter file = new FileWriter(directory.getAbsolutePath()+ "/WiFi" + format + ".json")) {
            file.write(arr.toString());
            file.flush();
            Toast.makeText(context, "ZAPISANO "+format, Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        }

//        try (FileWriter file = new FileWriter(directory.getAbsolutePath()+ "/plikTxt" + format + ".txt")) {
//            fileCounter++;
//            file.write(arr.toString());
//            file.flush();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        FileOutputStream fos;
        ObjectOutputStream oos;
        try {
            fos = context.openFileOutput(FILE_COUNTER_EXTENSION, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(fileCounter);
            oos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dataCollected.clear();
    }

    /**
     * Reads from file a value of the variable responsible for creating
     * a new file with a specific number- called fileCounter.
     *
     * If file which should contain file counter does not exist, method returns 0
     * @return integer number responsible for identifying new file
     */
    private static int readFileCounterFromFile() {
        int f = 0;
        File fileDir = context.getFilesDir();
        String counterFile = null;

        for (String file : fileDir.list()) {
            if (file.endsWith(FILE_COUNTER_EXTENSION)) {
                counterFile = file;
            }
        }

        FileInputStream fis;
        ObjectInputStream ois;

        try {
            fis = context.openFileInput(counterFile);
            ois = new ObjectInputStream(fis);
            f = (int) ois.readObject();
            fis.close();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return f;
    }

    /**
     * @return returns collected data from wifi manager: MAC address and signal strength
     * as an arraylist of WiFiElements
     */
    private static ArrayList<WiFiElement> returnAllWiFis() {
        ArrayList<WiFiElement> content = new ArrayList<>();
        List<ScanResult> scanResults = wifiManager.getScanResults();

        for (ScanResult s : scanResults) {
            content.add(new WiFiElement(s.BSSID, Integer.toString(s.level)));
        }
        return content;
    }


    /**
     * Method used outwardly, in Unity. It returns the information about readiness of the wifiscanner
     * in order to present this information in Unity UI
     * @return boolean isScannerReady
     */
    public static boolean isWiFiScannerReady() {
        return isScannerReady;
    }

    /**
     * BroadcastReceiver type class, responsible for listening the WiFi scanner if ready,
     * then collecting data, updating the RecyclerView, determining the displacement and
     * saving data collected to files. It is the singleton
     */
    static class WifiScanReceiver extends BroadcastReceiver {

        /**
         * Instance of WifiScanReceiver
         */
        private static WifiScanReceiver instance;

        /**
         * Singleton method for returning the instance of WifiScanReceiver
         * @return instance of a class
         */
        public static WifiScanReceiver getInstance() {
            if (instance == null) {
                instance = new WifiScanReceiver();
            }
            return instance;
        }
        /**
         * Responsible for listening the WiFi scanner if ready,
         * then collecting data, determining the displacement and giving the information that scanner is ready
         * @param c context of the application
         * @param intent intent from which actions are established
         */
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                JSONWiFiData jsonWiFiData = new JSONWiFiData(current_x_coord, current_y_coord, rotation_x, rotation_y, rotation_z, returnAllWiFis());
                dataCollected.add(jsonWiFiData.makeJSONObject());
                isScannerReady = true;
            }
        }

    }


}
