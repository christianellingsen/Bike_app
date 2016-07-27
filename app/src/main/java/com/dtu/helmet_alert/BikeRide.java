package com.dtu.helmet_alert;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by chris on 13-05-2016.
 */
public class BikeRide {

    // Fields Start time/date, end time/data, average speed, wore helmet, duration, distance?

    private long startTime;         // Time of start of bike ride = System.currentTimeMillis();
    private long endTime;           // Time of end of bike ride
    private long duration;          // Ride duration in minutes
    private double averageSpeed;    //km/t
    private ArrayList<Double> speedHistory;
    private boolean woreHelmetCorrect;
    private double totalDistanceKM;        //km
    String date;                    // Date for ride
    private String durationString;

    private ArrayList<String> violationTimeStamp;
    private ArrayList<String> violationType;

    public BikeRide() {

        this.totalDistanceKM = 0.0;
        this.startTime = System.currentTimeMillis();
        this.endTime = 0;
        this.duration = 0;
        this.averageSpeed = 0.0;
        this.speedHistory = new ArrayList<>();
        this.woreHelmetCorrect = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        this.date = dateFormat.format(new Date());
        this.durationString = "";

        violationTimeStamp = new ArrayList<>();
        violationType = new ArrayList<>();

        //Log.d("Distance","Trip init. Distance: " + totalDistanceKM+ " and speed: " +averageSpeed);

    }



    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getTotalDistanceKM() {
        return totalDistanceKM;
    }

    public void setTotalDistanceKM(double d) {
        this.totalDistanceKM = d;
    }

    @JsonIgnore
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @JsonIgnore
    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @JsonIgnore
    public long getDuration() {

        return endTime - startTime;
    }

    public String getDurationString(){
        long time = getDuration();
        final long hr = TimeUnit.MILLISECONDS.toHours(time);
        final long min = TimeUnit.MILLISECONDS.toMinutes(time - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(time - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        //final long ms = TimeUnit.MILLISECONDS.toMillis(time - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02d:%02d:%02d", hr, min, sec);
    }

    public void setDurationString(String duration){
        this.durationString = duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public double getAverageSpeed() {

        double acc_avg = 0;

        if (speedHistory.size()>0) {
            for (double s : speedHistory) {
                acc_avg = acc_avg + s;
            }
            acc_avg = acc_avg/(double)speedHistory.size();
        }


        return acc_avg;
    }

    public void setAverageSpeed(double averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public ArrayList<Double> getSpeedHistory() {
        return speedHistory;
    }

    public void setSpeedHistory(ArrayList<Double> speedHistory) {
        this.speedHistory = speedHistory;
    }

    public boolean isWoreHelmetCorrect() {
        return woreHelmetCorrect;
    }

    public void setWoreHelmetCorrect(boolean woreHelmetCorrect) {
        this.woreHelmetCorrect = woreHelmetCorrect;
    }

    @JsonIgnore
    public void updateDistance(){
        double dur_ms = (double)getDuration();
        //Log.d("updateDistance","dur_ms: "+dur_ms);
        double dur_s = dur_ms/1000.0;
        //Log.d("updateDistance","dur_s: "+dur_s);
        double avg_speed_kmt = getAverageSpeed();
        //Log.d("updateDistance","avg_speed_kmt: "+avg_speed_kmt);
        double speed_m_s = (avg_speed_kmt/(60.0*60.0))*1000.0;
        //Log.d("updateDistance","speed_m_s: "+speed_m_s);
        double dist_m = (dur_s*speed_m_s);
        //Log.d("updateDistance","dist_m: "+dist_m);
        double dist_km = DecimalUtils.round(dist_m/1000.0, 2);
        //Log.d("updateDistance","dist_km: "+dist_km);

        //if (dist_km>=0.0001){
            this.setTotalDistanceKM(this.getTotalDistanceKM() + dist_km);
        //}

        //Log.d("BluetoothService","Total dist: "+getTotalDistanceKM());

    }

    public static class DecimalUtils {

        public static double round(double value, int numberOfDigitsAfterDecimalPoint) {
            BigDecimal bigDecimal = new BigDecimal(value);
            bigDecimal = bigDecimal.setScale(numberOfDigitsAfterDecimalPoint,
                    BigDecimal.ROUND_HALF_UP);
            return bigDecimal.doubleValue();
        }
    }

    public ArrayList<String> getViolationTimeStamp() {
        return violationTimeStamp;
    }

    public void setViolationTimeStamp(ArrayList<String> violationTimeStamp) {
        this.violationTimeStamp = violationTimeStamp;
    }

    public ArrayList<String> getViolationType() {
        return violationType;
    }

    public void setViolationType(ArrayList<String> violationType) {
        this.violationType = violationType;
    }
}
