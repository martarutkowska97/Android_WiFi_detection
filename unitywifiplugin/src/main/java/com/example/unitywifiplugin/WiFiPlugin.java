package com.example.unitywifiplugin;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.unity3d.player.UnityPlayer;



public class WiFiPlugin {

    public static final int NUMBER_OF_SCANS = 3;
    public static final String FILE_COUNTER_EXTENSION="fileCounter.bin";


    private static WifiManager wifiManager;
    private static WifiScanReceiver wifiScanReceiver;
    private static int fileCounter;
    private static ArrayList<JSONObject> dataCollected;
    private static ArrayList<WiFiElement> lastScan;
    private static ArrayList<WiFiElement> currentScan;
    static final Context context=UnityPlayer.currentActivity;


    public static void initialize(){
        //checkAllPermissions();
        Log.e("initialize","!!!!!!!! INICJALIZUJE");

        wifiManager= (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager!=null){
            Log.e("initialize","!!!!!!!! UDALO SIE ZAINICJALZOWAC WIFIMANAGER");

        }

        wifiScanReceiver = WifiScanReceiver.getInstance();

        //TODO: CZY TU REJSETROWAĆ TEN LISTENER??????
        registerListener();
        lastScan=new ArrayList<>();
        currentScan = new ArrayList<>();
        fileCounter = readFileCounterFromFile();
        dataCollected=new ArrayList<>();
        checkAllPermissions();



    }

    private static void checkAllPermissions(){
        Log.e("checkAllPermissions","!!!!!!!! SPRAWDZAM PERMISSIONS");

        //TODO: pojedyncze ify pytaj o jedne
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},0);
        }
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
        }
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[] {Manifest.permission.CHANGE_WIFI_STATE},0);
        }
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[] {Manifest.permission.ACCESS_WIFI_STATE},0);
        }
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},0);
        }
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UnityPlayer.currentActivity, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},0);
        }
    }

    public static void takeMeasurement(final float x_coord, final float y_coord){

        Log.e("takeMeasurement","!!!!!!!!ROBIE POMIAR");
        wifiManager.startScan();
        lastScan = returnAllWiFis();
        JSONWiFiData jsonWiFiData = new JSONWiFiData(x_coord, y_coord, lastScan);
        dataCollected.add(jsonWiFiData.makeJSONObject());

    }


    public static void registerListener(){
        context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    public static void unregisterListener(){
        context.unregisterReceiver(wifiScanReceiver);
    }

    public static void finishAndSave() {

        Log.e("finishAndSave","!!!!!1 KOŃCZĘ I ZAPISUJĘ");

        //unregisterListener();

        JSONArray arr= new JSONArray();
        for(int i=0; i<dataCollected.size();i++){
            arr.put(dataCollected.get(i));
        }

        try (FileWriter file = new FileWriter(Environment.getExternalStorageDirectory()+"/plikJSON"+fileCounter+".json")) {
            file.write(arr.toString());
            file.flush();

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try (FileWriter file = new FileWriter(Environment.getExternalStorageDirectory()+"/plikTxt"+fileCounter+".txt")) {
            fileCounter++;
            file.write(arr.toString());
            file.flush();

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream fos;
        ObjectOutputStream oos;
        try
        {
            fos = context.openFileOutput(FILE_COUNTER_EXTENSION, Context.MODE_PRIVATE);
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


    private static int readFileCounterFromFile(){
        Log.e("read from file","!!!!1 WCZYTUJE Z PLIKU");

        int f=0;
        File fileDir=context.getFilesDir();
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
            fis=context.openFileInput(counterFile);
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

    private static boolean areArrayListsEqual(ArrayList<WiFiElement> arrayList1, ArrayList<WiFiElement> arrayList2){
        if(arrayList1.size()!=arrayList2.size()){
            return false;
        }
        else{
            for(int i=0;i<arrayList1.size();i++){
                if(arrayList1.get(i).getAddressMAC()!=arrayList2.get(i).getAddressMAC()||arrayList1.get(i).getSignalStrength()!=arrayList2.get(i).getSignalStrength()){
                    return false;
                }
            }

        }
        return true;
    }


    private static ArrayList<WiFiElement> returnAllWiFis(){
        Log.e("return all wifis","!!!!!!111 ZWRACAM WSZYSTKIE WIFI");

        ArrayList<WiFiElement> content = new ArrayList<>();
        List<ScanResult> scanResults = wifiManager.getScanResults();

        Log.e("scan size","!!!!!!111 "+Integer.toString(scanResults.size()));
        for(ScanResult s: scanResults){
            Log.e("result wifi1","!!!!!!111 "+s.BSSID+"strength"+Integer.toString(s.level));
            content.add(new WiFiElement(s.BSSID, Integer.toString(s.level)));
        }
        return content;
    }


    public static boolean isWiFiScannerReady(){
        Log.e("synchronized scan", "!!!!1  SPRAWDZAM STATUS");

        if(areArrayListsEqual(lastScan, currentScan)){
            Log.e("skaner: ","!!!!!!1 SKANER NIEGOTOWY");
            return false;
        }
        else if(lastScan.isEmpty()){
            Log.e("skaner: ","!!!!!!1 SKANER GOTOWY");
            return true;
        }
        else if(currentScan.isEmpty()){
            Log.e("skaner: ","!!!!!!1 SKANER GOTOWY");
            return true;
        }
        Log.e("skaner: ","!!!!!!1 SKANER GOTOWY");
        return true;
    }

    static class WifiScanReceiver extends BroadcastReceiver {

        private static WifiScanReceiver instance;

        public static WifiScanReceiver getInstance(){
            Log.e("getInstance","!!!!!!1 GET INSTANCE OF BROADCAST RECEIVER");

            if(instance==null){
                instance = new WifiScanReceiver();
            }
            return instance;
        }

        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

                Log.e("onReceive","!!!11 ON RECEIVE");
                currentScan = returnAllWiFis();
            }
        }

    }



}
