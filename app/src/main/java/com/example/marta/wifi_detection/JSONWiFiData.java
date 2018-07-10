package com.example.marta.wifi_detection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

/**
 * This class links the list of scanned WiFis (their MAC addresses and their signal strength)
 * with the specific point taken from the DisplacementCollector, as a point in which the measurement
 * was taken (x and y coordinate on the floor).
 * Used to store data used to save collected data as JSON object and
 * convert everything properly into a JSONObject.
 */
public class JSONWiFiData extends JSONObject{

    /**
     * String tags for the values of an JSON object.
     */
    public static final String X_COORD_TAG="x_coord";
    public static final String Y_COORD_TAG="y_coord";
    public static final String MEASUREMENTS_TAG="measurements";
    public static final String MAC_TAG="MAC";
    public static final String SIGNAL_TAG="signal";


    private float x_coord;
    private float y_coord;
    /**
     * An ArrayList containing instances of WiFiElement class taken in the specific x,y point.
     */
    private ArrayList<WiFiElement> measurements;

    /**
     * Constructor creating an instance of the JSONWiFiData class.
     * @param x_coord Float x coordinate of a place in which the measurement was taken.
     * @param y_coord Float y coordinate of a place in which the measurement was taken.
     * @param measurements List of all WiFi signals scanned in this point.
     */
    public JSONWiFiData(float x_coord, float y_coord, ArrayList<WiFiElement> measurements) {
        this.x_coord = x_coord;
        this.y_coord = y_coord;
        this.measurements = measurements;
    }

    /**
     * From the data collected creates a JSON object.
     * @return Returns a JSON object created from the data set and collected before.
     */
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
