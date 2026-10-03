package org.example.shop1.model.service.marketplace;

import java.math.BigDecimal;
import java.util.List;

/**
 * منبعِ قیمتِ یک بازارِ خارجی (دیجی‌کالا، ترب، …).
 * <p>
 * ⚠️ این کدها به اندپوینت‌هایِ <b>غیررسمی</b> وصل‌اند و بدونِ اطلاع عوض می‌شوند —
 * پس هر بازار پشتِ همین interface است تا وقتی یکی شکست، فقط همان کلاس عوض شود.
 * <p>
 * همهٔ بازارها نمی‌توانند خودکار باشند: جایی که سایتِ مقصد صراحتاً دسترسیِ خودکار
 * را می‌بندد (ترب)، پیاده‌سازی {@link #supportsAutomaticFetch()} را {@code false}
 * برمی‌گرداند و فقط {@link #searchPageUrl(String)} می‌دهد تا کارشناس دستی ببیند.
 * <b>سدّ ضدربات دور زده نمی‌شود.</b>
 */
public interface MarketplacePriceProvider {

    /** شناسهٔ بازار: {@code digikala} یا {@code torob}. */
    String marketKey();

    /** آیا واکشیِ خودکارِ قیمت ممکن/مجاز است؟ */
    boolean supportsAutomaticFetch();

    /** آدرسِ صفحهٔ جست‌وجویِ بازار برایِ بازکردنِ دستی توسطِ کارشناس. */
    String searchPageUrl(String query);

    /**
     * جست‌وجو و برگرداندنِ نامزدها برایِ <b>تأییدِ انسان</b>.
     * هرگز خودکار پذیرفته نمی‌شوند، حتی اگر فقط یک نتیجه باشد.
     */
    List<Candidate> searchCandidates(String query, int limit);

    /**
     * قیمتِ فعلیِ یک شناسهٔ تأییدشده — به <b>تومان</b>.
     * اگر پیدا نشد {@code null} (فراخواننده نباید مقدارِ قبلی را پاک کند).
     */
    BigDecimal fetchPriceToman(String externalId);

    /**
     * قیمت و فروشندهٔ باکسِ خرید، با <b>یک</b> درخواست.
     * <p>
     * ⚠️ عمداً پیش‌فرض دارد: بازاری که فروشنده را نمی‌دهد (یا خودکار نیست) لازم
     * نیست چیزی پیاده کند، و بازاری که می‌دهد دو بار درخواست نمی‌زند — همان
     * پاسخی که قیمت از آن درآمده، نامِ فروشنده را هم دارد.
     */
    default Snapshot fetchSnapshot(String externalId) {
        return new Snapshot(fetchPriceToman(externalId), null);
    }

    /** یک نامزدِ جست‌وجو؛ قیمت به تومان است، نه واحدِ خامِ سایتِ مقصد. */
    record Candidate(String externalId, String title, BigDecimal priceToman, String url) {}

    /** برداشتِ یک لحظه از بازار: قیمتِ تومانی و نامِ فروشندهٔ باکسِ خرید (ممکن است null باشد). */
    record Snapshot(BigDecimal priceToman, String sellerTitle) {}
}
