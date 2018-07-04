package com.example.marta.wifi_detection;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.constraint.solver.widgets.WidgetContainer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    RecyclerView recyclerView;
    ArrayList<WiFiElement> content;
    RVAdapter adapter;

    WifiManager wifiManager;
    //WifiScanReceiver wifiScanReceiver;
    Button scan;



    public static final int NUMBER_OF_SCANS = 3;
    public static final int SCANNING_TIME = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recycler_view);
        content=new ArrayList<>();
        adapter = new RVAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


       // wifiScanReceiver = new WifiScanReceiver();

        //registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        scan= findViewById(R.id.button_scan);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                for(int i=0; i<NUMBER_OF_SCANS;i++){
                    long startTime = System.currentTimeMillis();

                    //TODO: DOPRACOWAĆ CO JAKI CZAS
//                    while((System.currentTimeMillis()-startTime)<SCANNING_TIME){
//
//                    }
                    Toast.makeText(getApplicationContext(),"SAVED "+(i+1), Toast.LENGTH_SHORT).show();
                    returnAllWifis();

                }
            }
        });
        wifiManager.startScan();
    }

    private void returnAllWifis(){
        content.clear();
        List<ScanResult> scanResults = wifiManager.getScanResults();

        for(ScanResult s: scanResults){
            content.add(new WiFiElement(Integer.toString(s.level), s.BSSID));
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        //unregisterReceiver(wifiScanReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        //registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }

    class WifiScanReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                content.clear();
                List<ScanResult> scanResults = wifiManager.getScanResults();

                Toast.makeText(getApplicationContext(),"SCANNING", Toast.LENGTH_SHORT).show();

                for(ScanResult s: scanResults){
                    content.add(new WiFiElement(Integer.toString(s.level), s.BSSID));
                    adapter.notifyDataSetChanged();
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
