package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * دفترِ کدهایِ صادرشدهٔ کالا — یک سند به‌ازایِ <b>هر</b> کدی که تا به حال صادر شده،
 * چه خودکار روی یک محصول نشسته باشد و چه فقط رزرو شده باشد.
 * <p>
 * 🔴 <b>کد هرگز بازاستفاده نمی‌شود — حتی رزروِ رهاشده.</b> غریزهٔ اول این است که
 * کدِ رزروشده‌ای که هفته‌ها بی‌استفاده مانده آزاد شود. نباید. ممکن است شرکت همان
 * کد را از قبل در نرم‌افزارِ حسابداری روی یک کالا تایپ کرده باشد؛ آزادکردنش یعنی
 * دو کالای متفاوت با یک کد، و اولین همگام‌سازیِ موجودی عددها را جابه‌جا می‌نویسد.
 * کدِ محصولِ حذف‌شده هم به همین دلیل سوخته می‌ماند.
 * <p>
 * وجودِ این کالکشن (جدا از فیلدِ {@code holooCode} روی خودِ محصول) دقیقاً برای همین
 * است: فیلدِ محصول می‌گوید «این محصول چه کدی دارد»، این دفتر می‌گوید «چه کدهایی تا
 * به حال از شمارنده بیرون آمده‌اند» — و دومی با حذفِ محصول از بین نمی‌رود.
 */
@Document(collection = "holoo_codes")
public class HolooCode {

    public enum Status {
        /** صادر شده ولی هنوز به محصولی نچسبیده. */
        RESERVED,
        /** روی یک محصول نشسته. */
        ASSIGNED
    }

    @Id
    private String id;

    /** خودِ کد، مثلِ {@code DN-0001}. ایندکسِ یکتا دارد (MongoIndexInitializer). */
    private String code;

    private Status status = Status.RESERVED;

    /** محصولی که کد رویش نشسته — تا وقتی رزرو است نال. */
    private String productId;

    /**
     * یادداشتِ کارشناس: «این کد برای چه کالایی است؟»
     * <p>
     * بدونِ این، فهرستِ رزروها بعد از یک هفته فقط ستونی از عددِ بی‌معنی است و
     * کسی نمی‌داند کدام را می‌شود مصرف کرد.
     */
    private String note;

    private String issuedBy;
    private Instant issuedAt = Instant.now();

    private String assignedBy;
    private Instant assignedAt;

    public HolooCode() {
    }

    public HolooCode(String code, Status status, String note, String issuedBy) {
        this.code = code;
        this.status = status;
        this.note = note;
        this.issuedBy = issuedBy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant assignedAt) { this.assignedAt = assignedAt; }
}
