package com.example.lightningdriver.tools;

import com.google.android.gms.maps.model.LatLng;

public class DecodeTool {
    public static LatLng getLatLngFromString(String location) {
        LatLng latLng;

        String tempString = location.substring(location.indexOf("(")+1, location.indexOf(")"));
        String[] arrOfStr = tempString.split(",", 2);

        latLng = new LatLng(Double.parseDouble(arrOfStr[0]), Double.parseDouble(arrOfStr[1]));

        return latLng;
    }
}
