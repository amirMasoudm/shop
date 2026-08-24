package org.example.shop1.model.service.payment;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * تبدیلِ میلادی به جلالی — فقط برایِ فرمتِ {@code localDate}/{@code localTime}ِ درگاهِ ملت
 * ({@code YYYYMMDD} / {@code HHmmss}). عمداً بدونِ کتابخانهٔ جدید (طبقِ روحِ پرامپت: سبک و
 * کم‌ریسک) — الگوریتمِ تبدیل، الگوریتمِ استانداردِ عمومیِ میلادی↔جلالی است.
 */
final class JalaliDateUtil {

    private JalaliDateUtil() {}

    /** {@code localDate} به فرمتِ YYYYMMDD (جلالی)، بر اساسِ زمانِ تهران. */
    static String localDate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Tehran"));
        int[] jalali = toJalali(now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        return String.format("%04d%02d%02d", jalali[0], jalali[1], jalali[2]);
    }

    /** {@code localTime} به فرمتِ HHmmss، بر اساسِ زمانِ تهران. */
    static String localTime() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Tehran"));
        return String.format("%02d%02d%02d", now.getHour(), now.getMinute(), now.getSecond());
    }

    /** الگوریتمِ استانداردِ تبدیلِ میلادی به جلالی. خروجی: {year, month, day}. */
    static int[] toJalali(int gy, int gm, int gd) {
        int[] g_d_m = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        int gy2 = (gm > 2) ? (gy + 1) : gy;
        int days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100)
                + ((gy2 + 399) / 400) + gd + g_d_m[gm - 1];
        int jy = -1595 + (33 * (days / 12053));
        days %= 12053;
        jy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            jy += (days - 1) / 365;
            days = (days - 1) % 365;
        }
        int jm;
        int jd;
        if (days < 186) {
            jm = 1 + (days / 31);
            jd = 1 + (days % 31);
        } else {
            jm = 7 + ((days - 186) / 30);
            jd = 1 + ((days - 186) % 30);
        }
        return new int[]{jy, jm, jd};
    }
}
