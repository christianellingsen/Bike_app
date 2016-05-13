package com.dtu.susie_app2;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chris on 13-05-2016.
 */
public class BikeRide {

    // Fields Start time/date, end time/data, average speed, wore helmet, duration, distance?

    private long startTime;         // Time of start of bike ride = System.currentTimeMillis();
    private long endTime;           // Time of end of bike ride
    private long duration;          // Ride duration in minutes
    private double averageSpeed;    //km/t
    private double lastSpeed;       //km/t
    private boolean woreHelmetCorrect;
    private double distance;        //km
    String date;                    // Date for ride

    public BikeRide() {

        this.distance = 0;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.duration = 0;
        this.averageSpeed = 0;
        this.lastSpeed = 0;
        this.woreHelmetCorrect = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        this.date = dateFormat.format(new Date());
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getDistance() {
        distance = (getDuration()*60)*averageSpeed;
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        long seconds = (endTime - startTime) / 1000;
        duration = seconds*60;
        // returns duration in minutes
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public double getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public double getLastSpeed() {
        return lastSpeed;
    }

    public void setLastSpeed(double lastSpeed) {

        if (averageSpeed==0.0){
            averageSpeed=lastSpeed;
        }
        else {
            averageSpeed = (averageSpeed+lastSpeed)/2;
        }

        this.lastSpeed = lastSpeed;
    }

    public boolean isWoreHelmetCorrect() {
        return woreHelmetCorrect;
    }

    public void setWoreHelmetCorrect(boolean woreHelmetCorrect) {
        this.woreHelmetCorrect = woreHelmetCorrect;
    }


}
