package org.example.shop1.model.entity;

import org.example.shop1.model.enums.Role;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private Role role = Role.USER;
    @Indexed(unique = true)
    private String phoneNumber;

    // فیلدهای منتقل شده از Customer
//    private List<String> addresses = new ArrayList<>();
    private Instant createdAt = Instant.now();
    // در کلاس User.java
    // تغییر از List<String> به List<Address>
    private List<Address> addresses = new ArrayList<>();

    // اصلاح Getter و Setter


    public User() {}
// در کلاس User این بخش را اضافه یا اصلاح کنید:

    public User(String phoneNumber, Role role) {

        this.phoneNumber = phoneNumber;
        this.username = phoneNumber;
        this.role = role;
        this.password = null;
        this.addresses = new ArrayList<>();

    }
    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters

    public List<Address> getAddresses() { return addresses; }
    public void setAddresses(List<Address> addresses) { this.addresses = addresses; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }


    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}