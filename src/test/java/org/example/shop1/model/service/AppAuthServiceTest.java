package org.example.shop1.model.service;

import org.example.shop1.exeption.AppApiException;
import org.example.shop1.model.entity.AppPendingRegistration;
import org.example.shop1.model.entity.AppToken;
import org.example.shop1.model.entity.OtpData;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.enums.Role;
import org.example.shop1.model.reposritory.AppPendingRegistrationRepository;
import org.example.shop1.model.reposritory.AppRegistrationRepository;
import org.example.shop1.model.reposritory.AppTokenRepository;
import org.example.shop1.model.reposritory.OtpRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * قراردادِ {@code docs/dadehlink-api-contract.md} از سمتِ سرویس: هر فیلد، هر حدِ نرخ، کدِ
 * غلط تا ابطال، و دو قاعدهٔ داده که اگر بشکنند بی‌صدا می‌شکنند — «یک شماره، یک کاربر»
 * و «نامِ موجود بازنویسی نمی‌شود».
 */
class AppAuthServiceTest {

    private static final String INST = "0123456789abcdef0123456789abcdef";
    private static final String PHONE = "09123456789";

    AuthService authService;
    RateLimitService rate;
    UserRepository users;
    AppTokenRepository tokens;
    AppRegistrationRepository regs;
    AppPendingRegistrationRepository pendings;
    AppAuthService svc;
    /** کلیدهایی که این تست می‌خواهد «پر» باشند. */
    Map<String, Long> exceeded = new HashMap<>();

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        rate = mock(RateLimitService.class);
        users = mock(UserRepository.class);
        tokens = mock(AppTokenRepository.class);
        regs = mock(AppRegistrationRepository.class);
        pendings = mock(AppPendingRegistrationRepository.class);
        svc = new AppAuthService(authService, rate, users, tokens, regs, pendings);
        svc.phoneIntervalSec = 60; svc.rcPhonePerHour = 5; svc.rcInstallPerHour = 5; svc.rcIpPerHour = 10;
        svc.dailySmsCap = 300; svc.vInstallPerHour = 20; svc.vIpPerHour = 30; svc.stIpPerHour = 30;

