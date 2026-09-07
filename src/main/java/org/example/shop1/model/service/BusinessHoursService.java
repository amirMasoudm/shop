package org.example.shop1.model.service;

import org.example.shop1.model.entity.StoreSettings;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ساعتِ کاریِ چتِ پشتیبانی — <b>مرجعِ واحد، سمتِ سرور</b>.
 * <p>
 * ⚠️ چرا سمتِ سرور: ساعتِ دستگاهِ کاربر قابلِ اعتماد نیست و قفلِ کلاینتی هم با یک
 * درخواستِ مستقیم به API دور زده می‌شود. پس اندپوینتِ ارسالِ پیام خودش از همین
 * سرویس می‌پرسد و بیرونِ ساعت را رد می‌کند؛ قفلِ UI فقط برای این است که کاربر
 * پیامِ ردشده نبیند.
 * <p>
 * تنظیمات از {@link StoreSettings} می‌آید و از پنلِ ادمین ویرایش می‌شود — هیچ
 * روز/ساعت/منطقهٔ زمانیِ مشخصی در این کلاس هاردکد نیست.
 */
@Service
public class BusinessHoursService {

    /** پیامِ ثابتِ قفل. متن عمداً هیچ شماره/ایمیل/لینکی ندارد — قانونِ ضدِ دورزدن. */
    public static final String CLOSED_MESSAGE = "چت با پشتیبانی در ساعت کاری فعال می‌شود.";

    private final StoreSettingsService settingsService;

    public BusinessHoursService(StoreSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /** اگر ساعتِ کاری تنظیم نشده باشد، همیشه باز است. */
    public boolean isOpenNow() {
        StoreSettings s = settingsService.getSettings();
        if (!s.hasChatSchedule()) return true;

        ZonedDateTime now = ZonedDateTime.now(zoneOf(s));
        if (!s.getChatWorkingDays().contains(now.getDayOfWeek().getValue())) return false;

        LocalTime start = parseOrNull(s.getChatStartTime());
        LocalTime end = parseOrNull(s.getChatEndTime());
        if (start == null || end == null) return true;

        LocalTime nowTime = now.toLocalTime();
        // بازهٔ شبانه (مثلاً ۲۲:۰۰ تا ۰۶:۰۰) هم باید درست کار کند، نه فقط بازهٔ روزانه.
        return end.isAfter(start)
                ? !nowTime.isBefore(start) && nowTime.isBefore(end)
                : !nowTime.isBefore(start) || nowTime.isBefore(end);
    }

    /**
     * وضعیت برای کلاینت: باز/بسته، پیامِ قفل، و <b>اینکه چه ساعتی فعال می‌شود</b>.
     * سکوتِ بدونِ زمان بدتر از نبودنِ چت است، پس ساعت هم برگردانده می‌شود.
     */
    public Map<String, Object> status() {
        StoreSettings s = settingsService.getSettings();
        Map<String, Object> out = new LinkedHashMap<>();
        boolean open = isOpenNow();
        out.put("open", open);
        out.put("hasSchedule", s.hasChatSchedule());
        if (!open) {
            out.put("message", CLOSED_MESSAGE);
            out.put("schedule", humanSchedule(s));
        }
        return out;
    }

    /** مثلاً «شنبه تا چهارشنبه، ۹:۰۰ تا ۱۷:۰۰» — برای نشان‌دادن در پیامِ قفل. */
    private String humanSchedule(StoreSettings s) {
        if (!s.hasChatSchedule()) return null;
        List<Integer> days = s.getChatWorkingDays().stream().distinct().sorted().toList();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            if (i > 0) sb.append("، ");
            sb.append(persianDayName(days.get(i)));
        }
        return sb + "، ساعت " + s.getChatStartTime() + " تا " + s.getChatEndTime();
    }

    /**
     * نامِ فارسیِ روز از روی شمارهٔ ISO.
     * <p>
     * این تنها جایی است که زبان وارد می‌شود و عمداً فقط «نمایش» است، نه منطق: قاعدهٔ
     * باز/بسته کاملاً با شمارهٔ ISO کار می‌کند. یک اپِ دیگر با زبانِ دیگر فقط همین
     * جدول را عوض می‌کند.
     */
    private String persianDayName(int isoDay) {
        return switch (DayOfWeek.of(isoDay)) {
            case SATURDAY -> "شنبه";
            case SUNDAY -> "یک‌شنبه";
            case MONDAY -> "دوشنبه";
            case TUESDAY -> "سه‌شنبه";
            case WEDNESDAY -> "چهارشنبه";
            case THURSDAY -> "پنج‌شنبه";
            case FRIDAY -> "جمعه";
        };
    }

    private ZoneId zoneOf(StoreSettings s) {
        String tz = s.getChatTimeZone();
        if (tz == null || tz.isBlank()) return ZoneId.systemDefault();
        try {
            return ZoneId.of(tz);
        } catch (Exception e) {
            // منطقهٔ زمانیِ نامعتبر در تنظیمات نباید چت را بخواباند
            return ZoneId.systemDefault();
        }
    }

    private LocalTime parseOrNull(String hhmm) {
        try {
            return LocalTime.parse(hhmm.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
