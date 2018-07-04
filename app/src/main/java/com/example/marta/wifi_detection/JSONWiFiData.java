package com.example.marta.wifi_detection;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.HashMap;


public class JSONWiFiData extends AsyncTask<Void, Void, Void> {

    private static String url = "https://drive.google.com/drive/u/0/folders/1-aymI8CT2GHKlSc2MmqaCqqeLxlJDCT0";
    private String TAG = MainActivity.class.getSimpleName();


    public static final String X_COORD_TAG="x_coord";
    public static final String Y_COORD_TAG="y_coord";
    public static final String MEASUREMENTS_TAG="measurements";
    public static final String MAC_TAG="MAC";
    public static final String SIGNAL_TAG="signal";


    ArrayList<HashMap<String, String>> studentList;
    ProgressDialog pDialog;

    @Override
    protected Void doInBackground(Void... voids) {
        return null;
    }
}
