package com.example.oteptudlong.irespondecitizen;

public class Citizen {

    private String phoneNumber, uid;

    public Citizen() {}

    public Citizen(String phoneNumber, String uid) {
        this.phoneNumber = phoneNumber;
        this.uid = uid;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
