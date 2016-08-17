package com.dtu.helmet_alert.friends;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;

/**
 * Created by chris on 13-08-2016.
 */
public class Friend {
        private String firstName;
        private String lastName;
        private String email;
        //private String imageURL;
        private String u_key;
        private boolean isFavorite;


        public Friend() {
            this.u_key = "";
            this.firstName = "";
            this.lastName = "";
            this.email ="";
            //this.imageURL="";
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