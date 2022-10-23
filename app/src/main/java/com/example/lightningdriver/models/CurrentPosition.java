package com.example.lightningdriver.models;

public class CurrentPosition {
    private String driverId;
    private String position;
    private String time;

    public CurrentPosition(String driverId, String position, String time) {
        this.driverId = driverId;
        this.position = position;
        this.time = time;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "CurrentPosition{" +
                "driverId='" + driverId + '\'' +
                ", position='" + position + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
