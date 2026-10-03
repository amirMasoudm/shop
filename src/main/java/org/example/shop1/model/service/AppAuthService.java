package org.example.shop1.model.service;

import org.example.shop1.config.SecurityUtils;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.exeption.AppApiException;
import org.example.shop1.model.entity.AppPendingRegistration;
import org.example.shop1.model.entity.AppRegistration;
import org.example.shop1.model.entity.AppToken;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.AppPendingRegistrationRepository;
import org.example.shop1.model.reposritory.AppRegistrationRepository;
import org.example.shop1.model.reposritory.AppTokenRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * ورود و ثبت‌نامِ کلاینت‌های {@code /api/app/v1} — اپِ اندرویدی و ابزارِ سایت.
 * قرارداد: {@code docs/dadehlink-api-contract.md}.
 * <p>
 * 🔴 <b>پیامک از همان {@link AuthService} می‌رود</b>، نه نسخهٔ دوم. این کلاس فقط لایهٔ
 * بیرونی است: اعتبارسنجی، محدودیتِ نرخ، نگه‌داشتنِ نام تا تأیید، و توکن.
 * <p>
 * 🔴 <b>توکن هرگز لاگ نمی‌شود</b>، حتی کوتاه‌شده. در دیتابیس فقط هشِ SHA-256ـش هست.
 */
@Service
public class AppAuthService {

    private static final Logger log = LoggerFactory.getLogger(AppAuthService.class);

    static final Pattern PHONE = Pattern.compile("^09\\d{9}$");
    static final Pattern INSTALLATION = Pattern.compile("^[0-9a-f]{16,64}$");
    static final Pattern CODE = Pattern.compile("^\\d{5}$");
    private static final Pattern VERSION = Pattern.compile("^[0-9A-Za-z._+-]{1,32}$");
    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");
    private static final Duration PENDING_TTL = Duration.ofMinutes(10);
    private static final Duration HOUR = Duration.ofHours(1);

