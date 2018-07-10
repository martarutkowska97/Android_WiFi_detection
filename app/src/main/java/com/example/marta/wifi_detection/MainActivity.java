package com.example.marta.wifi_detection;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.constraint.solver.widgets.WidgetContainer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.WindowCallbackWrapper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The WiFi_detection program is used to collect data of all routers nearby
 * by detecting them, resolving their MAC address and signal strength
 * and link them to the coordinates, which are derived from the accelerometer.
 *
 * This application also saves collected data into JSON file in order to make
 * those data more useful and prepared for further calculations and processing.
 *
 * This class contains main content of the Activity and basic service
 *
 */
public class MainActivity extends Activity {

    /**
     * How many times the app collects data with one scan click
     */
    public static final int NUMBER_OF_SCANS = 3;
    /**
     * File name of the file which is used to store data about the file counter
     */
    public static final String FILE_COUNTER_EXTENSION="fileCounter.bin";

    /**
     * Arraylist of the objects containing information about current WiFis available nearby
     */
    private static ArrayList<WiFiElement> content;
    /**
     * Adapter for the RecyclerView
     */
    private RVAdapter adapter;
    private WifiManager wifiManager;
    /**
     * Broadcast receiver used to receive WiFi data by scanning nearby WiFis
     */
    private WifiScanReceiver wifiScanReceiver;
    private int fileCounter;
    /**
     * Used to collect data from the accelerometer about the current displacement
     */
    private DisplacementCollector displacementCollector;

    /**
     * Initialization of the activity and used services
     * @param savedInstanceState Bundle: If the activity is being re-initialized after previously
     * being shut down then this Bundle contains the data it most recently supplied in
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAllPermissions();

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        content=new ArrayList<>();
        adapter = new RVAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WifiScanReceiver();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        initializeScanButton();

        fileCounter = readFileCounterFromFile();
        displacementCollector=new DisplacementCollector(getApplicationContext());
    }

    /**
     * Checks if all required permissions are granted
     * if they are not application sends a request to the user
     */
    private void checkAllPermissions(){
        if(!(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    0);
        }
    }

