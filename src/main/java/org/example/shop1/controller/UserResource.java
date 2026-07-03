package org.example.shop1.controller;

import org.example.shop1.model.dto.UserForm;
import org.example.shop1.model.entity.Address; // ایمپورت
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/users")
public class UserResource {

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/admin/all")
    @ResponseBody
    public List<User> getAllUsersForAdmin() {
        return userService.getAllUsers();
    }

    // ... (متدهای لاگین و ساین‌آپ بدون تغییر) ...
    @PostMapping("/signup")
    @ResponseBody
    public ResponseEntity<String> registerUser(@RequestBody UserForm userForm) {
        String result = userService.registerUser(userForm.getUsername(), userForm.getPassword(), Role.USER);
        return result.equals("SUCCESS") ? ResponseEntity.ok("ثبت‌نام با موفقیت انجام شد") : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/api/current-user")
    @ResponseBody
    public Map<String, Object> currentUser(Authentication authentication) {
        System.out.println("in cun user");
        Map<String, Object> userInfo = new HashMap<>();
        try {
            User user = userService.getCurrentAuthenticatedUser();
            userInfo.put("username", user.getUsername());
            userInfo.put("firstName", user.getFirstName());
            userInfo.put("lastName", user.getLastName()); // اضافه کردن نام خانوادگی
            userInfo.put("addresses", user.getAddresses());
            userInfo.put("roles", authentication.getAuthorities().stream().map(Object::toString).toList());
        } catch (Exception e) {
            userInfo.put("username", null); // اگر لاگین نبود نال بفرست
        }
        return userInfo;
    }

    // --- متدهای اصلاح شده آدرس ---

    @GetMapping("/my/addresses")
    @ResponseBody
    public List<Address> getMyAddresses() {
        return userService.getUserAddresses();
    }

    @PostMapping("/my/addresses")
    @ResponseBody
    public User addMyAddress(@RequestBody Address address) {
        return userService.addAddress(address);
    }

    @PutMapping("/my/addresses/{index}")
    @ResponseBody
    public User updateMyAddress(@PathVariable("indexcnd") int index, @RequestBody Address address) {
        return userService.updateAddress(index, address);
    }

    @DeleteMapping("/my/addresses/{index}")
    @ResponseBody
    public ResponseEntity<?> removeMyAddress(@PathVariable("index") int index) {
        userService.removeAddress(index);
        return ResponseEntity.ok(Map.of("message", "آدرس با موفقیت حذف شد"));
    }
}