    private final AuthService authService;
    private final RateLimitService rateLimit;
    private final UserRepository users;
    private final AppTokenRepository tokens;
    private final AppRegistrationRepository registrations;
    private final AppPendingRegistrationRepository pendings;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.app-api.limit.request-code.phone-interval-sec:60}") long phoneIntervalSec;
    @Value("${app.app-api.limit.request-code.phone-per-hour:5}") long rcPhonePerHour;
    @Value("${app.app-api.limit.request-code.installation-per-hour:5}") long rcInstallPerHour;
    @Value("${app.app-api.limit.request-code.ip-per-hour:10}") long rcIpPerHour;
    @Value("${app.app-api.limit.request-code.daily-sms-cap:300}") long dailySmsCap;
    @Value("${app.app-api.limit.verify.installation-per-hour:20}") long vInstallPerHour;
    @Value("${app.app-api.limit.verify.ip-per-hour:30}") long vIpPerHour;
    @Value("${app.app-api.limit.session-token.ip-per-hour:30}") long stIpPerHour;

    public AppAuthService(AuthService authService, RateLimitService rateLimit, UserRepository users,
                          AppTokenRepository tokens, AppRegistrationRepository registrations,
                          AppPendingRegistrationRepository pendings) {
        this.authService = authService;
        this.rateLimit = rateLimit;
        this.users = users;
        this.tokens = tokens;
        this.registrations = registrations;
        this.pendings = pendings;
    }

    // ==========================================================
    // ورودی‌ها
    // ==========================================================

    public record RequestCodeInput(String name, String phone, String company, String installationId,
                                   String client, String appVersion, String consentVersion) {}

    /** ورودیِ اعتبارسنجی‌شده و نرمال‌شده. */
    record CleanRequest(String name, String phone, String company, String installationId,
                        String client, String appVersion, String consentVersion) {}

    /** ارقامِ فارسی و عربی به لاتین. */
    static String latinDigits(String s) {
        if (s == null) return null;
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c >= '۰' && c <= '۹') b.append((char) ('0' + (c - '۰')));
            else if (c >= '٠' && c <= '٩') b.append((char) ('0' + (c - '٠')));
            else b.append(c);
        }
        return b.toString();
    }

    /** شماره به شکلِ {@code 09xxxxxxxxx}، یا {@code null} اگر نامعتبر است. */
    static String normalizePhone(String raw) {
        if (raw == null) return null;
        String p = latinDigits(raw).replaceAll("[\\s\\-()]", "");
        if (p.startsWith("+98")) p = "0" + p.substring(3);
        else if (p.startsWith("0098")) p = "0" + p.substring(4);
        return PHONE.matcher(p).matches() ? p : null;
    }

    /** اولین فیلدِ خراب، به ترتیبِ بدنهٔ قرارداد. */
    static CleanRequest validate(RequestCodeInput in) {
        if (in == null) throw AppApiException.invalid("body");
        String name = in.name() == null ? "" : in.name().trim().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > 100) throw AppApiException.invalid("name");
        String phone = normalizePhone(in.phone());
        if (phone == null) throw AppApiException.invalid("phone");
        String company = in.company() == null ? "" : in.company().trim();
        if (company.length() > 150) throw AppApiException.invalid("company");
        String inst = in.installationId();
        if (inst == null || !INSTALLATION.matcher(inst).matches()) throw AppApiException.invalid("installationId");
        String client = in.client();
        if (!"android".equals(client) && !"web".equals(client)) throw AppApiException.invalid("client");
        String appVersion = blankToNull(in.appVersion());
        if (appVersion != null && !VERSION.matcher(appVersion).matches()) throw AppApiException.invalid("appVersion");
        String consent = blankToNull(in.consentVersion());
        if (consent != null && !VERSION.matcher(consent).matches()) throw AppApiException.invalid("consentVersion");
        return new CleanRequest(name, phone, company, inst, client, appVersion, consent);
    }

    // ==========================================================
    // ۱ — request-code
    // ==========================================================

    public long requestCode(RequestCodeInput in, String ip) {
        CleanRequest r = validate(in);

        // ترتیب: از گشادترین کلید به تنگ‌ترین، و سقفِ سراسری آخر — تا درخواستی که
        // به‌خاطرِ IP رد می‌شود سهمیهٔ روزانهٔ کلِ سرور را نخورد.
        enforce(rateLimit.hit("rc:ip:" + ip, rcIpPerHour, HOUR));
        enforce(rateLimit.hit("rc:inst:" + r.installationId(), rcInstallPerHour, HOUR));
        enforce(rateLimit.hit("rc:phone-h:" + r.phone(), rcPhonePerHour, HOUR));
        enforce(rateLimit.hit("rc:phone-gap:" + r.phone(), 1, Duration.ofSeconds(phoneIntervalSec)));
        enforceDailyCap();

        AppPendingRegistration p = new AppPendingRegistration();
        p.setId(AppPendingRegistration.key(r.phone(), r.installationId()));
        p.setPhone(r.phone());
        p.setInstallationId(r.installationId());
        p.setName(r.name());
        p.setCompany(r.company());
        p.setClient(r.client());
        p.setAppVersion(r.appVersion());
        p.setConsentVersion(r.consentVersion());
        p.setCreatedAt(Instant.now());
        pendings.save(p);

        try {
            authService.sendOtpCode(r.phone());
        } catch (ApiException e) {
            // AuthService فاصلهٔ ۶۰ ثانیه را خودش هم دارد (مثلاً کدی که همین الان از
            // ورودِ عادیِ سایت رفته). آن را به همان قالبِ قرارداد برگردان.
            if (e.getStatus() == HttpStatus.TOO_MANY_REQUESTS) throw AppApiException.rateLimited(phoneIntervalSec);
            if (e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE) {
                throw new AppApiException(HttpStatus.SERVICE_UNAVAILABLE, Map.of("error", "sms-unavailable"));
            }
            throw e;
        }
        return phoneIntervalSec;
    }

    private void enforceDailyCap() {
        String key = "rc:sms-day:" + LocalDate.now(TEHRAN);
        RateLimitService.Hit h = rateLimit.hit(key, dailySmsCap, Duration.ofHours(25));
        long warnAt = (long) Math.ceil(dailySmsCap * 0.8);
        if (h.count() == warnAt) {
            log.warn("⚠️ پیامکِ ورودِ اپ به ۸۰٪ سقفِ روزانه رسید: {} از {}", h.count(), dailySmsCap);
        }
        if (h.exceeded()) {
            if (h.count() == dailySmsCap + 1) log.error("⛔ سقفِ روزانهٔ پیامکِ ورودِ اپ پر شد ({}).", dailySmsCap);
            throw AppApiException.rateLimited(h.retryAfterSec());
        }
    }

    private static void enforce(RateLimitService.Hit h) {
        if (h.exceeded()) throw AppApiException.rateLimited(h.retryAfterSec());
    }

    // ==========================================================
    // ۲ — verify
    // ==========================================================

    /** نتیجهٔ موفقِ ثبت: توکنِ خام فقط همین یک بار در حافظه است و به کلاینت می‌رود. */
    public record Issued(String token, User user, String client, boolean userCreated) {}

    public Issued verify(String rawPhone, String rawCode, String installationId, String ip) {
        String phone = normalizePhone(rawPhone);
        if (phone == null) throw AppApiException.invalid("phone");
        String code = rawCode == null ? null : latinDigits(rawCode).trim();
        if (code == null || !CODE.matcher(code).matches()) throw AppApiException.invalid("code");
        if (installationId == null || !INSTALLATION.matcher(installationId).matches()) {
            throw AppApiException.invalid("installationId");
        }

        enforce(rateLimit.hit("v:ip:" + ip, vIpPerHour, HOUR));
        enforce(rateLimit.hit("v:inst:" + installationId, vInstallPerHour, HOUR));

        // نامِ واردشده باید از همین دستگاه آمده باشد؛ بدونِ آن، کد هم مصرف نمی‌شود.
        Optional<AppPendingRegistration> pending = pendings.findById(AppPendingRegistration.key(phone, installationId))
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(Instant.now().minus(PENDING_TTL)));
        if (pending.isEmpty()) throw expired();

        AuthService.CodeCheck check = authService.checkCode(phone, code);
        switch (check.status()) {
            case WRONG -> throw new AppApiException(HttpStatus.BAD_REQUEST,
                    Map.of("error", "wrong-code", "attemptsLeft", check.attemptsLeft()));
            case EXPIRED -> throw expired();
            case OK -> { }
        }

        AppPendingRegistration p = pending.get();
        pendings.deleteById(p.getId());

        UserResult ur = findOrCreateUser(phone);
        User user = fillProfileIfEmpty(ur.user(), p.getName(), p.getCompany());

        NewToken token = issueToken(user, installationId, p.getClient());
        saveRegistration(user, p.getName(), p.getCompany(), installationId, p.getClient(),
                p.getAppVersion(), p.getConsentVersion(), "sms", token.id());
        return new Issued(token.raw(), user, p.getClient(), ur.created());
    }

    private static AppApiException expired() {
        return new AppApiException(HttpStatus.GONE, Map.of("error", "expired"));
    }

    record UserResult(User user, boolean created) {}

    /** یک شماره، یک کاربر: اگر دو درخواستِ هم‌زمان هر دو «نیست» دیدند، ایندکسِ یکتا دومی را برمی‌گرداند. */
    UserResult findOrCreateUser(String phone) {
        Optional<User> existing = users.findByPhoneNumber(phone);
        if (existing.isPresent()) return new UserResult(existing.get(), false);
        try {
            return new UserResult(users.save(new User(phone, Role.USER)), true);
        } catch (DuplicateKeyException race) {
            return new UserResult(users.findByPhoneNumber(phone).orElseThrow(), false);
        }
    }

    /** نام فقط اگر نام و نام‌خانوادگی هر دو خالی‌اند؛ شرکت فقط اگر خالی است. */
    User fillProfileIfEmpty(User user, String fullName, String company) {
        boolean changed = false;
        if (isBlank(user.getFirstName()) && isBlank(user.getLastName()) && !isBlank(fullName)) {
            String clean = SecurityUtils.clean(fullName.trim());
            int sp = clean.indexOf(' ');
            user.setFirstName(sp < 0 ? clean : clean.substring(0, sp));
            user.setLastName(sp < 0 ? "" : clean.substring(sp + 1).trim());
            changed = true;
        }
        if (isBlank(user.getOrganization()) && !isBlank(company)) {
            user.setOrganization(SecurityUtils.clean(company.trim()));
            changed = true;
        }
        return changed ? users.save(user) : user;
    }

    // ==========================================================
    // ۵ — session-token (فقط سایت)
    // ==========================================================

    public Issued sessionToken(String username, String installationId, String ip) {
        if (installationId == null || !INSTALLATION.matcher(installationId).matches()) {
            throw AppApiException.invalid("installationId");
        }
        enforce(rateLimit.hit("st:ip:" + ip, stIpPerHour, HOUR));
        User user = users.findByUsername(username).orElseThrow(AppApiException::unauthorized);
        NewToken token = issueToken(user, installationId, "web");
        saveRegistration(user, displayName(user), user.getOrganization(), installationId, "web",
                null, null, "session", token.id());
        return new Issued(token.raw(), user, "web", false);
    }

    // ==========================================================
    // توکن
    // ==========================================================

    /** توکنِ خام (فقط برای پاسخ) و شناسهٔ رکوردش (برای پیوندِ ثبت‌نام و ابطال از پنل). */
    record NewToken(String raw, String id) {}

    /**
     * توکنِ قبلیِ همین دستگاه باطل و توکنِ تازه ساخته می‌شود. ایندکسِ یکتای جزئی روی
     * {@code installationId} (فقط فعال‌ها) مسابقهٔ دو تأییدِ هم‌زمان را می‌گیرد.
     */
    NewToken issueToken(User user, String installationId, String client) {
        for (AppToken old : tokens.findByInstallationIdAndActiveTrue(installationId)) {
            revoke(old);
        }
        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        AppToken t = new AppToken();
        t.setTokenHash(hash(token));
        t.setUserId(user.getId());
        t.setInstallationId(installationId);
        t.setClient(client);
        t.setCreatedAt(Instant.now());
        t.setLastUsedAt(t.getCreatedAt());
        t.setActive(true);
        try {
            return new NewToken(token, tokens.save(t).getId());
        } catch (DuplicateKeyException race) {
            for (AppToken old : tokens.findByInstallationIdAndActiveTrue(installationId)) revoke(old);
            return new NewToken(token, tokens.save(t).getId());
        }
    }

    public static String hash(String token) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** توکنِ فعال از روی مقدارِ خامِ سرآیند، یا خالی. */
    public Optional<AppToken> findActive(String rawToken) {
        if (rawToken == null || rawToken.length() < 20 || rawToken.length() > 100) return Optional.empty();
        return tokens.findByTokenHash(hash(rawToken)).filter(AppToken::isActive);
    }

    /** آخرین استفاده — حداکثر هر ده دقیقه یک نوشتن، تا هر {@code me} یک write نباشد. */
    public void touch(AppToken t) {
        Instant now = Instant.now();
        if (t.getLastUsedAt() == null || t.getLastUsedAt().isBefore(now.minus(Duration.ofMinutes(10)))) {
            t.setLastUsedAt(now);
            tokens.save(t);
        }
    }

    public void revoke(AppToken t) {
        t.setActive(false);
        t.setRevokedAt(Instant.now());
        tokens.save(t);
    }

    public Optional<User> userOf(AppToken t) {
        return users.findById(t.getUserId());
    }

    // ==========================================================
    // کمکی‌ها
    // ==========================================================

    private void saveRegistration(User user, String name, String company, String installationId, String client,
                                  String appVersion, String consentVersion, String method, String tokenId) {
        AppRegistration reg = new AppRegistration();
        reg.setUserId(user.getId());
        reg.setName(name);
        reg.setPhone(user.getPhoneNumber());
        reg.setCompany(blankToNull(company));
        reg.setInstallationId(installationId);
        reg.setClient(client);
        reg.setAppVersion(appVersion);
        reg.setConsentVersion(consentVersion);
        reg.setMethod(method);
        reg.setTokenId(tokenId);
        reg.setCreatedAt(Instant.now());
        registrations.save(reg);
    }

    public static String displayName(User u) {
        String n = ((u.getFirstName() == null ? "" : u.getFirstName()) + " "
                + (u.getLastName() == null ? "" : u.getLastName())).trim();
        return n.isEmpty() ? null : n;
    }

    /** {@code 0912***6789} */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return phone;
        return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 4);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
