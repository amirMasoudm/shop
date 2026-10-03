package org.example.shop1.model.service.analytics;

import jakarta.servlet.http.HttpServletRequest;
import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.Campaign;
import org.example.shop1.model.entity.StoreSettings;
import org.example.shop1.model.reposritory.CampaignRepository;
import org.example.shop1.model.reposritory.StoreSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ساخت و اعتبارسنجیِ کارزارها، و ساختنِ لینکِ برچسب‌خورده.
 *
 * <h3>چرا واژگان بسته است</h3>
 * {@code source} و {@code medium} فقط از فهرستِ {@link StoreSettings} پذیرفته می‌شوند —
 * نه از UI و نه از API. با ورودیِ آزاد، {@code telegram} و {@code Telegram} و {@code tg}
 * در گزارش سه کانالِ متفاوت می‌شوند؛ این شایع‌ترین شکستِ برچسب‌گذاری است و با انضباطِ
 * فردی حل نمی‌شود، فقط با بستنِ ورودی.
 *
 * <h3>چرا مقصد این‌قدر سخت‌گیرانه چک می‌شود</h3>
 * 🔴 {@code landingPath} مسیرِ نسبیِ داخلی است و هیچ راهی برای فرار از دامنهٔ خودمان
 * ندارد. اگر این چک شل باشد، {@code /l/{code}} یک ریدایرکتِ باز می‌شود و اعتبارِ
 * دامنهٔ ما خرجِ سایتِ جعلیِ کسِ دیگری. جزئیاتش در {@link #normalizeLandingPath}.
 */
@Service
public class CampaignService {

    /** بدونِ حروف و عددهای شبیه‌به‌هم: نه صفر و O، نه یک و l و I. */
    private static final String CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789";
    private static final int CODE_LENGTH = 7;
    private static final int MAX_COLLISION_RETRIES = 50;

    private final CampaignRepository repo;
    private final StoreSettingsRepository settingsRepo;
    private final SecureRandom random = new SecureRandom();

    public CampaignService(CampaignRepository repo, StoreSettingsRepository settingsRepo) {
        this.repo = repo;
        this.settingsRepo = settingsRepo;
    }

    // ==========================================================
    // واژگانِ مجاز
    // ==========================================================

    public Map<String, List<String>> vocabularies() {
        StoreSettings s = settings();
        Map<String, List<String>> out = new LinkedHashMap<>();
        out.put("sources", s.effectiveCampaignSources());
        out.put("mediums", s.effectiveCampaignMediums());
        return out;
    }

    private StoreSettings settings() {
        return settingsRepo.findById("origin_location").orElseGet(StoreSettings::new);
    }

    // ==========================================================
    // ساخت و ویرایش
    // ==========================================================

    public Campaign create(Campaign input) {
        Campaign c = new Campaign();
        apply(input, c);
        c.setSlug(uniqueSlug(slugify(input.getName()), null));
        c.setCode(uniqueCode());
        return repo.save(c);
    }

    public Campaign update(String id, Campaign input) {
        Campaign c = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "کارزار پیدا نشد"));
        apply(input, c);
        // ⚠️ اسلاگ و کد عمداً ثابت می‌مانند: لینک‌های منتشرشده و رویدادهای ثبت‌شده به
        // همین دو بند‌اند. عوض‌کردنشان یعنی شکستنِ QRهای چاپ‌شده و قطع‌شدنِ گزارش از گذشته.
        return repo.save(c);
    }

    private void apply(Campaign in, Campaign target) {
        String name = in.getName() == null ? "" : in.getName().trim();
        if (name.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "نامِ کارزار لازم است");
        target.setName(name);

        target.setSource(requireFromVocabulary("source", in.getSource(), settings().effectiveCampaignSources()));
        target.setMedium(requireFromVocabulary("medium", in.getMedium(), settings().effectiveCampaignMediums()));
        target.setTerm(normalizeTag(in.getTerm()));
        target.setContent(normalizeTag(in.getContent()));
        target.setLandingPath(normalizeLandingPath(in.getLandingPath()));
        target.setStartsAt(in.getStartsAt());
        target.setEndsAt(in.getEndsAt());
        target.setCost(in.getCost() != null && in.getCost() >= 0 ? in.getCost() : null);
        target.setActive(in.isActive());
        target.setNotes(in.getNotes());
    }

    /** 🔴 تنها دروازهٔ ورود. چه از پنل بیاید چه مستقیم از API، از اینجا رد می‌شود. */
    private String requireFromVocabulary(String field, String value, List<String> allowed) {
        String v = normalizeTag(value);
        if (v == null) throw new ApiException(HttpStatus.BAD_REQUEST, "مقدارِ «" + field + "» لازم است");
        boolean ok = allowed.stream().anyMatch(a -> a.equalsIgnoreCase(v));
        if (!ok) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "مقدارِ «" + v + "» برای «" + field + "» مجاز نیست. مقادیرِ مجاز: " + String.join("، ", allowed));
        }
        return v;
    }

    private static String normalizeTag(String value) {
        if (value == null) return null;
        String v = value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
        return v.isEmpty() ? null : v;
    }

    // ==========================================================
    // مقصد — بندِ امنیتیِ این تسک
    // ==========================================================

    /**
     * 🔴 مقصد باید مسیرِ داخلیِ خودمان باشد و هیچ راهی به بیرون نداشته باشد.
     * <p>
     * چیزهایی که عمداً رد می‌شوند و هر کدام یک راهِ شناخته‌شدهٔ فرار از این چک‌اند:
     * <ul>
     *   <li>{@code http://...} و هر چیزی با «:» — نشانیِ مطلق.</li>
     *   <li>{@code //evil.com} — مسیرِ پروتکل‌نسبی؛ مرورگر آن را دامنهٔ بیرونی می‌خواند.</li>
     *   <li>{@code /\evil.com} و بک‌اسلش — بعضی مرورگرها آن را مثلِ «/» می‌بینند.</li>
     *   <li>نویسهٔ کنترلی و خطِ جدید — تزریق به هدرِ {@code Location}.</li>
     *   <li>{@code /l/...} — لینکِ کوتاهی که به لینکِ کوتاه برود حلقه می‌سازد.</li>
     * </ul>
     */
    public static String normalizeLandingPath(String raw) {
        String p = raw == null ? "" : raw.trim();
        if (p.isEmpty()) p = "/";
        if (p.chars().anyMatch(ch -> ch < 0x20 || ch == 0x7F)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "مسیرِ مقصد نویسهٔ نامعتبر دارد");
        }
        if (p.contains("\\")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "مسیرِ مقصد نباید بک‌اسلش داشته باشد");
        }
        if (!p.startsWith("/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "مقصد باید مسیرِ داخلیِ سایت باشد و با «/» شروع شود، نه نشانیِ کامل");
        }
        if (p.startsWith("//")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "مسیرِ مقصد نباید با «//» شروع شود");
        }
        // «:» قبل از اولین «/» یعنی طرحِ پروتکل. چون مسیر با «/» شروع می‌شود، هر «:»ی
        // در بخشِ مسیر بی‌خطر است — ولی ساده‌تر و امن‌تر آن است که کلاً رد شود.
        String pathOnly = p.split("[?#]", 2)[0];
        if (pathOnly.contains(":")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "مسیرِ مقصد نباید «:» داشته باشد");
        }
        if (pathOnly.startsWith("/l/")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "مقصدِ لینکِ کوتاه نمی‌تواند خودش لینکِ کوتاه باشد");
        }
        return p;
    }

    // ==========================================================
    // اسلاگ و کد
    // ==========================================================

    /**
     * نامِ فارسی → اسلاگِ لاتینِ تمیز.
     * <p>
     * حرف‌به‌حرف برگردانده می‌شود چون اسلاگ باید در {@code utm_campaign} بنشیند و در
     * نوارِ آدرس، گزارشِ گوگل و فایلِ CSV خوانا بماند. درصد-کدشدهٔ فارسی هیچ‌کدام
     * را نمی‌دهد.
     */
    public static String slugify(String name) {
        if (name == null) return "";
        String s = name.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(s.length() * 2);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            String mapped = PERSIAN.get(ch);
            if (mapped != null) {
                sb.append(mapped);
            } else if (ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z') {
                sb.append(ch);
            } else {
                sb.append('-');
            }
        }
        String out = sb.toString().replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
        return out.isEmpty() ? "campaign" : out;
    }

    private static final Map<Character, String> PERSIAN = new LinkedHashMap<>();
    static {
        String[] pairs = {
                "ا", "a", "آ", "a", "أ", "a", "إ", "e", "ء", "", "ب", "b", "پ", "p", "ت", "t",
                "ث", "s", "ج", "j", "چ", "ch", "ح", "h", "خ", "kh", "د", "d", "ذ", "z", "ر", "r",
                "ز", "z", "ژ", "zh", "س", "s", "ش", "sh", "ص", "s", "ض", "z", "ط", "t", "ظ", "z",
                "ع", "a", "غ", "gh", "ف", "f", "ق", "gh", "ک", "k", "ك", "k", "گ", "g", "ل", "l",
                "م", "m", "ن", "n", "و", "v", "ؤ", "v", "ه", "h", "ة", "h", "ی", "y", "ي", "y",
                "ئ", "y", "‌", "-",
                // اعرابِ عربی حذف می‌شوند، نه اینکه به «-» تبدیل شوند
                "ً", "", "ٌ", "", "ٍ", "", "َ", "", "ُ", "", "ِ", "",
                "ّ", "", "ْ", "",
                // ارقامِ فارسی و عربی
                "۰", "0", "۱", "1", "۲", "2", "۳", "3", "۴", "4",
                "۵", "5", "۶", "6", "۷", "7", "۸", "8", "۹", "9",
                "٠", "0", "١", "1", "٢", "2", "٣", "3", "٤", "4",
                "٥", "5", "٦", "6", "٧", "7", "٨", "8", "٩", "9"
        };
        for (int i = 0; i < pairs.length; i += 2) {
            PERSIAN.put(pairs[i].charAt(0), pairs[i + 1]);
        }
    }

    private String uniqueSlug(String base, String ownId) {
        String candidate = base;
        int n = 2;
        while (repo.findBySlug(candidate).filter(c -> !c.getId().equals(ownId)).isPresent()) {
            candidate = base + "-" + n++;
            if (n > MAX_COLLISION_RETRIES + 2) {
                candidate = base + "-" + randomCode(4);
                break;
            }
        }
        return candidate;
    }

    private String uniqueCode() {
        for (int i = 0; i < MAX_COLLISION_RETRIES; i++) {
            String c = randomCode(CODE_LENGTH);
            if (!repo.existsByCode(c)) return c;
        }
        throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ساختِ کدِ یکتا ناموفق بود");
    }

    private String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    // ==========================================================
    // لینک‌ها
    // ==========================================================

    /** مسیرِ داخلیِ برچسب‌خورده — بدونِ دامنه. همین چیزی است که ریدایرکت به آن می‌رود. */
    public String taggedPath(Campaign c) {
        StringBuilder sb = new StringBuilder(c.getLandingPath());
        sb.append(c.getLandingPath().contains("?") ? '&' : '?');
        sb.append("utm_source=").append(enc(c.getSource()));
        sb.append("&utm_medium=").append(enc(c.getMedium()));
        sb.append("&utm_campaign=").append(enc(c.getSlug()));
        if (c.getTerm() != null) sb.append("&utm_term=").append(enc(c.getTerm()));
        if (c.getContent() != null) sb.append("&utm_content=").append(enc(c.getContent()));
        return sb.toString();
    }

    public String taggedUrl(Campaign c, String baseUrl) {
        return baseUrl + taggedPath(c);
    }

    public String shortUrl(Campaign c, String baseUrl) {
        return baseUrl + "/l/" + c.getCode();
    }

    /** پایهٔ نشانی از خودِ درخواست — همان قاعده‌ای که صفحه‌های SSR استفاده می‌کنند. */
    public static String baseUrlOf(HttpServletRequest request) {
        int port = request.getServerPort();
        return request.getScheme() + "://" + request.getServerName()
                + (port == 80 || port == 443 ? "" : ":" + port);
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
