package com.example.marta.wifi_detection;

public class WiFiElement {
    private String addressMAC;
    private String signalStrength;

    public WiFiElement(String addressMAC, String signalStrength){
        this.addressMAC=addressMAC;
        this.signalStrength=signalStrength;
    }

    public String getAddressMAC() {
        return addressMAC;
    }

    public void setAddressMAC(String addressMAC) {
        this.addressMAC = addressMAC;
    }

    public String getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(String signalStrength) {
        this.signalStrength = signalStrength;
    }
}
