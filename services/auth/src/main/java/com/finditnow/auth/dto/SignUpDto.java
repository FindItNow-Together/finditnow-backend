package com.finditnow.auth.dto;

import java.io.Serializable;

public class SignUpDto implements Serializable {
    String email;
    String password;
    String firstName;
    String phone;
    String role;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    public boolean isValid() {
        return (email != null || phone != null) && password != null && firstName != null;
    }
}
