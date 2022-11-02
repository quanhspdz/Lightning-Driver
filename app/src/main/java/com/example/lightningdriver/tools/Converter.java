package com.example.lightningdriver.tools;

public class Converter {
    public static float fromMPerSToKmPerHour(float speed) {
        float returnSpeed;
        returnSpeed = (float) (speed * 3.6);

        return returnSpeed;
    }
}
