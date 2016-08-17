package com.dtu.helmet_alert.friends;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by chris on 17-08-2016.
 */
public class Notification {


    private String fromU_ID, toU_ID, message, date;
    boolean isRead;

    public Notification() {
        fromU_ID="";
        toU_ID="";
        message="";
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        date = dateFormat.format(new Date());
        isRead=false;
    }

    public String getFromU_ID() {
        return fromU_ID;
    }

    public void setFromU_ID(String fromU_ID) {
        this.fromU_ID = fromU_ID;
    }

    public String getToU_ID() {
        return toU_ID;
    }

    public void setToU_ID(String toU_ID) {
        this.toU_ID = toU_ID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
