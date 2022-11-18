package com.example.lightningdriver.tools;

import com.example.lightningdriver.models.Trip;

import java.util.List;

public class Calculator {
    public static String calculateTotalMoney(List<Trip> listTrips) {
        double totalMoney = 0;
        for (int i = 0; i < listTrips.size(); i++) {
            Trip trip = listTrips.get(i);
            String tempStr = trip.getCost().substring(0, trip.getCost().indexOf(" ") - 1);
            tempStr = tempStr.replaceAll(",", "");
            double money = Double.parseDouble(tempStr);
            totalMoney += money;
        }

        return String.valueOf(totalMoney);
    }

    public static String getShortTime(String fullTime) {

        String dayMonth = fullTime.substring(0, fullTime.indexOf("GMT") - 2);
        dayMonth = dayMonth.substring(0, dayMonth.lastIndexOf(" "));
        String year = fullTime.substring(fullTime.lastIndexOf(" "), fullTime.length());

        return dayMonth + year;
    }
}
