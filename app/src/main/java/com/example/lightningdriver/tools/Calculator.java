package com.example.lightningdriver.tools;

import android.content.Context;
import android.widget.Toast;

import com.example.lightningdriver.models.Trip;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class Calculator {
    public static String calculateTotalMoney(List<Trip> listTrips, Context context) {
        double totalMoney = 0;
        for (int i = 0; i < listTrips.size(); i++) {
            Trip trip = listTrips.get(i);
            String tempStr = trip.getCost().substring(0, trip.getCost().indexOf("₫") - 1);
            tempStr = tempStr.replaceAll("\\.", "");
            tempStr = tempStr.replaceAll(",", ".");
            tempStr = tempStr.trim();
            double money = Double.parseDouble(tempStr);
            totalMoney += money;
        }

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return nf.format(totalMoney);
    }

    public static String getShortTime(String fullTime) {

        String dayMonth = fullTime.substring(0, fullTime.indexOf("GMT") - 2);
        dayMonth = dayMonth.substring(0, dayMonth.lastIndexOf(" "));
        String year = fullTime.substring(fullTime.lastIndexOf(" "), fullTime.length());

        return dayMonth + year;
    }

    public static String getDurationFromTime(String pickUpTime, String dropOffTime) {
        pickUpTime = pickUpTime.substring(0, pickUpTime.indexOf("GMT") - 1);
        dropOffTime = dropOffTime.substring(0, dropOffTime.indexOf("GMT") - 1);

        pickUpTime = pickUpTime.substring(pickUpTime.lastIndexOf(" ") + 1, pickUpTime.lastIndexOf(":"));
        dropOffTime = dropOffTime.substring(dropOffTime.lastIndexOf(" ") + 1, dropOffTime.lastIndexOf(":"));

        return pickUpTime + " - " + dropOffTime;
    }
}
