package com.dtu.helmet_alert;


import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;

/**
 * Created by chris on 27-04-2016.
 */
public class User {

    private String u_ID;
    private String firstName;
    private String lastName;
    private String email;
    private String provider;
    //private String imageURL;
    private ArrayList<String> storedTournamentsID;
    private ArrayList<String> storedPlayers;
    private String u_key;

    public User() {
        this.u_ID = "";
        this.u_key = "";
        this.firstName = "";
        this.lastName = "";
        this.email ="";
        this.provider="";
        //this.imageURL="";
        this.storedTournamentsID = new ArrayList<>();
        this.storedPlayers = new ArrayList<>();
    }

    public ArrayList<String> getStoredPlayers() {
        return storedPlayers;
    }

    public void setStoredPlayers(ArrayList<String> storedPlayers) {
        this.storedPlayers = storedPlayers;
    }

    public ArrayList<String> getStoredTournamentsID() {
        return storedTournamentsID;
    }

    public void setStoredTournamentsID(ArrayList<String> storedTournamentsID) {
        this.storedTournamentsID = storedTournamentsID;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getU_ID() {
        return u_ID;
    }

    public void setU_ID(String u_ID) {
        this.u_ID = u_ID;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @JsonIgnore
    public String getFullName(){
        return this.firstName + " " + this.lastName;
    }

    public void setU_key(String key) {
        this.u_key = key;
    }

    public String getU_key() {
        return u_key;
    }
}