        when(rate.hit(anyString(), anyLong(), any(Duration.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            long limit = inv.getArgument(1);
            for (var e : exceeded.entrySet()) {
                if (key.startsWith(e.getKey())) return new RateLimitService.Hit(limit + 1, limit, e.getValue());
            }
            return new RateLimitService.Hit(1, limit, 3600);
        });
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId("u1");
            return u;
        });
        when(tokens.save(any(AppToken.class))).thenAnswer(inv -> {
            AppToken t = inv.getArgument(0);
            if (t.getId() == null) t.setId("t1");
            return t;
        });
    }

    private static AppAuthService.RequestCodeInput input(String name, String phone, String company,
                                                         String inst, String client) {
        return new AppAuthService.RequestCodeInput(name, phone, company, inst, client, "1.2.0", "1");
    }

    private static AppApiException invalid(Runnable r) {
        return assertThrows(AppApiException.class, r::run);
    }

    // ---------------- اعتبارسنجیِ هر فیلد ----------------

    @Test
    void eachFieldIsValidatedAndNamedInTheError() {
        assertEquals("name", invalid(() -> AppAuthService.validate(input("  ", PHONE, "", INST, "web"))).getBody().get("field"));
        assertEquals("name", invalid(() -> AppAuthService.validate(input("ا".repeat(101), PHONE, "", INST, "web"))).getBody().get("field"));
        assertEquals("phone", invalid(() -> AppAuthService.validate(input("علی", "0912345678", "", INST, "web"))).getBody().get("field"));
        assertEquals("phone", invalid(() -> AppAuthService.validate(input("علی", "08123456789", "", INST, "web"))).getBody().get("field"));
        assertEquals("company", invalid(() -> AppAuthService.validate(input("علی", PHONE, "x".repeat(151), INST, "web"))).getBody().get("field"));
        assertEquals("installationId", invalid(() -> AppAuthService.validate(input("علی", PHONE, "", "ABCDEF0123456789", "web"))).getBody().get("field"));
        assertEquals("installationId", invalid(() -> AppAuthService.validate(input("علی", PHONE, "", "0123abcd", "web"))).getBody().get("field"));
        assertEquals("client", invalid(() -> AppAuthService.validate(input("علی", PHONE, "", INST, "ios"))).getBody().get("field"));
        // اولین فیلدِ خراب، به ترتیبِ بدنه
        assertEquals("name", invalid(() -> AppAuthService.validate(input("", "x", "", "x", "x"))).getBody().get("field"));
        assertEquals(HttpStatus.BAD_REQUEST,
                invalid(() -> AppAuthService.validate(input("", PHONE, "", INST, "web"))).getStatus());
    }

    @Test
    void persianAndArabicDigitsAndPlus98AreNormalized() {
        assertEquals(PHONE, AppAuthService.normalizePhone("۰۹۱۲۳۴۵۶۷۸۹"));
        assertEquals(PHONE, AppAuthService.normalizePhone("٠٩١٢٣٤٥٦٧٨٩"));
        assertEquals(PHONE, AppAuthService.normalizePhone("+989123456789"));
        assertEquals(PHONE, AppAuthService.normalizePhone("+۹۸۹۱۲۳۴۵۶۷۸۹"));
        assertEquals(PHONE, AppAuthService.normalizePhone("0912 345 6789"));
        assertNull(AppAuthService.normalizePhone("+1 912 345 6789"));
        var ok = AppAuthService.validate(input("  علی   رضایی ", "۰۹۱۲۳۴۵۶۷۸۹", " شرکت ", INST, "android"));
        assertEquals("علی رضایی", ok.name());
        assertEquals(PHONE, ok.phone());
        assertEquals("شرکت", ok.company());
    }

    // ---------------- حدهای نرخ ----------------

    @Test
    void everyRequestCodeLimitAnswers429WithRetryAfter() {
        for (String key : List.of("rc:ip:", "rc:inst:", "rc:phone-h:", "rc:phone-gap:", "rc:sms-day:")) {
            exceeded.clear();
            exceeded.put(key, 777L);
            AppApiException e = assertThrows(AppApiException.class,
                    () -> svc.requestCode(input("علی", PHONE, "", INST, "web"), "1.2.3.4"), key);
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus(), key);
            assertEquals("rate-limited", e.getBody().get("error"));
            assertEquals(777L, e.getBody().get("retryAfterSec"), key);
        }
        verify(authService, never()).sendOtpCode(anyString());
    }

    @Test
    void ipLimitIsCheckedBeforeTheDailyCapSoItDoesNotEatTheQuota() {
        exceeded.put("rc:ip:", 10L);
        assertThrows(AppApiException.class, () -> svc.requestCode(input("علی", PHONE, "", INST, "web"), "1.2.3.4"));
        verify(rate, never()).hit(startsWith("rc:sms-day:"), anyLong(), any());
    }

    @Test
    void everyVerifyLimitAnswers429() {
        for (String key : List.of("v:ip:", "v:inst:")) {
            exceeded.clear();
            exceeded.put(key, 55L);
            AppApiException e = assertThrows(AppApiException.class, () -> svc.verify(PHONE, "12345", INST, "ip"));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
            assertEquals(55L, e.getBody().get("retryAfterSec"));
        }
    }

    @Test
    void smsFailureBecomes503SmsUnavailable() {
        doThrow(new org.example.shop1.exeption.ApiException(HttpStatus.SERVICE_UNAVAILABLE, "x"))
                .when(authService).sendOtpCode(PHONE);
        AppApiException e = assertThrows(AppApiException.class,
                () -> svc.requestCode(input("علی", PHONE, "", INST, "web"), "ip"));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
        assertEquals("sms-unavailable", e.getBody().get("error"));
    }

    // ---------------- کدِ غلط تا انقضا ----------------

    @Test
    void wrongCodeCountsDownThenTheFifthWrongExpiresTheCode() {
        OtpRepository otps = mock(OtpRepository.class);
        AuthService real = new AuthService(users, mock(SmsService.class), otps);
        OtpData otp = new OtpData(PHONE, "11111", System.currentTimeMillis() + 60_000);
        when(otps.findById(PHONE)).thenAnswer(inv -> Optional.ofNullable(otp.getAttempts() < 0 ? null : otp));

        int[] expectedLeft = {4, 3, 2, 1};
        for (int left : expectedLeft) {
            AuthService.CodeCheck c = real.checkCode(PHONE, "22222");
            assertEquals(AuthService.CodeStatus.WRONG, c.status());
            assertEquals(left, c.attemptsLeft());
        }
        assertEquals(AuthService.CodeStatus.EXPIRED, real.checkCode(PHONE, "22222").status());
        verify(otps).deleteById(PHONE);
    }

    @Test
    void expiredOrMissingCodeIs410AndWrongIs400WithAttemptsLeft() {
        when(pendings.findById(AppPendingRegistration.key(PHONE, INST))).thenReturn(Optional.of(pending("web")));

        when(authService.checkCode(PHONE, "12345")).thenReturn(new AuthService.CodeCheck(AuthService.CodeStatus.WRONG, 3));
        AppApiException wrong = assertThrows(AppApiException.class, () -> svc.verify(PHONE, "12345", INST, "ip"));
        assertEquals(HttpStatus.BAD_REQUEST, wrong.getStatus());
        assertEquals(Map.of("error", "wrong-code", "attemptsLeft", 3), wrong.getBody());

        when(authService.checkCode(PHONE, "12345")).thenReturn(new AuthService.CodeCheck(AuthService.CodeStatus.EXPIRED, 0));
        AppApiException exp = assertThrows(AppApiException.class, () -> svc.verify(PHONE, "12345", INST, "ip"));
        assertEquals(HttpStatus.GONE, exp.getStatus());
        assertEquals(Map.of("error", "expired"), exp.getBody());
    }

    @Test
    void codeFromAnotherDeviceIsRejectedWithoutConsumingIt() {
        when(pendings.findById(anyString())).thenReturn(Optional.empty());
        AppApiException e = assertThrows(AppApiException.class, () -> svc.verify(PHONE, "12345", INST, "ip"));
        assertEquals(HttpStatus.GONE, e.getStatus());
        verify(authService, never()).checkCode(anyString(), anyString());
    }

    @Test
    void persianDigitsInTheCodeAreAccepted() {
        when(pendings.findById(anyString())).thenReturn(Optional.of(pending("android")));
        when(authService.checkCode(PHONE, "12345")).thenReturn(new AuthService.CodeCheck(AuthService.CodeStatus.OK, 5));
        when(users.findByPhoneNumber(PHONE)).thenReturn(Optional.empty());
        AppAuthService.Issued ok = svc.verify("۰۹۱۲۳۴۵۶۷۸۹", "۱۲۳۴۵", INST, "ip");
        assertEquals(43, ok.token().length(), "۳۲ بایت base64url بدونِ padding");
        verify(authService).checkCode(PHONE, "12345");
    }

    // ---------------- یک شماره، یک کاربر؛ نام دست نمی‌خورد ----------------

    @Test
    void samePhoneTwiceIsOneUser() {
        User existing = new User(PHONE, Role.USER);
        existing.setId("old");
        when(users.findByPhoneNumber(PHONE)).thenReturn(Optional.of(existing));
        assertSame(existing, svc.findOrCreateUser(PHONE).user());
        assertFalse(svc.findOrCreateUser(PHONE).created());
        verify(users, never()).save(any());
    }

    @Test
    void concurrentCreateFallsBackToTheUserTheOtherRequestMade() {
        User winner = new User(PHONE, Role.USER);
        winner.setId("winner");
        when(users.findByPhoneNumber(PHONE)).thenReturn(Optional.empty(), Optional.of(winner));
        when(users.save(any(User.class))).thenThrow(new DuplicateKeyException("uk_users_phoneNumber"));
        AppAuthService.UserResult r = svc.findOrCreateUser(PHONE);
        assertEquals("winner", r.user().getId());
        assertFalse(r.created());
    }

    @Test
    void existingNameAndCompanyAreNeverOverwritten() {
        User u = new User(PHONE, Role.USER);
        u.setFirstName("نامِ");
        u.setOrganization("شرکتِ قبلی");
        svc.fillProfileIfEmpty(u, "اسمِ تازه", "شرکتِ تازه");
        assertEquals("نامِ", u.getFirstName());
        assertNull(u.getLastName());
        assertEquals("شرکتِ قبلی", u.getOrganization());
        verify(users, never()).save(any());

        User lastOnly = new User(PHONE, Role.USER);
        lastOnly.setLastName("رضایی");
        svc.fillProfileIfEmpty(lastOnly, "علی محمدی", "");
        assertNull(lastOnly.getFirstName(), "فقط وقتی هر دو خالی‌اند");
    }

    @Test
    void emptyProfileIsFilledFromTheForm() {
        User u = new User(PHONE, Role.USER);
        svc.fillProfileIfEmpty(u, "علی اکبر رضایی", "داده");
        assertEquals("علی", u.getFirstName());
        assertEquals("اکبر رضایی", u.getLastName());
        assertEquals("داده", u.getOrganization());
    }

    // ---------------- توکن ----------------

    @Test
    void onlyTheHashIsStoredAndANewTokenRevokesTheOldOneOnTheSameDevice() {
        AppToken old = new AppToken();
        old.setActive(true);
        when(tokens.findByInstallationIdAndActiveTrue(INST)).thenReturn(List.of(old));
        User u = new User(PHONE, Role.USER);
        u.setId("u1");
        AppAuthService.NewToken t = svc.issueToken(u, INST, "web");

        assertFalse(old.isActive());
        assertNotNull(old.getRevokedAt());
        verify(tokens, atLeastOnce()).save(argThat(saved ->
                saved.isActive() && AppAuthService.hash(t.raw()).equals(saved.getTokenHash())
                        && !t.raw().equals(saved.getTokenHash())));
    }

    @Test
    void revokedTokenIsNotFound() {
        AppToken revoked = new AppToken();
        revoked.setActive(false);
        String raw = "a".repeat(43);
        when(tokens.findByTokenHash(AppAuthService.hash(raw))).thenReturn(Optional.of(revoked));
        assertTrue(svc.findActive(raw).isEmpty());
        assertTrue(svc.findActive(null).isEmpty());
        assertTrue(svc.findActive("short").isEmpty());
    }

    @Test
    void maskedPhone() {
        assertEquals("0912***6789", AppAuthService.maskPhone(PHONE));
    }

    private static AppPendingRegistration pending(String client) {
        AppPendingRegistration p = new AppPendingRegistration();
        p.setId(AppPendingRegistration.key(PHONE, INST));
        p.setPhone(PHONE);
        p.setInstallationId(INST);
        p.setName("علی رضایی");
        p.setCompany("");
        p.setClient(client);
        p.setCreatedAt(Instant.now());
        return p;
    }
}
