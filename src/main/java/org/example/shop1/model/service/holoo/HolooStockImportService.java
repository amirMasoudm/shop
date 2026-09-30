package org.example.shop1.model.service.holoo;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.StoreSettings;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.service.ActivityLogService;
import org.example.shop1.model.service.HolooCodeService;
import org.example.shop1.model.service.StockNotificationService;
import org.example.shop1.model.service.StoreSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ایمپورت و ریستِ موجودی از خروجیِ نرم‌افزارِ حسابداری.
 *
 * <h3>چرا دو مرحله‌ای است</h3>
 * «نبودن در فایل یعنی صفر» تنها تفسیرِ درستِ گزارشِ موجودی است — آن گزارش هرچه
 * موجودی دارد را می‌آورد. ولی همین قاعده خطرناک‌ترین بندِ کلِ این کار است: یک فایلِ
 * نیمه‌دانلود یا خروجیِ ناقص می‌تواند کلِ کاتالوگ را صفر کند و چون
 * {@code availability}ِ ترب از {@code stock > 0} می‌آید، اشتباه فوراً منتشر می‌شود.
 * پس هیچ ایمپورتی بی‌واسطه اعمال نمی‌شود: اول پیش‌نمایش، بعد تأییدِ جدا.
 *
 * <h3>🔴 تلهٔ پیامک</h3>
 * تریگرِ «موجود شد» روی گذارِ صفر → مثبت است. اولین ایمپورت صدها محصول را از صفر
 * به مثبت می‌برد، یعنی صدها پیامکِ هزینه‌دار با یک کلیک. تیکِ ارسال <b>پیش‌فرض
 * خاموش</b> است و سقفی هم دارد که از آن بیشتر، ایمپورت می‌ایستد و می‌پرسد.
 *
 * <h3>تطبیق فقط با کد</h3>
 * هرگز با نام. نام‌های خروجیِ حسابداری کثیف‌اند ({@code wap ac#}، {@code 'server '}،
 * {@code C2960X-48FPD-L  RF#}) و تمامِ دلیلِ وجودِ کدِ کالا حذفِ همین حدس است.
 */
@Service
public class HolooStockImportService {

    private static final Logger log = LoggerFactory.getLogger(HolooStockImportService.class);

    /** بیش از این درصدِ صفرشدن، بدونِ تأییدِ صریح رد می‌شود. */
    public static final int DEFAULT_MASS_ZERO_PERCENT = 30;

    /** سقفِ پیش‌فرضِ پیامک در یک ایمپورت. */
    public static final int DEFAULT_SMS_CAP = 20;

    /** پیش‌نمایش بعد از این مدت باطل می‌شود تا کسی فایلِ دیروز را امروز اعمال نکند. */
    private static final long PREVIEW_TTL_MINUTES = 30;

    private static final int MAX_LIVE_PREVIEWS = 20;

    /**
     * 🔴 نگاشتِ برچسبِ انبار به شعبه.
     * <p>
     * این دو کلیدواژه تنها جای این فایل‌اند که به فروشگاهِ خاصی گره می‌خورند، و
     * دلیلش این است که خودِ {@code Product} از قبل دقیقاً دو شعبهٔ نام‌دار دارد
     * ({@code stockIsfahan} و {@code stockTehran}) — تا وقتی مدلِ داده دو شعبهٔ
     * ثابت دارد، نگاشت هم نمی‌تواند کاملاً آزاد باشد. هر برچسبِ دیگری در فایل
     * بی‌صدا دور ریخته <b>نمی‌شود</b>: در پیش‌نمایش صریح گزارش می‌شود.
     */
    private static final Map<String, Branch> BRANCH_KEYWORDS = Map.of(
            "اصفهان", Branch.ISFAHAN,
            "تهران", Branch.TEHRAN);

    public enum Branch { ISFAHAN, TEHRAN }

    private final ProductRepository productRepo;
    private final StoreSettingsService settingsService;
    private final StockNotificationService stockNotificationService;
    private final ActivityLogService activityLog;

    /** پیش‌نمایش‌های زنده. در حافظه است: ری‌استارتِ اپ یعنی باید دوباره پیش‌نمایش گرفت. */
    private final Map<String, Preview> previews = new ConcurrentHashMap<>();

    public HolooStockImportService(ProductRepository productRepo, StoreSettingsService settingsService,
                                   StockNotificationService stockNotificationService,
                                   ActivityLogService activityLog) {
        this.productRepo = productRepo;
        this.settingsService = settingsService;
        this.stockNotificationService = stockNotificationService;
        this.activityLog = activityLog;
    }

    // ==========================================================
    // مدل‌های خروجی
    // ==========================================================

    /** یک تغییرِ پیشنهادی روی یک محصول. */
    public record Change(String productId, String productName, String holooCode,
                         Integer oldIsfahan, Integer newIsfahan,
                         Integer oldTehran, Integer newTehran,
                         int oldStock, int newStock,
                         boolean overwritesManualEdit,
                         boolean wouldSms,
                         boolean torobFlips) {

        public boolean zeroing() { return oldStock > 0 && newStock == 0; }
    }

    /** ردیفی که به هیچ محصولی نخورد — عمداً گزارش می‌شود، نه دور ریخته. */
    public record Unmatched(String sheet, int row, String code, String name, String warehouse, Integer quantity,
                            String reason) {}

    public record Preview(String token, Instant createdAt, List<String> fileNames,
                          int rowsRead, int rowsSkipped, int rowsMatched,
                          List<Change> changes, List<Unmatched> unmatched,
                          Set<String> warehouseLabels, Set<String> unmappedWarehouses,
                          List<Branch> coveredBranches,
                          int codedProducts, int zeroedCount, int zeroedPercent,
                          int massZeroThreshold, boolean massZeroTripped,
                          int smsCandidates, int smsCap,
                          int torobFlips) {

        public List<Change> manualOverwrites() {
            return changes.stream().filter(Change::overwritesManualEdit).toList();
        }
    }

    public record ApplyResult(int changed, int zeroed, int smsSent, int torobFlips, int skippedStale) {}

    // ==========================================================
    // مرحلهٔ ۱ — پیش‌نمایش
    // ==========================================================

    public Preview preview(List<UploadedFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "فایلی فرستاده نشده است");
        }

        List<ParsedRow> rows = new ArrayList<>();
        List<Unmatched> unmatched = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        int rowsRead = 0, rowsSkipped = 0;
        boolean anyCodeColumn = false;

        for (UploadedFile file : files) {
            fileNames.add(file.name());
            List<XlsxReader.Sheet> sheets;
            try {
                sheets = XlsxReader.read(file.bytes());
            } catch (Exception e) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "فایلِ «" + file.name() + "» خوانده نشد: " + e.getMessage());
            }

            for (XlsxReader.Sheet sheet : sheets) {
                SheetLayout layout = SheetLayout.detect(sheet.rows());
                if (layout == null) continue;          // برگهٔ راهنما و امثالش
                anyCodeColumn = true;

                for (int i = layout.firstDataRow(); i < sheet.rows().size(); i++) {
                    List<String> cells = sheet.rows().get(i);
                    int excelRow = i + 1;

                    // 🔴 ردیفِ جمعِ کل: نام یا شمارهٔ ردیف خالی دارد ولی عددِ بزرگی
                    // در ستونِ موجودی. بدونِ این گارد، یک «محصول» با موجودیِ ۱۸۰۱۰۱
                    // وارد می‌شد.
                    if (layout.isSummaryRow(cells)) {
                        rowsSkipped++;
                        continue;
                    }
                    if (layout.isBlank(cells)) {
                        rowsSkipped++;
                        continue;
                    }

                    rowsRead++;
                    String code = trim(layout.value(cells, layout.codeColumn()));
                    String name = trim(layout.value(cells, layout.nameColumn()));
                    String warehouse = trim(layout.value(cells, layout.warehouseColumn()));
                    Integer qty = toInt(layout.value(cells, layout.quantityColumn()));

                    if (code == null || !HolooCodeService.looksLikeCode(code)) {
                        unmatched.add(new Unmatched(sheet.name(), excelRow, code, name, warehouse, qty,
                                "کدِ کالا ندارد"));
                        continue;
                    }
                    if (qty == null) {
                        unmatched.add(new Unmatched(sheet.name(), excelRow, code, name, warehouse, qty,
                                "عددِ موجودی خوانده نشد"));
                        continue;
                    }
                    rows.add(new ParsedRow(sheet.name(), excelRow, code, name, warehouse, qty));
                }
            }
        }

        if (!anyCodeColumn) {
            // 🔴 خطای روشن، نه «صفر ردیف وارد شد». بی‌صدا رد شدن از این حالت یعنی
            // کاربر فکر می‌کند ایمپورت کار کرده و موجودی‌ها عمداً عوض نشده‌اند.
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "ستونِ کدِ کالا در فایل پیدا نشد. باید ستونی باشد که مقادیرش شکلِ «DN-۰۰۰۱» دارند "
                            + "— یعنی شرکت هنوز کدها را در نرم‌افزارِ حسابداری وارد نکرده است.");
        }

        return build(fileNames, rows, unmatched, rowsRead, rowsSkipped);
    }

    private Preview build(List<String> fileNames, List<ParsedRow> rows, List<Unmatched> unmatched,
                          int rowsRead, int rowsSkipped) {

        Set<String> warehouseLabels = new LinkedHashSet<>();
        Set<String> unmapped = new LinkedHashSet<>();
        Set<Branch> covered = new LinkedHashSet<>();

        // کد → شعبه → تعداد. تکرارِ همان کد در همان شعبه جمع می‌شود (تفکیکِ سری/لات).
        Map<String, Map<Branch, Integer>> byCode = new LinkedHashMap<>();

        for (ParsedRow r : rows) {
            if (r.warehouse() != null) warehouseLabels.add(r.warehouse());
            Branch branch = branchOf(r.warehouse());
            if (branch == null) {
                if (r.warehouse() != null) unmapped.add(r.warehouse());
                unmatched.add(new Unmatched(r.sheet(), r.row(), r.code(), r.name(), r.warehouse(), r.quantity(),
                        "انبارِ ردیف شناخته نشد"));
                continue;
            }
            covered.add(branch);
            byCode.computeIfAbsent(r.code(), k -> new LinkedHashMap<>())
                    .merge(branch, r.quantity(), Integer::sum);
        }

        List<Product> coded = productRepo.findAll().stream()
                .filter(p -> p.getHolooCode() != null && !p.getHolooCode().isBlank())
                .toList();
        Map<String, Product> byProductCode = new LinkedHashMap<>();
        for (Product p : coded) byProductCode.put(p.getHolooCode(), p);

        int matched = 0;
        List<Change> changes = new ArrayList<>();

        for (Map.Entry<String, Map<Branch, Integer>> e : byCode.entrySet()) {
            Product p = byProductCode.get(e.getKey());
            if (p == null) {
                unmatched.add(new Unmatched(null, 0, e.getKey(), null, null, null,
                        "کد به هیچ محصولی روی سایت نمی‌خورد"));
                continue;
            }
            matched++;
            Change c = changeFor(p, e.getValue(), covered);
            if (c != null) changes.add(c);
        }

        // «نبودن در فایل یعنی صفر» — فقط برایِ شعبه‌هایی که این فایل پوشش می‌دهد
        for (Product p : coded) {
            if (byCode.containsKey(p.getHolooCode())) continue;
            Change c = changeFor(p, Map.of(), covered);
            if (c != null) changes.add(c);
        }

        changes.sort(Comparator.comparing(Change::overwritesManualEdit).reversed()
                .thenComparing(Change::zeroing, Comparator.reverseOrder())
                .thenComparing(c -> c.productName() == null ? "" : c.productName()));

        StoreSettings settings = settingsService.getSettings();
        int threshold = settings.getStockImportMassZeroPercent() == null
                ? DEFAULT_MASS_ZERO_PERCENT : settings.getStockImportMassZeroPercent();
        int smsCap = settings.getStockImportSmsCap() == null
                ? DEFAULT_SMS_CAP : settings.getStockImportSmsCap();

        int zeroed = (int) changes.stream().filter(Change::zeroing).count();
        int zeroedPercent = coded.isEmpty() ? 0 : (int) Math.round(zeroed * 100.0 / coded.size());
        int smsCandidates = (int) changes.stream().filter(Change::wouldSms).count();
        int torobFlips = (int) changes.stream().filter(Change::torobFlips).count();

        Preview preview = new Preview(UUID.randomUUID().toString(), Instant.now(), fileNames,
                rowsRead, rowsSkipped, matched, changes, unmatched,
                warehouseLabels, unmapped, List.copyOf(covered),
                coded.size(), zeroed, zeroedPercent, threshold, zeroedPercent > threshold,
                smsCandidates, smsCap, torobFlips);

        remember(preview);
        return preview;
    }

    /** تغییرِ یک محصول، یا نال اگر چیزی عوض نمی‌شود (پایهٔ بی‌اثر‌پذیری). */
    private Change changeFor(Product p, Map<Branch, Integer> fromFile, Set<Branch> covered) {
        Integer oldIsfahan = p.getStockIsfahan();
        Integer oldTehran = p.getStockTehran();

        Integer newIsfahan = covered.contains(Branch.ISFAHAN)
                ? fromFile.getOrDefault(Branch.ISFAHAN, 0) : oldIsfahan;
        Integer newTehran = covered.contains(Branch.TEHRAN)
                ? fromFile.getOrDefault(Branch.TEHRAN, 0) : oldTehran;

        int oldStock = p.getStock() == null ? 0 : p.getStock();
        int newStock = n(newIsfahan) + n(newTehran);

        // 🔴 شرطِ دوم — ناسازگاریِ stock با مجموعِ انبارها.
        //
        // اولین ایمپورتِ واقعی نشان داد ۵۷ کالا از تور رد شدند: فیلدِ انبارشان null
        // بود و در فایل هم نبودند، پس newIsfahan/newTehran صفر می‌شد و
        // eq(null, 0) → true. هیچ Changeای ساخته نمی‌شد، و چون stock فقط داخلِ
        // apply بازمحاسبه می‌شود، روی مقدارِ میراثیِ ۹۹۹ می‌ماند — یعنی قاعدهٔ
        // «نبودن در فایل یعنی صفر» دقیقاً روی کالاهایی که بیشترین نیاز را داشتند
        // بی‌اثر بود و آن‌ها روی سایت «موجود» می‌ماندند.
        //
        // ⚠️ برابریِ null با ۰ عمداً دست‌نخورده ماند: «انبار خالی» و «انبار صفر»
        // واقعاً یک معنا دارند. چیزی که اضافه شد این است که برابریِ انبارها به‌تنهایی
        // دیگر کافی نیست — stock هم باید با مجموعشان بخواند.
        boolean branchesUnchanged = eq(oldIsfahan, newIsfahan) && eq(oldTehran, newTehran);
        if (branchesUnchanged && oldStock == newStock) return null;

        boolean sms = oldStock <= 0 && newStock > 0;
        boolean torob = p.isTorobVisible() && !p.isProductionStopped() && (oldStock > 0) != (newStock > 0);

        return new Change(p.getId(), p.getName(), p.getHolooCode(),
                oldIsfahan, newIsfahan, oldTehran, newTehran, oldStock, newStock,
                p.isStockManuallyTouchedSinceImport(), sms, torob);
    }

    // ==========================================================
    // مرحلهٔ ۲ — اعمال
    // ==========================================================

    /**
     * @param sendSms          تیکِ صریحِ کاربر. پیش‌فرضِ UI خاموش است.
     * @param confirmMassZero  تأییدِ صریح وقتی گاردِ صفرشدنِ انبوه گیر کرده.
     */
    public ApplyResult apply(String token, boolean sendSms, boolean confirmMassZero) {
        Preview preview = previews.get(token);
        if (preview == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "پیش‌نمایش پیدا نشد یا منقضی شده — دوباره فایل را پیش‌نمایش بگیرید");
        }
        if (preview.createdAt().plusSeconds(PREVIEW_TTL_MINUTES * 60).isBefore(Instant.now())) {
            previews.remove(token);
            throw new ApiException(HttpStatus.BAD_REQUEST, "پیش‌نمایش منقضی شده — دوباره بگیرید");
        }
        if (preview.massZeroTripped() && !confirmMassZero) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "این ایمپورت می‌خواهد موجودیِ " + preview.zeroedPercent() + "٪ از محصولاتِ کددار را صفر کند "
                            + "(آستانه: " + preview.massZeroThreshold() + "٪). اگر فایل کامل است، تأییدِ صریح بزنید.");
        }
        if (sendSms && preview.smsCandidates() > preview.smsCap()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "این ایمپورت " + preview.smsCandidates() + " پیامکِ «موجود شد» می‌فرستد که از سقفِ "
                            + preview.smsCap() + " بیشتر است. یا تیکِ پیامک را بردارید یا سقف را در تنظیمات بالا ببرید.");
        }

        Instant now = Instant.now();
        int changed = 0, zeroed = 0, smsSent = 0, skippedStale = 0;

        for (Change c : preview.changes()) {
            Product p = productRepo.findById(c.productId()).orElse(null);
            if (p == null) continue;

            // ⚠️ بینِ پیش‌نمایش و تأیید ممکن است کسی همان عدد را دستی عوض کرده باشد.
            // چیزی که کاربر تأیید کرده «از X به Y» بود؛ اگر X دیگر X نیست، آن تأیید
            // دربارهٔ این ردیف نبوده. رد می‌شود و در نتیجه گزارش می‌شود، نه اینکه
            // بی‌صدا کارِ تازه را بازنویسی کند.
            if (!eq(p.getStockIsfahan(), c.oldIsfahan()) || !eq(p.getStockTehran(), c.oldTehran())) {
                skippedStale++;
                log.warn("ردیفِ «{}» رد شد: موجودی بینِ پیش‌نمایش و اعمال عوض شده", p.getName());
                continue;
            }

            p.setStockIsfahan(c.newIsfahan());
            p.setStockTehran(c.newTehran());
            p.setStock(p.getSellableStock());
            p.setStockImportedAt(now);
            p.setUpdatedAt(now);
            productRepo.save(p);
            changed++;
            if (c.zeroing()) zeroed++;

            // 🔴 عمداً هیچ رکوردِ تک‌محصولی ثبت نمی‌شود. یک ایمپورتِ معمولی ۸۷ تغییر
            // دارد و ۸۷ رکورد، تاریخچهٔ تغییرات را می‌پوشاند — همان جایی که باید
            // دید «چه کسی دستی چه کرد». خلاصهٔ پایینِ همین متد جای آن را می‌گیرد.
            //
            // ⚠️ این فقط دربارهٔ Source.HOLOO است. ویرایشِ دستیِ موجودی در میزِ کار
            // (PricingWorkspaceService) همچنان تک‌تک لاگ می‌شود و نباید دست بخورد.

            // 🔴 پیامک فقط با تیکِ صریح. بدونِ آن، اولین ایمپورت صدها پیامک می‌فرستد.
            if (sendSms && c.wouldSms()) {
                stockNotificationService.notifyBackInStock(p.getId(), p.getName());
                smsSent++;
            }
        }

        activityLog.record(ActivityLog.Action.HOLOO_STOCK_IMPORT, ActivityLog.Source.HOLOO,
                ActivityLogService.ENTITY_PRODUCT, null, String.join("، ", preview.fileNames()),
                "stockImport",
                "خوانده " + preview.rowsRead() + " · ردشده " + preview.rowsSkipped()
                        + " · تطبیق " + preview.rowsMatched(),
                "عوض‌شده " + changed + " · صفرشده " + zeroed + " · پیامک " + smsSent
                        + " · ردشده " + skippedStale);

        previews.remove(token);
        log.info("ایمپورتِ موجودی: {} تغییر، {} صفرشده، {} پیامک، {} ردشده",
                changed, zeroed, smsSent, skippedStale);
        return new ApplyResult(changed, zeroed, smsSent, preview.torobFlips(), skippedStale);
    }

    // ==========================================================
    // کمکی‌ها
    // ==========================================================

    private void remember(Preview p) {
        Instant cutoff = Instant.now().minusSeconds(PREVIEW_TTL_MINUTES * 60);
        previews.values().removeIf(old -> old.createdAt().isBefore(cutoff));
        if (previews.size() >= MAX_LIVE_PREVIEWS) {
            previews.values().stream().min(Comparator.comparing(Preview::createdAt))
                    .ifPresent(oldest -> previews.remove(oldest.token()));
        }
        previews.put(p.token(), p);
    }

    /** برچسبِ انبار → شعبه. نگاشتِ تنظیماتی مقدم است، بعد کلیدواژه. */
    Branch branchOf(String warehouseLabel) {
        if (warehouseLabel == null) return null;
        String label = warehouseLabel.trim();
        if (label.isEmpty()) return null;
        for (Map.Entry<String, Branch> e : BRANCH_KEYWORDS.entrySet()) {
            if (label.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    private static boolean eq(Integer a, Integer b) {
        return n(a) == n(b);
    }

    private static int n(Integer v) {
        return v == null ? 0 : v;
    }

    static String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    static Integer toInt(String s) {
        if (s == null) return null;
        String t = s.trim().replace(",", "");
        // ارقامِ فارسی/عربی در خروجیِ بعضی نرم‌افزارها می‌آیند
        StringBuilder sb = new StringBuilder();
        for (char ch : t.toCharArray()) {
            if (ch >= '۰' && ch <= '۹') sb.append((char) ('0' + ch - '۰'));
            else if (ch >= '٠' && ch <= '٩') sb.append((char) ('0' + ch - '٠'));
            else sb.append(ch);
        }
        try {
            return (int) Math.round(Double.parseDouble(sb.toString()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** فایلِ آپلودشده، بدونِ وابستگی به نوعِ وبِ اسپرینگ (تا تست‌پذیر بماند). */
    public record UploadedFile(String name, byte[] bytes) {}

    private record ParsedRow(String sheet, int row, String code, String name, String warehouse, int quantity) {}

}
