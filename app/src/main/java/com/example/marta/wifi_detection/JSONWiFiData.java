package com.example.marta.wifi_detection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;


public class JSONWiFiData extends JSONObject{

    public static final String X_COORD_TAG="x_coord";
    public static final String Y_COORD_TAG="y_coord";
    public static final String MEASUREMENTS_TAG="measurements";
    public static final String MAC_TAG="MAC";
    public static final String SIGNAL_TAG="signal";


    private int x_coord;
    private int y_coord;
    private ArrayList<WiFiElement> measurements;

    public JSONWiFiData(int x_coord, int y_coord, ArrayList<WiFiElement> measurements) {
        this.x_coord = x_coord;
        this.y_coord = y_coord;
        this.measurements = measurements;
    }

    public JSONObject makeJSONObject(){
        JSONObject obj = new JSONObject();

        try {
            obj.put(X_COORD_TAG, x_coord);
            obj.put(Y_COORD_TAG, y_coord);

            JSONArray array= new JSONArray();
            for(WiFiElement w: measurements){
                JSONObject measurement=new JSONObject();
                measurement.put(MAC_TAG,w.getAddressMAC());
                measurement.put(SIGNAL_TAG, w.getSignalStrength());
                array.put(measurement);
            }
            obj.put(MEASUREMENTS_TAG, array);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }


}
