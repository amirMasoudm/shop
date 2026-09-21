package org.example.shop1.model.service.marketplace;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ترب — <b>عمداً نیمه‌خودکار</b>.
 * <p>
 * 🔴 آزمایشِ واقعیِ ۱۸ اوت: درخواستِ اول ۲۰۰ داد، درخواستِ دوم {@code HTTP 490} با
 * صفحهٔ «آیا شما یک ربات هستید؟» — یعنی ترب صراحتاً دسترسیِ خودکار را تشخیص می‌دهد
 * و می‌بندد، حتی با هدرهایِ کاملِ مرورگرمانند.
 * <p>
 * <b>این یک سیگنالِ صریح از طرفِ آن‌هاست و دور زده نمی‌شود</b> — نه چرخشِ IP،
 * نه حلِ چالش، نه هیچ ترفندِ دیگر. به‌جایش فقط یک لینکِ گوگل ساخته می‌شود
 * تا کارشناس با مرورگرِ خودش واردِ صفحهٔ محصول شود و ردیفِ فروشگاه را
 * کپی کند. این ۹۰٪ ارزش را نگه می‌دارد (کارشناس
 * دیگر لازم نیست کوئری بنویسد) بدونِ جنگیدن با سیستمِ آن‌ها.
 * <p>
 * مسیرِ اصلی همچنان تیکتِ رسمیِ پنلِ فروشندگانِ ترب است؛ اگر دادهٔ «کمترین قیمتِ
 * رقبا» را بدهند، جایگزینِ کاملِ این می‌شود.
 */
@Component
public class TorobPriceProvider implements MarketplacePriceProvider {

    /**
     * ⚠️ عمداً گوگل است، نه جست‌وجویِ خودِ ترب.
     * <p>
     * قاعدهٔ مالک این است: «اولین نتیجهٔ ترب که در سرچِ گوگل می‌آید» — آن
     * لینک کارشناس را مستقیم می‌برد به صفحهٔ خودِ محصول. جست‌وجویِ داخلیِ ترب
     * دو ایراد داشت: یکی اینکه فهرستِ کاندیدا می‌داد و یک کلیکِ اضافه می‌خواست،
     * دوم اینکه {@code /search/} در robots.txtِ خودِ ترب Disallow است.
     * <p>
     * {@code site:torob.com} یعنی نتیجهٔ اول قطعاً همان «اولین نتیجهٔ ترب» است،
     * نه چیزی که کارشناس باید بینِ فروشگاه‌های دیگر دنبالش بگردد.
     */
    private static final String GOOGLE_SEARCH = "https://www.google.com/search?q=";

    @Override
    public String marketKey() { return "torob"; }

    /** ترب دسترسیِ خودکار را می‌بندد؛ عمداً false. */
    @Override
    public boolean supportsAutomaticFetch() { return false; }

    @Override
    public String searchPageUrl(String query) {
        String q = "خرید " + (query == null ? "" : query) + " site:torob.com";
        return GOOGLE_SEARCH + java.net.URLEncoder.encode(q, StandardCharsets.UTF_8);
    }

    @Override
    public List<Candidate> searchCandidates(String query, int limit) {
        return List.of(); // خودکار جست‌وجو نمی‌کنیم
    }

    @Override
    public BigDecimal fetchPriceToman(String externalId) {
        return null; // قیمت را کارشناس دستی وارد می‌کند
    }
}
