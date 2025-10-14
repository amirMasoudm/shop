package org.example.shop1.controller;



import org.example.shop1.model.dto.UserForm;
import org.example.shop1.model.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/users")
public class UserResource {

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }


    // =======================
    // فرم Signup
    // =======================
//    @GetMapping("/signup")
//    public String showSignupForm(Model model) {
//        model.addAttribute("userForm", new UserForm());
//        return "userAuth";
//    }
    @PostMapping("/signup")
    public ResponseEntity<String> registerUser(@RequestBody UserForm userForm) {
        String result = userService.registerUser(
                userForm.getUsername(),
                userForm.getPassword(),
                userForm.getRole()
        );

        if (!result.equals("SUCCESS")) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok("User registered successfully");
    }
//    @PostMapping("/signup")
//    public String processSignup(@ModelAttribute("userForm") UserForm userForm, Model model) {
//        System.out.println("in signuppppppppppppppppppppp");
//        String result = userService.registerUser(userForm.getUsername(), userForm.getPassword(), userForm.getRole());
//        if (!result.equals("SUCCESS")) {
//            model.addAttribute("errorMessage", result);
//            return "userAuth";
//        }
//        model.addAttribute("successMessage", "User registered successfully! You can login now.");
//        return "login";
//    }

    // =======================
    // فرم Login
    // =======================
    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out");
        }
        return "login";
    }

    // =======================
    // Home بعد از Login
    // =======================

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
//        model.addAttribute("username", authentication.getName());
        return "redirect:/home.html";
    }
    @GetMapping("/api/current-user")
    @ResponseBody
    public Map<String, Object> currentUser(Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();
        if (authentication != null) {
            userInfo.put("username", authentication.getName());
            userInfo.put("roles", authentication.getAuthorities()
                    .stream()
                    .map(a -> a.getAuthority())
                    .toList());
        } else {
            userInfo.put("username", "ناشناس");
            userInfo.put("roles", List.of("بدون نقش"));
        }
        return userInfo;
    }

}
