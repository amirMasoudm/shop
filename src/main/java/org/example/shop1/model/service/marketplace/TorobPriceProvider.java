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
 * نه حلِ چالش، نه هیچ ترفندِ دیگر. اینجا فقط یک <b>لینک</b> ساخته می‌شود که
 * کارشناس با مرورگرِ خودش بازش می‌کند؛ اگر ترب کپچا نشان داد، همان آدم حلش
 * می‌کند. این فرقِ بنیادیِ «بازدیدِ انسان» با «خزندهٔ خودکار» است، و ما
 * همیشه در سمتِ اول می‌مانیم.
 * <p>
 * مسیرِ اصلی همچنان تیکتِ رسمیِ پنلِ فروشندگانِ ترب است؛ اگر دادهٔ «کمترین قیمتِ
 * رقبا» را بدهند، جایگزینِ کاملِ این می‌شود.
 */
@Component
public class TorobPriceProvider implements MarketplacePriceProvider {

    /**
     * جست‌وجویِ <b>خودِ ترب</b> — تصمیمِ صریحِ مالک.
     * <p>
     * 🔴 قبلاً اینجا گوگل و بعد داک‌داک‌گو با {@code site:torob.com} بود. مالک
     * سه بار گفت و من نفهمیدم: مقصد، جست‌وجویِ داخلیِ خودِ ترب است. دلیلش هم
     * روشن است — کارشناس همان‌جا فهرستِ محصولاتِ مشابه را می‌بیند و خودش
     * تشخیص می‌دهد کدام دقیقاً همین محصول است؛ موتورِ بیرونی این انتخاب را
     * از او می‌گیرد و گاهی به محصولِ شبیه (ولی غلط) می‌برد.
     * <p>
     * <b>ایرادِ قبلیِ من به {@code /search/} چرا دیگر وارد نیست:</b> آن مسیر در
     * robots.txtِ ترب Disallow است، و robots.txt قرارداد با <b>خزنده‌ها</b>ست،
     * نه با آدمی که لینکی را در مرورگرِ خودش باز می‌کند. ما اینجا هیچ درخواستی
     * نمی‌زنیم — فقط یک رشتهٔ آدرس می‌سازیم.
     * <p>
     * ⚠️ ترب ممکن است کپچا نشان دهد. مالک این را می‌داند و پذیرفته: کارشناس
     * یک‌بار حلش می‌کند. <b>هیچ کدی برای دور زدنِ آن نوشته نمی‌شود.</b>
     * <p>
     * ℹ️ اگر کارشناس آدرسِ دقیقِ صفحهٔ محصول را ذخیره کرده باشد، اصلاً به اینجا
     * نمی‌رسد؛ اولویت با لینکِ ذخیره‌شده است (منطقش در میزِ کار است).
     */
    private static final String TOROB_SEARCH = "https://torob.com/search/?query=";

    @Override
    public String marketKey() { return "torob"; }

    /** ترب دسترسیِ خودکار را می‌بندد؛ عمداً false. */
    @Override
    public boolean supportsAutomaticFetch() { return false; }

    @Override
    public String searchPageUrl(String query) {
        // ⚠️ «خرید» و site: عمداً حذف شدند: داخلِ خودِ ترب آن کلمه‌ها فقط
        // نتیجه را خراب می‌کنند — اینجا دیگر موتورِ عمومی نیست.
        return TOROB_SEARCH + java.net.URLEncoder.encode(
                query == null ? "" : query.trim(), StandardCharsets.UTF_8);
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
