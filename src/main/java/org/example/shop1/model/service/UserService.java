package org.example.shop1.model.service;

import org.example.shop1.model.entity.Address;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    // دریافت همه کاربران برای پنل ادمین
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    // متد کمکی برای دریافت کاربر فعلی از سشن
    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new RuntimeException("کاربر احراز هویت نشده است.");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("کاربر در پایگاه داده یافت نشد."));
    }

    public String registerUser(String username, String rawPassword, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return "Username already exists!";
        }
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(username, hashedPassword, role);
        userRepository.save(user);
        return "SUCCESS";
    }

    public List<Address> getUserAddresses() {
        return getCurrentAuthenticatedUser().getAddresses();
    }

    // تغییر ورودی به Address
    public User addAddress(Address newAddress) {
        User user = getCurrentAuthenticatedUser();
        if (user.getAddresses() == null) {
            user.setAddresses(new java.util.ArrayList<>());
        }
        user.getAddresses().add(newAddress);
        return userRepository.save(user);
    }

    public User updateAddress(int index, Address updatedAddress) {
        User user = getCurrentAuthenticatedUser();
        List<Address> addresses = user.getAddresses(); // اصلاح نوع لیست

        if (addresses == null || index < 0 || index >= addresses.size()) {
            throw new RuntimeException("ایندکس آدرس نامعتبر است");
        }
        addresses.set(index, updatedAddress);
        return userRepository.save(user);
    }

    public void removeAddress(int index) {
        User user = getCurrentAuthenticatedUser();
        List<Address> addresses = user.getAddresses(); // اصلاح نوع لیست

        if (addresses == null || index < 0 || index >= addresses.size()) {
            throw new RuntimeException("ایندکس آدرس نامعتبر است");
        }
        addresses.remove(index);
        userRepository.save(user);
    }
}