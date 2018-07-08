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

public class MainActivity extends Activity {

    public static final int NUMBER_OF_SCANS = 3;
    public static final String FILE_COUNTER_EXTENSION="fileCounter.bin";

    private static ArrayList<WiFiElement> content;
    private RVAdapter adapter;

    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;

    private int fileCounter;
    private ArrayList<JSONObject> dataCollection;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAllPermssions();


        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        content=new ArrayList<>();
        dataCollection=new ArrayList<>();
        adapter = new RVAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanReceiver = new WifiScanReceiver();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        initializeScanButton();

        fileCounter = readFileCounterFromFile();

    }

    private void checkAllPermssions(){
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

    private void saveToJSONFile(){
        JSONWiFiData jsonWiFiData=new JSONWiFiData(-1,-1,content);
        JSONObject jsonObject=jsonWiFiData.makeJSONObject();


        try (FileWriter file = new FileWriter(Environment.getExternalStorageDirectory()+"/plikJSON"+fileCounter+".json")) {
            fileCounter++;
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

    private void returnAllWifis(){
        content.clear();
        List<ScanResult> scanResults = wifiManager.getScanResults();

        for(ScanResult s: scanResults){
            content.add(new WiFiElement(Integer.toString(s.level), s.BSSID));
            System.out.println(s.BSSID);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(wifiScanReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    class WifiScanReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                returnAllWifis();
                JSONWiFiData jsonWiFiData=new JSONWiFiData(-1,-1,content);
                JSONObject jsonObject=jsonWiFiData.makeJSONObject();
                dataCollection.add(jsonObject);
                saveToJSONFile();
                synchronized (MainActivity.this) {
                    MainActivity.this.notifyAll();
                }
            }
        }
    }


    class RVAdapter extends RecyclerView.Adapter<MainActivity.ViewHolder>{

        LayoutInflater mLayoutInflater;

        public RVAdapter(){
            mLayoutInflater=getLayoutInflater();
        }

        @Override
        public MainActivity.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view=mLayoutInflater.inflate(R.layout.wifi_element,parent,false);
            MainActivity.ViewHolder holder=new MainActivity.ViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(MainActivity.ViewHolder holder, int position) {
            holder.addressMAC.setText(content.get(position).getAddressMAC());
            holder.signalStrength.setText(content.get(position).getSignalStrength());
        }

        @Override
        public int getItemCount() {
            return content.size();
        }

        public WiFiElement getItem(int position) {
            return content.get(position);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView addressMAC;
        TextView signalStrength;

        public ViewHolder(View view){
            super(view);

            addressMAC= view.findViewById(R.id.mac_address);
            signalStrength= view.findViewById(R.id.signal_strength);
        }
    }
}
