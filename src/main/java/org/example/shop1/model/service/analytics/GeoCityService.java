package org.example.shop1.model.service.analytics;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.config.AnalyticsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * شهرِ بازدیدکننده از روی IPِ <b>بریده‌شده</b>.
 * <p>
 * 🔴 <b>IPِ کامل هرگز ذخیره نمی‌شود</b> — حتی در لاگِ موقت. پیش از هر کاری هشت‌تاییِ
 * آخر صفر می‌شود ({@code /24}) و فقط همان برایِ جست‌وجو استفاده می‌شود. بریدن به
 * {@code /24} دقتِ شهری را عملاً کم نمی‌کند، چون گرانولاریتیِ این دیتابیس‌ها از این
 * درشت‌تر است.
 * <p>
 * 🔴 <b>هیچ فراخوانیِ APIِ بیرونی در مسیرِ درخواست نیست.</b> فایل یک‌بار هنگامِ
 * بالاآمدن خوانده می‌شود: وابستگیِ بیرونی از سرورِ ایران قابلِ اتکا نیست و هر بازدید
 * را هم کند می‌کند.
 * <p>
 * اگر فایلی تنظیم نشده باشد، سرویس بی‌صدا غیرفعال است و {@code city} خالی می‌ماند —
 * بقیهٔ ردگیری کامل کار می‌کند. انتخابِ فایل و لایسنسش تصمیمِ مدیر است.
 * <p>
 * ⚠️ دقتِ شهر در ایران به‌خاطرِ CGNATِ اپراتورها پایین است؛ کاربرِ اصفهانی ممکن است
 * تهران دیده شود. این عدد جهت‌نماست، نه قابلِ استناد — و در فازِ ۲ باید با همین قید
 * نمایش داده شود.
 */
@Service
public class GeoCityService {

    private static final Logger log = LoggerFactory.getLogger(GeoCityService.class);

    private final AnalyticsProperties props;

    /** بازه‌های مرتب‌شده؛ جست‌وجو با دودویی روی {@link #rangeStart}. */
    private long[] rangeStart = new long[0];
    private long[] rangeEnd = new long[0];
    private String[] cities = new String[0];

    public GeoCityService(AnalyticsProperties props) {
        this.props = props;
    }

    @PostConstruct
    void load() {
        String configured = props.getGeoCsv();
        if (configured == null || configured.isBlank()) {
            log.info("ژئوی تحلیل غیرفعال است (app.analytics.geo-csv خالی) — شهر ثبت نمی‌شود.");
            return;
        }
        Path path = Paths.get(configured);
        if (!Files.exists(path)) {
            log.warn("فایلِ ژئو پیدا نشد: {} — شهر ثبت نمی‌شود.", path);
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<long[]> bounds = new ArrayList<>();
            List<String> names = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] cols = splitCsv(line);
                // قالبِ IP2Location LITE: ip_from,ip_to,country_code,country_name,region,city
                if (cols.length < 6) continue;
                try {
                    bounds.add(new long[]{Long.parseLong(cols[0].trim()), Long.parseLong(cols[1].trim())});
                    names.add(cols[5].trim());
                } catch (NumberFormatException ignored) {
                    // ردیفِ سرستون یا خرابی — رد شود، کلِ فایل نباید به‌خاطرش دور ریخته شود
                }
            }
            rangeStart = new long[bounds.size()];
            rangeEnd = new long[bounds.size()];
            cities = names.toArray(new String[0]);
            for (int i = 0; i < bounds.size(); i++) {
                rangeStart[i] = bounds.get(i)[0];
                rangeEnd[i] = bounds.get(i)[1];
            }
            log.info("ژئوی تحلیل بارگذاری شد: {} بازه از {}", cities.length, path);
        } catch (Exception e) {
            log.error("خواندنِ فایلِ ژئو شکست خورد ({}) — شهر ثبت نمی‌شود: {}", path, e.toString());
            rangeStart = new long[0];
            rangeEnd = new long[0];
            cities = new String[0];
        }
    }

    /** IPِ بریده‌شده به {@code /24}؛ همان چیزی که تنها شکلِ مجازِ نگه‌داری است. */
    public String truncatedIp(HttpServletRequest request) {
        String ip = clientIp(request);
        if (ip == null) return null;
        int lastDot = ip.lastIndexOf('.');
        // فقط IPv4 بریده می‌شود؛ برایِ IPv6 چیزی برنمی‌گردانیم تا آدرسِ کامل جایی ننشیند.
        if (lastDot <= 0 || ip.indexOf(':') >= 0) return null;
        return ip.substring(0, lastDot) + ".0";
    }

    public String cityOf(String truncatedIp) {
        if (truncatedIp == null || cities.length == 0) return null;
        long value = toLong(truncatedIp);
        if (value < 0) return null;

        int lo = 0, hi = rangeStart.length - 1, found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (rangeStart[mid] <= value) {
                found = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        if (found < 0 || value > rangeEnd[found]) return null;
        String city = cities[found];
        return (city == null || city.isBlank() || "-".equals(city)) ? null : city;
    }

    /**
     * IPِ کلاینت از پشتِ nginx.
     * <p>
     * ⚠️ مقدارِ برگشتی فقط داخلِ همین کلاس می‌ماند و بلافاصله بریده می‌شود؛ هیچ
     * فراخوانی‌ای این را ذخیره یا لاگ نمی‌کند.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    private long toLong(String ipv4) {
        String[] parts = ipv4.split("\\.");
        if (parts.length != 4) return -1;
        long value = 0;
        for (String part : parts) {
            try {
                int octet = Integer.parseInt(part);
                if (octet < 0 || octet > 255) return -1;
                value = (value << 8) + octet;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return value;
    }

    /** جداکنندهٔ CSV با پشتیبانی از فیلدِ داخلِ گیومه. */
    private String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (char c : line.toCharArray()) {
            if (c == '"') quoted = !quoted;
            else if (c == ',' && !quoted) { out.add(current.toString()); current.setLength(0); }
            else current.append(c);
        }
        out.add(current.toString());
        return out.toArray(new String[0]);
    }
}
