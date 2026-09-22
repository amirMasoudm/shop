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
 * نه حلِ چالش، نه هیچ ترفندِ دیگر. اینجا فقط یک <b>لینکِ سرچِ گوگل</b> ساخته
 * می‌شود که کارشناس با مرورگرِ خودش بازش می‌کند و از نتایج، ترب را انتخاب
 * می‌کند. این فرقِ بنیادیِ «بازدیدِ انسان» با «خزندهٔ خودکار» است، و ما همیشه
 * در سمتِ اول می‌مانیم.
 * <p>
 * مسیرِ اصلی همچنان تیکتِ رسمیِ پنلِ فروشندگانِ ترب است؛ اگر دادهٔ «کمترین قیمتِ
 * رقبا» را بدهند، جایگزینِ کاملِ این می‌شود.
 */
@Component
public class TorobPriceProvider implements MarketplacePriceProvider {

    /**
     * <b>سرچِ سادهٔ گوگل</b> — دقیقاً همان کاری که مالک با دست می‌کند.
     * <p>
     * 🔴 این خط سه بار عوض شد و هر بار من چیزِ اضافه‌ای به آن چسبانده بودم:
     * {@code &btnI=1}ِ گوگل، بعد پیشوندِ {@code \}ِ داک‌داک‌گو، بعد
     * {@code site:torob.com}، بعد جست‌وجویِ داخلیِ ترب. هیچ‌کدام خواستهٔ مالک
     * نبود. خواسته این است: «<b>خرید فلان محصول</b>» را در گوگل بزن — همان
     * جمله‌ای که خودِ خریدار می‌زند — و بگذار کارشناس از بینِ نتایج، ترب را
     * باز کند.
     * <p>
     * <b>⚠️ چرا هیچ عملگری اضافه نمی‌شود:</b> {@code site:} و «برو به نتیجهٔ
     * اول» هر دو انتخاب را از کارشناس می‌گیرند و گاهی به محصولِ <i>شبیه</i>
     * می‌برند. و جست‌وجویِ داخلیِ ترب کپچا می‌دهد. سرچِ سادهٔ گوگل هیچ‌کدام
     * را ندارد.
     * <p>
     * ⚠️ خودِ متن اینجا ساخته نمی‌شود. «خرید » را میزِ کار جلویِ نامِ محصول
     * می‌گذارد، چون آنجا <b>قابلِ ویرایش و ذخیره</b> است: کارشناس متن را عوض
     * می‌کند و دفعهٔ بعد همان می‌آید. اگر اینجا می‌چسباندیم، روی متنِ دستیِ
     * خودش هم می‌رفت و «خرید خرید …» می‌شد.
     * <p>
     * ℹ️ اگر آدرسِ صفحهٔ محصول ذخیره شده باشد اصلاً به اینجا نمی‌رسد؛ اولویت
     * با لینک است (منطقش در میزِ کار).
     * <p>
     * ℹ️ هیچ درخواستی از اینجا به گوگل نمی‌رود — فقط یک رشتهٔ آدرس ساخته
     * می‌شود که کارشناس با مرورگرِ خودش بازش می‌کند.
     */
    private static final String GOOGLE_SEARCH = "https://www.google.com/search?q=";

    @Override
    public String marketKey() { return "torob"; }

    /** ترب دسترسیِ خودکار را می‌بندد؛ عمداً false. */
    @Override
    public boolean supportsAutomaticFetch() { return false; }

    @Override
    public String searchPageUrl(String query) {
        // ⚠️ متن دست‌نخورده می‌رود: نه پیشوند، نه site:, نه I'm-feeling-lucky.
        return GOOGLE_SEARCH + java.net.URLEncoder.encode(
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
