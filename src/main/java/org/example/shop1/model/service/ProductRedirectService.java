package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.ProductRedirect;
import org.example.shop1.model.reposritory.ProductRedirectRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ریدایرکتِ اسلاگِ محصولِ حذف‌شده به محصولِ زنده (۳۰۱ به‌جایِ ۴۰۴).
 *
 * @see org.example.shop1.model.entity.ProductRedirect برایِ چراییِ کالکشن
 */
@Service
public class ProductRedirectService {

    /**
     * سقفِ پرش در زنجیره. زنجیره طبیعی است (A ادغام در B، بعداً B ادغام در C)، ولی
     * عمقِ واقعی همیشه کم می‌ماند؛ این سقف فقط گاردِ دوم در کنارِ تشخیصِ حلقه است.
     */
    private static final int MAX_HOPS = 10;

    private final ProductRedirectRepository redirectRepo;
    private final ProductRepository productRepo;

    public ProductRedirectService(ProductRedirectRepository redirectRepo, ProductRepository productRepo) {
        this.redirectRepo = redirectRepo;
        this.productRepo = productRepo;
    }

    /**
     * محصولِ مقصدِ نهاییِ یک اسلاگ را پیدا می‌کند — با دنبال‌کردنِ زنجیره تا محصولِ زنده.
     * <p>
     * فقط وقتی صدا زده می‌شود که {@code resolveProduct} شکست خورده باشد، پس محصولِ زنده
     * همیشه بر ریدایرکت مقدم است و یک ریدایرکتِ اشتباه نمی‌تواند صفحهٔ سالمی را بدزدد.
     * <p>
     * حلقه ({@code A→B→A}) با مجموعهٔ {@code visited} می‌میرد و {@code empty} برمی‌گرداند،
     * یعنی فراخوان ۴۰۴ واقعی می‌دهد — نه ریدایرکتِ بی‌نهایت.
     *
     * @return محصولِ مقصد، یا خالی اگر ریدایرکتی نبود / زنجیره به محصولِ زنده نرسید / حلقه بود
     */
    public Optional<Product> resolveTarget(String slug) {
        if (slug == null || slug.isEmpty()) return Optional.empty();

        Set<String> visited = new HashSet<>();
        String current = slug;

        for (int hop = 0; hop < MAX_HOPS; hop++) {
            if (!visited.add(current)) return Optional.empty(); // حلقه

            Optional<ProductRedirect> redirect = redirectRepo.findByFromSlug(current);
            if (redirect.isEmpty()) return Optional.empty();

            String target = redirect.get().getToResolver();
            if (target == null || target.isEmpty()) return Optional.empty();

            Optional<Product> live = findLive(target);
            if (live.isPresent()) return live;

            // مقصد خودش زنده نیست: شاید او هم ادغام شده — یک پرشِ دیگر
            current = target;
        }
        return Optional.empty();
    }

    /** resolveِ محصولِ زنده: اول اسلاگ، بعد شناسه — همان ترتیبِ {@code StoreWebController}. */
    private Optional<Product> findLive(String resolver) {
        Optional<Product> found = productRepo.findBySlug(resolver);
        if (found.isEmpty()) found = productRepo.findById(resolver);
        return found;
    }

    // ================= مدیریت (ادمین) =================

    public List<ProductRedirect> findAll() {
        return redirectRepo.findAllByOrderByCreatedAtDesc();
    }

    /**
     * ثبتِ یک ریدایرکتِ تازه.
     * <p>
     * <b>مقصدِ فعلاً حل‌نشدنی رد نمی‌شود</b> — عمداً: گردشِ کارِ امنِ ادغام «اول ریدایرکت،
     * بعد حذف» است تا پنجره‌ای نماند که آدرس ۴۰۴ بدهد؛ و زنجیره هم به‌مرور طبیعی است.
     * به‌جایِ رد کردن، وضعیتِ حل‌شدنِ مقصد به فراخوان گزارش می‌شود
     * ({@link #targetResolves}) تا تایپوی مقصد بی‌صدا نماند.
     */
    public ProductRedirect create(String fromSlug, String toResolver, String note) {
        String from = fromSlug == null ? "" : fromSlug.trim();
        String to = toResolver == null ? "" : toResolver.trim();

        if (from.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "اسلاگِ مبدأ الزامی است");
        if (to.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "مقصدِ ریدایرکت الزامی است");
        if (from.equals(to)) throw new ApiException(HttpStatus.BAD_REQUEST, "مبدأ و مقصد یکی است — ریدایرکت به خودش بی‌معناست");
        if (redirectRepo.existsByFromSlug(from)) {
            throw new ApiException(HttpStatus.CONFLICT, "برای این اسلاگ از قبل ریدایرکت ثبت شده است: " + from);
        }
        // اگر دنبال‌کردنِ زنجیره از مقصد دوباره به مبدأ برسد، این رکورد حلقه می‌سازد.
        if (leadsBackTo(to, from)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "این ریدایرکت حلقه می‌سازد: " + from + " → " + to);
        }

        ProductRedirect redirect = new ProductRedirect();
        redirect.setFromSlug(from);
        redirect.setToResolver(to);
        redirect.setNote(note == null ? null : note.trim());
        return redirectRepo.save(redirect);
    }

    public void delete(String id) {
        if (!redirectRepo.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ریدایرکت یافت نشد");
        }
        redirectRepo.deleteById(id);
    }

    /** آیا مقصد همین حالا به یک محصولِ زنده می‌رسد؟ (برایِ گزارش به ادمین، نه اعتبارسنجیِ سخت) */
    public boolean targetResolves(String toResolver) {
        if (findLive(toResolver).isPresent()) return true;
        return resolveTarget(toResolver).isPresent();
    }

    /** آیا دنبال‌کردنِ زنجیره از {@code start} دوباره به {@code needle} می‌رسد؟ */
    private boolean leadsBackTo(String start, String needle) {
        Set<String> visited = new HashSet<>();
        String current = start;
        for (int hop = 0; hop < MAX_HOPS; hop++) {
            if (current.equals(needle)) return true;
            if (!visited.add(current)) return false; // حلقهٔ از قبل موجود — ولی مبدأ ما درش نیست
            Optional<ProductRedirect> redirect = redirectRepo.findByFromSlug(current);
            if (redirect.isEmpty()) return false;
            String next = redirect.get().getToResolver();
            if (next == null || next.isEmpty()) return false;
            current = next;
        }
        return false;
    }
}
