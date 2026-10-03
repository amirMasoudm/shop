package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * شمارندهٔ اتمیکِ عمومی — یک سند به‌ازایِ هر دنبالهٔ شماره.
 * <p>
 * 🔴 <b>چرا سندِ جدا و نه {@code max+1}:</b> دو دلیلِ مستقل.
 * <ol>
 *   <li><b>هم‌زمانی.</b> دو ساختِ هم‌زمان با {@code max+1} هر دو همان عدد را
 *       می‌خوانند و یک شماره می‌گیرند. {@code findAndModify} + {@code $inc} در
 *       مونگو اتمیک است و این را از ریشه می‌بندد.</li>
 *   <li><b>شمارهٔ رزروشده هنوز روی هیچ رکوردی نیست.</b> کدی که صادر و رزرو شده
 *       ولی هنوز به محصولی نچسبیده، در {@code max}ِ محصولات دیده نمی‌شود — پس
 *       {@code max+1} همان کد را دوباره می‌دهد.</li>
 * </ol>
 * عمداً ژنریک است: هر دنباله‌ای (کدِ کالا، شمارهٔ فاکتور، …) با یک {@code name}
 * جدا از همین کالکشن استفاده می‌کند.
 * <p>
 * ⚠️ <b>این تنها ساکنِ کالکشنِ {@code counters} نیست.</b>
 * {@code PaymentRefNumberGenerator} از قبل دنبالهٔ {@code order_payment_ref} را با
 * همین شکلِ سند و همین الگویِ {@code findAndModify} می‌سازد، ولی با یک کلاسِ داخلیِ
 * خودش. تداخلی ندارند (کلیدها جدا هستند)، ولی اگر روزی یکی را عوض کردید، آن یکی
 * را هم ببینید — یا هر دو را پشتِ یک سرویسِ مشترک ببرید.
 */
@Document(collection = "counters")
public class Counter {

    /** نامِ دنباله — همان {@code _id} است، پس یکتاییِ آن را خودِ مونگو تضمین می‌کند. */
    @Id
    private String id;

    /** آخرین شمارهٔ صادرشده. شمارهٔ بعدی {@code seq + 1} است. */
    private long seq;

    public Counter() {
    }

    public Counter(String id, long seq) {
        this.id = id;
        this.seq = seq;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }
}
