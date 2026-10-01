package org.example.shop1.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // توجه: ساختِ واقعیِ این ایندکس‌ها با MongoIndexInitializer انجام می‌شود، نه با
    // spring.data.mongodb.auto-index-creation (که عمداً خاموش است تا اول تکراری‌ها بررسی شوند).
    // اگر این تعریف‌ها را عوض کردی، همان‌جا هم عوضشان کن وگرنه ساختِ ایندکس با خطای
    // IndexOptionsConflict شکست می‌خورد.
    @Indexed(name = "uk_users_username", unique = true,
             partialFilter = "{ 'username': { '$type': 'string' } }")
    private String username;

    // هشِ رمز هرگز در پاسخِ JSON بیرون نرود (مثلاً /api/users/admin/all).
    // WRITE_ONLY یعنی خواندن از JSON مجاز است ولی نوشتن در JSON نه.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String firstName;
    private String lastName;
    private String email;

    /** نامِ شرکت یا سازمانِ کاربر — اختیاری. اولین بار از ثبت‌نامِ کلاینت‌های اپ پر شد. */
    private String organization;
    private Role role = Role.USER;

    /**
     * جنسیتِ کارشناس — فقط برایِ خطابِ «آقای/خانم» در چت و بالای پنل.
     * <p>
     * {@code null} یعنی مشخص نشده؛ آن وقت فقط نام نوشته می‌شود، بدونِ پیشوند.
     * برای مشتری اصلاً پرسیده نمی‌شود و هیچ تصمیمی به آن وابسته نیست.
     */
    private org.example.shop1.model.enums.Gender gender;
    // partialFilter لازم است: همهٔ کاربران شماره ندارند (مثلاً حسابِ پشتیبان که از پنل
    // بدونِ موبایل ساخته می‌شود). مونگو در ایندکسِ unique همهٔ رکوردهای بدون‌مقدار را
    // «یک nullِ واحد» حساب می‌کند، پس بدونِ این فیلتر دومین کاربرِ بی‌شماره رد می‌شد.
    // sparse کافی نیست — آن فقط فیلدِ غایب را کنار می‌گذارد، nullِ صریح را نه.
    @Indexed(name = "uk_users_phoneNumber", unique = true,
             partialFilter = "{ 'phoneNumber': { '$type': 'string' } }")
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

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public org.example.shop1.model.enums.Gender getGender() { return gender; }
    public void setGender(org.example.shop1.model.enums.Gender gender) { this.gender = gender; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }


    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}