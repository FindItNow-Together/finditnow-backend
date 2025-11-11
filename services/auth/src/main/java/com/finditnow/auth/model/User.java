package com.finditnow.auth.model;

public class User {
    private String id;
    private String userName;
    private String email;
    private String phoneNo;
    private String passwordHash;

    public User() {

    }

    public User(String id, String userName, String email, String phoneNo, String passwordHash) {
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.phoneNo = phoneNo;
        this.passwordHash = passwordHash;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUsername(String username) {
        this.userName = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}

