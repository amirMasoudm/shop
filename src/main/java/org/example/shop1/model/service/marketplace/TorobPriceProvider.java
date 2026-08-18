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
 * نه حلِ چالش، نه هیچ ترفندِ دیگر. به‌جایش فقط آدرسِ جست‌وجویِ آماده ساخته می‌شود
 * تا کارشناس خودش ببیند و عدد را وارد کند. این ۹۰٪ ارزش را نگه می‌دارد (کارشناس
 * دیگر لازم نیست کوئری بنویسد) بدونِ جنگیدن با سیستمِ آن‌ها.
 * <p>
 * مسیرِ اصلی همچنان تیکتِ رسمیِ پنلِ فروشندگانِ ترب است؛ اگر دادهٔ «کمترین قیمتِ
 * رقبا» را بدهند، جایگزینِ کاملِ این می‌شود.
 */
@Component
public class TorobPriceProvider implements MarketplacePriceProvider {

    private static final String SEARCH_PAGE = "https://torob.com/search/?query=";

    @Override
    public String marketKey() { return "torob"; }

    /** ترب دسترسیِ خودکار را می‌بندد؛ عمداً false. */
    @Override
    public boolean supportsAutomaticFetch() { return false; }

    @Override
    public String searchPageUrl(String query) {
        return SEARCH_PAGE + java.net.URLEncoder.encode(
                query == null ? "" : query, StandardCharsets.UTF_8);
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
