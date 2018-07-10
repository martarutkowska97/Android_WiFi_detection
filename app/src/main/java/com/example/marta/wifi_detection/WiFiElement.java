package com.example.marta.wifi_detection;

/**
 * A class holding data about one of the WiFi scanned
 */
public class WiFiElement {

    private String addressMAC;
    private String signalStrength;

    /**
     * Creates an instance of WiFiElement class, sets the MAC address and value of signal strength
     * @param addressMAC value of MAC address
     * @param signalStrength value of signal strength of WiFi
     */
    public WiFiElement(String addressMAC, String signalStrength){
        this.addressMAC=addressMAC;
        this.signalStrength=signalStrength;
    }

    /**
     * @return Returns the value of MAC address as String
     */
    public String getAddressMAC() {
        return addressMAC;
    }

    /**
     * @return Returns the value of signal strength as String
     */
    public String getSignalStrength() {
        return signalStrength;
    }

}