    /**
     * Initializes button used to trigger a WiFi scan
     * It initializes also a Thread which controls number of scans and
     * controls starting next scan when the scanner is ready
     */
    private void initializeScanButton(){
        Button scan= findViewById(R.id.button_scan);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int count = 0;
                        while (count < NUMBER_OF_SCANS) {
                            try {
                                wifiManager.startScan();
                                synchronized (MainActivity.this) {
                                    MainActivity.this.wait();
                                    count++;
                                }
                                final int c = count;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "SAVED "+c, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                if(count < NUMBER_OF_SCANS)
                                    Thread.sleep(2000);
                            } catch (InterruptedException e) {
                            }

                        }
                    }
                }).start();

            }
        });
    }

    /**
     * Reads from file a value of the variable responsible for creating
     * a new file with a specific number- called fileCounter.
     *
     * If file which should contain file counter does not exist, method returns 0
     * @return integer number responsible for identifying new file
     */
    private int readFileCounterFromFile(){
      int f=0;
      File fileDir=getApplication().getFilesDir();
      String counterFile=null;

        for(String file: fileDir.list())
        {
            if(file.endsWith(FILE_COUNTER_EXTENSION)){
                counterFile=file;
            }
        }

        FileInputStream fis;
        ObjectInputStream ois;

        try{
            fis=getApplicationContext().openFileInput(counterFile);
            ois= new ObjectInputStream(fis);
            f=(int) ois.readObject();
            fis.close();
            ois.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
      return f;
    }

    /**
     * Called when the activity is no longer visible to the user, because another activity
     * has been resumed and is covering this one. This method saves to file the file counter also
     */
    @Override
    protected void onStop() {
        super.onStop();
        FileOutputStream fos;
        ObjectOutputStream oos;
        try
        {
            fos = getApplication().openFileOutput(FILE_COUNTER_EXTENSION, Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(fileCounter);
            oos.close();
            fos.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Converts the data into JSON object and saves it to files: JSON file and .txt file.
     * @param jsonWiFiData JSONWiFiData it holds the data which are later converted to JSOn object
     */
    private void saveToJSONFile(JSONWiFiData jsonWiFiData){
        JSONObject jsonObject=jsonWiFiData.makeJSONObject();

        try (FileWriter file = new FileWriter(Environment.getExternalStorageDirectory()+"/plikJSON"+fileCounter+".json")) {
            file.write(jsonObject.toString());
            file.flush();

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try (FileWriter file = new FileWriter(Environment.getExternalStorageDirectory()+"/plikTxt"+fileCounter+".txt")) {
            fileCounter++;
            file.write(jsonObject.toString());
            file.flush();

        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Clears the Arraylist holding data of nearby routers with their signals, then
     * adds new data collected and notifies an adapter to refresh the RecyclerView
     */
    private void returnAllWifis(){
        content.clear();
        List<ScanResult> scanResults = wifiManager.getScanResults();

        for(ScanResult s: scanResults){
            content.add(new WiFiElement(s.BSSID, Integer.toString(s.level)));
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     * Unregisters the broadcast receiver.
     */
    @Override
    protected void onPause() {
        unregisterReceiver(wifiScanReceiver);
        super.onPause();
    }


    /**
     * Called when the activity will start interacting with the user.
     * Registers the broadcast receiver.
     */
    @Override
    protected void onResume() {
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    /**
     * BroadcastReceiver type class, responsible for listening the WiFi scanner if ready,
     * then collecting data, updating the RecyclerView, determining the displacement and
     * saving data collected to files.
     */
    class WifiScanReceiver extends BroadcastReceiver{

        /**
         * Responsible for listening the WiFi scanner if ready,
         * then collecting data, updating the RecyclerView, determining the displacement and
         * saving data collected to files.
         * @param c context of the application
         * @param intent intent from which actions are established
         */
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                returnAllWifis();
                float[] disp = displacementCollector.getDisplacement();
                JSONWiFiData jsonWiFiData=new JSONWiFiData(disp[0],disp[1],content);
                saveToJSONFile(jsonWiFiData);
                synchronized (MainActivity.this) {
                    MainActivity.this.notifyAll();
                }
            }
        }
    }


    /**
     * This class inherits from the RecyclerView.Adapter, it implements specific adapter for our
     * RecyclerView
     */
    class RVAdapter extends RecyclerView.Adapter<MainActivity.ViewHolder>{

        /**
         *
         */
        LayoutInflater mLayoutInflater;

        /**
         *
         */
        public RVAdapter(){
            mLayoutInflater=getLayoutInflater();
        }

        /**
         *
         * @param parent
         * @param viewType
         * @return
         */
        @Override
        public MainActivity.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view=mLayoutInflater.inflate(R.layout.wifi_element,parent,false);
            MainActivity.ViewHolder holder=new MainActivity.ViewHolder(view);
            return holder;
        }

        /**
         *
         * @param holder
         * @param position
         */
        @Override
        public void onBindViewHolder(MainActivity.ViewHolder holder, int position) {
            holder.addressMAC.setText(content.get(position).getAddressMAC());
            holder.signalStrength.setText(content.get(position).getSignalStrength());
        }

        /**
         *
         * @return
         */
        @Override
        public int getItemCount() {
            return content.size();
        }

    }


    /**
     * Class responsible for creating a view holder for a recycle view.
     * This class inherits properties from a class RecyclerView.ViewHolder
     *
     */
    public class ViewHolder extends RecyclerView.ViewHolder{

        /**
         * TextView holding an information
         */
        TextView addressMAC;
        /**
         * TextView holding an information about the signal strength of specific router in text
         */
        TextView signalStrength;

        /**
         * Constructor creating an instance of a ViewHolder class, initializing TextViews
         * @param view
         */
        public ViewHolder(View view){
            super(view);
            addressMAC= view.findViewById(R.id.mac_address);
            signalStrength= view.findViewById(R.id.signal_strength);
        }
    }
}
