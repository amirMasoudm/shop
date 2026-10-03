package org.example.shop1.controller;

import org.example.shop1.config.SecurityUtils;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.dto.AdminUserForm;
import org.example.shop1.model.entity.Address; // ایمپورت
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.service.UserService;
import org.springframework.http.HttpStatus;
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

    /**
     * فهرستِ مشتریان برایِ پنلِ فروشِ حضوری — هر چهار نقشِ پنل می‌بینند.
     * <p>
     * 🔴 خروجی از {@link org.example.shop1.model.dto.CustomerSummaryDto} رد می‌شود،
     * پس شمارهٔ تلفن اصلاً در پاسخ نیست. مسیرش هم عمداً زیرِ {@code /admin/} نیست،
     * چون آن شاخه فقط ADMIN است و اینجا کارشناس هم باید ببیند.
     */
    @GetMapping("/panel/customers")
    @ResponseBody
    public List<org.example.shop1.model.dto.CustomerSummaryDto> customersForPanel() {
        return userService.getCustomers().stream()
                .map(org.example.shop1.model.dto.CustomerSummaryDto::of)
                .toList();
    }

    // ===== مدیریتِ کارکنان (ادمین/پشتیبان) =====
    // کلِ /api/users/admin/** در SecurityConfig فقط برای ROLE_ADMIN باز است.

    @GetMapping("/admin/staff")
    @ResponseBody
    public List<User> getStaffUsers() {
        return userService.getStaffUsers();
    }

    @PostMapping("/admin/staff")
    @ResponseBody
    public User createStaffUser(@RequestBody AdminUserForm form) {
        return userService.createStaffUser(form);
    }

    /** ویرایشِ کاملِ کارشناس. رمزِ خالی یعنی «دست نزن». */
    @PutMapping("/admin/staff/{id}")
    @ResponseBody
    public User updateStaffUser(@PathVariable("id") String id, @RequestBody AdminUserForm form) {
        return userService.updateStaffUser(id, form);
    }

    @PutMapping("/admin/staff/{id}/role")
    @ResponseBody
    public User changeUserRole(@PathVariable("id") String id, @RequestBody Map<String, String> payload) {
        Role role;
        try {
            role = Role.valueOf(String.valueOf(payload.get("role")).trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "نقشِ ارسال‌شده نامعتبر است");
        }
        return userService.changeUserRole(id, role);
    }

    @PutMapping("/admin/staff/{id}/password")
    @ResponseBody
    public ResponseEntity<String> resetUserPassword(@PathVariable("id") String id,
                                                    @RequestBody Map<String, String> payload) {
        userService.resetUserPassword(id, payload.get("password"));
        return ResponseEntity.ok("رمز عبور تغییر کرد");
    }

    @DeleteMapping("/admin/staff/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteStaffUser(@PathVariable("id") String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("کاربر حذف شد");
    }

    /**
     * چکِ سبکِ زنده‌بودنِ سشن برایِ پنل‌ها (نگهبانِ سشن در /js/session-watch.js).
     * <p>
     * عمداً هیچ خواندنی از دیتابیس ندارد و بدنه هم برنمی‌گرداند: اگر سشن باطل شده
     * باشد (مثلاً بعد از تغییرِ نقش) خودِ لایه‌ی امنیت ۴۰۱ می‌دهد و همین برایِ
     * بیرون‌انداختنِ کاربر کافی است. هزینه‌اش یک درخواستِ خالی در دقیقه است، آن هم
     * فقط وقتی تبِ کاربر باز و دیده‌شدنی باشد.
     */
    @GetMapping("/api/session-check")
    @ResponseBody
    public ResponseEntity<Void> sessionCheck() {
        return ResponseEntity.noContent().build();
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
            // پنلِ فروش با همین خطاب («آقای/خانم») نامِ کارشناس را بالای صفحه می‌نویسد
            userInfo.put("gender", user.getGender());
            userInfo.put("addresses", user.getAddresses());
            userInfo.put("roles", authentication.getAuthorities().stream().map(Object::toString).toList());
        } catch (Exception e) {
            userInfo.put("username", null); // اگر لاگین نبود نال بفرست
        }
        return userInfo;
    }

    /**
     * ویرایشِ پروفایلِ کاربرِ لاگین‌کرده از پنلِ مشتری.
     * قبلاً فقط GET این مسیر وجود داشت؛ دکمه‌ی «ثبت تغییرات پروفایل» PATCH می‌فرستاد
     * و ۴۰۵ می‌گرفت — چون فرانت‌اند else نداشت، بی‌صدا شکست می‌خورد.
     */
    @PatchMapping("/api/current-user")
    @ResponseBody
    public Map<String, Object> updateCurrentUser(@RequestBody Map<String, String> payload) {
        User updated = userService.updateCurrentUserProfile(
                payload.get("firstName"), payload.get("lastName"));

        Map<String, Object> result = new HashMap<>();
        result.put("username", updated.getUsername());
        result.put("firstName", updated.getFirstName());
        result.put("lastName", updated.getLastName());
        return result;
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
        // پاکسازی فیلدهای متنی برای جلوگیری از XSS
        address.setRecipientName(SecurityUtils.clean(address.getRecipientName()));
        address.setRecipientPhone(SecurityUtils.clean(address.getRecipientPhone()));
        address.setFullAddress(SecurityUtils.clean(address.getFullAddress()));
        address.setPostalCode(SecurityUtils.clean(address.getPostalCode()));
        address.setState(SecurityUtils.clean(address.getState()));
        address.setCity(SecurityUtils.clean(address.getCity()));

        // فیلدهای Double (Latitude/Longitude) نیاز به پاکسازی ندارند
        return userService.addAddress(address);
    }


    @PutMapping("/my/addresses/{index}")
    @ResponseBody
    public User updateMyAddress(@PathVariable("index") int index, @RequestBody Address address) {
        // پاکسازی فیلدها قبل از آپدیت
        address.setRecipientName(SecurityUtils.clean(address.getRecipientName()));
        address.setRecipientPhone(SecurityUtils.clean(address.getRecipientPhone()));
        address.setFullAddress(SecurityUtils.clean(address.getFullAddress()));
        address.setPostalCode(SecurityUtils.clean(address.getPostalCode()));
        address.setState(SecurityUtils.clean(address.getState()));
        address.setCity(SecurityUtils.clean(address.getCity()));

        return userService.updateAddress(index, address);
    }

    @DeleteMapping("/my/addresses/{index}")
    @ResponseBody
    public ResponseEntity<?> removeMyAddress(@PathVariable("index") int index) {
        userService.removeAddress(index);
        return ResponseEntity.ok(Map.of("message", "آدرس با موفقیت حذف شد"));
    }
}