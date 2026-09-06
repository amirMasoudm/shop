package org.example.shop1.model.service;

import org.example.shop1.config.SecurityUtils;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.dto.AdminUserForm;
import org.example.shop1.model.entity.Address;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    /** حداقل طولِ رمز برای حساب‌های کارکنان. */
    private static final int MIN_PASSWORD_LENGTH = 8;

    /** نامِ پیش‌فرضِ کالکشنِ spring-session-data-mongodb (در تنظیمات بازنویسی نشده). */
    private static final String SESSION_COLLECTION = "sessions";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoOperations mongoOperations;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       MongoOperations mongoOperations) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoOperations = mongoOperations;
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

    /**
     * ویرایشِ پروفایلِ خودِ کاربرِ لاگین‌کرده (نام و نام خانوادگی).
     * فقط همین دو فیلد؛ نقش/شماره/رمز از این مسیر قابلِ تغییر نیستند.
     */
    public User updateCurrentUserProfile(String firstName, String lastName) {
        User user = getCurrentAuthenticatedUser();

        String cleanFirst = SecurityUtils.clean(firstName);
        if (cleanFirst == null || cleanFirst.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "نام نمی‌تواند خالی باشد");
        }
        user.setFirstName(cleanFirst.trim());
        // نام خانوادگی اختیاری است؛ خالی‌بودنش خطا نیست
        String cleanLast = SecurityUtils.clean(lastName);
        user.setLastName(cleanLast == null ? "" : cleanLast.trim());

        return userRepository.save(user);
    }

    // ==========================================================
    // مدیریتِ کاربرانِ کارکنان (ادمین/پشتیبان) از پنل
    // دسترسی در SecurityConfig روی /api/users/admin/** محدود شده است.
    // توجه: @EnableMethodSecurity فعال نیست، پس @PreAuthorize اینجا بی‌اثر
    // خواهد بود — تکیه فقط روی قاعدهٔ مسیر است.
    // ==========================================================

    /** ساختِ حسابِ ادمین/پشتیبانِ جدید. رمز با همان انکودرِ اپ هش می‌شود. */
    public User createStaffUser(AdminUserForm form) {
        String username = trimOrEmpty(form.getUsername());
        String phone = trimOrEmpty(form.getPhoneNumber());
        Role role = form.getRole() == null ? Role.ADMIN : form.getRole();

        if (username.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "نام کاربری الزامی است");
        }
        if (form.getPassword() == null || form.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "رمز عبور باید حداقل " + MIN_PASSWORD_LENGTH + " کاراکتر باشد");
        }
        // ورودِ پنل دو مرحله‌ای است و کد تایید پیامک می‌شود؛ بدون شماره اصلاً نمی‌تواند وارد شود.
        // این برایِ هر سه نقشِ کارکنان صدق می‌کند، نه فقط ادمین — قبلاً فقط ADMIN چک
        // می‌شد و کارشناسِ بدونِ شماره موقعِ ورود با خطای نامفهومِ «شماره معتبر نیست» گیر می‌کرد.
        if ((role == Role.ADMIN || role == Role.PRICER || role == Role.SALES || role == Role.SUPPORT) && phone.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "برای این نقش شماره موبایل الزامی است، چون کد ورود پیامک می‌شود");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "این نام کاربری قبلاً ثبت شده است");
        }
        if (!phone.isEmpty() && userRepository.existsByPhoneNumber(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "این شماره موبایل قبلاً ثبت شده است");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setPhoneNumber(phone.isEmpty() ? null : phone);
        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());
        user.setRole(role);
        user.setAddresses(new ArrayList<>());

        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException e) {
            // فاصلهٔ بینِ چکِ بالا و save؛ ایندکسِ unique جلویش را گرفت.
            throw new ApiException(HttpStatus.CONFLICT,
                    "این نام کاربری یا شماره موبایل هم‌زمان توسط کسِ دیگری ثبت شد");
        }
    }

    /** تغییرِ نقشِ یک کاربر، با محافظت در برابر قفل‌شدنِ بیرونِ پنل. */
    public User changeUserRole(String userId, Role newRole) {
        if (newRole == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "نقشِ ارسال‌شده نامعتبر است");
        }
        User target = requireUser(userId);

        if (isCurrentUser(target) && newRole != Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "نمی‌توانید نقشِ خودتان را از ادمین بردارید");
        }
        if (target.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            ensureNotLastAdmin();
        }
        if (newRole == Role.ADMIN && trimOrEmpty(target.getPhoneNumber()).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "این کاربر شماره موبایل ندارد و نمی‌تواند کد ورود دریافت کند");
        }

        target.setRole(newRole);
        User saved = userRepository.save(target);
        // نقشِ کاربر تازه عوض شد؛ اگر سشنِ فعالی با نقشِ قدیمی روی مرورگرش باز
        // مانده، همان‌جا باطلش کن تا مجبور شود دوباره لاگین کند و نقشِ تازه از
        // سرور خوانده شود — بدونِ این، تا لاگ‌اوتِ دستی، نقشِ کهنه اعمال می‌ماند.
        invalidateSessionsFor(saved.getUsername());
        return saved;
    }

    /**
     * سشن‌هایِ این پروژه در مانگو ذخیره می‌شوند (spring-session-data-mongodb)، نه
     * در حافظه‌یِ تامکت؛ برایِ همین با ری‌استارتِ بک‌اند هم زنده می‌مانند. پس
     * باطل‌کردنشان باید از همان مخزن انجام شود — SessionRegistryِ درون‌حافظه‌ای
     * اینجا بی‌اثر است (با هر ری‌استارت خالی می‌شود و سشنِ مانگو دست‌نخورده می‌ماند).
     * <p>
     * عمداً حذفِ مستقیم است، نه {@code findByPrincipalName}: آن متد هر سندِ سشن را
     * دیسریالایز می‌کند و یک سندِ خرابِ قدیمی کلِ تغییرِ نقش را با ۵۰۰ می‌انداخت.
     * این کوئری همهٔ سشن‌هایِ کاربر را روی هر دستگاهی که باشد یک‌جا پاک می‌کند.
     */
    private void invalidateSessionsFor(String username) {
        mongoOperations.remove(
                Query.query(Criteria.where("principal").is(username)),
                SESSION_COLLECTION);
    }

    /** بازنشانیِ رمزِ یک کاربر توسط ادمین. */
    public void resetUserPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "رمز عبور باید حداقل " + MIN_PASSWORD_LENGTH + " کاراکتر باشد");
        }
        User target = requireUser(userId);
        target.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(target);
    }

    /** حذفِ کاربر، با محافظت در برابر حذفِ خود و حذفِ آخرین ادمین. */
    public void deleteUser(String userId) {
        User target = requireUser(userId);

        if (isCurrentUser(target)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "نمی‌توانید حسابِ خودتان را حذف کنید");
        }
        if (target.getRole() == Role.ADMIN) {
            ensureNotLastAdmin();
        }
        userRepository.delete(target);
    }

    /** فقط کاربرانِ دارای نقشِ کارکنان (برای تبِ مدیریتِ ادمین‌ها). */
    public List<User> getStaffUsers() {
        List<User> staff = new ArrayList<>(userRepository.findByRole(Role.ADMIN));
        staff.addAll(userRepository.findByRole(Role.PRICER));
        staff.addAll(userRepository.findByRole(Role.SALES));
        staff.addAll(userRepository.findByRole(Role.SUPPORT));
        return staff;
    }

    private User requireUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "کاربر یافت نشد"));
    }

    private boolean isCurrentUser(User target) {
        User current = getCurrentAuthenticatedUser();
        return current.getId() != null && current.getId().equals(target.getId());
    }

    private void ensureNotLastAdmin() {
        if (userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "این تنها ادمینِ باقی‌مانده است؛ اول یک ادمینِ دیگر بسازید");
        }
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
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