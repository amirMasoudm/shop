package org.example.shop1.model.dto;

import org.example.shop1.model.enums.Role;

/** ورودیِ ساختِ کاربرِ کارکنان (ادمین/پشتیبان) از پنل. */
public class AdminUserForm {

    private String username;
    private String password;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private Role role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
